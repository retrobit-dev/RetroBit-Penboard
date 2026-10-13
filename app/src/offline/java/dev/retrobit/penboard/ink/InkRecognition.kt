package dev.retrobit.penboard.ink

import android.content.Context

/**
 * Varian "offline": tidak menyertakan mesin pengenalan tulisan sehingga APK tetap
 * ringan dan aplikasi tidak meminta izin internet sama sekali.
 */
object InkRecognition {

    const val isSupported: Boolean = false

    const val engineName: String = "tidak disertakan"

    fun isServicesAvailable(context: Context): Boolean = false

    fun offlineMessage(): String =
        "Pengenalan tulisan hanya ada pada varian Penboard OCR (APK bertanda \u201cink\u201d)."


    fun downloadFailureHint(): String =
        "Varian ini tanpa mesin pengenalan tulisan. Pasang APK Penboard OCR (bertanda \u201cink\u201d) untuk memakai fitur ini."

    fun modelStatus(language: String, callback: (ModelStatus) -> Unit) {
        callback(ModelStatus.UNKNOWN)
    }

    fun downloadModel(language: String, callback: (ModelStatus) -> Unit) {
        callback(ModelStatus.UNKNOWN)
    }

    fun recognize(capture: InkCapture, language: String, callback: RecognitionCallback) {
        callback.onResult(null, RecognitionError.NOT_SUPPORTED)
    }

    fun close() = Unit
}
