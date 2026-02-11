package com.example.hebarcodescanner

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MeasurementSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measurement_settings)

        MeasurementSettings.load(this)

        val radioGroup: RadioGroup = findViewById(R.id.radioGroupMeasurement)
        val radioMetric: RadioButton = findViewById(R.id.radioMetric)
        val radioImperial: RadioButton = findViewById(R.id.radioImperial)
        val btnSave: Button = findViewById(R.id.btnSaveMeasurement)

        when (MeasurementSettings.system) {
            MeasurementSystem.METRIC -> radioMetric.isChecked = true
            MeasurementSystem.IMPERIAL -> radioImperial.isChecked = true
        }

        btnSave.setOnClickListener {
            val system = when (radioGroup.checkedRadioButtonId) {
                R.id.radioMetric -> MeasurementSystem.METRIC
                R.id.radioImperial -> MeasurementSystem.IMPERIAL
                else -> MeasurementSystem.METRIC
            }

            MeasurementSettings.setSystem(this, system)
            Toast.makeText(this, "Measurement settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
