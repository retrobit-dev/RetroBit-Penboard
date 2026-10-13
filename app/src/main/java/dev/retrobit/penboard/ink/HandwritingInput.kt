package dev.retrobit.penboard.ink

import android.os.Handler
import android.os.Looper
import dev.retrobit.penboard.board.BoardView

/**
 * Menghubungkan papan tulis dengan mesin pengenalan tulisan.
 *
 * Dipakai baik oleh keyboard (IME) maupun papan melayang (overlay) supaya perilakunya sama:
 * - tombol "Tulisan → teks" memicu pengenalan dan langsung mengirim teksnya;
 * - bila pengenalan otomatis aktif, tulisan dikenali sendiri sesaat setelah pena diangkat;
 * - setelah dikenali, goresan yang sudah jadi teks dibersihkan (masih bisa diurungkan).
 */
class HandwritingInput(
    private val boardProvider: () -> BoardView?,
    private val languageProvider: () -> String,
    private val configAutoRecognize: () -> Boolean,
    private val configDelayMs: () -> Int,
    private val configLines: () -> Int,
    private val configClearAfter: () -> Boolean,
    private val commit: (String) -> Boolean,
    private val notify: (String) -> Unit
) {

    private val handler = Handler(Looper.getMainLooper())
    private val autoTask = Runnable { recognize(clearAfter = true, announce = false) }

    /** Dipanggil setiap kali satu goresan selesai ditulis. */
    fun onStrokeCommitted() {
        if (!configAutoRecognize()) return
        handler.removeCallbacks(autoTask)
        handler.postDelayed(autoTask, configDelayMs().toLong())
    }

    fun cancelPending() {
        handler.removeCallbacks(autoTask)
    }

    /**
     * Kenali tulisan pada beberapa baris terakhir.
     * @param clearAfter null = ikuti pengaturan profil, true/false = paksa.
     */
    fun recognize(clearAfter: Boolean? = null, announce: Boolean = true) {
        handler.removeCallbacks(autoTask)
        val board = boardProvider() ?: return
        if (!InkRecognition.isSupported) {
            if (announce) notify(OFFLINE_MESSAGE)
            return
        }
        if (!board.hasContent) {
            if (announce) notify(EMPTY_MESSAGE)
            return
        }
        val capture = InkExtractor.capture(
            strokes = board.contentStrokes(),
            gapPx = board.lineGapPx,
            lines = configLines(),
            width = board.width,
            height = board.height
        )
        if (capture.isEmpty) {
            if (announce) notify(EMPTY_MESSAGE)
            return
        }
        val language = RecognitionLanguages.normalize(languageProvider())
        val shouldClear = clearAfter ?: configClearAfter()
        InkRecognition.modelStatus(language) { status ->
            when (status) {
                ModelStatus.READY -> run(capture, language, shouldClear, announce)
                ModelStatus.MISSING -> {
                    notify("Mengunduh model tulisan " + RecognitionLanguages.label(language) + "…")
                    InkRecognition.downloadModel(language) { state ->
                        if (state == ModelStatus.READY) {
                            run(capture, language, shouldClear, announce)
                        } else {
                            notify(InkRecognition.downloadFailureHint())
                        }
                    }
                }

                ModelStatus.UNKNOWN -> notify(InkRecognition.downloadFailureHint())
            }
        }
    }

    private fun run(capture: InkCapture, language: String, clearAfter: Boolean, announce: Boolean) {
        val board = boardProvider() ?: return
        val toClear = if (clearAfter) board.contentStrokes() else emptyList()
        InkRecognition.recognize(capture, language) { text, error ->
            when {
                !text.isNullOrBlank() -> {
                    val clean = text.trim()
                    if (commit("$clean ")) {
                        if (announce) notify("Terkirim: $clean")
                        if (clearAfter && toClear.isNotEmpty()) board.removeStrokes(toClear)
                    } else {
                        notify(NO_FIELD_MESSAGE)
                    }
                }

                error == RecognitionError.MODEL_MISSING -> notify(MODEL_MISSING_MESSAGE)
                error == RecognitionError.EMPTY -> notify(EMPTY_MESSAGE)
                error == RecognitionError.NOT_SUPPORTED -> notify(OFFLINE_MESSAGE)
                else -> if (announce) notify(FAILED_MESSAGE)
            }
        }
    }

    companion object {
        const val OFFLINE_MESSAGE = "Pengenalan tulisan hanya ada pada varian Penboard OCR (APK bertanda \u201cink\u201d)."
        const val EMPTY_MESSAGE = "Belum ada tulisan yang bisa dikenali."
        const val MODEL_MISSING_MESSAGE = "Model bahasa belum tersedia. Unduh dulu di aplikasi Penboard."
        const val NO_FIELD_MESSAGE = "Tidak ada kolom teks yang aktif. Sentuh kolom teks lalu coba lagi."
        const val FAILED_MESSAGE = "Tulisan belum bisa dikenali \u2014 coba tulis lebih jelas."
    }
}
