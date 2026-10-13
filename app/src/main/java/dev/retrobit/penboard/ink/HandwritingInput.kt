package dev.retrobit.penboard.ink

import android.content.Context
import android.os.Handler
import android.os.Looper
import dev.retrobit.penboard.R
import dev.retrobit.penboard.board.BoardView
import dev.retrobit.penboard.data.PenboardConfig

/**
 * Logika pengenalan tulisan pada keyboard Penboard:
 *
 * - **Input otomatis**: bila tidak ada goresan baru selama
 *   [PenboardConfig.autoRecognizeDelayMs] (bawaan 2 detik), tulisan langsung
 *   dikenali dan diinput sebagai teks. Setiap goresan baru mengulang hitungan.
 *   Selama menunggu, papan menampilkan garis hitung mundur; setelah teks terkirim,
 *   papan menampilkan kilasan hasilnya.
 * - **Manual**: tombol "Teks" atau tombol samping pena.
 * - Tulisan dianggap satu kata, lalu satu spasi ditambahkan untuk kata berikutnya.
 * - Bila mesin pengenalan bawaan tidak tersedia (mis. perangkat Huawei tanpa
 *   Google Play services), host diberi tahu lewat [onUnavailable] supaya bisa
 *   mengalihkan pengguna ke keyboard tulisan tangan perangkat.
 */
class HandwritingInput(
    private val context: Context,
    private val board: BoardView,
    private val config: () -> PenboardConfig,
    /** Kirim teks ke kolom isian; kembalikan true bila teks benar-benar terkirim. */
    private val commit: (String) -> Boolean,
    private val notify: (String) -> Unit,
    /** Teks yang sudah ada sebelum kursor (membantu mesin memilih kata). */
    private val preContext: () -> String? = { null },
    /** Mesin pengenalan bawaan tidak tersedia (mis. tanpa Google Play services). */
    private val onUnavailable: (String) -> Unit = {}
) {

    private val handler = Handler(Looper.getMainLooper())
    private val autoTask = Runnable { recognize(clearAfter = null, announce = false) }

    @Volatile
    private var busy = false

    /** Pengalihan ke keyboard bawaan hanya ditawarkan sekali per sesi. */
    private var fallbackOffered = false

    /**
     * Pena mulai menulis: batalkan jadwal "jadi teks" supaya tulisan yang sedang
     * ditulis tidak terpotong dan papan tidak dikosongkan di tengah goresan.
     */
    fun onStrokeStarted() {
        handler.removeCallbacks(autoTask)
        board.cancelPending()
    }

    /** Dipanggil setiap goresan selesai: mulai hitungan jeda input otomatis. */
    fun onStrokeCommitted() {
        scheduleAuto()
    }

    /** Jadwalkan pengenalan otomatis sesuai jeda pengaturan. */
    fun scheduleAuto() {
        handler.removeCallbacks(autoTask)
        val current = config()
        if (!current.autoRecognize) {
            board.cancelPending()
            return
        }
        board.beginPending(current.autoRecognizeDelayMs)
        handler.postDelayed(autoTask, current.autoRecognizeDelayMs.toLong())
    }

    fun cancelAuto() {
        handler.removeCallbacks(autoTask)
        board.cancelPending()
    }

    fun recognize(clearAfter: Boolean? = null, announce: Boolean = true) {
        handler.removeCallbacks(autoTask)
        board.cancelPending()
        if (busy) return
        if (board.isWriting) {
            // Pena masih menempel di papan: tunggu diangkat dulu, jangan potong tulisan.
            handler.postDelayed(autoTask, RECHECK_MS)
            return
        }
        val current = config()
        if (!board.hasContent) {
            if (announce) notify(context.getString(R.string.msg_board_empty))
            return
        }
        if (!InkRecognition.isServicesAvailable(context)) {
            // Mesin bawaan tidak bisa dipakai (biasanya Google Play services).
            // Sebutkan keadaannya, bukan sekadar "tidak terbaca".
            if (announce || !fallbackOffered) {
                fallbackOffered = true
                board.showFlash(context.getString(R.string.flash_no_engine))
                val status = InkRecognition.servicesStatus(context)
                onUnavailable(context.getString(R.string.msg_engine_missing_detail, status.summary))
            }
            return
        }
        val raw = board.contentStrokes()
        // Beberapa versi tinta (ukuran berbeda) dicoba berurutan oleh mesin bila
        // percobaan sebelumnya tidak menghasilkan teks sama sekali.
        val variants = InkNormalizer.variants(raw, board.lineGapPx)
        if (variants.isEmpty()) {
            if (announce) notify(context.getString(R.string.msg_board_empty))
            return
        }
        busy = true
        val language = current.recognitionLanguage
        val before = try {
            preContext()
        } catch (error: Throwable) {
            null
        }
        InkRecognition.recognize(
            variants = variants,
            language = language,
            preContext = before,
            // Model bahasa diunduh otomatis sekali; pengguna diberi tahu supaya tidak menunggu bingung.
            onDownloading = { notify(context.getString(R.string.msg_model_downloading_auto)) }
        ) { text, failure ->
            busy = false
            handleResult(text, failure, current, clearAfter, announce, language)
        }
    }

    fun release() {
        handler.removeCallbacks(autoTask)
        board.cancelPending()
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
            if (delivered) {
                // Papan memberi tahu apa yang baru saja masuk ke kolom isian.
                board.showFlash("\u2713 " + prepared.trim())
                if (clearAfter ?: current.clearAfterRecognize) board.clear()
            }
            return
        }
        when (failure) {
            RecognitionFailure.MODEL_MISSING -> {
                if (!InkRecognition.engineReady(context)) {
                    // Mesinnya yang tidak ada — bukan soal tulisan atau model.
                    board.showFlash(context.getString(R.string.flash_no_engine))
                    val status = InkRecognition.servicesStatus(context)
                    notify(context.getString(R.string.msg_engine_missing_detail, status.summary))
                    onUnavailable(context.getString(R.string.msg_engine_missing_detail, status.summary))
                } else {
                    // Mesin siap, tetapi bahasa ini tidak dikenal mesin.
                    board.showFlash(context.getString(R.string.flash_no_model))
                    notify(context.getString(R.string.msg_language_unsupported, language))
                }
            }
            RecognitionFailure.EMPTY -> if (announce) notify(context.getString(R.string.msg_board_empty))
            RecognitionFailure.MODEL_NOT_DOWNLOADED -> {
                // Bukan salah tulisannya: model bahasa memang belum ada di perangkat.
                // Beri tahu jelas, lalu tawarkan keyboard tulisan tangan perangkat.
                board.showFlash(context.getString(R.string.flash_no_model))
                notify(context.getString(R.string.msg_model_failed))
                onUnavailable(context.getString(R.string.msg_model_offline))
            }
            else -> {
                // Papan memberi tahu bahwa tulisannya belum terbaca, walau tanpa suara.
                board.showFlash(context.getString(R.string.flash_unreadable))
                if (announce) notify(notRecognizedMessage())
            }
        }
    }

    /**
     * Pesan gagal baca. Kalau mesin sebenarnya menjawab (ada kandidat) tetapi
     * jawabannya dianggap bukan kata, kandidat itu ikut ditampilkan supaya jelas;
     * kalau tidak ada jawaban sama sekali, alasan teknisnya yang ditampilkan.
     */
    private fun notRecognizedMessage(): String {
        val candidates = InkRecognition.lastCandidateTexts()
        if (candidates.isNotEmpty()) {
            return context.getString(
                R.string.msg_not_recognized_candidates,
                candidates.take(3).joinToString(", ")
            )
        }
        val reason = InkRecognition.lastErrorMessage()
        return if (reason.isNullOrBlank()) {
            context.getString(R.string.msg_not_recognized)
        } else {
            context.getString(R.string.msg_not_recognized_reason, reason.take(90))
        }
    }

    private companion object {
        /** Selang pemeriksaan ulang saat pena masih menempel di papan (ms). */
        const val RECHECK_MS = 400L
    }
}
