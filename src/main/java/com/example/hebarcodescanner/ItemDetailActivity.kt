package com.example.hebarcodescanner

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var txtCode: TextView
    private lateinit var txtType: TextView
    private lateinit var txtHistoryCount: TextView
    private lateinit var txtLastScanned: TextView
    private lateinit var txtFields: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button
    private lateinit var btnResend: Button
    private lateinit var btnSearchWeb: Button
    private lateinit var btnExport: Button
    private lateinit var btnViewHistory: Button

    private lateinit var code: String
    private lateinit var type: String
    private var inventoryItem: InventoryItem? = null
    private var packagingItem: PackagingItem? = null
    private var historyItems: List<ScanHistoryItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)

        code = intent.getStringExtra("code") ?: ""
        type = intent.getStringExtra("type") ?: "inventory"

        txtCode = findViewById(R.id.txtItemCode)
        txtType = findViewById(R.id.txtItemType)
        txtHistoryCount = findViewById(R.id.txtHistoryCount)
        txtLastScanned = findViewById(R.id.txtLastScanned)
        txtFields = findViewById(R.id.txtItemFields)
        btnEdit = findViewById(R.id.btnEditItem)
        btnDelete = findViewById(R.id.btnDeleteItem)
        btnResend = findViewById(R.id.btnResendItem)
        btnSearchWeb = findViewById(R.id.btnSearchWebItem)
        btnExport = findViewById(R.id.btnExportItem)
        btnViewHistory = findViewById(R.id.btnViewItemHistory)

        loadData()
        updateUI()

        btnEdit.setOnClickListener { editItem() }
        btnDelete.setOnClickListener { deleteItem() }
        btnResend.setOnClickListener { resendItem() }
        btnSearchWeb.setOnClickListener { searchWeb() }
        btnExport.setOnClickListener { exportItem() }
        btnViewHistory.setOnClickListener { viewHistory() }
    }

    private fun loadData() {
        if (type == "inventory") {
            inventoryItem = ItemStore.getAllInventory(this)[code]
        } else {
            packagingItem = ItemStore.getAllPackaging(this)[code]
        }

        historyItems = HistoryStore.getAll(this).filter { it.code == code }
    }

    private fun updateUI() {
        txtCode.text = "Code: $code"
        txtType.text = "Type: ${type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
        txtHistoryCount.text = "Scanned ${historyItems.size} time(s)"

        val lastScan = historyItems.maxByOrNull { it.timestamp }
        txtLastScanned.text = if (lastScan != null) {
            "Last scanned: ${formatTimestamp(lastScan.timestamp)}"
        } else {
            "Never scanned"
        }

        val fieldsText = buildString {
            if (type == "inventory") {
                inventoryItem?.let { item ->
                    appendLine("Item Name: ${item.itemName ?: "—"}")
                    appendLine("Category: ${item.category ?: "—"}")
                    appendLine("Version: ${item.version ?: "—"}")
                    appendLine("Group: ${item.group ?: "—"}")
                    appendLine("Storage: ${item.storageLocations ?: "—"}")
                    appendLine("Scan Reason: ${item.scanReason ?: "—"}")
                    appendLine("Image URL: ${item.imageUrl ?: "—"}")
                    appendLine("Qty Added: ${item.quantityAdded ?: "—"}")
                    appendLine("Qty Removed: ${item.quantityRemoved ?: "—"}")
                    appendLine("Notes: ${item.notes ?: "—"}")
                    if (item.currencyFields.isNotEmpty()) {
                        appendLine("\nCurrency Fields:")
                        item.currencyFields.forEach { (key, value) ->
                            appendLine("  $key:")
                            value.local?.let { appendLine("    Local: ${it.symbol}${it.value}") }
                            value.global?.let { appendLine("    Global: ${it.symbol}${it.value}") }
                        }
                    }
                }
            } else {
                packagingItem?.let { item ->
                    appendLine("Item: ${item.item ?: "—"}")
                    appendLine("Supplier: ${item.supplier ?: "—"}")
                    appendLine("Qty Per Unit: ${item.quantityPerUnit ?: "—"}")
                    appendLine("Unit Qty Added: ${item.unitQuantityAdded ?: "—"}")
                    appendLine("Unit Qty Removed: ${item.unitQuantityRemoved ?: "—"}")
                    appendLine("Last Ordered: ${item.lastOrdered ?: "—"}")
                    appendLine("Supplier Link: ${item.supplierLink ?: "—"}")
                    appendLine("Notes: ${item.notes ?: "—"}")
                    if (item.currencyFields.isNotEmpty()) {
                        appendLine("\nCurrency Fields:")
                        item.currencyFields.forEach { (key, value) ->
                            appendLine("  $key:")
                            value.local?.let { appendLine("    Local: ${it.symbol}${it.value}") }
                            value.global?.let { appendLine("    Global: ${it.symbol}${it.value}") }
                        }
                    }
                }
            }
        }
        txtFields.text = fieldsText.ifBlank { "No data" }

        btnResend.isEnabled = historyItems.isNotEmpty()
        btnViewHistory.isEnabled = historyItems.isNotEmpty()
    }


    private fun formatTimestamp(ts: Long): String {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = ts
        val year = cal.get(java.util.Calendar.YEAR)
        val month = (cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val minute = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
        return "$year-$month-$day $hour:$minute"
    }

    private fun editItem() {
        Toast.makeText(this, "Edit via scanning and updating fields", Toast.LENGTH_LONG).show()
    }

    private fun deleteItem() {
        AlertDialog.Builder(this)
            .setTitle("Delete item")
            .setMessage("Delete $code? This will remove all stored data but not history entries.")
            .setPositiveButton("Delete") { dialog, _ ->
                dialog.dismiss()
                if (type == "inventory") {
                    val all = ItemStore.getAllInventory(this).toMutableMap()
                    all.remove(code)
                    ItemStore.saveInventory(this, all)
                } else {
                    val all = ItemStore.getAllPackaging(this).toMutableMap()
                    all.remove(code)
                    ItemStore.savePackaging(this, all)
                }
                Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resendItem() {
        if (historyItems.isEmpty()) {
            Toast.makeText(this, "No history to resend", Toast.LENGTH_SHORT).show()
            return
        }

        val lastHistoryItem = historyItems.maxByOrNull { it.timestamp }!!
        val payload = lastHistoryItem.payload

        if (payload.isNullOrBlank()) {
            Toast.makeText(this, "No payload available", Toast.LENGTH_SHORT).show()
            return
        }

        val webhooks = WebhookConfigStore.getAll(this)
        if (webhooks.isEmpty()) {
            Toast.makeText(this, "No webhooks configured", Toast.LENGTH_SHORT).show()
            return
        }

        val labels = webhooks.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Resend to webhook")
            .setItems(labels) { dialog, which ->
                dialog.dismiss()
                val webhook = webhooks[which]
                WebhookClient.sendJson(
                    url = webhook.url,
                    jsonBody = payload,
                    headers = emptyMap()
                ) { success, message ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Sent to ${webhook.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun searchWeb() {
        val providers = SearchProvider.entries.toTypedArray()
        val labels = providers.map { it.displayName }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Search $code on")
            .setItems(labels) { dialog, which ->
                dialog.dismiss()
                val provider = providers[which]
                val template = if (provider == SearchProvider.CUSTOM) {
                    SearchSettings.loadTemplate(this)
                } else {
                    provider.baseUrl + "%s"
                }
                val encoded = URLEncoder.encode(code, StandardCharsets.UTF_8.toString())
                val url = template.replace("%s", encoded)
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportItem() {
        val options = arrayOf("Export as JSON", "Export as CSV", "Share text")

        AlertDialog.Builder(this)
            .setTitle("Export item")
            .setItems(options) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> exportAsJson()
                    1 -> exportAsCsv()
                    2 -> shareAsText()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportAsJson() {
        val json = if (type == "inventory") {
            inventoryItem?.let { buildInventoryJson(it) } ?: JSONObject().apply {
                put("code", code)
                put("error", "Item not found")
            }.toString()
        } else {
            packagingItem?.let { buildPackagingJson(it) } ?: JSONObject().apply {
                put("code", code)
                put("error", "Item not found")
            }.toString()
        }

        val fileName = "item_${code}_${System.currentTimeMillis()}.json"
        val file = File(cacheDir, fileName)
        file.writeText(json)

        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Export JSON"))
    }

    private fun exportAsCsv() {
        val csv = buildString {
            appendLine("field,value")
            appendLine("\"code\",\"$code\"")
            appendLine("\"type\",\"$type\"")
            appendLine("\"history_count\",\"${historyItems.size}\"")

            if (type == "inventory") {
                inventoryItem?.let { item ->
                    appendLine("\"itemName\",\"${item.itemName ?: ""}\"")
                    appendLine("\"category\",\"${item.category ?: ""}\"")
                    appendLine("\"version\",\"${item.version ?: ""}\"")
                    appendLine("\"group\",\"${item.group ?: ""}\"")
                    appendLine("\"storageLocations\",\"${item.storageLocations ?: ""}\"")
                    appendLine("\"notes\",\"${item.notes ?: ""}\"")
                }
            } else {
                packagingItem?.let { item ->
                    appendLine("\"item\",\"${item.item ?: ""}\"")
                    appendLine("\"supplier\",\"${item.supplier ?: ""}\"")
                    appendLine("\"quantityPerUnit\",\"${item.quantityPerUnit ?: ""}\"")
                    appendLine("\"notes\",\"${item.notes ?: ""}\"")
                }
            }
        }

        val fileName = "item_${code}_${System.currentTimeMillis()}.csv"
        val file = File(cacheDir, fileName)
        file.writeText(csv)

        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Export CSV"))
    }

    private fun shareAsText() {
        val text = buildString {
            appendLine("Code: $code")
            appendLine("Type: $type")
            appendLine("History count: ${historyItems.size}")
            appendLine()
            append(txtFields.text)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, "Share item"))
    }

    private fun viewHistory() {
        if (historyItems.isEmpty()) {
            Toast.makeText(this, "No history entries", Toast.LENGTH_SHORT).show()
            return
        }

        val labels = historyItems.map { h ->
            val ts = formatTimestamp(h.timestamp)
            val status = if (h.sent) "✓" else "⏳"
            "$status $ts - ${h.presetName ?: h.webhookName ?: "Log only"}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("History for $code (${historyItems.size})")
            .setItems(labels) { dialog, which ->
                dialog.dismiss()
                // Could open detailed view of this history item
                val historyItem = historyItems[which]
                showHistoryItemDetail(historyItem)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showHistoryItemDetail(item: ScanHistoryItem) {
        val text = buildString {
            appendLine("Code: ${item.code}")
            appendLine("Timestamp: ${formatTimestamp(item.timestamp)}")
            appendLine("Sent: ${item.sent}")
            appendLine("Preset: ${item.presetName ?: "—"}")
            appendLine("Webhook: ${item.webhookName ?: item.webhookUrl ?: "—"}")
            if (item.payload != null) {
                appendLine("\nPayload:")
                appendLine(item.payload)
            }
        }

        val scrollView = ScrollView(this)
        val textView = TextView(this).apply {
            setText(text)
            setPadding(48, 24, 48, 24)
            setTextIsSelectable(true)
        }
        scrollView.addView(textView)

        AlertDialog.Builder(this)
            .setTitle("History entry")
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun buildInventoryJson(item: InventoryItem): String {
        val obj = JSONObject()
        obj.put("code", item.code)
        obj.put("itemName", item.itemName)
        obj.put("imageUrl", item.imageUrl)
        obj.put("category", item.category)
        obj.put("version", item.version)
        obj.put("group", item.group)
        obj.put("scanReason", item.scanReason)
        obj.put("storageLocations", item.storageLocations)
        obj.put("notes", item.notes)
        obj.put("quantityAdded", item.quantityAdded)
        obj.put("quantityRemoved", item.quantityRemoved)
        obj.put("historyCount", historyItems.size)
        return obj.toString(2)
    }

    private fun buildPackagingJson(item: PackagingItem): String {
        val obj = JSONObject()
        obj.put("code", item.code)
        obj.put("item", item.item)
        obj.put("supplier", item.supplier)
        obj.put("quantityPerUnit", item.quantityPerUnit)
        obj.put("unitQuantityAdded", item.unitQuantityAdded)
        obj.put("unitQuantityRemoved", item.unitQuantityRemoved)
        obj.put("lastOrdered", item.lastOrdered)
        obj.put("supplierLink", item.supplierLink)
        obj.put("notes", item.notes)
        obj.put("historyCount", historyItems.size)
        return obj.toString(2)
    }
}
