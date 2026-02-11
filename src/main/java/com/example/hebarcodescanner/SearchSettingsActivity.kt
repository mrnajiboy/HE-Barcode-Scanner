package com.example.hebarcodescanner

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SearchSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_settings)

        val spinner: Spinner = findViewById(R.id.spinnerProvider)
        val templateInput: EditText = findViewById(R.id.editTemplate)
        val btnSave: Button = findViewById(R.id.btnSaveSearchSettings)

        val providers = SearchProvider.entries.toTypedArray()
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            providers.map { it.displayName }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val currentProvider = SearchSettings.loadProvider(this)
        val currentTemplate = SearchSettings.loadTemplate(this)
        val idx = providers.indexOfFirst { it == currentProvider }.coerceAtLeast(0)
        spinner.setSelection(idx)
        templateInput.setText(currentTemplate)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val chosen = providers[position]
                if (chosen != SearchProvider.CUSTOM) {
                    templateInput.setText(chosen.baseUrl + "%s")
                }
                // CUSTOM keeps whatever is already in the box
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSave.setOnClickListener {
            val chosen = providers[spinner.selectedItemPosition]
            val template = templateInput.text.toString().trim()
            if (!template.contains("%s")) {
                Toast.makeText(
                    this,
                    "Template must contain %s for the query",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            SearchSettings.saveProvider(this, chosen)
            SearchSettings.saveTemplate(this, template)
            Toast.makeText(this, "Search settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
