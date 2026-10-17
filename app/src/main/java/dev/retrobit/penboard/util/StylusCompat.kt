package dev.retrobit.penboard.util

import android.os.Build
import android.view.View

/**
 * Tabrakan dengan tulisan tangan bawaan perangkat.
 *
 * Sejak Android 14, kolom teks bisa otomatis memunculkan bantalan tulisan tangan
 * stylus (pada perangkat Huawei/Honor memakai mesin Celia/Huawei Handwriting).
 * Pada kolom teks milik aplikasi ini fitur tersebut dimatikan supaya yang muncul
 * selalu papan Penboard, bukan panel bawaan.
 */
object StylusCompat {

    fun disableSystemHandwriting(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                view.setAutoHandwritingEnabled(false)
            } catch (error: Throwable) {
                // Sebagian perangkat (HarmonyOS) tidak menyediakan API ini — abaikan.
            }
        }
    }
}
