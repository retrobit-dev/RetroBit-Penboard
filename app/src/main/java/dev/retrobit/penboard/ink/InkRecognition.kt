package dev.retrobit.penboard.ink

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea
import dev.retrobit.penboard.util.GmsStatus
import dev.retrobit.penboard.util.LanguageTags

enum class ModelStatus { READY, NOT_DOWNLOADED, MISSING, UNKNOWN }

enum class RecognitionFailure {
    /** Papan kosong. */
    EMPTY,

    /** Bahasa tidak dikenal ML Kit / mesin tidak bisa dipakai. */
    MODEL_MISSING,

    /** Model bahasa belum terunduh dan unduhan gagal (biasanya tanpa internet). */
    MODEL_NOT_DOWNLOADED,

    /** Mesin sudah berjalan tetapi tulisannya tidak terbaca. */
    FAILED
}

/**
 * Mesin pengenalan tulisan tangan Penboard: ML Kit Digital Ink.
 *
 * Berjalan di perangkat dan luring **setelah** model bahasa terunduh (±20 MB per
 * bahasa). Karena itu [recognize] memeriksa dulu apakah model sudah ada:
 *
 *  - sudah ada          → langsung dikenali,
 *  - belum ada, daring  → diunduh dulu (sekali), lalu langsung dikenali,
 *  - belum ada, luring  → dilaporkan [RecognitionFailure.MODEL_NOT_DOWNLOADED]
 *    supaya pengguna diberi tahu dengan jelas (dulu kejadian ini muncul sebagai
 *    "tidak terbaca" sehingga membingungkan).
 *
 * Pesan kesalahan terakhir disimpan di [lastMessage] agar bisa dibaca di halaman
 * pengaturan (kartu pemeriksaan) ketika pengenalan gagal.
 */
object InkRecognition {

    private const val TAG = "PenboardInk"

    private val remoteModelManager: RemoteModelManager = RemoteModelManager.getInstance()
    private var recognizer: DigitalInkRecognizer? = null
    private var recognizerLanguage: String? = null

    /** Pesan kesalahan/kegagalan terakhir dari mesin pengenalan. */
    @Volatile
    private var lastMessage: String? = null

    /** Kandidat teks terakhir dari mesin (untuk laporan diagnostik). */
    @Volatile
    private var lastCandidates: List<String> = emptyList()

    /** Ringkasan tinta terakhir yang dikirim ke mesin. */
    @Volatile
    private var lastInk: InkReport.Ink? = null

    /** Batas jumlah kandidat yang disimpan untuk laporan. */
    private const val MAX_REPORTED_CANDIDATES = 5

    /** Panjang teks sebelum kursor yang dikirim sebagai keterangan (huruf). */
    private const val PRE_CONTEXT_LENGTH = 20

    fun lastErrorMessage(): String? = lastMessage

    fun clearLastErrorMessage() {
        lastMessage = null
    }

    /** Kandidat teks terakhir (boleh kosong). */
    fun lastCandidateTexts(): List<String> = lastCandidates

    /** Ringkasan tinta terakhir yang dikirim ke mesin (boleh null). */
    fun lastInkSummary(): InkReport.Ink? = lastInk

    /** Catat ringkasan tinta yang dikirim ke mesin (dipakai [HandwritingInput]). */
    fun recordInk(ink: InkReport.Ink?) {
        lastInk = ink
    }

    /**
     * Keadaan Google Play services. ML Kit membutuhkannya; perangkat Huawei tanpa
     * GMS tidak bisa memakai mesin bawaan, sedangkan GMS **versi lama** biasanya
     * masih bisa — karena itu keadaannya dibedakan, bukan hanya ada/tidak ada.
     */
    fun servicesStatus(context: Context): GmsStatus = try {
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        GmsStatus.from(code)
    } catch (error: Throwable) {
        Log.w(TAG, "Gagal memeriksa Google Play services", error)
        GmsStatus.UNKNOWN
    }

    /** ML Kit butuh Google Play services yang siap dipakai. */
    fun isServicesAvailable(context: Context): Boolean = servicesStatus(context).canAttempt

    /** Mesin benar-benar siap (GMS ok). Dipakai untuk pesan yang lebih tepat. */
    fun engineReady(context: Context): Boolean = servicesStatus(context) == GmsStatus.READY

    fun modelStatus(language: String, onStatus: (ModelStatus) -> Unit) {
        val model = buildModel(language)
        if (model == null) {
            onStatus(ModelStatus.MISSING)
            return
        }
        remoteModelManager.isModelDownloaded(model)
            .addOnSuccessListener { downloaded ->
                onStatus(if (downloaded) ModelStatus.READY else ModelStatus.NOT_DOWNLOADED)
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Gagal memeriksa model", error)
                onStatus(ModelStatus.UNKNOWN)
            }
    }

    fun downloadModel(language: String, onDone: (Boolean) -> Unit) {
        val model = buildModel(language)
        if (model == null) {
            onDone(false)
            return
        }
        try {
            remoteModelManager.download(model, DownloadConditions.Builder().build())
                .addOnSuccessListener { onDone(true) }
                .addOnFailureListener { error ->
                    Log.w(TAG, "Gagal mengunduh model", error)
                    lastMessage = describe(error)
                    onDone(false)
                }
        } catch (error: Throwable) {
            Log.w(TAG, "Unduhan model tidak bisa dijalankan", error)
            lastMessage = describe(error)
            onDone(false)
        }
    }

    /**
     * Kenali [strokes]. Model bahasa dipastikan ada lebih dulu; [onDownloading]
     * dipanggil bila model harus diunduh (supaya pengguna tidak menunggu tanpa kabar).
     *
     * Keadaan Google Play services diperiksa **pemanggil** (lihat
     * [servicesStatus]) karena fungsi ini tidak memegang `Context`.
     */
    fun recognize(
        variants: List<List<NormalizedStroke>>,
        language: String,
        preContext: String? = null,
        onDownloading: () -> Unit = {},
        onResult: (String?, RecognitionFailure?) -> Unit
    ) {
        lastCandidates = emptyList()
        val ladder = variants.filter { it.isNotEmpty() }
        lastInk = InkReport.inkOf(ladder.firstOrNull().orEmpty())
        if (ladder.isEmpty()) {
            onResult(null, RecognitionFailure.EMPTY)
            return
        }
        val model = buildModel(language)
        if (model == null) {
            lastMessage = "bahasa '$language' tidak dikenal mesin pengenalan"
            onResult(null, RecognitionFailure.MODEL_MISSING)
            return
        }
        val context = preContext?.trim()?.takeLast(PRE_CONTEXT_LENGTH)
        try {
            remoteModelManager.isModelDownloaded(model)
                .addOnSuccessListener { downloaded ->
                    if (downloaded) {
                        attempt(ladder, 0, language, context, onDownloading, allowDownload = false, onResult)
                    } else {
                        downloadThenRecognize(model, ladder, language, context, onDownloading, onResult)
                    }
                }
                .addOnFailureListener { error ->
                    // Status model tidak terbaca: coba saja dulu, unduhan ditangani bila gagal.
                    Log.w(TAG, "Gagal memeriksa model, mencoba mengenali langsung", error)
                    attempt(ladder, 0, language, context, onDownloading, allowDownload = true, onResult)
                }
        } catch (error: Throwable) {
            // Tanpa Google Play services pemanggilan ini bisa langsung gagal.
            Log.w(TAG, "Pemeriksaan model tidak bisa dijalankan", error)
            lastMessage = describe(error)
            onResult(null, RecognitionFailure.MODEL_NOT_DOWNLOADED)
        }
    }

    fun close() {
        recognizer?.close()
        recognizer = null
        recognizerLanguage = null
    }

    // ------------------------------------------------------------------ internal

    /** Unduh model dulu (sekali), lalu kenali tulisannya. */
    private fun downloadThenRecognize(
        model: DigitalInkRecognitionModel,
        variants: List<List<NormalizedStroke>>,
        language: String,
        preContext: String?,
        onDownloading: () -> Unit,
        onResult: (String?, RecognitionFailure?) -> Unit
    ) {
        onDownloading()
        remoteModelManager.download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener {
                lastMessage = null
                attempt(variants, 0, language, preContext, onDownloading, allowDownload = false, onResult)
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Gagal mengunduh model", error)
                lastMessage = describe(error)
                onResult(null, RecognitionFailure.MODEL_NOT_DOWNLOADED)
            }
    }

    /**
     * Satu percobaan pengenalan memakai versi tinta ke-[index]. Bila mesin tidak
     * menemukan kandidat teks sama sekali dan masih ada versi berikutnya, tinta
     * yang sama dicoba lagi dengan normalisasi berbeda (ukuran tulisan berbeda).
     */
    private fun attempt(
        variants: List<List<NormalizedStroke>>,
        index: Int,
        language: String,
        preContext: String?,
        onDownloading: () -> Unit,
        allowDownload: Boolean,
        onResult: (String?, RecognitionFailure?) -> Unit
    ) {
        val strokes = variants.getOrNull(index)
        if (strokes == null) {
            lastMessage = "mesin tidak menemukan kandidat teks setelah ${variants.size} percobaan"
            onResult(null, RecognitionFailure.FAILED)
            return
        }
        val active = ensureRecognizer(language)
        if (active == null) {
            lastMessage = "mesin pengenalan untuk bahasa '$language' tidak bisa dibuat"
            onResult(null, RecognitionFailure.MODEL_MISSING)
            return
        }
        val ink = toInk(strokes)
        val context = buildContext(strokes, preContext)
        val task = if (context != null) active.recognize(ink, context) else active.recognize(ink)
        task
            .addOnSuccessListener { result ->
                val candidates = result.candidates
                    .mapNotNull { candidate -> candidate.text?.takeIf { it.isNotBlank() } }
                lastCandidates = candidates.take(MAX_REPORTED_CANDIDATES)
                val text = lastCandidates.firstOrNull()
                if (text != null) {
                    lastMessage = null
                    onResult(text, null)
                    return@addOnSuccessListener
                }
                if (index + 1 < variants.size) {
                    lastMessage = "percobaan ${index + 1} tanpa teks; mencoba normalisasi berikutnya"
                    attempt(variants, index + 1, language, preContext, onDownloading, allowDownload = false, onResult)
                } else {
                    lastMessage = "mesin tidak menemukan kandidat teks setelah ${variants.size} percobaan"
                    onResult(null, RecognitionFailure.FAILED)
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Pengenalan gagal", error)
                lastMessage = describe(error)
                val model = buildModel(language)
                if (allowDownload && model != null && looksLikeModelProblem(error)) {
                    // Barangkali modelnya terhapus/dipangkas sistem: unduh sekali lalu ulangi.
                    downloadThenRecognize(model, variants, language, preContext, onDownloading, onResult)
                } else {
                    onResult(null, RecognitionFailure.FAILED)
                }
            }
    }

    /**
     * Keterangan tambahan untuk mesin: ukuran bidang tulisan (membantu membedakan
     * huruf kecil/besar) dan beberapa huruf sebelum kursor (membantu memilih kata).
     */
    private fun buildContext(strokes: List<NormalizedStroke>, preContext: String?): RecognitionContext? {
        val ink = InkReport.inkOf(strokes) ?: return null
        return try {
            val builder = RecognitionContext.builder()
                .setWritingArea(
                    WritingArea(
                        ink.width.toFloat().coerceAtLeast(1f),
                        ink.height.toFloat().coerceAtLeast(1f)
                    )
                )
            if (!preContext.isNullOrBlank()) builder.setPreContext(preContext)
            builder.build()
        } catch (error: Throwable) {
            Log.w(TAG, "Gagal menyiapkan keterangan tulisan", error)
            null
        }
    }

    private fun buildModel(language: String): DigitalInkRecognitionModel? {
        val identifier = languageIdentifier(language) ?: return null
        return DigitalInkRecognitionModel.builder(identifier).build()
    }

    private fun languageIdentifier(language: String): DigitalInkRecognitionModelIdentifier? {
        for (candidate in LanguageTags.candidates(language)) {
            try {
                val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(candidate)
                if (identifier != null) return identifier
            } catch (error: Throwable) {
                Log.d(TAG, "Bahasa '$candidate' tidak dikenal ML Kit", error)
            }
        }
        return null
    }

    private fun ensureRecognizer(language: String): DigitalInkRecognizer? {
        val existing = recognizer
        if (existing != null && recognizerLanguage.equals(language, ignoreCase = true)) return existing
        val model = buildModel(language) ?: return null
        return try {
            existing?.close()
            val created = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
            recognizer = created
            recognizerLanguage = language
            created
        } catch (error: Throwable) {
            // Mis. perangkat tanpa Google Play services: jangan sampai aplikasi berhenti.
            Log.w(TAG, "Gagal membuat mesin pengenalan", error)
            lastMessage = "mesin pengenalan tidak bisa dibuat: " + (error.message ?: error.javaClass.simpleName)
            null
        }
    }

    /** Apakah kegagalan ini soal model (belum ada / unduhan), bukan soal tulisannya. */
    private fun looksLikeModelProblem(error: Throwable): Boolean {
        val message = error.message.orEmpty().lowercase()
        val code = (error as? MlKitException)?.errorCode
        return message.contains("model") ||
            message.contains("download") ||
            code == MlKitException.NOT_FOUND ||
            code == MlKitException.UNAVAILABLE
    }

    /** Ringkasan kesalahan untuk ditampilkan ke pengguna. */
    private fun describe(error: Throwable): String {
        val code = (error as? MlKitException)?.errorCode
        val message = error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
        return if (code != null && code != 0) "[$code] $message" else message
    }

    private fun toInk(strokes: List<NormalizedStroke>): Ink {
        val inkBuilder = Ink.builder()
        var time = 0L
        for (stroke in strokes) {
            val strokeBuilder = Ink.Stroke.builder()
            for (point in stroke.points) {
                strokeBuilder.addPoint(Ink.Point.create(point.x, point.y, time))
                time += 16
            }
            inkBuilder.addStroke(strokeBuilder.build())
        }
        return inkBuilder.build()
    }
}
