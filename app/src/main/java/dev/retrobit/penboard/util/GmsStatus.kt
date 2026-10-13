package dev.retrobit.penboard.util

/**
 * Keadaan **Google Play services (GMS)** di perangkat (murni Kotlin, mudah diuji).
 *
 * Mesin pengenalan Penboard (ML Kit Digital Ink) memakai Google Play services.
 * Sebelumnya aplikasi hanya membedakan "ada" dan "tidak ada", padahal hasil
 * pemeriksaan GMS punya banyak sebab — dan yang paling sering terjadi di
 * perangkat Huawei bukan "tidak ada", melainkan **versi lama** atau dinonaktifkan.
 * Perbedaan itu penting: pada versi lama mesin masih bisa dicoba.
 */
enum class GmsStatus(
    val code: Int,
    val canAttempt: Boolean,
    val message: String,
    val advice: String
) {
    READY(
        0,
        canAttempt = true,
        message = "Google Play services siap.",
        advice = "Mesin pengenalan Penboard bisa dipakai."
    ),
    MISSING(
        1,
        canAttempt = false,
        message = "Google Play services tidak terpasang di perangkat ini.",
        advice = "Pakai keyboard tulisan tangan perangkat (mis. Huawei/Celia) lewat tombol coretan tangan."
    ),
    UPDATE_REQUIRED(
        2,
        canAttempt = true,
        message = "Google Play services di perangkat ini perlu diperbarui.",
        advice = "Buka Play Store untuk memperbaruinya; sementara itu tulisan bisa dipakai lewat keyboard tulisan tangan perangkat."
    ),
    DISABLED(
        3,
        canAttempt = false,
        message = "Google Play services sedang dinonaktifkan di perangkat ini.",
        advice = "Nyalakan lagi Google Play services di Pengaturan aplikasi, atau pakai keyboard tulisan tangan perangkat."
    ),
    INVALID(
        9,
        canAttempt = false,
        message = "Google Play services di perangkat ini tidak sah (bukan versi resmi).",
        advice = "Pakai keyboard tulisan tangan perangkat (mis. Huawei/Celia) lewat tombol coretan tangan."
    ),
    UPDATING(
        18,
        canAttempt = true,
        message = "Google Play services sedang diperbarui.",
        advice = "Coba lagi sebentar lagi."
    ),
    UNKNOWN(
        -1,
        canAttempt = true,
        message = "Keadaan Google Play services tidak bisa dibaca.",
        advice = "Penboard tetap mencoba; bila gagal, pakai keyboard tulisan tangan perangkat."
    );

    /** Ringkasan untuk ditampilkan di halaman pengaturan. */
    val summary: String get() = "$message $advice"

    companion object {

        /**
         * Hasil pemeriksaan GMS dari `ConnectionResult`. Kode yang tidak dikenal
         * (mis. profil terbatas = 20) dianggap [UNKNOWN] supaya Penboard tetap
         * mencoba, bukan langsung menyerah.
         */
        fun from(code: Int): GmsStatus =
            entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}
