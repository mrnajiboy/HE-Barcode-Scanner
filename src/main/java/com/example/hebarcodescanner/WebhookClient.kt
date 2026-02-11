package com.example.hebarcodescanner

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object WebhookClient {

    private val client = OkHttpClient()

    fun sendJson(
        url: String,
        jsonBody: String,
        headers: Map<String, String> = emptyMap(),
        onResult: (Boolean, String?) -> Unit
    ) {
        val mediaType = "application/json".toMediaType()
        val body = jsonBody.toRequestBody(mediaType)

        val builder = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")

        headers.forEach { (k, v) -> builder.addHeader(k, v) }

        val request = builder.build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                onResult(false, e.message)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                onResult(response.isSuccessful, response.body?.string())
            }
        })
    }
}
