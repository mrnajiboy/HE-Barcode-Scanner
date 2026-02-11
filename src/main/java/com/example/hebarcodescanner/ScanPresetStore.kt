package com.example.hebarcodescanner

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object ScanPresetStore {
    private const val PREF_NAME = "scan_presets"
    private const val KEY_JSON = "items"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getAll(ctx: Context): List<ScanPreset> {
        val raw = prefs(ctx).getString(KEY_JSON, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<ScanPreset>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                ScanPreset(
                    id = o.optString("id", ""),
                    name = o.optString("name", ""),
                    defaultItemType = o.optString("defaultItemType", ""),
                    usePayloadPresetId = o.optString("usePayloadPresetId", ""),
                    inventoryFieldKeys = o.optJSONArray("inventoryFieldKeys")?.let { ja ->
                        List(ja.length()) { idx -> ja.getString(idx) }
                    } ?: emptyList(),
                    packagingFieldKeys = o.optJSONArray("packagingFieldKeys")?.let { ja ->
                        List(ja.length()) { idx -> ja.getString(idx) }
                    } ?: emptyList()
                )
            )
        }
        return list
    }

    fun saveAll(ctx: Context, items: List<ScanPreset>) {
        val arr = JSONArray()
        items.forEach { sp ->
            val o = JSONObject()
            o.put("id", sp.id)
            o.put("name", sp.name)
            o.put("defaultItemType", sp.defaultItemType)
            o.put("usePayloadPresetId", sp.usePayloadPresetId)
            o.put("inventoryFieldKeys", JSONArray(sp.inventoryFieldKeys))
            o.put("packagingFieldKeys", JSONArray(sp.packagingFieldKeys))
            arr.put(o)
        }
        prefs(ctx).edit {
            putString(KEY_JSON, arr.toString())
        }
    }

    fun add(ctx: Context, preset: ScanPreset) {
        val list = getAll(ctx).toMutableList()
        list.add(preset)
        saveAll(ctx, list)
    }
}
