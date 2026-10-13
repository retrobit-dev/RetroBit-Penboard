package dev.retrobit.penboard.util

/**
 * Pencocokan id layanan aksesibilitas (tanpa Android).
 *
 * Android menuliskan id layanan dalam beberapa bentuk, mis.
 * `dev.retrobit.penboard/.overlay.PenboardTypingService` atau
 * `dev.retrobit.penboard/dev.retrobit.penboard.overlay.PenboardTypingService`.
 * Keduanya harus dikenali supaya status di halaman pengaturan benar.
 */
object AccessibilityIds {

    fun matches(id: String?, packageName: String, className: String): Boolean {
        if (id.isNullOrEmpty()) return false
        // Samakan dulu bentuknya: 'pkg/.Kelas', 'pkg/pkg.Kelas', dan 'pkg.Kelas'.
        val normalized = id.replace('/', '.').removePrefix(packageName).trimStart('.')
        val simple = className.removePrefix(packageName).trimStart('.')
        return normalized == simple || normalized == className
    }
}
