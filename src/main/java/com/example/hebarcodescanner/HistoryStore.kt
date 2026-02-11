package com.example.hebarcodescanner

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object HistoryStore {

    private const val PREFS_NAME = "history"
    private const val KEY_HISTORY_JSON = "history_json"
    private const val MAX_ENTRIES = 100

    fun getAll(context: Context): List<ScanHistoryItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY_JSON, null) ?: return emptyList()
        val arr = JSONArray(json)
        val list = mutableListOf<ScanHistoryItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                ScanHistoryItem(
                    code = o.getString("code"),
                    timestamp = o.getLong("timestamp"),
                    presetId = if (o.isNull("presetId")) null else o.getString("presetId"),
                    presetName = if (o.isNull("presetName")) null else o.getString("presetName"),
                    webhookUrl = if (o.isNull("webhookUrl")) null else o.getString("webhookUrl"),
                    webhookName = if (o.isNull("webhookName")) null else o.getString("webhookName"),
                    sent = o.getBoolean("sent"),
                    payload = if (o.has("payload") && !o.isNull("payload")) o.getString("payload") else null  // NEW
                )
            )
        }
        return list
    }

    fun add(context: Context, item: ScanHistoryItem) {
        val list = getAll(context).toMutableList()
        list.add(0, item)
        if (list.size > MAX_ENTRIES) {
            list.subList(MAX_ENTRIES, list.size).clear()
        }
        saveAll(context, list)
    }

    // Delete a single history item (match by timestamp + code)
    fun remove(context: Context, item: ScanHistoryItem) {
        val list = getAll(context).toMutableList()
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            val h = iterator.next()
            if (h.timestamp == item.timestamp && h.code == item.code) {
                iterator.remove()
                break
            }
        }
        saveAll(context, list)
    }

    // Wipe all history
    fun clear(context: Context) {
        saveAll(context, emptyList())
    }

    private fun saveAll(context: Context, items: List<ScanHistoryItem>) {
        val arr = JSONArray()
        items.forEach { h ->
            val o = JSONObject()
            o.put("code", h.code)
            o.put("timestamp", h.timestamp)
            o.put("presetId", h.presetId)
            o.put("presetName", h.presetName)
            o.put("webhookUrl", h.webhookUrl)
            o.put("webhookName", h.webhookName)
            o.put("sent", h.sent)
            o.put("payload", h.payload)  // NEW
            arr.put(o)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY_JSON, arr.toString()).apply()
    }
}
