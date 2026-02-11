package com.example.hebarcodescanner

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object PresetStore {

    private const val PREF_NAME = "presets"
    private const val KEY_PRESETS = "preset_items"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getAll(ctx: Context): List<Preset> {
        val raw = prefs(ctx).getString(KEY_PRESETS, null) ?: return emptyList()
        val arr = JSONArray(raw)
        val list = mutableListOf<Preset>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Preset(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    webhookUrl = o.optString("webhookUrl"),
                    bodyTemplate = o.optString("bodyTemplate"),
                    description = o.optString("description", "")
                )
            )
        }
        return list
    }

    fun saveAll(ctx: Context, items: List<Preset>) {
        val arr = JSONArray()
        items.forEach { preset ->
            val o = JSONObject()
            o.put("id", preset.id)
            o.put("name", preset.name)
            o.put("webhookUrl", preset.webhookUrl)
            o.put("bodyTemplate", preset.bodyTemplate)
            o.put("description", preset.description)
            arr.put(o)
        }
        prefs(ctx).edit {
            putString(KEY_PRESETS, arr.toString())
        }
    }

    fun add(ctx: Context, preset: Preset) {
        val all = getAll(ctx).toMutableList()
        all.add(preset)
        saveAll(ctx, all)
    }

    fun update(ctx: Context, preset: Preset) {
        val all = getAll(ctx).toMutableList()
        val idx = all.indexOfFirst { it.id == preset.id }
        if (idx >= 0) {
            all[idx] = preset
            saveAll(ctx, all)
        }
    }

    fun delete(ctx: Context, id: String) {
        val all = getAll(ctx).toMutableList()
        val removed = all.removeAll { it.id == id }
        if (removed) {
            saveAll(ctx, all)
        }
    }

    fun findById(ctx: Context, id: String): Preset? =
        getAll(ctx).firstOrNull { it.id == id }

    fun forceReseed(ctx: Context) {
        val all = getAll(ctx).toMutableList()
        all.removeAll {
            it.name.startsWith("Inventory - ") ||
                    it.name.startsWith("Packaging - ") ||
                    it.name.startsWith("Shipment - ")
        }
        saveAll(ctx, all)
        ensureDefaultsSeeded(ctx)
    }

    fun ensureDefaultsSeeded(ctx: Context): Int {
        val existing = getAll(ctx)

        val hasDefaults = existing.any {
            it.name.startsWith("Inventory - ") ||
                    it.name.startsWith("Packaging - ") ||
                    it.name.startsWith("Shipment - ")
        }

        if (hasDefaults) return 0

        val webhooks = WebhookConfigStore.getAll(ctx)
        if (webhooks.isEmpty()) return 0

        val defaultWebhook = webhooks.first()

        val inventoryType = ItemTypeStore.getAll(ctx).firstOrNull { it.id == "inventory" }
        val packagingType = ItemTypeStore.getAll(ctx).firstOrNull { it.id == "packaging" }
        val shipmentType = ItemTypeStore.getAll(ctx).firstOrNull { it.id == "shipment" }

        val defaultPresets = mutableListOf<Preset>()
        var idCounter = System.currentTimeMillis()

        // Inventory Presets
        if (inventoryType != null) {
            defaultPresets.add(createPreset(idCounter++, "Inventory - Create", "Create new inventory item",
                defaultWebhook.url, inventoryType, "Create", quantityAdded = 1))

            defaultPresets.add(createPreset(idCounter++, "Inventory - Update", "Update existing inventory item",
                defaultWebhook.url, inventoryType, "Update"))

            defaultPresets.add(createPreset(idCounter++, "Inventory - Add Inventory", "Add inventory stock",
                defaultWebhook.url, inventoryType, "Add Inventory", quantityAdded = 1))

            defaultPresets.add(createPreset(idCounter++, "Inventory - Remove Inventory", "Remove inventory stock",
                defaultWebhook.url, inventoryType, "Remove Inventory", quantityRemoved = 1))

            defaultPresets.add(createPreset(idCounter++, "Inventory - Sale", "Record inventory sale",
                defaultWebhook.url, inventoryType, "Sale", quantityRemoved = 1))

            defaultPresets.add(createPreset(idCounter++, "Inventory - Return", "Record inventory return",
                defaultWebhook.url, inventoryType, "Return", quantityAdded = 1))
        }

        // Packaging Presets
        if (packagingType != null) {
            defaultPresets.add(createPreset(idCounter++, "Packaging - Create", "Create new packaging item",
                defaultWebhook.url, packagingType, "Create", unitQuantityAdded = 1))

            defaultPresets.add(createPreset(idCounter++, "Packaging - Update", "Update existing packaging item",
                defaultWebhook.url, packagingType, "Update"))

            defaultPresets.add(createPreset(idCounter++, "Packaging - Add Inventory", "Add packaging stock",
                defaultWebhook.url, packagingType, "Add Inventory", unitQuantityAdded = 1))

            defaultPresets.add(createPreset(idCounter++, "Packaging - Remove Inventory", "Remove packaging stock",
                defaultWebhook.url, packagingType, "Remove Inventory", unitQuantityRemoved = 1))

            defaultPresets.add(createPreset(idCounter++, "Packaging - Usage", "Record packaging usage",
                defaultWebhook.url, packagingType, "Usage", unitQuantityRemoved = 1))
        }

        // Shipment Presets
        if (shipmentType != null) {
            val shipmentReasons = listOf(
                "Create", "Update", "Preparing", "Ready to Ship",
                "Out for Pickup", "Dropped Off", "In Transit",
                "Received", "Returned", "Rejected", "Return To Sender"
            )

            shipmentReasons.forEach { reason ->
                defaultPresets.add(createPreset(
                    idCounter++,
                    "Shipment - $reason",
                    "Shipment status: $reason",
                    defaultWebhook.url,
                    shipmentType,
                    reason
                ))
            }
        }

        if (defaultPresets.isNotEmpty()) {
            val all = existing.toMutableList()
            all.addAll(defaultPresets)
            saveAll(ctx, all)
        }

        return defaultPresets.size
    }

    private fun createPreset(
        id: Long,
        name: String,
        description: String,
        webhookUrl: String,
        type: ItemType,
        scanReason: String,
        quantityAdded: Int? = null,
        quantityRemoved: Int? = null,
        unitQuantityAdded: Int? = null,
        unitQuantityRemoved: Int? = null
    ): Preset {
        val obj = JSONObject()
        obj.put("code", "{{code}}")
        obj.put("scanQuantity", "{{scanQuantity}}")
        obj.put("timestamp", "{{timestamp}}")
        obj.put("itemType", type.name)
        obj.put("scanReason", scanReason)

        // Add specific quantity fields
        quantityAdded?.let { obj.put("quantityAdded", it) }
        quantityRemoved?.let { obj.put("quantityRemoved", it) }
        unitQuantityAdded?.let { obj.put("unitQuantityAdded", it) }
        unitQuantityRemoved?.let { obj.put("unitQuantityRemoved", it) }

        // Add all other fields as empty/defaults
        type.fields.forEach { field ->
            if (field.key == "scanReason") return@forEach
            if (field.key == "quantityAdded" && quantityAdded != null) return@forEach
            if (field.key == "quantityRemoved" && quantityRemoved != null) return@forEach
            if (field.key == "unitQuantityAdded" && unitQuantityAdded != null) return@forEach
            if (field.key == "unitQuantityRemoved" && unitQuantityRemoved != null) return@forEach

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

        return Preset(
            id = "preset_$id",
            name = name,
            description = description,
            webhookUrl = webhookUrl,
            bodyTemplate = obj.toString(2)
        )
    }

    private fun buildDefaultPayloadForType(ctx: Context, type: ItemType, scanReason: String): String {
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
}
