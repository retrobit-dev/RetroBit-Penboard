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

    fun lastErrorMessage(): String? = lastMessage

    fun clearLastErrorMessage() {
        lastMessage = null
    }

    /** ML Kit butuh Google Play services; sebagian perangkat (Huawei tanpa GMS) tidak punya. */
    fun isServicesAvailable(context: Context): Boolean = try {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    } catch (error: Throwable) {
        Log.w(TAG, "Gagal memeriksa Google Play services", error)
        false
    }

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
        remoteModelManager.download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { error ->
                Log.w(TAG, "Gagal mengunduh model", error)
                lastMessage = describe(error)
                onDone(false)
            }
    }

    /**
     * Kenali [strokes]. Model bahasa dipastikan ada lebih dulu; [onDownloading]
     * dipanggil bila model harus diunduh (supaya pengguna tidak menunggu tanpa kabar).
     */
    fun recognize(
        strokes: List<NormalizedStroke>,
        language: String,
        onDownloading: () -> Unit = {},
        onResult: (String?, RecognitionFailure?) -> Unit
    ) {
        if (strokes.isEmpty()) {
            onResult(null, RecognitionFailure.EMPTY)
            return
        }
        val model = buildModel(language)
        if (model == null) {
            onResult(null, RecognitionFailure.MODEL_MISSING)
            return
        }
        remoteModelManager.isModelDownloaded(model)
            .addOnSuccessListener { downloaded ->
                if (downloaded) {
                    runRecognizer(strokes, language, onResult, canDownload = false)
                } else {
                    downloadThenRecognize(model, strokes, language, onDownloading, onResult)
                }
            }
            .addOnFailureListener { error ->
                // Status model tidak terbaca: coba saja dulu, unduhan ditangani bila gagal.
                Log.w(TAG, "Gagal memeriksa model, mencoba mengenali langsung", error)
                runRecognizer(strokes, language, onResult, canDownload = true, onDownloading = onDownloading)
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
        strokes: List<NormalizedStroke>,
        language: String,
        onDownloading: () -> Unit,
        onResult: (String?, RecognitionFailure?) -> Unit
    ) {
        onDownloading()
        remoteModelManager.download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener {
                lastMessage = null
                runRecognizer(strokes, language, onResult, canDownload = false)
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Gagal mengunduh model", error)
                lastMessage = describe(error)
                onResult(null, RecognitionFailure.MODEL_NOT_DOWNLOADED)
            }
    }

    private fun runRecognizer(
        strokes: List<NormalizedStroke>,
        language: String,
        onResult: (String?, RecognitionFailure?) -> Unit,
        canDownload: Boolean,
        onDownloading: () -> Unit = {}
    ) {
        val active = ensureRecognizer(language)
        if (active == null) {
            onResult(null, RecognitionFailure.MODEL_MISSING)
            return
        }
        active.recognize(toInk(strokes))
            .addOnSuccessListener { result ->
                val text = result.candidates.firstOrNull()?.text
                if (text.isNullOrBlank()) {
                    lastMessage = "mesin tidak menemukan kandidat teks"
                    onResult(null, RecognitionFailure.FAILED)
                } else {
                    lastMessage = null
                    onResult(text, null)
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Pengenalan gagal", error)
                lastMessage = describe(error)
                val model = buildModel(language)
                if (canDownload && model != null && looksLikeModelProblem(error)) {
                    // Barangkali modelnya terhapus/dipangkas sistem: unduh sekali lalu ulangi.
                    downloadThenRecognize(model, strokes, language, onDownloading, onResult)
                } else {
                    onResult(null, RecognitionFailure.FAILED)
                }
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
        existing?.close()
        val created = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
        recognizer = created
        recognizerLanguage = language
        return created
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
