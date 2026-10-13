package dev.retrobit.penboard.ink

import android.os.Handler
import android.os.Looper
import dev.retrobit.penboard.board.BoardView

/**
 * Menghubungkan papan tulis dengan mesin pengenalan tulisan.
 *
 * Dipakai baik oleh keyboard (IME) maupun papan melayang (overlay) supaya perilakunya sama:
 * - tombol "Tulisan menjadi teks" memicu pengenalan dan langsung mengirim teksnya;
 * - **satu tulisan = satu kata**, dan spasi ditambahkan otomatis untuk kata berikutnya;
 * - bila pengenalan otomatis aktif, tulisan dikenali sendiri sesaat setelah pena diangkat;
 * - setelah dikenali, goresan dibersihkan (masih bisa diurungkan) supaya siap menulis kata berikutnya.
 */
class HandwritingInput(
    private val boardProvider: () -> BoardView?,
    private val languageProvider: () -> String,
    private val configAutoRecognize: () -> Boolean,
    private val configDelayMs: () -> Int,
    private val configLines: () -> Int,
    private val configClearAfter: () -> Boolean,
    private val configAutoSpace: () -> Boolean,
    private val commit: (String) -> Boolean,
    private val notify: (String) -> Unit,
    /** Dipanggil saat mesin pengenalan tidak tersedia, agar pengguna diarahkan ke alternatifnya. */
    private val onUnavailable: (String) -> Unit = { }
) {

    private val handler = Handler(Looper.getMainLooper())
    private val autoTask = Runnable { recognize(clearAfter = true, announce = false) }

    /**
     * Dipanggil setiap kali satu goresan selesai ditulis.
     *
     * Setiap goresan baru mengulang hitungan mundur, sehingga tulisan baru diinput sebagai teks
     * setelah pengguna berhenti menulis selama [configDelayMs] (bawaan 2 detik).
     */
    fun onStrokeCommitted() {
        if (!configAutoRecognize()) return
        if (boardProvider()?.hasContent != true) return
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
            if (announce) onUnavailable(InkRecognition.offlineMessage())
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
                    if (announce) notify("Mengunduh model tulisan " + RecognitionLanguages.label(language) + "\u2026")
                    InkRecognition.downloadModel(language) { state ->
                        if (state == ModelStatus.READY) {
                            run(capture, language, shouldClear, announce)
                        } else {
                            onUnavailable(InkRecognition.downloadFailureHint())
                        }
                    }
                }

                ModelStatus.UNKNOWN -> onUnavailable(InkRecognition.downloadFailureHint())
            }
        }
    }

    private fun run(capture: InkCapture, language: String, clearAfter: Boolean, announce: Boolean) {
        val board = boardProvider() ?: return
        val toClear = if (clearAfter) board.contentStrokes() else emptyList()
        InkRecognition.recognize(capture, language) { text, error ->
            if (RecognitionText.isMeaningful(text)) {
                // Satu tulisan = satu kata: tambahkan spasi untuk kata berikutnya.
                val prepared = RecognitionText.prepare(text, configAutoSpace())
                if (commit(prepared)) {
                    if (announce) notify("Terkirim: " + prepared.trim())
                    if (clearAfter && toClear.isNotEmpty()) board.removeStrokes(toClear)
                } else {
                    notify(NO_FIELD_MESSAGE)
                }
                return@recognize
            }
            when (error) {
                RecognitionError.MODEL_MISSING -> notify(MODEL_MISSING_MESSAGE)
                RecognitionError.EMPTY -> notify(EMPTY_MESSAGE)
                RecognitionError.NOT_SUPPORTED -> onUnavailable(InkRecognition.offlineMessage())
                else -> if (announce) notify(FAILED_MESSAGE)
            }
        }
    }

    companion object {
        const val EMPTY_MESSAGE = "Belum ada tulisan yang bisa dikenali."
        const val MODEL_MISSING_MESSAGE = "Model bahasa belum tersedia. Unduh dulu di aplikasi Penboard."
        const val NO_FIELD_MESSAGE = "Tidak ada kolom teks yang aktif. Sentuh kolom teks lalu coba lagi."
        const val FAILED_MESSAGE = "Tulisan belum bisa dikenali \u2014 coba tulis lebih jelas."
    }
}
