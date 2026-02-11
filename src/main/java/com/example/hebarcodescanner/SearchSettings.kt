package com.example.hebarcodescanner

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

enum class SearchProvider(val displayName: String, val baseUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    NAVER("Naver", "https://search.naver.com/search.naver?query="),
    DAUM("Daum", "https://search.daum.net/search?q="),
    EBAY("eBay", "https://www.ebay.com/sch/i.html?_nkw="),
    TARGET("Target", "https://www.target.com/s?searchTerm="),
    REDDIT("Reddit", "https://www.reddit.com/search/?q="),
    BRAVE("Brave", "https://search.brave.com/search?q="),
    YANDEX("Yandex", "https://yandex.com/search/?text="),
    CUSTOM("Custom", "") // uses user template only
}

object SearchSettings {
    private const val PREF_NAME = "search_settings"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_TEMPLATE = "template"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun loadProvider(ctx: Context): SearchProvider {
        val name = prefs(ctx).getString(KEY_PROVIDER, null)
        return SearchProvider.entries.firstOrNull { it.name == name } ?: SearchProvider.GOOGLE
    }

    fun loadTemplate(ctx: Context): String {
        val saved = prefs(ctx).getString(KEY_TEMPLATE, null)
        if (saved != null) return saved
        val provider = loadProvider(ctx)
        val base = provider.baseUrl
        return if (base.isBlank()) "%s" else base + "%s"
    }

    fun saveProvider(ctx: Context, provider: SearchProvider) {
        prefs(ctx).edit {
            putString(KEY_PROVIDER, provider.name)
        }
    }

    fun saveTemplate(ctx: Context, template: String) {
        prefs(ctx).edit {
            putString(KEY_TEMPLATE, template)
        }
    }

}
