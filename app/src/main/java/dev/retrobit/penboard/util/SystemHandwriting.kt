package dev.retrobit.penboard.util

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager

/**
 * Jembatan ke **keyboard tulisan tangan bawaan perangkat** (mis. Celia/Huawei Handwriting
 * pada perangkat Huawei, atau metode masukan tulisan tangan lain yang terpasang).
 *
 * Huawei tidak menyediakan API pengenalan tulisan tangan untuk aplikasi pihak ketiga
 * (HMS Core hanya punya OCR gambar), sehingga cara yang bisa dipakai adalah berpindah
 * ke keyboard tulisan tangan tersebut ketika pengguna memang ingin pengenalan Huawei.
 */
object SystemHandwriting {

    private const val TAG = "PenboardSystemInk"

    /** Kata kunci untuk mengenali keyboard tulisan tangan bawaan. */
    val keywords: List<String> = listOf(
        "huawei", "celia", "honor", "harmony", "hiai",
        "handwrit", "tulisan tangan", "tulis tangan", "\u624b\u5199"
    )

    data class Candidate(
        val id: String,
        val packageName: String,
        val label: String
    ) {
        /** Untuk uji & perbandingan tanpa Android. */
        val searchable: String get() = (packageName + " " + label).lowercase()
    }

    /** Apakah keyboard ini kemungkinan menyediakan tulisan tangan bawaan. */
    fun isCandidate(packageName: String, label: String): Boolean {
        val haystack = (packageName + " " + label).lowercase()
        return keywords.any { haystack.contains(it) }
    }

    /**
     * Pilih kandidat terbaik dari daftar keyboard: utamakan yang namanya mengandung kata
     * kunci Huawei/Celia, lalu keyboard tulisan tangan lain; `null` bila tidak ada.
     */
    fun pickBest(candidates: List<Candidate>, ownPackage: String): Candidate? {
        val others = candidates.filter { it.packageName != ownPackage }
        return others.firstOrNull { candidate -> candidate.searchable.contains("huawei") || candidate.searchable.contains("celia") }
            ?: others.firstOrNull { candidate -> candidate.searchable.contains("honor") || candidate.searchable.contains("harmony") }
            ?: others.firstOrNull { candidate ->
                keywords.any { candidate.searchable.contains(it) }
            }
    }

    /** Daftar keyboard tulisan tangan yang tersedia di perangkat. */
    fun candidates(context: Context): List<Candidate> = try {
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.enabledInputMethodList.mapNotNull { info -> info.toCandidate(context) }
    } catch (error: Throwable) {
        Log.w(TAG, "Gagal membaca daftar keyboard", error)
        emptyList()
    }

    private fun InputMethodInfo.toCandidate(context: Context): Candidate? = try {
        Candidate(
            id = id,
            packageName = packageName,
            label = loadLabel(context.packageManager).toString()
        )
    } catch (error: Throwable) {
        null
    }

    /**
     * Pindah ke keyboard tulisan tangan bawaan dari dalam keyboard Penboard.
     * Mengembalikan [Candidate] yang dipakai, atau `null` bila hanya bisa membuka pemilih keyboard.
     */
    fun switchFromIme(ime: InputMethodService): Candidate? {
        val target = pickBest(candidates(ime), ime.packageName)
        if (target == null) {
            openPicker(ime)
            return null
        }
        val switched = try {
            val manager = ime.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val token = ime.window?.window?.attributes?.token
            manager.setInputMethod(token, target.id)
            true
        } catch (error: Throwable) {
            Log.w(TAG, "Gagal berpindah keyboard", error)
            false
        }
        if (!switched) openPicker(ime)
        return target
    }

    /** Buka dialog pemilih keyboard Android. */
    fun openPicker(context: Context): Boolean = try {
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.showInputMethodPicker()
        true
    } catch (error: Throwable) {
        Log.w(TAG, "Gagal membuka pemilih keyboard", error)
        false
    }
}
