package com.example.hebarcodescanner

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class WebhookListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnAdd: Button
    private val items = mutableListOf<WebhookConfig>()
    private lateinit var adapter: ArrayAdapter<WebhookConfig>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_webhook_list)

        listView = findViewById(R.id.listWebhooks)
        btnAdd = findViewById(R.id.btnAddWebhook)

        items.clear()
        items.addAll(WebhookConfigStore.getAll(this))

        adapter = object : ArrayAdapter<WebhookConfig>(
            this,
            R.layout.row_webhook,
            items
        ) {
            override fun getView(
                position: Int,
                convertView: android.view.View?,
                parent: android.view.ViewGroup
            ): android.view.View {
                val view = convertView ?: layoutInflater.inflate(R.layout.row_webhook, parent, false)
                val cfg = getItem(position)!!
                view.findViewById<TextView>(R.id.txtWebhookName).text = cfg.name
                view.findViewById<TextView>(R.id.txtWebhookUrl).text = cfg.url
                return view
            }
        }

        listView.adapter = adapter

        btnAdd.setOnClickListener { showEditWebhookDialog(null) }

        listView.setOnItemClickListener { _, _, position, _ ->
            val cfg = items[position]
            showEditWebhookDialog(cfg)
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            val cfg = items[position]
            AlertDialog.Builder(this)
                .setTitle("Delete webhook")
                .setMessage("Delete ${cfg.name}?")
                .setPositiveButton("Delete") { dialog, _ ->
                    dialog.dismiss()
                    items.removeAt(position)
                    WebhookConfigStore.saveAll(this, items)
                    refreshList()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
            true
        }
    }

    private fun refreshList() {
        adapter.notifyDataSetChanged()
    }

    private fun showEditWebhookDialog(existing: WebhookConfig?) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val nameInput = EditText(this).apply {
            hint = "Name"
            setText(existing?.name ?: "")
        }

        val urlInput = EditText(this).apply {
            hint = "Webhook URL (https://...)"
            setText(existing?.url ?: "")
        }

        val headersInput = EditText(this).apply {
            hint = "Custom headers (JSON object, e.g. {\"X-API-Key\":\"abcd\"})"
            setText(existing?.headersJson ?: "")
            minLines = 2
            maxLines = 4
        }

        val payloadInput = EditText(this).apply {
            hint = "Payload template (optional JSON, supports {{code}}, {{quantity}}, {{timestamp}})"
            setText(existing?.payloadTemplate ?: "")
            minLines = 3
            maxLines = 6
        }

        container.addView(TextView(this).apply { text = "Webhook name" })
        container.addView(nameInput)
        container.addView(TextView(this).apply { text = "Webhook URL" })
        container.addView(urlInput)
        container.addView(TextView(this).apply { text = "Custom headers (JSON)" })
        container.addView(headersInput)
        container.addView(TextView(this).apply { text = "Payload template (optional)" })
        container.addView(payloadInput)

        val title = if (existing == null) "New webhook" else "Edit webhook"

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Save") { dialog, _ ->
                dialog.dismiss()
                val name = nameInput.text.toString().trim()
                val url = urlInput.text.toString().trim()
                val headersRaw = headersInput.text.toString().trim()
                val payloadRaw = payloadInput.text.toString().trim()

                if (name.isBlank() || url.isBlank()) {
                    Toast.makeText(this, "Name and URL required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val headersJsonClean = if (headersRaw.isNotBlank()) {
                    try {
                        val obj = JSONObject(headersRaw)
                        obj.toString()
                    } catch (_: Exception) {
                        Toast.makeText(this, "Headers must be valid JSON object", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                } else null

                val payloadTemplateClean = payloadRaw.ifBlank { null }

                if (existing == null) {
                    val cfg = WebhookConfig(
                        id = System.currentTimeMillis().toString(),
                        name = name,
                        url = url,
                        headersJson = headersJsonClean,
                        payloadTemplate = payloadTemplateClean
                    )
                    items.add(cfg)
                } else {
                    val idx = items.indexOfFirst { it.id == existing.id }
                    if (idx >= 0) {
                        items[idx] = existing.copy(
                            name = name,
                            url = url,
                            headersJson = headersJsonClean,
                            payloadTemplate = payloadTemplateClean
                        )
                    }
                }

                WebhookConfigStore.saveAll(this, items)
                refreshList()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
