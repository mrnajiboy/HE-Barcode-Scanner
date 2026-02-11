package com.example.hebarcodescanner

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

data class WebhookConfig(
    val id: String,
    val name: String,
    val url: String,
    val headersJson: String? = null,
    val payloadTemplate: String? = null
)

object WebhookConfigStore {

    private const val PREF_NAME = "webhook_configs"
    private const val KEY_JSON = "configs"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getAll(ctx: Context): List<WebhookConfig> {
        val raw = prefs(ctx).getString(KEY_JSON, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<WebhookConfig>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                WebhookConfig(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    url = o.optString("url"),
                    headersJson = o.optString("headersJson", null),
                    payloadTemplate = o.optString("payloadTemplate", null)
                )
            )
        }
        return list
    }

    fun saveAll(ctx: Context, items: List<WebhookConfig>) {
        val arr = JSONArray()
        items.forEach { cfg ->
            val o = JSONObject()
            o.put("id", cfg.id)
            o.put("name", cfg.name)
            o.put("url", cfg.url)
            if (!cfg.headersJson.isNullOrBlank()) {
                o.put("headersJson", cfg.headersJson)
            }
            if (!cfg.payloadTemplate.isNullOrBlank()) {
                o.put("payloadTemplate", cfg.payloadTemplate)
            }
            arr.put(o)
        }
        prefs(ctx).edit {
            putString(KEY_JSON, arr.toString())
        }
    }

    fun add(ctx: Context, cfg: WebhookConfig) {
        val list = getAll(ctx).toMutableList()
        list.add(cfg)
        saveAll(ctx, list)
    }
}
