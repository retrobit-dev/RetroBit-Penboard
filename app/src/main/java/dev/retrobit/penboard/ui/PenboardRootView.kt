package dev.retrobit.penboard.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * Akar tampilan keyboard: papan tulis + ruang gerak di bawahnya.
 *
 * Tingginya sendiri mengikuti isinya, tetapi saat diukur oleh jendela keyboard
 * ([onMeasure]) tinggi maksimum yang **diizinkan** dicatat di [maxHeightPx]. Angka itu
 * dipakai [BoardPanel] untuk membatasi seberapa tinggi papan boleh diangkat dengan
 * menarik bilah judul — papan tidak pernah ditarik keluar dari area keyboard.
 */
class PenboardRootView(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    /** Tinggi maksimum area keyboard (px); 0 = belum diukur. */
    var maxHeightPx: Int = 0
        private set

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            val size = MeasureSpec.getSize(heightMeasureSpec)
            if (size > 0) maxHeightPx = size
        }
    }
}
