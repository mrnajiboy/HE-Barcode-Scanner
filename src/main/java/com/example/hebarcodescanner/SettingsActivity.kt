package com.example.hebarcodescanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var btnDevTools: Button
    private lateinit var btnManageTypes: Button
    private lateinit var btnManageWebhooks: Button
    private lateinit var btnManagePresets: Button
    private lateinit var btnManageItems: Button
    private lateinit var btnSearchSettings: Button
    private lateinit var btnMeasurementSettings: Button
    private lateinit var switchTimeFormat: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CurrencySettings.load(this)
        TimeSettings.load(this)

        setContentView(R.layout.activity_settings)

        btnManageTypes = findViewById(R.id.btnManageTypes)
        btnManageWebhooks = findViewById(R.id.btnManageWebhooks)
        btnManagePresets = findViewById(R.id.btnManagePresets)
        btnManageItems = findViewById(R.id.btnManageItems)
        btnSearchSettings = findViewById(R.id.btnSearchSettings)
        switchTimeFormat = findViewById(R.id.switchTimeFormat)

        // Time format toggle
        switchTimeFormat.isChecked = TimeSettings.use24Hour
        switchTimeFormat.setOnCheckedChangeListener { _, isChecked ->
            TimeSettings.setUse24Hour(this, isChecked)
        }

        btnManageTypes.setOnClickListener {
            startActivity(Intent(this, TypeListActivity::class.java))
        }

        btnManageWebhooks.setOnClickListener {
            startActivity(Intent(this, WebhookListActivity::class.java))
        }

        btnManagePresets.setOnClickListener {
            startActivity(Intent(this, PresetListActivity::class.java))
        }

        btnManageItems.setOnClickListener {
            startActivity(Intent(this, ItemListActivity::class.java))
        }

        btnSearchSettings.setOnClickListener {
            startActivity(Intent(this, SearchSettingsActivity::class.java))
        }

        btnMeasurementSettings = findViewById(R.id.btnMeasurementSettings)

        btnMeasurementSettings.setOnClickListener {
            startActivity(Intent(this, MeasurementSettingsActivity::class.java))
        }
        btnDevTools = findViewById(R.id.btnDevTools)

        btnDevTools.setOnClickListener {
            showDevToolsDialog()
        }

        btnManageTypes.setOnLongClickListener {
            btnDevTools.visibility = android.view.View.VISIBLE
            Toast.makeText(this, "Dev tools unlocked", Toast.LENGTH_SHORT).show()
            true
        }
    }
    private fun showDevToolsDialog() {
        val options = arrayOf(
            "Re-seed Types",
            "Re-seed Presets",
            "Clear All Data",
            "Force Migration to Latest",
            "Reset Setup Wizard"
        )

        AlertDialog.Builder(this)
            .setTitle("Developer Tools")
            .setItems(options) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> {
                        ItemTypeStore.forceReseed(this)
                        Toast.makeText(this, "Types re-seeded", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        PresetStore.forceReseed(this)
                        Toast.makeText(this, "Presets re-seeded", Toast.LENGTH_SHORT).show()
                    }
                    2 -> confirmClearAllData()
                    3 -> {
                        AppVersion.runMigrations(this)
                        Toast.makeText(this, "Migrations run", Toast.LENGTH_SHORT).show()
                    }
                    4 -> {
                        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("setup_complete", false)
                            .apply()
                        Toast.makeText(this, "Setup reset - restart app", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmClearAllData() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Clear ALL Data?")
            .setMessage("This will delete:\n• All history\n• All items\n• All types\n• All webhooks\n• All presets\n\nThis CANNOT be undone!")
            .setPositiveButton("DELETE EVERYTHING") { d, _ ->
                d.dismiss()
                HistoryStore.clear(this)
                ItemStore.saveInventory(this, emptyMap())
                ItemStore.savePackaging(this, emptyMap())
                ItemStore.saveGeneric(this, emptyMap())
                ItemTypeStore.saveAll(this, emptyList())
                WebhookConfigStore.saveAll(this, emptyList())
                PresetStore.saveAll(this, emptyList())
                Toast.makeText(this, "All data cleared", Toast.LENGTH_LONG).show()
                recreate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
