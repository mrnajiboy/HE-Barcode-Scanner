package com.example.hebarcodescanner

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PresetEditActivity : AppCompatActivity() {

    private lateinit var nameInput: EditText
    private lateinit var urlInput: EditText
    private lateinit var bodyInput: EditText
    private lateinit var descInput: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var existingPreset: Preset? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preset_edit)

        nameInput = findViewById(R.id.editPresetName)
        urlInput = findViewById(R.id.editPresetUrl)
        bodyInput = findViewById(R.id.editPresetBody)
        descInput = findViewById(R.id.editPresetDescription)
        btnSave = findViewById(R.id.btnSavePreset)
        btnCancel = findViewById(R.id.btnCancelPreset)

        val presetId = intent.getStringExtra(EXTRA_PRESET_ID)
        if (!presetId.isNullOrBlank()) {
            existingPreset = PresetStore.findById(this, presetId)
        }

        existingPreset?.let { preset ->
            nameInput.setText(preset.name)
            urlInput.setText(preset.webhookUrl)
            bodyInput.setText(preset.bodyTemplate)
            descInput.setText(preset.description)
        }

        btnSave.setOnClickListener {
            savePreset()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun savePreset() {
        val name = nameInput.text.toString().trim()
        val url = urlInput.text.toString().trim()
        val body = bodyInput.text.toString().trim()
        val desc = descInput.text.toString().trim()

        if (name.isBlank()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (url.isBlank()) {
            Toast.makeText(this, "Webhook URL is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (body.isBlank()) {
            Toast.makeText(this, "Body template is required", Toast.LENGTH_SHORT).show()
            return
        }

        val all = PresetStore.getAll(this).toMutableList()
        val preset = existingPreset?.copy(
            name = name,
            webhookUrl = url,
            bodyTemplate = body,
            description = desc
        ) ?: Preset(
            id = System.currentTimeMillis().toString(),
            name = name,
            webhookUrl = url,
            bodyTemplate = body,
            description = desc
        )

        val idx = all.indexOfFirst { it.id == preset.id }
        if (idx >= 0) {
            all[idx] = preset
        } else {
            all.add(preset)
        }
        PresetStore.saveAll(this, all)

        Toast.makeText(this, "Preset saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        const val EXTRA_PRESET_ID = "extra_preset_id"
    }
}