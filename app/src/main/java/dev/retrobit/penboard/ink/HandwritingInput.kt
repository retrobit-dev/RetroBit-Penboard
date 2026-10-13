package dev.retrobit.penboard.ink

import android.content.Context
import android.os.Handler
import android.os.Looper
import dev.retrobit.penboard.R
import dev.retrobit.penboard.board.BoardView
import dev.retrobit.penboard.data.PenboardConfig

/**
 * Logika bersama keyboard dan papan melayang:
 *
 * - **Input otomatis**: bila tidak ada goresan baru selama
 *   [PenboardConfig.autoRecognizeDelayMs] (bawaan 2 detik), tulisan langsung
 *   dikenali dan diinput sebagai teks. Setiap goresan baru mengulang hitungan.
 * - **Manual**: tombol "Teks" atau tombol samping pena.
 * - Tulisan dianggap satu kata, lalu satu spasi ditambahkan untuk kata berikutnya.
 */
class HandwritingInput(
    private val context: Context,
    private val board: BoardView,
    private val config: () -> PenboardConfig,
    /** Kirim teks ke kolom isian; kembalikan true bila teks benar-benar terkirim. */
    private val commit: (String) -> Boolean,
    private val notify: (String) -> Unit,
    /** Mesin pengenalan bawaan tidak tersedia (mis. tanpa Google Play services). */
    private val onUnavailable: (String) -> Unit = {}
) {

    private val handler = Handler(Looper.getMainLooper())
    private val autoTask = Runnable { recognize(clearAfter = null, announce = false) }

    @Volatile
    private var busy = false

    /** Dipanggil setiap goresan selesai: mulai hitungan jeda input otomatis. */
    fun onStrokeCommitted() {
        scheduleAuto()
    }

    /** Jadwalkan pengenalan otomatis sesuai jeda pengaturan. */
    fun scheduleAuto() {
        handler.removeCallbacks(autoTask)
        val current = config()
        if (!current.autoRecognize) return
        handler.postDelayed(autoTask, current.autoRecognizeDelayMs.toLong())
    }

    fun cancelAuto() {
        handler.removeCallbacks(autoTask)
    }

    fun recognize(clearAfter: Boolean? = null, announce: Boolean = true) {
        handler.removeCallbacks(autoTask)
        if (busy) return
        val current = config()
        if (!board.hasContent) {
            if (announce) notify(context.getString(R.string.msg_board_empty))
            return
        }
        if (!InkRecognition.isServicesAvailable(context)) {
            if (announce) onUnavailable(context.getString(R.string.msg_engine_missing))
            return
        }
        val strokes = InkNormalizer.normalize(board.contentStrokes(), board.lineGapPx)
        if (strokes.isEmpty()) {
            if (announce) notify(context.getString(R.string.msg_board_empty))
            return
        }
        busy = true
        val language = current.recognitionLanguage
        InkRecognition.recognize(strokes, language) { text, failure ->
            busy = false
            handleResult(text, failure, current, clearAfter, announce, language)
        }
    }

    fun release() {
        handler.removeCallbacks(autoTask)
    }

    // ------------------------------------------------------------------ internal

    private fun handleResult(
        text: String?,
        failure: RecognitionFailure?,
        current: PenboardConfig,
        clearAfter: Boolean?,
        announce: Boolean,
        language: String
    ) {
        if (RecognitionText.isMeaningful(text)) {
            val prepared = RecognitionText.prepare(text, current.autoSpace)
            val delivered = commit(prepared)
            if (delivered && (clearAfter ?: current.clearAfterRecognize)) board.clear()
            return
        }
        when (failure) {
            RecognitionFailure.MODEL_MISSING -> {
                onUnavailable(context.getString(R.string.msg_model_missing))
                InkRecognition.downloadModel(language) { ok ->
                    notify(context.getString(if (ok) R.string.msg_model_ready else R.string.msg_model_failed))
                }
            }
            RecognitionFailure.EMPTY -> if (announce) notify(context.getString(R.string.msg_board_empty))
            else -> if (announce) notify(context.getString(R.string.msg_not_recognized))
        }
    }
}
