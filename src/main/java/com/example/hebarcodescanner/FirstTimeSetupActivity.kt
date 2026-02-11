package com.example.hebarcodescanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class FirstTimeSetupActivity : AppCompatActivity() {

    private lateinit var txtWelcome: TextView
    private lateinit var txtStep: TextView
    private lateinit var editWebhookName: EditText
    private lateinit var editWebhookUrl: EditText
    private lateinit var btnNext: Button
    private lateinit var btnSkip: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_time_setup)

        txtWelcome = findViewById(R.id.txtWelcome)
        txtStep = findViewById(R.id.txtStep)
        editWebhookName = findViewById(R.id.editWebhookName)
        editWebhookUrl = findViewById(R.id.editWebhookUrl)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)

        txtWelcome.text = "Welcome to HE Barcode Scanner!"
        txtStep.text = "To get started, let's create your first webhook endpoint.\n\nThis is where scan data will be sent."

        btnNext.setOnClickListener {
            val name = editWebhookName.text.toString().trim()
            val url = editWebhookUrl.text.toString().trim()

            if (name.isBlank() || url.isBlank()) {
                Toast.makeText(this, "Please enter both name and URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create webhook
            val webhook = WebhookConfig(
                id = System.currentTimeMillis().toString(),
                name = name,
                url = url
            )
            WebhookConfigStore.add(this, webhook)

            // Seed presets and capture count
            val presetCount = PresetStore.ensureDefaultsSeeded(this)

            // Mark setup complete
            markSetupComplete()

            // Show completion message with count
            showCompletionDialog(presetCount)
        }

        btnSkip.setOnClickListener {
            markSetupComplete()
            finish()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun markSetupComplete() {
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("setup_complete", true)
            .apply()
    }

    private fun showCompletionDialog(presetCount: Int) {
        val message = if (presetCount > 0) {
            "✓ Webhook created\n✓ $presetCount default presets created\n✓ 3 item types ready (Inventory, Packaging, Shipment)\n\nYou're ready to start scanning!"
        } else {
            "✓ Webhook created\n✓ Setup complete\n\nYou can create presets in Settings → Manage Presets"
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Setup Complete!")
            .setMessage(message)
            .setPositiveButton("Start Scanning") { dialog, _ ->
                dialog.dismiss()
                finish()
                startActivity(Intent(this, MainActivity::class.java))
            }
            .setCancelable(false)
            .show()
    }

    companion object {
        fun isSetupComplete(context: Context): Boolean {
            return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getBoolean("setup_complete", false)
        }
    }
}
