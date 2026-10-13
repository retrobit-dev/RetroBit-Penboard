package dev.retrobit.penboard.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Penyimpanan profil di SharedPreferences (JSON). Tanpa jaringan, tanpa izin khusus.
 */
class PresetRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var cache: MutableList<PenboardConfig>? = null

    fun profiles(): MutableList<PenboardConfig> {
        cache?.let { return it }
        val loaded = load()
        return loaded.also { cache = it }
    }

    fun profile(id: String): PenboardConfig? = profiles().firstOrNull { it.id == id }

    fun activeId(): String {
        val stored = prefs.getString(KEY_ACTIVE, null)
        val list = profiles()
        if (stored != null && list.any { it.id == stored }) return stored
        val fallback = list.firstOrNull() ?: Presets.defaultConfig().also { presets -> add(presets) }
        prefs.edit().putString(KEY_ACTIVE, fallback.id).apply()
        return fallback.id
    }

    fun active(): PenboardConfig = profile(activeId()) ?: profiles().first()

    fun setActive(id: String) {
        prefs.edit().putString(KEY_ACTIVE, id).apply()
    }

    fun add(config: PenboardConfig): PenboardConfig {
        val list = profiles()
        list.add(config.sanitized())
        persist(list)
        return config
    }

    fun upsert(config: PenboardConfig) {
        val list = profiles()
        val safe = config.sanitized()
        val index = list.indexOfFirst { it.id == safe.id }
        if (index >= 0) list[index] = safe else list.add(safe)
        persist(list)
    }

    fun duplicate(id: String, newName: String): PenboardConfig? {
        val original = profile(id) ?: return null
        val copy = original.boardCopy(newName)
        add(copy)
        return copy
    }

    fun delete(id: String) {
        val list = profiles()
        if (list.size <= 1) return
        list.removeAll { it.id == id }
        if (list.isEmpty()) list.add(Presets.defaultConfig())
        persist(list)
        if (activeId() == id) setActive(list.first().id)
    }

    fun rename(id: String, newName: String) {
        profile(id)?.let { upsert(it.copy(name = newName)) }
    }

    fun restoreFactory(id: String): PenboardConfig? {
        val current = profile(id) ?: return null
        val factory = Presets.factory(current.builtinId) ?: return null
        val restored = factory.copy(id = current.id)
        upsert(restored)
        return restored
    }

    fun resetEverything() {
        cache = null
        prefs.edit().clear().apply()
        profiles()
        setActive(profiles().first().id)
    }

    // ---------------------------------------------------------------- internal

    private fun load(): MutableList<PenboardConfig> {
        val raw = prefs.getString(KEY_PROFILES, null)
        val list = mutableListOf<PenboardConfig>()
        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { list.add(PenboardConfig.fromJson(it)) }
                }
            } catch (_: Exception) {
                list.clear()
            }
        }
        if (list.isEmpty()) {
            list.addAll(Presets.builtins())
            persist(list)
            prefs.edit().putString(KEY_ACTIVE, list.first().id).apply()
        }
        return list
    }

    private fun persist(list: List<PenboardConfig>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        val json = JSONObject().put("profiles", arr).toString()
        prefs.edit().putString(KEY_PROFILES, json).apply()
        cache = list.toMutableList()
    }

    companion object {
        private const val PREFS = "penboard_profiles"
        private const val KEY_PROFILES = "profiles"
        private const val KEY_ACTIVE = "active_id"
    }
}
