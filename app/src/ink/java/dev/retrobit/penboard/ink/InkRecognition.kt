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

/**
 * Varian "ink": pengenalan tulisan tangan memakai ML Kit Digital Ink.
 *
 * Catatan penting:
 * - Setiap bahasa perlu **unduhan model sekali** (~20 MB) lewat `downloadModel()`.
 * - Setelah model ada, pengenalan berjalan **di perangkat** (tidak ada tulisan yang dikirim keluar).
 */
object InkRecognition {

    const val isSupported: Boolean = true

    const val engineName: String = "ML Kit Digital Ink"

    private const val TAG = "PenboardInk"

    private val remoteModelManager = RemoteModelManager.getInstance()

    private var recognizer: DigitalInkRecognizer? = null
    private var recognizerLanguage: String? = null

    /** Apakah perangkat punya Google Play services (syarat ML Kit). */
    fun isServicesAvailable(context: Context): Boolean = try {
        GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context.applicationContext) == ConnectionResult.SUCCESS
    } catch (error: Throwable) {
        Log.w(TAG, "Tidak bisa memeriksa Google Play services", error)
        false
    }

    fun downloadFailureHint(): String =
        "Gagal menyiapkan model tulisan. Fitur ini memerlukan Google Play services; " +
            "pada perangkat tanpa layanan Google (mis. sebagian Huawei), pengenalan tulisan tidak tersedia." 

    fun modelStatus(language: String, callback: (ModelStatus) -> Unit) {
        val model = buildModel(language)
        if (model == null) {
            callback(ModelStatus.UNKNOWN)
            return
        }
        remoteModelManager.isModelDownloaded(model)
            .addOnSuccessListener { downloaded -> callback(if (downloaded == true) ModelStatus.READY else ModelStatus.MISSING) }
            .addOnFailureListener { error ->
                Log.w(TAG, "Gagal memeriksa model", error)
                callback(ModelStatus.UNKNOWN)
            }
    }

    fun downloadModel(language: String, callback: (ModelStatus) -> Unit) {
        val model = buildModel(language)
        if (model == null) {
            callback(ModelStatus.UNKNOWN)
            return
        }
        remoteModelManager.download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener { callback(ModelStatus.READY) }
            .addOnFailureListener { error ->
                Log.w(TAG, "Gagal mengunduh model", error)
                callback(ModelStatus.UNKNOWN)
            }
    }

    fun recognize(capture: InkCapture, language: String, callback: RecognitionCallback) {
        if (capture.isEmpty) {
            callback.onResult(null, RecognitionError.EMPTY)
            return
        }
        val active = ensureRecognizer(language)
        if (active == null) {
            callback.onResult(null, RecognitionError.MODEL_MISSING)
            return
        }
        val ink = toInk(capture)
        active.recognize(ink)
            .addOnSuccessListener { result ->
                val text = result.candidates.firstOrNull()?.text
                if (text.isNullOrBlank()) {
                    callback.onResult(null, RecognitionError.FAILED)
                } else {
                    callback.onResult(text, null)
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Pengenalan gagal", error)
                callback.onResult(null, RecognitionError.FAILED)
            }
    }

    fun close() {
        recognizer?.close()
        recognizer = null
        recognizerLanguage = null
    }

    // ------------------------------------------------------------------ internal

    private fun buildModel(language: String): DigitalInkRecognitionModel? {
        val identifier = languageIdentifier(language) ?: return null
        return DigitalInkRecognitionModel.builder(identifier).build()
    }

    private fun languageIdentifier(language: String): DigitalInkRecognitionModelIdentifier? {
        for (candidate in candidates(language)) {
            try {
                val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(candidate)
                if (identifier != null) return identifier
            } catch (error: MlKitException) {
                Log.d(TAG, "Bahasa '$candidate' tidak dikenal ML Kit")
            }
        }
        return null
    }

    /** Coba tag lengkap dulu (mis. id-ID), lalu bahasa saja (mis. id). */
    private fun candidates(language: String): List<String> {
        val clean = language.trim()
        if (clean.isEmpty()) return listOf("en-US")
        val short = clean.substringBefore('-').substringBefore('_')
        return if (short.equals(clean, ignoreCase = true)) listOf(clean) else listOf(clean, short)
    }

    private fun ensureRecognizer(language: String): DigitalInkRecognizer? {
        if (recognizer != null && recognizerLanguage.equals(language, ignoreCase = true)) {
            return recognizer
        }
        val model = buildModel(language) ?: return null
        recognizer?.close()
        val created = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
        recognizer = created
        recognizerLanguage = language
        return created
    }

    private fun toInk(capture: InkCapture): Ink {
        val inkBuilder = Ink.builder()
        val now = System.currentTimeMillis()
        var offset = 0L
        for (line in capture.lines) {
            val strokeBuilder = Ink.Stroke.builder()
            for (point in line.points) {
                strokeBuilder.addPoint(Ink.Point.create(point.x, point.y, now + offset))
                offset += 16
            }
            inkBuilder.addStroke(strokeBuilder.build())
        }
        return inkBuilder.build()
    }
}
