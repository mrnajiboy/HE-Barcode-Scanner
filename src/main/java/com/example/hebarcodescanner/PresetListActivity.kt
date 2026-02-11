package com.example.hebarcodescanner

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class PresetListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnAdd: Button
    private lateinit var btnCreateDefaults: Button

    private val presets = mutableListOf<Preset>()
    private lateinit var adapter: ArrayAdapter<Preset>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preset_list)

        listView = findViewById(R.id.listPresets)
        btnAdd = findViewById(R.id.btnAddPreset)
        btnCreateDefaults = findViewById(R.id.btnCreateDefaults)

        presets.clear()
        presets.addAll(PresetStore.getAll(this))

        adapter = object : ArrayAdapter<Preset>(
            this,
            R.layout.row_preset,
            presets
        ) {
            override fun getView(
                position: Int,
                convertView: android.view.View?,
                parent: android.view.ViewGroup
            ): android.view.View {
                val view = convertView ?: layoutInflater.inflate(R.layout.row_preset, parent, false)
                val preset = getItem(position)!!
                view.findViewById<TextView>(R.id.txtPresetName).text = preset.name
                view.findViewById<TextView>(R.id.txtPresetUrl).text = preset.webhookUrl
                val desc = view.findViewById<TextView>(R.id.txtPresetDescription)
                if (preset.description.isNotBlank()) {
                    desc.text = preset.description
                    desc.visibility = android.view.View.VISIBLE
                } else {
                    desc.visibility = android.view.View.GONE
                }
                return view
            }
        }

        listView.adapter = adapter

        btnAdd.setOnClickListener {
            showEditPresetDialog(null)
        }

        btnCreateDefaults.setOnClickListener {
            showCreateDefaultPresetsDialog()
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val preset = presets[position]
            showPresetOptionsDialog(preset)
        }
    }

    private fun refreshList() {
        presets.clear()
        presets.addAll(PresetStore.getAll(this))
        adapter.clear()
        adapter.addAll(presets)
        adapter.notifyDataSetChanged()
    }

    private fun showCreateDefaultPresetsDialog() {
        val webhooks = WebhookConfigStore.getAll(this)
        if (webhooks.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("No webhooks")
                .setMessage("Create at least one webhook first.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val labels = webhooks.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select webhook for default presets")
            .setItems(labels) { dialog, which ->
                dialog.dismiss()
                val webhook = webhooks[which]
                createDefaultPresets(webhook)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createDefaultPresets(webhook: WebhookConfig) {
        val inventoryType = ItemTypeStore.getAll(this).firstOrNull { it.id == "inventory" }
        val packagingType = ItemTypeStore.getAll(this).firstOrNull { it.id == "packaging" }

        val defaultPresets = mutableListOf<Preset>()

        // Inventory Create
        if (inventoryType != null) {
            val invCreatePayload = buildDefaultPayload(inventoryType, "Create Inventory")
            defaultPresets.add(
                Preset(
                    id = "default_inventory_create_${System.currentTimeMillis()}",
                    name = "Inventory - Create",
                    description = "Create new inventory item with all fields",
                    webhookUrl = webhook.url,
                    bodyTemplate = invCreatePayload
                )
            )

            // Inventory Update
            val invUpdatePayload = buildDefaultPayload(inventoryType, "Update Inventory")
            defaultPresets.add(
                Preset(
                    id = "default_inventory_update_${System.currentTimeMillis() + 1}",
                    name = "Inventory - Update",
                    description = "Update existing inventory item",
                    webhookUrl = webhook.url,
                    bodyTemplate = invUpdatePayload
                )
            )
        }

        // Packaging Create
        if (packagingType != null) {
            val packCreatePayload = buildDefaultPayload(packagingType, "Create Packaging")
            defaultPresets.add(
                Preset(
                    id = "default_packaging_create_${System.currentTimeMillis() + 2}",
                    name = "Packaging - Create",
                    description = "Create new packaging item with all fields",
                    webhookUrl = webhook.url,
                    bodyTemplate = packCreatePayload
                )
            )

            // Packaging Update
            val packUpdatePayload = buildDefaultPayload(packagingType, "Update Packaging")
            defaultPresets.add(
                Preset(
                    id = "default_packaging_update_${System.currentTimeMillis() + 3}",
                    name = "Packaging - Update",
                    description = "Update existing packaging item",
                    webhookUrl = webhook.url,
                    bodyTemplate = packUpdatePayload
                )
            )
        }

        val all = PresetStore.getAll(this).toMutableList()
        all.addAll(defaultPresets)
        PresetStore.saveAll(this, all)

        refreshList()
        Toast.makeText(
            this,
            "Created ${defaultPresets.size} default presets",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun buildDefaultPayload(type: ItemType, scanReason: String): String {
        val obj = JSONObject()
        obj.put("code", "{{code}}")
        obj.put("scanQuantity", "{{scanQuantity}}")
        obj.put("timestamp", "{{timestamp}}")
        obj.put("itemType", type.name)

        type.fields.forEach { field ->
            when (field.key) {
                "scanReason" -> obj.put(field.key, scanReason)
                else -> {
                    when (field.type) {
                        FieldType.STRING -> obj.put(field.key, "")
                        FieldType.NUMBER -> obj.put(field.key, 0)
                        FieldType.DATE_TIME -> obj.put(field.key, "")
                        FieldType.BOOLEAN -> obj.put(field.key, false)
                        FieldType.CURRENCY -> {
                            val currArr = JSONArray()
                            val currObj = JSONObject()
                            currObj.put("localUnit", JSONObject().apply {
                                put("localValue", 0)
                                put("localCurrency", CurrencySettings.localCode)
                                put("localSymbol", CurrencySettings.localSymbol)
                            })
                            currObj.put("globalUnit", JSONObject().apply {
                                put("globalValue", 0)
                                put("globalCurrency", CurrencySettings.globalCode)
                                put("globalSymbol", CurrencySettings.globalSymbol)
                            })
                            currArr.put(currObj)
                            obj.put(field.key, currArr)
                        }
                        FieldType.MEASUREMENT_WEIGHT -> {
                            val measArr = JSONArray()
                            val measObj = JSONObject()
                            measObj.put("metric", JSONObject().apply {
                                put("value", 0)
                                put("unit", "kg")
                                put("symbol", "kg")
                            })
                            measObj.put("imperial", JSONObject().apply {
                                put("value", 0)
                                put("unit", "lbs")
                                put("symbol", "lbs")
                            })
                            measArr.put(measObj)
                            obj.put(field.key, measArr)
                        }
                        FieldType.MEASUREMENT_DIMENSION -> {
                            val measArr = JSONArray()
                            val measObj = JSONObject()
                            measObj.put("metric", JSONObject().apply {
                                put("value", 0)
                                put("unit", "cm")
                                put("symbol", "cm")
                            })
                            measObj.put("imperial", JSONObject().apply {
                                put("value", 0)
                                put("unit", "in")
                                put("symbol", "in")
                            })
                            measArr.put(measObj)
                            obj.put(field.key, measArr)
                        }
                    }
                }
            }
        }

        return obj.toString(2)
    }

    private fun showPresetOptionsDialog(preset: Preset) {
        val options = arrayOf("Edit", "Delete")

        AlertDialog.Builder(this)
            .setTitle(preset.name)
            .setItems(options) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> showEditPresetDialog(preset)
                    1 -> confirmDeletePreset(preset)
                }
            }
            .setNegativeButton("Back") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun confirmDeletePreset(preset: Preset) {
        AlertDialog.Builder(this)
            .setTitle("Delete preset")
            .setMessage("Delete \"${preset.name}\"?")
            .setPositiveButton("Delete") { dialog, _ ->
                dialog.dismiss()
                deletePreset(preset)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun deletePreset(preset: Preset) {
        val all = PresetStore.getAll(this).toMutableList()
        val removed = all.removeAll { it.id == preset.id }
        if (removed) {
            PresetStore.saveAll(this, all)
            refreshList()
            Toast.makeText(this, "Preset deleted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Preset not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditPresetDialog(existing: Preset?) {
        val isNew = existing == null

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val nameInput = EditText(this).apply {
            hint = "Preset name"
            setText(existing?.name ?: "")
        }

        val webhookSpinner = Spinner(this)
        val webhooks = WebhookConfigStore.getAll(this)
        if (webhooks.isEmpty()) {
            Toast.makeText(this, "Create a webhook first", Toast.LENGTH_SHORT).show()
            return
        }

        val webhookLabels = webhooks.map { it.name }.toTypedArray()
        webhookSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            webhookLabels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val currentWebhookIndex = webhooks.indexOfFirst { it.url == existing?.webhookUrl }
        if (currentWebhookIndex >= 0) {
            webhookSpinner.setSelection(currentWebhookIndex)
        }

        val bodyInput = EditText(this).apply {
            hint = "Body template (use {{code}}, {{scanQuantity}}, {{timestamp}})"
            setText(existing?.bodyTemplate ?: "")
            minLines = 4
            maxLines = 8
            isSingleLine = false
        }

        val descInput = EditText(this).apply {
            hint = "Description (optional)"
            setText(existing?.description ?: "")
        }

        root.addView(TextView(this).apply { text = "Name" })
        root.addView(nameInput)
        root.addView(TextView(this).apply { text = "Webhook" })
        root.addView(webhookSpinner)
        root.addView(TextView(this).apply { text = "Body template" })
        root.addView(bodyInput)
        root.addView(TextView(this).apply { text = "Description" })
        root.addView(descInput)

        AlertDialog.Builder(this)
            .setTitle(if (isNew) "New preset" else "Edit preset")
            .setView(root)
            .setPositiveButton("Save") { dialog, _ ->
                dialog.dismiss()
                val name = nameInput.text.toString().trim()
                val body = bodyInput.text.toString().trim()
                val desc = descInput.text.toString().trim()

                if (name.isBlank()) {
                    Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (body.isBlank()) {
                    Toast.makeText(this, "Body template is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val selectedWebhook = webhooks[webhookSpinner.selectedItemPosition]

                val all = PresetStore.getAll(this).toMutableList()
                if (isNew) {
                    val newPreset = Preset(
                        id = System.currentTimeMillis().toString(),
                        name = name,
                        webhookUrl = selectedWebhook.url,
                        bodyTemplate = body,
                        description = desc
                    )
                    all.add(newPreset)
                } else {
                    val idx = all.indexOfFirst { it.id == existing!!.id }
                    if (idx >= 0) {
                        all[idx] = existing.copy(
                            name = name,
                            webhookUrl = selectedWebhook.url,
                            bodyTemplate = body,
                            description = desc
                        )
                    }
                }

                PresetStore.saveAll(this, all)
                refreshList()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    companion object {
        fun newIntent(context: android.content.Context) =
            android.content.Intent(context, PresetListActivity::class.java)
    }
}
