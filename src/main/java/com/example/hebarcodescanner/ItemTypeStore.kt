package com.example.hebarcodescanner

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object ItemTypeStore {

    private const val PREF_NAME = "item_types"
    private const val KEY_TYPES = "types"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun ensureSeeded(ctx: Context) {
        if (getAll(ctx).isNotEmpty()) return

        val inventory = ItemType(
            id = "inventory",
            name = "Inventory",
            fields = listOf(
                ItemField("itemName", "Item Name", FieldType.STRING, true),
                ItemField("imageUrl", "Image", FieldType.STRING, false),
                ItemField("category", "Category", FieldType.STRING, false),
                ItemField("version", "Version", FieldType.STRING, false),
                ItemField("group", "Group", FieldType.STRING, false),
                ItemField("scanReason", "Scan Reason", FieldType.STRING, false),
                ItemField("costPerUnit", "Cost Per Unit", FieldType.CURRENCY, false),
                ItemField("floorPrice", "Floor Price", FieldType.CURRENCY, false),
                ItemField("targetPrice", "Target Price", FieldType.CURRENCY, false),
                ItemField("storageLocations", "Storage Locations", FieldType.STRING, false),
                ItemField("notes", "Notes", FieldType.STRING, false),
                ItemField("quantityAdded", "Quantity Added", FieldType.NUMBER, false),
                ItemField("quantityRemoved", "Quantity Removed", FieldType.NUMBER, false),
            )
        )

        val packaging = ItemType(
            id = "packaging",
            name = "Packaging",
            fields = listOf(
                ItemField("item", "Item", FieldType.STRING, true),
                ItemField("supplier", "Supplier", FieldType.STRING, false),
                ItemField("scanReason", "Scan Reason", FieldType.STRING, false),
                ItemField("quantityPerUnit", "Quantity Per Unit", FieldType.NUMBER, false),
                ItemField("costPerUnit", "Cost Per Unit", FieldType.CURRENCY, false),
                ItemField("unitQuantityAdded", "Unit Quantity Added", FieldType.NUMBER, false),
                ItemField("unitQuantityRemoved", "Unit Quantity Removed", FieldType.NUMBER, false),
                ItemField("lastOrdered", "Last Ordered", FieldType.DATE_TIME, false),
                ItemField("supplierLink", "Supplier Link", FieldType.STRING, false),
                ItemField("notes", "Notes", FieldType.STRING, false),
            )
        )

        val shipment = ItemType(
            id = "shipment",
            name = "Shipment",
            fields = listOf(
                ItemField("trackingNumber", "Tracking Number", FieldType.STRING, true),
                ItemField("buyerName", "Buyer Name", FieldType.STRING, false),
                ItemField("buyerCountry", "Buyer Country", FieldType.STRING, false),
                ItemField("shippedDate", "Shipped Date", FieldType.DATE_TIME, false),
                ItemField("estDeliveryDate", "Est. Delivery Date", FieldType.DATE_TIME, false),
                ItemField("fulfillmentLocation", "Fulfillment Location", FieldType.STRING, false),
                ItemField("lastHandledBy", "Last Handled By", FieldType.STRING, false),
                ItemField("scanReason", "Scan Reason", FieldType.STRING, false),
                ItemField("weight", "Weight", FieldType.MEASUREMENT_WEIGHT, false),
                ItemField("height", "Height", FieldType.MEASUREMENT_DIMENSION, false),
                ItemField("width", "Width", FieldType.MEASUREMENT_DIMENSION, false),
                ItemField("depth", "Depth", FieldType.MEASUREMENT_DIMENSION, false),
                ItemField("shippingCost", "Shipping Cost", FieldType.CURRENCY, false),
                ItemField("declaredCustomsValue", "Declared Customs Value", FieldType.CURRENCY, false),
                ItemField("notes", "Notes", FieldType.STRING, false),
            )
        )

        saveAll(ctx, listOf(inventory, packaging, shipment))
    }

    fun getAll(ctx: Context): List<ItemType> {
        val json = prefs(ctx).getString(KEY_TYPES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<ItemType>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(parseItemType(obj))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAll(ctx: Context, types: List<ItemType>) {
        val arr = JSONArray()
        types.forEach { type ->
            arr.put(serializeItemType(type))
        }
        prefs(ctx).edit {
            putString(KEY_TYPES, arr.toString())
        }
    }

    fun addOrUpdate(ctx: Context, type: ItemType) {
        val all = getAll(ctx).toMutableList()
        val idx = all.indexOfFirst { it.id == type.id }
        if (idx >= 0) {
            all[idx] = type
        } else {
            all.add(type)
        }
        saveAll(ctx, all)
    }

    fun delete(ctx: Context, id: String) {
        val all = getAll(ctx).filterNot { it.id == id }
        saveAll(ctx, all)
    }

    private fun parseItemType(obj: JSONObject): ItemType {
        val id = obj.optString("id", "")
        val name = obj.optString("name", id)
        val fieldsArr = obj.optJSONArray("fields") ?: JSONArray()
        val fields = mutableListOf<ItemField>()
        for (i in 0 until fieldsArr.length()) {
            val fObj = fieldsArr.getJSONObject(i)
            fields.add(parseItemField(fObj))
        }
        return ItemType(
            id = id,
            name = name,
            fields = fields
        )
    }

    private fun serializeItemType(type: ItemType): JSONObject {
        val obj = JSONObject()
        obj.put("id", type.id)
        obj.put("name", type.name)
        val fieldsArr = JSONArray()
        type.fields.forEach { field ->
            fieldsArr.put(serializeItemField(field))
        }
        obj.put("fields", fieldsArr)
        return obj
    }

    private fun parseItemField(obj: JSONObject): ItemField {
        val key = obj.optString("key", "")
        val label = obj.optString("label", key)
        val typeName = obj.optString("type", FieldType.STRING.name)
        val required = obj.optBoolean("required", false)
        val fieldType = FieldType.entries.firstOrNull { it.name == typeName }
            ?: FieldType.STRING
        return ItemField(
            key = key,
            label = label,
            type = fieldType,
            required = required
        )
    }

    private fun serializeItemField(field: ItemField): JSONObject {
        val obj = JSONObject()
        obj.put("key", field.key)
        obj.put("label", field.label)
        obj.put("type", field.type.name)
        obj.put("required", field.required)
        return obj
    }
    fun forceReseed(ctx: Context) {
        prefs(ctx).edit().remove(KEY_TYPES).apply()
        ensureSeeded(ctx)
    }
}
