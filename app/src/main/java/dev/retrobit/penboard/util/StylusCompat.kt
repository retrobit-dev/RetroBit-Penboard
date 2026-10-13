package dev.retrobit.penboard.util

import android.os.Build
import android.view.View

/**
 * Penyesuaian stylus untuk perangkat dengan tulisan tangan bawaan (mis. Huawei/HarmonyOS,
 * atau Android 14+ yang mendukung mode tulisan tangan sistem).
 */
object StylusCompat {

    /**
     * Matikan inisiasi tulisan tangan otomatis oleh sistem pada kolom teks milik Penboard,
     * supaya semua tulisan stylus masuk lewat papan Penboard dan tidak bertabrakan dengan
     * panel tulisan tangan bawaan perangkat.
     */
    fun disableSystemHandwriting(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        runCatching { view.setAutoHandwritingEnabled(false) }
    }
}
