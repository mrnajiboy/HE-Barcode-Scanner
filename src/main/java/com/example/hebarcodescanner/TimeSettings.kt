package com.example.hebarcodescanner

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object TimeSettings {

    private const val PREF_NAME = "time_settings"
    private const val KEY_USE_24H = "use_24h"

    var use24Hour: Boolean = true
        private set

    fun load(ctx: Context) {
        val prefs: SharedPreferences =
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        use24Hour = prefs.getBoolean(KEY_USE_24H, true)
    }

    fun setUse24Hour(ctx: Context, value: Boolean) {
        use24Hour = value
        val prefs: SharedPreferences =
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_USE_24H, value)
        }
    }
}
