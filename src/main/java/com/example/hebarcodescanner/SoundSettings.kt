package com.example.hebarcodescanner

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SoundSettings {

    private const val PREF_NAME = "sound_settings"
    private const val KEY_BEEP_ENABLED = "beep_enabled"

    var beepEnabled: Boolean = true
        private set

    fun load(ctx: Context) {
        val prefs: SharedPreferences =
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        beepEnabled = prefs.getBoolean(KEY_BEEP_ENABLED, true)
    }

    fun setBeepEnabled(ctx: Context, enabled: Boolean) {
        beepEnabled = enabled
        val prefs: SharedPreferences =
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_BEEP_ENABLED, enabled)
        }
    }
}
