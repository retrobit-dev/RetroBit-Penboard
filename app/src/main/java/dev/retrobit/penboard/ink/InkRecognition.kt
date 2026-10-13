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

enum class ModelStatus { READY, NOT_DOWNLOADED, MISSING, UNKNOWN }

enum class RecognitionFailure { EMPTY, MODEL_MISSING, FAILED }

/**
 * Mesin pengenalan tulisan tangan Penboard: ML Kit Digital Ink.
 *
 * Berjalan di perangkat dan luring setelah model bahasa terunduh (±20 MB per bahasa).
 * Model diunduh otomatis saat pertama kali dipakai.
 */
object InkRecognition {

    private const val TAG = "PenboardInk"

    private val remoteModelManager: RemoteModelManager = RemoteModelManager.getInstance()
    private var recognizer: DigitalInkRecognizer? = null
    private var recognizerLanguage: String? = null

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
                onDone(false)
            }
    }

    fun recognize(
        strokes: List<NormalizedStroke>,
        language: String,
        onResult: (String?, RecognitionFailure?) -> Unit
    ) {
        if (strokes.isEmpty()) {
            onResult(null, RecognitionFailure.EMPTY)
            return
        }
        val active = ensureRecognizer(language)
        if (active == null) {
            onResult(null, RecognitionFailure.MODEL_MISSING)
            return
        }
        active.recognize(toInk(strokes))
            .addOnSuccessListener { result ->
                val text = result.candidates.firstOrNull()?.text
                if (text.isNullOrBlank()) {
                    onResult(null, RecognitionFailure.FAILED)
                } else {
                    onResult(text, null)
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Pengenalan gagal", error)
                onResult(null, RecognitionFailure.FAILED)
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
        for (candidate in tagCandidates(language)) {
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
    private fun tagCandidates(language: String): List<String> {
        val clean = language.trim()
        if (clean.isEmpty()) return listOf("en-US")
        val short = clean.substringBefore('-').substringBefore('_')
        return if (short.equals(clean, ignoreCase = true)) listOf(clean) else listOf(clean, short)
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
