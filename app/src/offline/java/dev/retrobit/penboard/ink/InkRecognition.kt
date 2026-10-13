package dev.retrobit.penboard.ink

/**
 * Varian "offline": tidak menyertakan mesin pengenalan tulisan sehingga APK tetap
 * ringan dan aplikasi tidak meminta izin internet sama sekali.
 */
object InkRecognition {

    const val isSupported: Boolean = false

    const val engineName: String = "tidak disertakan"

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
