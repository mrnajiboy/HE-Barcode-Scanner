package com.example.hebarcodescanner

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Currency
import java.util.Locale

enum class CurrencyDisplayMode { SYMBOL, LABEL }

object CurrencySettings {

    private const val PREF_NAME = "currency_settings"
    private const val KEY_LOCAL = "local_code"
    private const val KEY_GLOBAL = "global_code"
    private const val KEY_SHOW_MODE = "show_mode"

    var localCode: String = "KRW"
    var globalCode: String = "USD"

    var displayMode: CurrencyDisplayMode = CurrencyDisplayMode.SYMBOL
        private set

    fun load(ctx: Context) {
        val prefs: SharedPreferences =
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        localCode = prefs.getString(KEY_LOCAL, localCode) ?: localCode
        globalCode = prefs.getString(KEY_GLOBAL, globalCode) ?: globalCode

        val modeName = prefs.getString(KEY_SHOW_MODE, CurrencyDisplayMode.SYMBOL.name)
        displayMode = CurrencyDisplayMode.values().firstOrNull { it.name == modeName }
            ?: CurrencyDisplayMode.SYMBOL
    }

    fun save(ctx: Context) {
        val prefs: SharedPreferences =
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_LOCAL, localCode)
            putString(KEY_GLOBAL, globalCode)
            putString(KEY_SHOW_MODE, displayMode.name)
        }
    }

    fun setDisplayMode(ctx: Context, mode: CurrencyDisplayMode) {
        displayMode = mode
        save(ctx)
    }

    val localSymbol: String
        get() = symbolFor(localCode)

    val globalSymbol: String
        get() = symbolFor(globalCode)

    val localLabel: String
        get() = when (displayMode) {
            CurrencyDisplayMode.SYMBOL -> localSymbol
            CurrencyDisplayMode.LABEL -> "Local"
        }

    val globalLabel: String
        get() = when (displayMode) {
            CurrencyDisplayMode.SYMBOL -> globalSymbol
            CurrencyDisplayMode.LABEL -> "Global"
        }

    private fun symbolFor(code: String): String {
        return try {
            val currency = Currency.getInstance(code)
            currency.getSymbol(Locale.getDefault())
        } catch (e: Exception) {
            code
        }
    }
}
