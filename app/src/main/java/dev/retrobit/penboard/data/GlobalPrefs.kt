package dev.retrobit.penboard.data

import android.content.Context

/** Preferensi global (berlaku untuk semua profil). */
class GlobalPrefs(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("penboard_global", Context.MODE_PRIVATE)

    var hapticsEnabled: Boolean
        get() = prefs.getBoolean("haptics", true)
        set(value) = prefs.edit().putBoolean("haptics", value).apply()

    var showGuideOnStart: Boolean
        get() = prefs.getBoolean("show_guide", true)
        set(value) = prefs.edit().putBoolean("show_guide", value).apply()

    var lastUsedProfileId: String?
        get() = prefs.getString("last_profile", null)
        set(value) = prefs.edit().putString("last_profile", value).apply()
}
