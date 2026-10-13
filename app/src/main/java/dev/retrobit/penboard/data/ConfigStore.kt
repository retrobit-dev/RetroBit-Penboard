package dev.retrobit.penboard.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Penyimpanan satu profil pengaturan (JSON di SharedPreferences) + pemberitahuan
 * perubahan supaya keyboard langsung memakai pengaturan terbaru.
 */
class ConfigStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("penboard", Context.MODE_PRIVATE)

    private val listeners = CopyOnWriteArrayList<(PenboardConfig) -> Unit>()

    @Volatile
    var config: PenboardConfig = PenboardConfig()
        private set

    init {
        config = load()
    }

    fun load(): PenboardConfig = try {
        val raw = prefs.getString(KEY_CONFIG, null)
        if (raw.isNullOrBlank()) PenboardConfig() else PenboardConfig.fromJson(JSONObject(raw))
    } catch (error: Throwable) {
        PenboardConfig()
    }

    fun update(transform: (PenboardConfig) -> PenboardConfig): PenboardConfig {
        val next = transform(config).sanitized()
        config = next
        prefs.edit().putString(KEY_CONFIG, next.toJson().toString()).apply()
        listeners.forEach { listener ->
            try {
                listener(next)
            } catch (error: Throwable) {
                // jangan sampai satu pendengar merusak yang lain
            }
        }
        return next
    }

    fun reset(): PenboardConfig = update { PenboardConfig() }

    fun addListener(listener: (PenboardConfig) -> Unit) {
        listeners.addIfAbsent(listener)
    }

    fun removeListener(listener: (PenboardConfig) -> Unit) {
        listeners.remove(listener)
    }

    companion object {
        private const val KEY_CONFIG = "config_json"

        @Volatile
        private var instance: ConfigStore? = null

        fun get(context: Context): ConfigStore {
            val existing = instance
            if (existing != null) return existing
            return synchronized(this) {
                instance ?: ConfigStore(context).also { instance = it }
            }
        }
    }
}
