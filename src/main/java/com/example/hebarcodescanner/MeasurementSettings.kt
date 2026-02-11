package com.example.hebarcodescanner

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

enum class MeasurementSystem { METRIC, IMPERIAL }

object MeasurementSettings {

    private const val PREF_NAME = "measurement_settings"
    private const val KEY_SYSTEM = "system"

    var system: MeasurementSystem = MeasurementSystem.METRIC
        private set

    fun load(ctx: Context) {
        val prefs: SharedPreferences =
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val systemName = prefs.getString(KEY_SYSTEM, MeasurementSystem.METRIC.name)
        system = MeasurementSystem.values().firstOrNull { it.name == systemName }
            ?: MeasurementSystem.METRIC
    }

    fun save(ctx: Context) {
        val prefs: SharedPreferences =
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(KEY_SYSTEM, system.name)
        }
    }

    fun setSystem(ctx: Context, sys: MeasurementSystem) {
        system = sys
        save(ctx)
    }

    // Weight units
    val weightMetricUnit: String = "kg"
    val weightMetricSymbol: String = "kg"
    val weightImperialUnit: String = "lbs"
    val weightImperialSymbol: String = "lbs"

    // Dimension units
    val dimensionMetricUnit: String = "cm"
    val dimensionMetricSymbol: String = "cm"
    val dimensionImperialUnit: String = "in"
    val dimensionImperialSymbol: String = "in"

    // Labels for display
    val weightLabel: String
        get() = when (system) {
            MeasurementSystem.METRIC -> weightMetricSymbol
            MeasurementSystem.IMPERIAL -> weightImperialSymbol
        }

    val dimensionLabel: String
        get() = when (system) {
            MeasurementSystem.METRIC -> dimensionMetricSymbol
            MeasurementSystem.IMPERIAL -> dimensionImperialSymbol
        }
}
