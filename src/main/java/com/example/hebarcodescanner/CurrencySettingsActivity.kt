package com.example.hebarcodescanner

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class CurrencySettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_currency_settings)

        CurrencySettings.load(this)

        val spinnerLocal: Spinner = findViewById(R.id.spinnerLocalCode)
        val spinnerGlobal: Spinner = findViewById(R.id.spinnerGlobalCode)
        val btnSave: Button = findViewById(R.id.btnSaveCurrency)
        val switchLabels: Switch = findViewById(R.id.switchCurrencyLabels)

        // you can extend this list as needed
        val codes = listOf("KRW", "USD", "EUR", "JPY", "CNY")

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            codes
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerLocal.adapter = adapter
        spinnerGlobal.adapter = adapter

        val localIndex = codes.indexOf(CurrencySettings.localCode).coerceAtLeast(0)
        val globalIndex = codes.indexOf(CurrencySettings.globalCode).coerceAtLeast(0)

        spinnerLocal.setSelection(localIndex)
        spinnerGlobal.setSelection(globalIndex)

        switchLabels.isChecked = (CurrencySettings.displayMode == CurrencyDisplayMode.LABEL)

        switchLabels.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) {
                CurrencyDisplayMode.LABEL
            } else {
                CurrencyDisplayMode.SYMBOL
            }
            CurrencySettings.setDisplayMode(this, mode)
        }

        btnSave.setOnClickListener {
            val local = codes[spinnerLocal.selectedItemPosition]
            val global = codes[spinnerGlobal.selectedItemPosition]

            CurrencySettings.localCode = local
            CurrencySettings.globalCode = global
            CurrencySettings.save(this)

            Toast.makeText(this, "Currency settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
