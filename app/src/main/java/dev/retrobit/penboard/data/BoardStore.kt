package dev.retrobit.penboard.data

import android.content.Context
import java.io.File

/**
 * Menyimpan isi papan tulis (goresan) ke berkas kecil di penyimpanan internal aplikasi.
 * Satu berkas per profil papan, sehingga tulisan tetap ada walau keyboard ditutup
 * atau HP dimatikan.
 */
class BoardStore(context: Context) {

    private val directory = File(context.applicationContext.filesDir, "boards")

    fun save(profileId: String, data: String) {
        val safeId = safeName(profileId) ?: return
        if (data.isBlank()) {
            clear(profileId)
            return
        }
        runCatching {
            if (!directory.exists()) directory.mkdirs()
            File(directory, "$safeId.board").writeText(data)
        }
    }

    fun load(profileId: String): String? {
        val safeId = safeName(profileId) ?: return null
        return runCatching {
            val file = File(directory, "$safeId.board")
            if (file.isFile && file.length() > 0) file.readText() else null
        }.getOrNull()
    }

    fun hasBoard(profileId: String): Boolean = !load(profileId).isNullOrBlank()

    fun clear(profileId: String) {
        val safeId = safeName(profileId) ?: return
        runCatching { File(directory, "$safeId.board").delete() }
    }

    fun clearAll() {
        runCatching {
            directory.listFiles()?.forEach { file -> file.delete() }
            directory.delete()
        }
    }

    private fun safeName(profileId: String): String? {
        val safe = profileId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
        return safe.takeIf { it.isNotEmpty() && it.length <= 64 }
    }
}
