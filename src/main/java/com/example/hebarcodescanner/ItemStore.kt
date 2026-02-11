package com.example.hebarcodescanner

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object ItemStore {

    private const val PREF_NAME = "items"
    private const val KEY_INVENTORY = "inventory_items"
    private const val KEY_PACKAGING = "packaging_items"
    private const val KEY_GENERIC = "generic_items"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ---- Inventory ----

    fun getAllInventory(ctx: Context): Map<String, InventoryItem> {
        val raw = prefs(ctx).getString(KEY_INVENTORY, "{}") ?: "{}"
        val obj = JSONObject(raw)
        val map = mutableMapOf<String, InventoryItem>()
        val keys = obj.keys()

        while (keys.hasNext()) {
            val code = keys.next()
            val o = obj.getJSONObject(code)

            val currencyFields = parseCurrencyFields(o.optJSONObject("currencyFields"))

            map[code] = InventoryItem(
                code = code,
                itemName = o.optString("itemName", null),
                imageUrl = o.optString("imageUrl", null),
                category = o.optString("category", null),
                version = o.optString("version", null),
                group = o.optString("group", null),
                scanReason = o.optString("scanReason", null),
                storageLocations = o.optString("storageLocations", null),
                notes = o.optString("notes", null),
                quantityAdded = o.optIntOrNull("quantityAdded"),
                quantityRemoved = o.optIntOrNull("quantityRemoved"),
                currencyFields = currencyFields
            )
        }

        return map
    }

    fun saveInventory(ctx: Context, items: Map<String, InventoryItem>) {
        val root = JSONObject()
        items.forEach { (code, item) ->
            val o = JSONObject()
            o.putOpt("itemName", item.itemName)
            o.putOpt("imageUrl", item.imageUrl)
            o.putOpt("category", item.category)
            o.putOpt("version", item.version)
            o.putOpt("group", item.group)
            o.putOpt("scanReason", item.scanReason)
            o.putOpt("storageLocations", item.storageLocations)
            o.putOpt("notes", item.notes)
            o.putOpt("quantityAdded", item.quantityAdded)
            o.putOpt("quantityRemoved", item.quantityRemoved)
            o.put("currencyFields", serializeCurrencyFields(item.currencyFields))
            root.put(code, o)
        }
        prefs(ctx).edit {
            putString(KEY_INVENTORY, root.toString())
        }
    }

    fun upsertInventory(ctx: Context, item: InventoryItem) {
        val current = getAllInventory(ctx).toMutableMap()
        current[item.code] = item
        saveInventory(ctx, current)
    }

    // ---- Packaging ----

    fun getAllPackaging(ctx: Context): Map<String, PackagingItem> {
        val raw = prefs(ctx).getString(KEY_PACKAGING, "{}") ?: "{}"
        val obj = JSONObject(raw)
        val map = mutableMapOf<String, PackagingItem>()
        val keys = obj.keys()

        while (keys.hasNext()) {
            val code = keys.next()
            val o = obj.getJSONObject(code)

            val currencyFields = parseCurrencyFields(o.optJSONObject("currencyFields"))

            map[code] = PackagingItem(
                code = code,
                item = o.optString("item", null),
                supplier = o.optString("supplier", null),
                scanReason = o.optString("scanReason", null),
                quantityPerUnit = o.optIntOrNull("quantityPerUnit"),
                unitQuantityAdded = o.optIntOrNull("unitQuantityAdded"),
                unitQuantityRemoved = o.optIntOrNull("unitQuantityRemoved"),
                lastOrdered = o.optString("lastOrdered", null),
                supplierLink = o.optString("supplierLink", null),
                notes = o.optString("notes", null),
                currencyFields = currencyFields
            )
        }

        return map
    }

    fun savePackaging(ctx: Context, items: Map<String, PackagingItem>) {
        val root = JSONObject()
        items.forEach { (code, item) ->
            val o = JSONObject()
            o.putOpt("item", item.item)
            o.putOpt("supplier", item.supplier)
            o.putOpt("scanReason", item.scanReason)
            o.putOpt("quantityPerUnit", item.quantityPerUnit)
            o.putOpt("unitQuantityAdded", item.unitQuantityAdded)
            o.putOpt("unitQuantityRemoved", item.unitQuantityRemoved)
            o.putOpt("lastOrdered", item.lastOrdered)
            o.putOpt("supplierLink", item.supplierLink)
            o.putOpt("notes", item.notes)
            o.put("currencyFields", serializeCurrencyFields(item.currencyFields))
            root.put(code, o)
        }
        prefs(ctx).edit {
            putString(KEY_PACKAGING, root.toString())
        }
    }

    fun upsertPackaging(ctx: Context, item: PackagingItem) {
        val current = getAllPackaging(ctx).toMutableMap()
        current[item.code] = item
        savePackaging(ctx, current)
    }

// ---- Shipment Items ----

    private const val KEY_SHIPMENT = "shipment_items"

    fun getAllShipment(ctx: Context): Map<String, ShipmentItem> {
        val raw = prefs(ctx).getString(KEY_SHIPMENT, "{}") ?: "{}"
        val obj = JSONObject(raw)
        val map = mutableMapOf<String, ShipmentItem>()
        val keys = obj.keys()

        while (keys.hasNext()) {
            val code = keys.next()
            val o = obj.getJSONObject(code)

            val currencyFields = parseCurrencyFields(o.optJSONObject("currencyFields"))
            val weight = parseMeasurementValue(o.optJSONArray("weight"))
            val height = parseMeasurementValue(o.optJSONArray("height"))
            val width = parseMeasurementValue(o.optJSONArray("width"))
            val depth = parseMeasurementValue(o.optJSONArray("depth"))

            map[code] = ShipmentItem(
                code = code,
                trackingNumber = o.optString("trackingNumber", null),
                buyerName = o.optString("buyerName", null),
                buyerCountry = o.optString("buyerCountry", null),
                shippedDate = o.optString("shippedDate", null),
                estDeliveryDate = o.optString("estDeliveryDate", null),
                fulfillmentLocation = o.optString("fulfillmentLocation", null),
                lastHandledBy = o.optString("lastHandledBy", null),
                scanReason = o.optString("scanReason", null),
                notes = o.optString("notes", null),
                weight = weight,
                height = height,
                width = width,
                depth = depth,
                currencyFields = currencyFields
            )
        }

        return map
    }

    fun saveShipment(ctx: Context, items: Map<String, ShipmentItem>) {
        val root = JSONObject()
        items.forEach { (code, item) ->
            val o = JSONObject()
            o.putOpt("trackingNumber", item.trackingNumber)
            o.putOpt("buyerName", item.buyerName)
            o.putOpt("buyerCountry", item.buyerCountry)
            o.putOpt("shippedDate", item.shippedDate)
            o.putOpt("estDeliveryDate", item.estDeliveryDate)
            o.putOpt("fulfillmentLocation", item.fulfillmentLocation)
            o.putOpt("lastHandledBy", item.lastHandledBy)
            o.putOpt("scanReason", item.scanReason)
            o.putOpt("notes", item.notes)

            item.weight?.let { o.put("weight", serializeMeasurementValue(it)) }
            item.height?.let { o.put("height", serializeMeasurementValue(it)) }
            item.width?.let { o.put("width", serializeMeasurementValue(it)) }
            item.depth?.let { o.put("depth", serializeMeasurementValue(it)) }

            o.put("currencyFields", serializeCurrencyFields(item.currencyFields))
            root.put(code, o)
        }
        prefs(ctx).edit {
            putString(KEY_SHIPMENT, root.toString())
        }
    }

    fun upsertShipment(ctx: Context, item: ShipmentItem) {
        val current = getAllShipment(ctx).toMutableMap()
        current[item.code] = item
        saveShipment(ctx, current)
    }

    private fun parseMeasurementValue(arr: JSONArray?): MeasurementValue? {
        if (arr == null || arr.length() == 0) return null
        val obj = arr.optJSONObject(0) ?: return null

        val metric = obj.optJSONObject("metric")?.let { m ->
            MeasurementUnit(
                value = m.optDoubleOrNull("value"),
                unit = m.optString("unit", ""),
                symbol = m.optString("symbol", "")
            )
        }

        val imperial = obj.optJSONObject("imperial")?.let { i ->
            MeasurementUnit(
                value = i.optDoubleOrNull("value"),
                unit = i.optString("unit", ""),
                symbol = i.optString("symbol", "")
            )
        }

        if (metric == null && imperial == null) return null
        return MeasurementValue(metric = metric, imperial = imperial)
    }

    private fun serializeMeasurementValue(value: MeasurementValue): JSONArray {
        val arr = JSONArray()
        val obj = JSONObject()

        value.metric?.let { m ->
            val metricObj = JSONObject()
            metricObj.putOpt("value", m.value)
            metricObj.put("unit", m.unit)
            metricObj.put("symbol", m.symbol)
            obj.put("metric", metricObj)
        }

        value.imperial?.let { i ->
            val imperialObj = JSONObject()
            imperialObj.putOpt("value", i.value)
            imperialObj.put("unit", i.unit)
            imperialObj.put("symbol", i.symbol)
            obj.put("imperial", imperialObj)
        }

        if (obj.length() > 0) {
            arr.put(obj)
        }

        return arr
    }
    // ---- Generic Items ----

    fun getAllGeneric(ctx: Context): Map<String, GenericItem> {
        val raw = prefs(ctx).getString(KEY_GENERIC, "{}") ?: "{}"
        val obj = JSONObject(raw)
        val map = mutableMapOf<String, GenericItem>()
        val keys = obj.keys()

        while (keys.hasNext()) {
            val code = keys.next()
            val itemObj = obj.getJSONObject(code)

            val stringFields = parseStringMap(itemObj.optJSONObject("stringFields"))
            val numberFields = parseNumberMap(itemObj.optJSONObject("numberFields"))
            val dateTimeFields = parseStringMap(itemObj.optJSONObject("dateTimeFields"))
            val booleanFields = parseBooleanMap(itemObj.optJSONObject("booleanFields"))
            val currencyFields = parseCurrencyFields(itemObj.optJSONObject("currencyFields"))

            map[code] = GenericItem(
                code = code,
                typeId = itemObj.optString("typeId", ""),
                stringFields = stringFields,
                numberFields = numberFields,
                dateTimeFields = dateTimeFields,
                booleanFields = booleanFields,
                currencyFields = currencyFields
            )
        }

        return map
    }

    fun saveGeneric(ctx: Context, items: Map<String, GenericItem>) {
        val root = JSONObject()
        items.forEach { (code, item) ->
            val o = JSONObject()
            o.put("typeId", item.typeId)
            o.put("stringFields", serializeStringMap(item.stringFields))
            o.put("numberFields", serializeNumberMap(item.numberFields))
            o.put("dateTimeFields", serializeStringMap(item.dateTimeFields))
            o.put("booleanFields", serializeBooleanMap(item.booleanFields))
            o.put("currencyFields", serializeCurrencyFields(item.currencyFields))
            root.put(code, o)
        }
        prefs(ctx).edit {
            putString(KEY_GENERIC, root.toString())
        }
    }

    fun upsertGeneric(ctx: Context, item: GenericItem) {
        val current = getAllGeneric(ctx).toMutableMap()
        current[item.code] = item
        saveGeneric(ctx, current)
    }

    // ---- Helper functions ----

    private fun parseStringMap(obj: JSONObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        val map = mutableMapOf<String, String>()
        obj.keys().forEach { key ->
            map[key] = obj.optString(key, "")
        }
        return map
    }

    private fun parseNumberMap(obj: JSONObject?): Map<String, Double> {
        if (obj == null) return emptyMap()
        val map = mutableMapOf<String, Double>()
        obj.keys().forEach { key ->
            map[key] = obj.optDouble(key, 0.0)
        }
        return map
    }

    private fun parseBooleanMap(obj: JSONObject?): Map<String, Boolean> {
        if (obj == null) return emptyMap()
        val map = mutableMapOf<String, Boolean>()
        obj.keys().forEach { key ->
            map[key] = obj.optBoolean(key, false)
        }
        return map
    }

    private fun serializeStringMap(map: Map<String, String>): JSONObject {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj
    }

    private fun serializeNumberMap(map: Map<String, Double>): JSONObject {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj
    }

    private fun serializeBooleanMap(map: Map<String, Boolean>): JSONObject {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj
    }

    // ---- Currency fields (shared) ----

    private fun parseCurrencyFields(obj: JSONObject?): Map<String, CurrencyValue> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<String, CurrencyValue>()
        val keys = obj.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val arr = obj.optJSONArray(key) ?: continue
            if (arr.length() == 0) continue
            val entry = arr.optJSONObject(0) ?: continue

            val localUnit = entry.optJSONObject("localUnit")?.let { lu ->
                CurrencyUnit(
                    value = lu.optDoubleOrNull("localValue"),
                    currencyCode = lu.optString("localCurrency", ""),
                    symbol = lu.optString("localSymbol", "")
                )
            }

            val globalUnit = entry.optJSONObject("globalUnit")?.let { gu ->
                CurrencyUnit(
                    value = gu.optDoubleOrNull("globalValue"),
                    currencyCode = gu.optString("globalCurrency", ""),
                    symbol = gu.optString("globalSymbol", "")
                )
            }

            if (localUnit != null || globalUnit != null) {
                result[key] = CurrencyValue(local = localUnit, global = globalUnit)
            }
        }

        return result
    }

    private fun serializeCurrencyFields(fields: Map<String, CurrencyValue>): JSONObject {
        val root = JSONObject()
        fields.forEach { (key, value) ->
            val entry = JSONObject()

            value.local?.let { local ->
                val localObj = JSONObject()
                localObj.putOpt("localValue", local.value)
                localObj.put("localCurrency", local.currencyCode)
                localObj.put("localSymbol", local.symbol)
                entry.put("localUnit", localObj)
            }

            value.global?.let { global ->
                val globalObj = JSONObject()
                globalObj.putOpt("globalValue", global.value)
                globalObj.put("globalCurrency", global.currencyCode)
                globalObj.put("globalSymbol", global.symbol)
                entry.put("globalUnit", globalObj)
            }

            if (entry.length() > 0) {
                val arr = JSONArray()
                arr.put(entry)
                root.put(key, arr)
            }
        }
        return root
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? =
        if (has(name) && !isNull(name)) optDouble(name) else null

    private fun JSONObject.optIntOrNull(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null
}
