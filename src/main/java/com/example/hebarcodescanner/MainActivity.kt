package com.example.hebarcodescanner

import android.app.DatePickerDialog
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanCustomCode
import io.github.g00fy2.quickie.config.BarcodeFormat
import io.github.g00fy2.quickie.config.ScannerConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private data class CurrencyInputs(
        val localValue: EditText,
        val globalValue: EditText
    )

    private lateinit var btnScan: Button
    private lateinit var btnSearchWeb: Button
    private lateinit var btnSettings: Button
    private lateinit var btnExportHistory: Button
    private lateinit var btnClearHistory: Button
    private lateinit var listHistory: ListView
    private lateinit var switchQuickScan: Switch
    private lateinit var btnSelectQuickPreset: Button
    private lateinit var switchBeep: Switch

    private val history = mutableListOf<ScanHistoryItem>()
    private lateinit var historyAdapter: ArrayAdapter<String>

    internal val inventoryCache: MutableMap<String, InventoryItem> = mutableMapOf()
    internal val packagingCache: MutableMap<String, PackagingItem> = mutableMapOf()

    private var previousConfigUrl: String? = null
    private var previousConfigBody: String? = null
    private var previousConfigHeaders: Map<String, String> = emptyMap()

    private var quickScanMode = false
    private var quickScanPreset: Preset? = null

    private val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

    private val scannerConfig = ScannerConfig.build {
        setBarcodeFormats(
            listOf(
                BarcodeFormat.FORMAT_QR_CODE,
                BarcodeFormat.FORMAT_AZTEC,
                BarcodeFormat.FORMAT_DATA_MATRIX,
                BarcodeFormat.FORMAT_EAN_8,
                BarcodeFormat.FORMAT_EAN_13,
                BarcodeFormat.FORMAT_UPC_A,
                BarcodeFormat.FORMAT_UPC_E,
                BarcodeFormat.FORMAT_CODE_39,
                BarcodeFormat.FORMAT_CODE_93,
                BarcodeFormat.FORMAT_CODE_128,
                BarcodeFormat.FORMAT_ITF,
            )
        )
    }

    private val scanLauncher = registerForActivityResult(ScanCustomCode()) { result: QRResult ->
        when (result) {
            is QRResult.QRSuccess -> {
                val content = result.content.rawValue
                if (!content.isNullOrBlank()) {
                    if (SoundSettings.beepEnabled) {
                        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    }
                    onScanCompleted(content)
                } else {
                    Toast.makeText(this, "Empty code", Toast.LENGTH_SHORT).show()
                }
            }

            is QRResult.QRUserCanceled -> Unit
            is QRResult.QRMissingPermission ->
                Toast.makeText(this, "Camera permission missing", Toast.LENGTH_SHORT).show()

            is QRResult.QRError ->
                Toast.makeText(this, "Scan error: ${result.exception.message}", Toast.LENGTH_SHORT)
                    .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!FirstTimeSetupActivity.isSetupComplete(this)) {
            startActivity(Intent(this, FirstTimeSetupActivity::class.java))
            finish()
            return
        }

        CurrencySettings.load(this)
        TimeSettings.load(this)
        SoundSettings.load(this)
        ItemTypeStore.ensureSeeded(this)
        MeasurementSettings.load(this)

        AppVersion.runMigrations(this)

        PresetStore.ensureDefaultsSeeded(this)

        setContentView(R.layout.activity_main)

        btnScan = findViewById(R.id.btnScan)
        btnSearchWeb = findViewById(R.id.btnSearchWeb)
        btnSettings = findViewById(R.id.btnSettings)
        btnExportHistory = findViewById(R.id.btnExportHistory)
        btnClearHistory = findViewById(R.id.btnClearHistory)
        listHistory = findViewById(R.id.listHistory)
        switchQuickScan = findViewById(R.id.switchQuickScan)
        btnSelectQuickPreset = findViewById(R.id.btnSelectQuickPreset)
        switchBeep = findViewById(R.id.switchBeep)

        btnClearHistory.setOnClickListener {
            confirmWipeHistory()
        }

        history.clear()
        history.addAll(HistoryStore.getAll(this))
        history.sortByDescending { it.timestamp }

        historyAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            history.map { formatHistoryRow(it) }
        )
        listHistory.adapter = historyAdapter

        listHistory.setOnItemClickListener { _, _, position, _ ->
            val item = history[position]
            showHistoryDetailDialog(item)
        }

        btnScan.setOnClickListener {
            if (quickScanMode && quickScanPreset == null) {
                showQuickScanPresetSelector()
            } else {
                scanLauncher.launch(scannerConfig)
            }
        }

        btnSearchWeb.setOnClickListener {
            val lastCode = history.firstOrNull()?.code
            if (lastCode.isNullOrBlank()) {
                Toast.makeText(this, "No scans yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val template = SearchSettings.loadTemplate(this)
            val encoded = lastCode.encodeUrlComponent()
            val url = String.format(template, encoded)
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnExportHistory.setOnClickListener {
            exportHistory()
        }

        switchQuickScan.setOnCheckedChangeListener { _, isChecked ->
            quickScanMode = isChecked
            btnSelectQuickPreset.visibility =
                if (isChecked) android.view.View.VISIBLE else android.view.View.GONE

            if (isChecked && quickScanPreset == null) {
                showQuickScanPresetSelector()
            }
        }

        btnSelectQuickPreset.setOnClickListener {
            showQuickScanPresetSelector()
        }

        switchBeep.isChecked = SoundSettings.beepEnabled
        switchBeep.setOnCheckedChangeListener { _, isChecked ->
            SoundSettings.setBeepEnabled(this, isChecked)
            if (isChecked) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator.release()
    }

    // ------------------ Helpers ------------------

    private fun String.encodeUrlComponent(): String =
        URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

    private fun formatTimestampHuman(ts: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ts

        val now = Calendar.getInstance()

        val sameDay = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

        val use24h = TimeSettings.use24Hour

        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val hh24 = hour.toString().padStart(2, '0')
        val mm = minute.toString().padStart(2, '0')

        val timePart = if (use24h) {
            "$hh24:$mm"
        } else {
            val amPm = if (hour < 12) "AM" else "PM"
            val h12Raw = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val h12 = h12Raw.toString()
            "$h12:$mm $amPm"
        }

        return if (sameDay) {
            timePart
        } else {
            val year = cal.get(Calendar.YEAR)
            val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            val day = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
            "$year.$month.$day $timePart"
        }
    }

    private fun formatHistoryRow(item: ScanHistoryItem): String {
        val status = if (item.sent) "Sent" else "Pending"

        val webhookName = item.webhookName
            ?: item.webhookUrl?.let { url ->
                WebhookConfigStore.getAll(this).firstOrNull { it.url == url }?.name
            }

        val label = when {
            !webhookName.isNullOrBlank() -> webhookName
            !item.presetName.isNullOrBlank() -> "preset: ${item.presetName}"
            !item.webhookUrl.isNullOrBlank() -> item.webhookUrl
            else -> "no webhook"
        }

        val tsText = formatTimestampHuman(item.timestamp)
        return "${item.code} - $status → $label  ($tsText)"
    }

    private fun refreshHistoryList() {
        history.sortByDescending { it.timestamp }
        historyAdapter.clear()
        historyAdapter.addAll(history.map { formatHistoryRow(it) })
        historyAdapter.notifyDataSetChanged()
    }

    private fun confirmWipeHistory() {
        if (history.isEmpty()) {
            Toast.makeText(this, "History is already empty", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Clear all history?")
            .setMessage("This will delete all scans from history. This cannot be undone.")
            .setPositiveButton("Delete all") { dialog, _ ->
                dialog.dismiss()
                HistoryStore.clear(this)
                history.clear()
                refreshHistoryList()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // ------------------ Scan flow ------------------

    private fun onScanCompleted(code: String) {
        val timestamp = System.currentTimeMillis()

        if (quickScanMode && quickScanPreset != null) {
            // Skip confirmation in quick scan mode
            handleQuickScan(code, timestamp)
        } else {
            showScanConfirmationDialog(code, timestamp)
        }
    }

    private fun showScanConfirmationDialog(code: String, timestamp: Long) {
        AlertDialog.Builder(this)
            .setTitle("Scanned Code")
            .setMessage("Code: $code\n\nIs this correct?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                showActionDialog(code, timestamp)
            }
            .setNegativeButton("Rescan") { dialog, _ ->
                dialog.dismiss()
                scanLauncher.launch(scannerConfig)
            }
            .show()
    }

    private fun handleQuickScan(code: String, timestamp: Long) {
        val preset = quickScanPreset ?: return

        // Build basic payload with placeholders replaced
        val bodyPayload = preset.bodyTemplate
            .replace("{{code}}", code)
            .replace("{{scanQuantity}}", "1")
            .replace("{{timestamp}}", System.currentTimeMillis().toString())

        // Send immediately
        WebhookClient.sendJson(
            url = preset.webhookUrl,
            jsonBody = bodyPayload,
            headers = mapOf("Content-Type" to "application/json")
        ) { success, message ->
            runOnUiThread {
                if (!success) {
                    Toast.makeText(this, "Error: $message", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "✓", Toast.LENGTH_SHORT).show()
                    // Immediately ready for next scan
                    scanLauncher.launch(scannerConfig)
                }
            }
        }

        val historyItem = ScanHistoryItem(
            code = code,
            timestamp = timestamp,
            presetId = preset.id,
            presetName = preset.name,
            webhookUrl = preset.webhookUrl,
            webhookName = null,
            sent = true,
            payload = bodyPayload
        )
        HistoryStore.add(this, historyItem)
        history.add(0, historyItem)
        refreshHistoryList()
    }

    private fun showQuickScanPresetSelector() {
        val presets = PresetStore.getAll(this)
        if (presets.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("No presets")
                .setMessage("Create a preset first in Settings → Manage Presets.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val labels = presets.map { it.name }.toTypedArray()
        val currentIndex = presets.indexOfFirst { it.id == quickScanPreset?.id }.coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Select Quick Scan Preset")
            .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
                quickScanPreset = presets[which]
                dialog.dismiss()
                Toast.makeText(this, "Quick scan: ${presets[which].name}", Toast.LENGTH_SHORT)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showActionDialog(code: String, timestamp: Long) {
        val actions = arrayOf("Log only", "Update SKU", "Create SKU")

        AlertDialog.Builder(this)
            .setTitle("What do you want to do?")
            .setItems(actions) { dialog, which ->
                dialog.dismiss()
                when (which) {
                    0 -> handleLogOnly(code, timestamp)
                    1, 2 -> showItemTypeDialog(code, timestamp)
                }
            }
            .show()
    }

    private fun handleLogOnly(code: String, timestamp: Long) {
        val historyItem = ScanHistoryItem(
            code = code,
            timestamp = timestamp,
            presetId = null,
            presetName = null,
            webhookUrl = null,
            webhookName = null,
            sent = false,
            payload = null
        )
        HistoryStore.add(this, historyItem)
        history.add(0, historyItem)
        refreshHistoryList()
        Toast.makeText(this, "Logged scan only", Toast.LENGTH_SHORT).show()
    }

    private fun showItemTypeDialog(code: String, timestamp: Long) {
        val types = ItemTypeStore.getAll(this)
        val typeNames = types.map { it.name }.toMutableList()
        typeNames.add("Create new type")

        AlertDialog.Builder(this)
            .setTitle("Select item type")
            .setItems(typeNames.toTypedArray()) { dialog, which ->
                dialog.dismiss()
                if (which == types.size) {
                    val intent = Intent(this, TypeListActivity::class.java)
                    startActivity(intent)
                    Toast.makeText(
                        this,
                        "Create your type, then scan again",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val selectedType = types[which]
                    showFieldPickerForType(code, timestamp, selectedType)
                }
            }
            .setNegativeButton("Back") { dialog, _ ->
                dialog.dismiss()
                onScanCompleted(code)
            }
            .show()
    }

    // ------------------ Field picker ------------------

    private fun showFieldPickerForType(
        code: String,
        timestamp: Long,
        type: ItemType
    ) {
        val fields = type.fields
        if (fields.isEmpty()) {
            Toast.makeText(this, "No fields in ${type.name}", Toast.LENGTH_SHORT).show()
            return
        }

        val labels = fields.map { it.label }.toTypedArray()
        val checked = BooleanArray(fields.size)

        AlertDialog.Builder(this)
            .setTitle("${type.name} fields")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("Next") { dialog, _ ->
                dialog.dismiss()
                val selected = fields.filterIndexed { idx, _ -> checked[idx] }
                if (selected.isEmpty()) {
                    Toast.makeText(this, "No parameters selected", Toast.LENGTH_SHORT).show()
                    showFieldPickerForType(code, timestamp, type)
                    return@setPositiveButton
                }
                showUniversalParameterDialog(code, timestamp, type, selected)
            }
            .setNegativeButton("Back") { dialog, _ ->
                dialog.dismiss()
                showItemTypeDialog(code, timestamp)
            }
            .setNeutralButton("Manage Types") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, TypeListActivity::class.java)
                intent.putExtra("focus_type_id", type.id)
                startActivity(intent)
            }
            .show()
    }

    private fun showUniversalParameterDialog(
        code: String,
        timestamp: Long,
        type: ItemType,
        selectedFields: List<ItemField>
    ) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }

        val container = FrameLayout(this).apply {
            val padding = 24
            setPadding(padding, padding, padding, padding)
            addView(
                scrollView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val textInputs = mutableMapOf<String, EditText>()

        val currencyInputsByKey = mutableMapOf<String, CurrencyInputs>()

        data class MeasurementInputs(
            val metricValue: EditText,
            val imperialValue: EditText,
            val isWeight: Boolean
        )

        val measurementInputsByKey = mutableMapOf<String, MeasurementInputs>()

        // Load existing data
        val existingGeneric = ItemStore.getAllGeneric(this)[code]
        val existingInv = if (type.id == "inventory") inventoryCache[code]
            ?: ItemStore.getAllInventory(this)[code] else null
        val existingPack = if (type.id == "packaging") packagingCache[code]
            ?: ItemStore.getAllPackaging(this)[code] else null

        fun getPrefillValue(field: ItemField): String? {
            return when (type.id) {
                "inventory" -> when (field.key) {
                    "itemName" -> existingInv?.itemName
                    "imageUrl" -> existingInv?.imageUrl
                    "category" -> existingInv?.category
                    "version" -> existingInv?.version
                    "group" -> existingInv?.group
                    "scanReason" -> existingInv?.scanReason
                    "storageLocations" -> existingInv?.storageLocations
                    "notes" -> existingInv?.notes
                    "quantityAdded" -> existingInv?.quantityAdded?.toString()
                    "quantityRemoved" -> existingInv?.quantityRemoved?.toString()
                    else -> null
                }

                "packaging" -> when (field.key) {
                    "item" -> existingPack?.item
                    "supplier" -> existingPack?.supplier
                    "scanReason" -> existingPack?.scanReason
                    "quantityPerUnit" -> existingPack?.quantityPerUnit?.toString()
                    "unitQuantityAdded" -> existingPack?.unitQuantityAdded?.toString()
                    "unitQuantityRemoved" -> existingPack?.unitQuantityRemoved?.toString()
                    "lastOrdered" -> existingPack?.lastOrdered
                    "supplierLink" -> existingPack?.supplierLink
                    "notes" -> existingPack?.notes
                    else -> null
                }

                else -> {
                    when (field.type) {
                        FieldType.STRING -> existingGeneric?.stringFields?.get(field.key)
                        FieldType.NUMBER -> existingGeneric?.numberFields?.get(field.key)
                            ?.toString()

                        FieldType.DATE_TIME -> existingGeneric?.dateTimeFields?.get(field.key)
                        FieldType.BOOLEAN -> existingGeneric?.booleanFields?.get(field.key)
                            ?.toString()

                        else -> null
                    }
                }
            }
        }

        fun getCurrencyPrefill(field: ItemField): CurrencyValue? {
            return when (type.id) {
                "inventory" -> existingInv?.currencyFields?.get(field.key)
                "packaging" -> existingPack?.currencyFields?.get(field.key)
                else -> existingGeneric?.currencyFields?.get(field.key)
            }
        }

        fun getMeasurementPrefill(field: ItemField): MeasurementValue? {
            // For now, shipment items would have measurement data
            // Extend as needed for other types
            return null // TODO: Implement when ShipmentItem storage is added
        }
        selectedFields.forEach { field ->
            when (field.type) {
                FieldType.CURRENCY -> {
                    val existing = getCurrencyPrefill(field)
                    val label = TextView(this).apply { text = field.label }
                    root.addView(label)

                    val localLabel = TextView(this).apply {
                        text = "Local (${CurrencySettings.localSymbol})"
                    }
                    val localEdit = EditText(this).apply {
                        hint = "e.g. 3000"
                        setText(existing?.local?.value?.toString() ?: "")
                        inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    }
                    root.addView(localLabel)
                    root.addView(localEdit)

                    val globalLabel = TextView(this).apply {
                        text = "Global (${CurrencySettings.globalSymbol})"
                    }
                    val globalEdit = EditText(this).apply {
                        hint = "e.g. 2.50"
                        setText(existing?.global?.value?.toString() ?: "")
                        inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    }
                    root.addView(globalLabel)
                    root.addView(globalEdit)

                    currencyInputsByKey[field.key] = CurrencyInputs(
                        localValue = localEdit,
                        globalValue = globalEdit
                    )
                }

                FieldType.MEASUREMENT_WEIGHT -> {
                    val existing = getMeasurementPrefill(field)
                    val label = TextView(this).apply { text = field.label }
                    root.addView(label)

                    val metricLabel = TextView(this).apply {
                        text = "Metric (${MeasurementSettings.weightMetricSymbol})"
                    }
                    val metricEdit = EditText(this).apply {
                        hint = "e.g. 2.5"
                        setText(existing?.metric?.value?.toString() ?: "")
                        inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    }
                    root.addView(metricLabel)
                    root.addView(metricEdit)

                    val imperialLabel = TextView(this).apply {
                        text = "Imperial (${MeasurementSettings.weightImperialSymbol})"
                    }
                    val imperialEdit = EditText(this).apply {
                        hint = "e.g. 5.5"
                        setText(existing?.imperial?.value?.toString() ?: "")
                        inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    }
                    root.addView(imperialLabel)
                    root.addView(imperialEdit)

                    measurementInputsByKey[field.key] = MeasurementInputs(
                        metricValue = metricEdit,
                        imperialValue = imperialEdit,
                        isWeight = true
                    )
                }

                FieldType.MEASUREMENT_DIMENSION -> {
                    val existing = getMeasurementPrefill(field)
                    val label = TextView(this).apply { text = field.label }
                    root.addView(label)

                    val metricLabel = TextView(this).apply {
                        text = "Metric (${MeasurementSettings.dimensionMetricSymbol})"
                    }
                    val metricEdit = EditText(this).apply {
                        hint = "e.g. 30.5"
                        setText(existing?.metric?.value?.toString() ?: "")
                        inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    }
                    root.addView(metricLabel)
                    root.addView(metricEdit)

                    val imperialLabel = TextView(this).apply {
                        text = "Imperial (${MeasurementSettings.dimensionImperialSymbol})"
                    }
                    val imperialEdit = EditText(this).apply {
                        hint = "e.g. 12"
                        setText(existing?.imperial?.value?.toString() ?: "")
                        inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    }
                    root.addView(imperialLabel)
                    root.addView(imperialEdit)

                    measurementInputsByKey[field.key] = MeasurementInputs(
                        metricValue = metricEdit,
                        imperialValue = imperialEdit,
                        isWeight = false
                    )
                }

                FieldType.DATE_TIME -> {
                    val label = TextView(this).apply { text = field.label }
                    val edit = EditText(this).apply {
                        isFocusable = false
                        hint = "Pick date and time (YYYY/MM/DD HH:MM)"
                        setText(getPrefillValue(field) ?: "")
                        setOnClickListener {
                            showDateTimePicker { dtStr ->
                                setText(dtStr)
                            }
                        }
                    }
                    root.addView(label)
                    root.addView(edit)
                    textInputs[field.key] = edit
                }

                FieldType.BOOLEAN -> {
                    val checkBox = CheckBox(this).apply {
                        text = field.label
                        isChecked = getPrefillValue(field)?.toBoolean() ?: false
                    }
                    root.addView(checkBox)
                    val hiddenEdit = EditText(this).apply {
                        visibility = android.view.View.GONE
                    }
                    textInputs[field.key] = hiddenEdit
                    checkBox.setOnCheckedChangeListener { _, isChecked ->
                        hiddenEdit.setText(isChecked.toString())
                    }
                    hiddenEdit.setText(checkBox.isChecked.toString())
                }

                FieldType.NUMBER -> {
                    val label = TextView(this).apply { text = field.label }
                    val edit = EditText(this).apply {
                        hint = "Number (e.g. 3000, 2.50)"
                        setText(getPrefillValue(field) ?: "")
                        inputType =
                            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
                    }
                    root.addView(label)
                    root.addView(edit)
                    textInputs[field.key] = edit
                }

                FieldType.STRING -> {
                    val label = TextView(this).apply { text = field.label }
                    val edit = EditText(this).apply {
                        hint = field.label
                        setText(getPrefillValue(field) ?: "")
                    }
                    root.addView(label)
                    root.addView(edit)
                    textInputs[field.key] = edit
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle("${type.name} details for $code")
            .setView(container)
            .setPositiveButton("Next") { dialog, _ ->
                dialog.dismiss()
                saveUniversalItem(
                    code,
                    timestamp,
                    type,
                    selectedFields,
                    textInputs,
                    currencyInputsByKey
                )
            }
            .setNegativeButton("Back") { dialog, _ ->
                dialog.dismiss()
                showFieldPickerForType(code, timestamp, type)
            }
            .show()
    }

    private fun saveUniversalItem(
        code: String,
        timestamp: Long,
        type: ItemType,
        selectedFields: List<ItemField>,
        textInputs: Map<String, EditText>,
        currencyInputs: Map<String, CurrencyInputs>
    ) {
        fun g(key: String): String = textInputs[key]?.text?.toString()?.trim().orEmpty()

        // Handle built-in types specially
        if (type.id == "inventory") {
            val existing = inventoryCache[code] ?: ItemStore.getAllInventory(this)[code]
            val baseCurrency = existing?.currencyFields ?: emptyMap()
            val newCurrency = baseCurrency.toMutableMap()

            for ((key, pair) in currencyInputs) {
                val localVal = pair.localValue.text.toString().trim()
                val globalVal = pair.globalValue.text.toString().trim()

                val localUnit = if (localVal.isNotEmpty()) {
                    CurrencyUnit(
                        value = localVal.toDoubleOrNull(),
                        currencyCode = CurrencySettings.localCode,
                        symbol = CurrencySettings.localSymbol
                    )
                } else null

                val globalUnit = if (globalVal.isNotEmpty()) {
                    CurrencyUnit(
                        value = globalVal.toDoubleOrNull(),
                        currencyCode = CurrencySettings.globalCode,
                        symbol = CurrencySettings.globalSymbol
                    )
                } else null

                if (localUnit != null || globalUnit != null) {
                    newCurrency[key] = CurrencyValue(local = localUnit, global = globalUnit)
                }
            }
            val item = InventoryItem(
                code = code,
                itemName = if (selectedFields.any { it.key == "itemName" }) g("itemName") else existing?.itemName,
                imageUrl = if (selectedFields.any { it.key == "imageUrl" }) g("imageUrl") else existing?.imageUrl,
                category = if (selectedFields.any { it.key == "category" }) g("category") else existing?.category,
                version = if (selectedFields.any { it.key == "version" }) g("version") else existing?.version,
                group = if (selectedFields.any { it.key == "group" }) g("group") else existing?.group,
                scanReason = if (selectedFields.any { it.key == "scanReason" }) g("scanReason") else existing?.scanReason,
                storageLocations = if (selectedFields.any { it.key == "storageLocations" }) g("storageLocations") else existing?.storageLocations,
                notes = if (selectedFields.any { it.key == "notes" }) g("notes") else existing?.notes,
                quantityAdded = if (selectedFields.any { it.key == "quantityAdded" }) g("quantityAdded").toIntOrNull()
                    ?: existing?.quantityAdded else existing?.quantityAdded,
                quantityRemoved = if (selectedFields.any { it.key == "quantityRemoved" }) g("quantityRemoved").toIntOrNull()
                    ?: existing?.quantityRemoved else existing?.quantityRemoved,
                currencyFields = newCurrency
            )

            inventoryCache[code] = item
            ItemStore.upsertInventory(this, item)

            val payload = buildInventoryPayload(code, 1, item, selectedFields)
            showSendMethodDialog(code, timestamp, payload) {
                showUniversalParameterDialog(code, timestamp, type, selectedFields)
            }
        } else if (type.id == "packaging") {
            val existing = packagingCache[code] ?: ItemStore.getAllPackaging(this)[code]
            val baseCurrency = existing?.currencyFields ?: emptyMap()
            val newCurrency = baseCurrency.toMutableMap()

            for ((key, pair) in currencyInputs) {
                val localVal = pair.localValue.text.toString().trim()
                val globalVal = pair.globalValue.text.toString().trim()

                val localUnit = if (localVal.isNotEmpty()) {
                    CurrencyUnit(
                        value = localVal.toDoubleOrNull(),
                        currencyCode = CurrencySettings.localCode,
                        symbol = CurrencySettings.localSymbol
                    )
                } else null

                val globalUnit = if (globalVal.isNotEmpty()) {
                    CurrencyUnit(
                        value = globalVal.toDoubleOrNull(),
                        currencyCode = CurrencySettings.globalCode,
                        symbol = CurrencySettings.globalSymbol
                    )
                } else null

                if (localUnit != null || globalUnit != null) {
                    newCurrency[key] = CurrencyValue(local = localUnit, global = globalUnit)
                }
            }

            val item = PackagingItem(
                code = code,
                item = if (selectedFields.any { it.key == "item" }) g("item") else existing?.item,
                supplier = if (selectedFields.any { it.key == "supplier" }) g("supplier") else existing?.supplier,
                scanReason = if (selectedFields.any { it.key == "scanReason" }) g("scanReason") else existing?.scanReason,
                quantityPerUnit = if (selectedFields.any { it.key == "quantityPerUnit" }) g("quantityPerUnit").toIntOrNull()
                    ?: existing?.quantityPerUnit else existing?.quantityPerUnit,
                unitQuantityAdded = if (selectedFields.any { it.key == "unitQuantityAdded" }) g("unitQuantityAdded").toIntOrNull()
                    ?: existing?.unitQuantityAdded else existing?.unitQuantityAdded,
                unitQuantityRemoved = if (selectedFields.any { it.key == "unitQuantityRemoved" }) g(
                    "unitQuantityRemoved"
                ).toIntOrNull() ?: existing?.unitQuantityRemoved else existing?.unitQuantityRemoved,
                lastOrdered = if (selectedFields.any { it.key == "lastOrdered" }) g("lastOrdered") else existing?.lastOrdered,
                supplierLink = if (selectedFields.any { it.key == "supplierLink" }) g("supplierLink") else existing?.supplierLink,
                notes = if (selectedFields.any { it.key == "notes" }) g("notes") else existing?.notes,
                currencyFields = newCurrency
            )

            packagingCache[code] = item
            ItemStore.upsertPackaging(this, item)

            val payload = buildPackagingPayload(code, 1, item, selectedFields)
            showSendMethodDialog(code, timestamp, payload) {
                showUniversalParameterDialog(code, timestamp, type, selectedFields)
            }
        } else {
            // Generic custom type
            val existing = ItemStore.getAllGeneric(this)[code]

            val stringFields = mutableMapOf<String, String>()
            val numberFields = mutableMapOf<String, Double>()
            val dateTimeFields = mutableMapOf<String, String>()
            val booleanFields = mutableMapOf<String, Boolean>()
            val newCurrency = mutableMapOf<String, CurrencyValue>()

            // Preserve existing data
            existing?.let {
                stringFields.putAll(it.stringFields)
                numberFields.putAll(it.numberFields)
                dateTimeFields.putAll(it.dateTimeFields)
                booleanFields.putAll(it.booleanFields)
                newCurrency.putAll(it.currencyFields)
            }

            // Update with new values
            selectedFields.forEach { field ->
                when (field.type) {
                    FieldType.STRING -> {
                        val value = g(field.key)
                        if (value.isNotBlank()) stringFields[field.key] = value
                    }
                    FieldType.NUMBER -> {
                        val value = g(field.key).toDoubleOrNull()
                        if (value != null) numberFields[field.key] = value
                    }
                    FieldType.DATE_TIME -> {
                        val value = g(field.key)
                        if (value.isNotBlank()) dateTimeFields[field.key] = value
                    }
                    FieldType.BOOLEAN -> {
                        val value = g(field.key).toBooleanStrictOrNull()
                        if (value != null) booleanFields[field.key] = value
                    }
                    FieldType.CURRENCY -> {
                        val pair = currencyInputs[field.key]
                        if (pair != null) {
                            val localVal = pair.localValue.text.toString().trim()
                            val globalVal = pair.globalValue.text.toString().trim()

                            val localUnit = if (localVal.isNotEmpty()) {
                                CurrencyUnit(
                                    value = localVal.toDoubleOrNull(),
                                    currencyCode = CurrencySettings.localCode,
                                    symbol = CurrencySettings.localSymbol
                                )
                            } else null

                            val globalUnit = if (globalVal.isNotEmpty()) {
                                CurrencyUnit(
                                    value = globalVal.toDoubleOrNull(),
                                    currencyCode = CurrencySettings.globalCode,
                                    symbol = CurrencySettings.globalSymbol
                                )
                            } else null

                            if (localUnit != null || globalUnit != null) {
                                newCurrency[field.key] = CurrencyValue(local = localUnit, global = globalUnit)
                            }
                        }
                    }
                    FieldType.MEASUREMENT_WEIGHT, FieldType.MEASUREMENT_DIMENSION -> {
                        // For now store as string until full measurement support added
                        val value = g(field.key)
                        if (value.isNotBlank()) stringFields[field.key] = value
                    }
                }
            }

            val item = GenericItem(
                code = code,
                typeId = type.id,
                stringFields = stringFields,
                numberFields = numberFields,
                dateTimeFields = dateTimeFields,
                booleanFields = booleanFields,
                currencyFields = newCurrency
            )

            ItemStore.upsertGeneric(this, item)

            val payload = buildGenericPayload(code, 1, item, type, selectedFields)
            showSendMethodDialog(code, timestamp, payload) {
                showUniversalParameterDialog(code, timestamp, type, selectedFields)
            }
        }
    }

    // ------------------ Payload builders ------------------

private fun buildInventoryPayload(
    code: String,
    scanQuantity: Int,
    item: InventoryItem?,
    selected: List<ItemField>
): String {
    val obj = JSONObject()
    obj.put("code", code)
    obj.put("scanQuantity", scanQuantity)
    obj.put("timestamp", System.currentTimeMillis())

    fun addIfSelected(key: String, value: Any?) {
        if (selected.any { it.key == key } && value != null) {
            obj.put(key, value)
        }
    }

    if (item != null) {
        addIfSelected("itemName", item.itemName)
        addIfSelected("imageUrl", item.imageUrl)
        addIfSelected("category", item.category)
        addIfSelected("version", item.version)
        addIfSelected("group", item.group)
        addIfSelected("scanReason", item.scanReason)
        addIfSelected("storageLocations", item.storageLocations)
        addIfSelected("notes", item.notes)
        addIfSelected("quantityAdded", item.quantityAdded)
        addIfSelected("quantityRemoved", item.quantityRemoved)

        item.currencyFields.forEach { (logicalKey, currencyValue) ->
            if (selected.any { it.key == logicalKey }) {
                putCurrencyField(obj, logicalKey, currencyValue)
            }
        }
    }

    return obj.toString()
}

private fun buildPackagingPayload(
    code: String,
    scanQuantity: Int,
    item: PackagingItem,
    selected: List<ItemField>
): String {
    val obj = JSONObject()
    obj.put("code", code)
    obj.put("scanQuantity", scanQuantity)
    obj.put("timestamp", System.currentTimeMillis())

    fun addIfSelected(key: String, value: Any?) {
        if (selected.any { it.key == key } && value != null) {
            obj.put(key, value)
        }
    }

    addIfSelected("item", item.item)
    addIfSelected("supplier", item.supplier)
    addIfSelected("scanReason", item.scanReason)
    addIfSelected("quantityPerUnit", item.quantityPerUnit)
    addIfSelected("unitQuantityAdded", item.unitQuantityAdded)
    addIfSelected("unitQuantityRemoved", item.unitQuantityRemoved)
    addIfSelected("lastOrdered", item.lastOrdered)
    addIfSelected("supplierLink", item.supplierLink)
    addIfSelected("notes", item.notes)

    item.currencyFields.forEach { (logicalKey, currencyValue) ->
        if (selected.any { it.key == logicalKey }) {
            putCurrencyField(obj, logicalKey, currencyValue)
        }
    }

    return obj.toString()
}

private fun buildGenericPayload(
    code: String,
    scanQuantity: Int,
    item: GenericItem,
    type: ItemType,
    selected: List<ItemField>
): String {
    val obj = JSONObject()
    obj.put("code", code)
    obj.put("scanQuantity", scanQuantity)
    obj.put("timestamp", System.currentTimeMillis())
    obj.put("itemType", type.name)

    selected.forEach { field ->
        when (field.type) {
            FieldType.STRING -> item.stringFields[field.key]?.let { obj.put(field.key, it) }
            FieldType.NUMBER -> item.numberFields[field.key]?.let { obj.put(field.key, it) }
            FieldType.DATE_TIME -> item.dateTimeFields[field.key]?.let { obj.put(field.key, it) }
            FieldType.BOOLEAN -> item.booleanFields[field.key]?.let { obj.put(field.key, it) }
            FieldType.CURRENCY -> item.currencyFields[field.key]?.let {
                putCurrencyField(obj, field.key, it)
            }

            FieldType.MEASUREMENT_WEIGHT -> {
                // Measurements stored in stringFields for now (or add to GenericItem)
                item.stringFields[field.key]?.let { obj.put(field.key, it) }
            }

            FieldType.MEASUREMENT_DIMENSION -> {
                // Measurements stored in stringFields for now (or add to GenericItem)
                item.stringFields[field.key]?.let { obj.put(field.key, it) }
            }
        }
    }

    return obj.toString()
}

private fun putCurrencyField(
    obj: JSONObject,
    fieldKey: String,
    value: CurrencyValue?
) {
    if (value == null) return
    if (value.local == null && value.global == null) return

    val fieldArray = JSONArray()
    val fieldObj = JSONObject()

    value.local?.let { local ->
        val localObj = JSONObject()
        localObj.put("localValue", local.value)
        localObj.put("localCurrency", local.currencyCode)
        localObj.put("localSymbol", local.symbol)
        fieldObj.put("localUnit", localObj)
    }

    value.global?.let { global ->
        val globalObj = JSONObject()
        globalObj.put("globalValue", global.value)
        globalObj.put("globalCurrency", global.currencyCode)
        globalObj.put("globalSymbol", global.symbol)
        fieldObj.put("globalUnit", globalObj)
    }

    if (fieldObj.length() > 0) {
        fieldArray.put(fieldObj)
        obj.put(fieldKey, fieldArray)
    }
}

private fun headersFromConfig(cfg: WebhookConfig?): Map<String, String> {
    if (cfg == null || cfg.headersJson.isNullOrBlank()) return emptyMap()
    return try {
        val obj = JSONObject(cfg.headersJson)
        val map = mutableMapOf<String, String>()
        obj.keys().forEach { key ->
            val v = obj.optString(key, null)
            if (v != null) map[key] = v
        }
        map
    } catch (_: Exception) {
        emptyMap()
    }
}

// ------------------ Send / history ------------------

private fun showSendMethodDialog(
    code: String,
    timestamp: Long,
    jsonBody: String,
    onBack: () -> Unit
) {
    val options = arrayOf("Send full payload", "Send with preset", "Send with previous config")

    AlertDialog.Builder(this)
        .setTitle("How do you want to send?")
        .setItems(options) { dialog, which ->
            dialog.dismiss()
            when (which) {
                0 -> {
                    showWebhookPicker { cfg ->
                        if (cfg != null) {
                            val url = cfg.url
                            if (url.isBlank()) {
                                Toast.makeText(
                                    this,
                                    "Webhook URL is empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val headers = headersFromConfig(cfg)
                                val bodyToSend = jsonBody

                                sendRawJson(url, bodyToSend, headers)
                                previousConfigUrl = url
                                previousConfigBody = bodyToSend
                                previousConfigHeaders = headers

                                val historyItem = ScanHistoryItem(
                                    code = code,
                                    timestamp = timestamp,
                                    presetId = null,
                                    presetName = null,
                                    webhookUrl = url,
                                    webhookName = cfg.name,
                                    sent = true,
                                    payload = bodyToSend
                                )
                                HistoryStore.add(this, historyItem)
                                history.add(0, historyItem)
                                refreshHistoryList()
                            }
                        }
                    }
                }

                1 -> {
                    showPresetPickerForCode(code) { preset ->
                        if (preset != null) {
                            showQuantityDialog { scanQuantity ->
                                if (scanQuantity != null) {
                                    val bodyPayload = preset.bodyTemplate
                                        .replace("{{code}}", code)
                                        .replace("{{scanQuantity}}", scanQuantity.toString())
                                        .replace(
                                            "{{timestamp}}",
                                            System.currentTimeMillis().toString()
                                        )

                                    WebhookClient.sendJson(
                                        url = preset.webhookUrl,
                                        jsonBody = bodyPayload,
                                        headers = mapOf("Content-Type" to "application/json")
                                    ) { success, message ->
                                        runOnUiThread {
                                            if (!success) {
                                                Toast.makeText(
                                                    this,
                                                    "Error: $message",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    this,
                                                    "Sent ${scanQuantity}x via ${preset.name}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }

                                    val historyItem = ScanHistoryItem(
                                        code = code,
                                        timestamp = timestamp,
                                        presetId = preset.id,
                                        presetName = preset.name,
                                        webhookUrl = preset.webhookUrl,
                                        webhookName = null,
                                        sent = true,
                                        payload = bodyPayload
                                    )
                                    HistoryStore.add(this, historyItem)
                                    history.add(0, historyItem)
                                    refreshHistoryList()
                                }
                            }
                        }
                    }
                }

                2 -> {
                    val url = previousConfigUrl
                    val bodyPayload = previousConfigBody
                    if (url.isNullOrBlank() || bodyPayload.isNullOrBlank()) {
                        Toast.makeText(
                            this,
                            "No previous config available",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        sendRawJson(url, bodyPayload, previousConfigHeaders)
                        val historyItem = ScanHistoryItem(
                            code = code,
                            timestamp = timestamp,
                            presetId = null,
                            presetName = "Previous config",
                            webhookUrl = url,
                            webhookName = null,
                            sent = true,
                            payload = bodyPayload
                        )
                        HistoryStore.add(this, historyItem)
                        history.add(0, historyItem)
                        refreshHistoryList()
                    }
                }
            }
        }
        .setNegativeButton("Back") { dialog, _ ->
            dialog.dismiss()
            onBack()
        }
        .show()
}

private fun showWebhookPicker(onChosen: (WebhookConfig?) -> Unit) {
    val configs = WebhookConfigStore.getAll(this)
    if (configs.isEmpty()) {
        AlertDialog.Builder(this)
            .setTitle("No webhooks configured")
            .setMessage("Create a webhook first in Settings → Manage Webhooks.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onChosen(null)
            }
            .show()
        return
    }

    val labels = configs.map { it.name }.toMutableList()
    labels.add("Create new webhook")

    AlertDialog.Builder(this)
        .setTitle("Select webhook")
        .setItems(labels.toTypedArray()) { dialog, which ->
            dialog.dismiss()
            if (which == configs.size) {
                showEditWebhookDialog(null)
                onChosen(null)
            } else {
                onChosen(configs[which])
            }
        }
        .setNegativeButton("Back") { dialog, _ ->
            dialog.dismiss()
            onChosen(null)
        }
        .show()
}

private fun showQuantityDialog(onDone: (Int?) -> Unit) {
    val input = EditText(this).apply {
        hint = "Quantity"
        inputType = InputType.TYPE_CLASS_NUMBER
        setText("1")
        setSelection(text.length)
    }

    AlertDialog.Builder(this)
        .setTitle("Quantity")
        .setView(input)
        .setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
            val value = input.text.toString().trim()
            val qty = value.toIntOrNull()
            if (qty == null || qty <= 0) {
                Toast.makeText(this, "Invalid quantity", Toast.LENGTH_SHORT).show()
                onDone(null)
            } else {
                onDone(qty)
            }
        }
        .setNegativeButton("Back") { dialog, _ ->
            dialog.dismiss()
            onDone(null)
        }
        .show()
}

private fun showPresetPickerForCode(
    code: String,
    onChosen: (Preset?) -> Unit
) {
    val presets = PresetStore.getAll(this)
    if (presets.isEmpty()) {
        AlertDialog.Builder(this)
            .setTitle("No presets")
            .setMessage("Create a preset first in Settings → Manage Presets.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onChosen(null)
            }
            .show()
        return
    }

    val labels = presets.map { it.name }.toTypedArray()

    AlertDialog.Builder(this)
        .setTitle("Select preset for $code")
        .setItems(labels) { dialog, which ->
            dialog.dismiss()
            onChosen(presets[which])
        }
        .setNegativeButton("Back") { dialog, _ ->
            dialog.dismiss()
            onChosen(null)
        }
        .show()
}

private fun sendRawJson(
    url: String,
    jsonBody: String,
    extraHeaders: Map<String, String> = emptyMap()
) {
    WebhookClient.sendJson(
        url = url,
        jsonBody = jsonBody,
        headers = extraHeaders
    ) { success, message ->
        runOnUiThread {
            if (success) {
                Toast.makeText(this, "Sent to webhook", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private fun sendWithPresetTemplate(
    code: String,
    preset: Preset,
    scanQuantity: Int,
    fromHistory: Boolean
) {
    val body = preset.bodyTemplate
        .replace("{{code}}", code)
        .replace("{{scanQuantity}}", scanQuantity.toString())

    WebhookClient.sendJson(
        url = preset.webhookUrl,
        jsonBody = body,
        headers = mapOf("Content-Type" to "application/json")
    ) { success, message ->
        runOnUiThread {
            if (!success) {
                Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
            } else if (!fromHistory) {
                Toast.makeText(
                    this,
                    "Sent ${scanQuantity}x via ${preset.name}",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Resent via ${preset.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

private fun showHistoryDetailDialog(item: ScanHistoryItem) {
    val status = if (item.sent) "Sent" else "Pending"

    val webhookName = item.webhookName
        ?: item.webhookUrl?.let { url ->
            WebhookConfigStore.getAll(this).firstOrNull { it.url == url }?.name
        }

    val namePart = webhookName ?: item.presetName ?: "none"
    val urlPart = item.webhookUrl ?: "none"
    val tsText = formatTimestampHuman(item.timestamp)

    val view = layoutInflater.inflate(R.layout.dialog_scan_details, null)
    view.findViewById<TextView>(R.id.txtCode).text = item.code
    view.findViewById<TextView>(R.id.txtStatus).text = "Status: $status"
    view.findViewById<TextView>(R.id.txtTimestamp).text = "Scanned: $tsText"
    view.findViewById<TextView>(R.id.txtWebhook).text = "Webhook/Preset: $namePart"
    view.findViewById<TextView>(R.id.txtUrl).text = urlPart

    AlertDialog.Builder(this)
        .setTitle("Scan details")
        .setView(view)
        .setPositiveButton("More") { d, _ ->
            d.dismiss()
            showMoreOptionsDialog(item)
        }
        .setNeutralButton("View payload") { d, _ ->
            d.dismiss()
            showPayloadForHistory(item)
        }
        .setNegativeButton("Share") { d, _ ->
            d.dismiss()
            showShareItemDialog(item)
        }
        .show()
}

private fun showMoreOptionsDialog(item: ScanHistoryItem) {
    val options = arrayOf("Resend with preset", "Delete")

    AlertDialog.Builder(this)
        .setTitle("More options")
        .setItems(options) { dialog, which ->
            dialog.dismiss()
            when (which) {
                0 -> {
                    showPresetPickerForCode(item.code) { preset ->
                        if (preset != null) {
                            sendWithPresetTemplate(
                                code = item.code,
                                preset = preset,
                                scanQuantity = 1,
                                fromHistory = true
                            )
                        }
                    }
                }

                1 -> confirmDeleteHistoryItem(item)
            }
        }
        .setPositiveButton("Back") { dialog, _ ->
            dialog.dismiss()
            showHistoryDetailDialog(item)
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            showHistoryDetailDialog(item)
        }
        .show()
}

private fun confirmDeleteHistoryItem(item: ScanHistoryItem) {
    AlertDialog.Builder(this)
        .setTitle("Delete this scan?")
        .setMessage("This will remove this scan from history. This cannot be undone.")
        .setPositiveButton("Delete") { dialog, _ ->
            dialog.dismiss()
            HistoryStore.remove(this, item)
            history.remove(item)
            refreshHistoryList()
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
            showHistoryDetailDialog(item)
        }
        .show()
}

private fun showShareItemDialog(item: ScanHistoryItem) {
    val options = arrayOf("Share as text", "Share as CSV", "Share payload JSON")

    AlertDialog.Builder(this)
        .setTitle("Share scan")
        .setItems(options) { dialog, which ->
            dialog.dismiss()
            when (which) {
                0 -> shareHistoryItemFile(item, asCsv = false)
                1 -> shareHistoryItemFile(item, asCsv = true)
                2 -> {
                    val payload = item.payload ?: run {
                        val code = item.code
                        val inv = inventoryCache[code]
                        val pack = packagingCache[code]

                        when {
                            inv != null -> buildInventoryPayload(
                                code = code,
                                scanQuantity = 1,
                                item = inv,
                                selected = ItemTypeStore.getAll(this)
                                    .firstOrNull { it.id == "inventory" }
                                    ?.fields
                                    ?: emptyList()
                            )

                            pack != null -> buildPackagingPayload(
                                code = code,
                                scanQuantity = 1,
                                item = pack,
                                selected = ItemTypeStore.getAll(this)
                                    .firstOrNull { it.id == "packaging" }
                                    ?.fields
                                    ?: emptyList()
                            )

                            else -> "{ \"code\": \"${item.code}\", \"info\": \"No payload available\" }"
                        }
                    }
                    sharePayload(item, payload)
                }
            }
        }
        .setNegativeButton("Back") { dialog, _ ->
            dialog.dismiss()
            showHistoryDetailDialog(item)
        }
        .show()
}

private fun showPayloadForHistory(item: ScanHistoryItem) {
    val payload = item.payload ?: run {
        val code = item.code
        val inv = inventoryCache[code]
        val pack = packagingCache[code]

        when {
            inv != null -> buildInventoryPayload(
                code = code,
                scanQuantity = 1,
                item = inv,
                selected = ItemTypeStore.getAll(this)
                    .firstOrNull { it.id == "inventory" }
                    ?.fields
                    ?: emptyList()
            )

            pack != null -> buildPackagingPayload(
                code = code,
                scanQuantity = 1,
                item = pack,
                selected = ItemTypeStore.getAll(this)
                    .firstOrNull { it.id == "packaging" }
                    ?.fields
                    ?: emptyList()
            )

            else -> "{ \"code\": \"$code\", \"info\": \"No payload available\" }"
        }
    }

    val scrollView = ScrollView(this)
    val textView = TextView(this).apply {
        text = payload
        setPadding(48, 24, 48, 24)
        textSize = 12f
        setTextIsSelectable(true)
    }
    scrollView.addView(textView)

    AlertDialog.Builder(this)
        .setTitle("Payload for ${item.code}")
        .setView(scrollView)
        .setPositiveButton("Back") { dialog, _ ->
            dialog.dismiss()
            showHistoryDetailDialog(item)
        }
        .setNeutralButton("Share") { dialog, _ ->
            dialog.dismiss()
            sharePayload(item, payload)
        }
        .setNegativeButton("Save as preset") { dialog, _ ->
            dialog.dismiss()
            showSavePresetFromPayloadDialog(item, payload)
        }
        .show()
}

// ------------------ Full history export ------------------

private fun exportHistory() {
    if (history.isEmpty()) {
        Toast.makeText(this, "No history to export", Toast.LENGTH_SHORT).show()
        return
    }

    val sb = StringBuilder()
    sb.append("code,timestamp,formattedTimestamp,presetId,presetName,webhookUrl,webhookName,sent,payload\n")
    history.forEach { h ->
        fun esc(s: String?) = (s ?: "").replace("\"", "\"\"")
        val code = esc(h.code)
        val presetName = esc(h.presetName)
        val webhookUrl = esc(h.webhookUrl)
        val webhookName = esc(
            h.webhookName
                ?: h.webhookUrl?.let { url ->
                    WebhookConfigStore.getAll(this).firstOrNull { it.url == url }?.name
                }
        )
        val formattedTs = esc(formatTimestampHuman(h.timestamp))
        val payload = esc(h.payload)
        sb.append(
            "\"$code\",${h.timestamp},\"$formattedTs\",${h.presetId ?: ""},\"$presetName\",\"$webhookUrl\",\"$webhookName\",${h.sent},\"$payload\"\n"
        )
    }

    val csvBytes = sb.toString().toByteArray(Charsets.UTF_8)
    val fileName = "scan_history_${System.currentTimeMillis()}.csv"
    val file = File(cacheDir, fileName)
    file.outputStream().use { it.write(csvBytes) }

    val uri = FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Scan history export")
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "Share history"))
}

// ------------------ Per-item export helpers ------------------

private fun buildHistoryItemText(item: ScanHistoryItem): String {
    val status = if (item.sent) "Sent" else "Pending"
    val tsText = formatTimestampHuman(item.timestamp)
    val webhookName = item.webhookName
        ?: item.webhookUrl?.let { url ->
            WebhookConfigStore.getAll(this).firstOrNull { it.url == url }?.name
        }

    return buildString {
        appendLine("Code: ${item.code}")
        appendLine("Status: $status")
        appendLine("Scanned: $tsText")
        appendLine("Preset ID: ${item.presetId ?: "none"}")
        appendLine("Preset name: ${item.presetName ?: "none"}")
        appendLine("Webhook name: ${webhookName ?: "none"}")
        appendLine("Webhook URL: ${item.webhookUrl ?: "none"}")
        appendLine("Sent: ${item.sent}")
        if (item.payload != null) {
            appendLine()
            appendLine("Payload:")
            appendLine(item.payload)
        }
    }
}

private fun buildHistoryItemCsv(item: ScanHistoryItem): String {
    fun esc(s: String?) = (s ?: "").replace("\"", "\"\"")
    val code = esc(item.code)
    val presetName = esc(item.presetName)
    val webhookUrl = esc(item.webhookUrl)
    val webhookName = esc(
        item.webhookName
            ?: item.webhookUrl?.let { url ->
                WebhookConfigStore.getAll(this).firstOrNull { it.url == url }?.name
            }
    )
    val formattedTs = esc(formatTimestampHuman(item.timestamp))
    val payload = esc(item.payload)

    val header =
        "code,timestamp,formattedTimestamp,presetId,presetName,webhookUrl,webhookName,sent,payload\n"
    val row =
        "\"$code\",${item.timestamp},\"$formattedTs\",${item.presetId ?: ""},\"$presetName\",\"$webhookUrl\",\"$webhookName\",${item.sent},\"$payload\"\n"

    return header + row
}

private fun shareHistoryItemFile(item: ScanHistoryItem, asCsv: Boolean) {
    val content = if (asCsv) {
        buildHistoryItemCsv(item)
    } else {
        buildHistoryItemText(item)
    }

    val ext = if (asCsv) "csv" else "txt"
    val mimeType = if (asCsv) "text/csv" else "text/plain"
    val cleanCode = item.code.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val fileName = "scan_${cleanCode}_${item.timestamp}.$ext"

    val file = File(cacheDir, fileName)
    file.outputStream().use { it.write(content.toByteArray(Charsets.UTF_8)) }

    val uri = FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_SUBJECT, "Scan export: ${item.code}")
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "Share scan"))
}

private fun sharePayload(item: ScanHistoryItem, payload: String) {
    val cleanCode = item.code.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val fileName = "payload_${cleanCode}_${item.timestamp}.json"

    val file = File(cacheDir, fileName)
    file.outputStream().use { it.write(payload.toByteArray(Charsets.UTF_8)) }

    val uri = FileProvider.getUriForFile(
        this,
        "${packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_SUBJECT, "Payload for ${item.code}")
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "Share payload"))
}

private fun showSavePresetFromPayloadDialog(item: ScanHistoryItem, payload: String) {
    val obj = try {
        JSONObject(payload)
    } catch (_: Exception) {
        Toast.makeText(this, "Payload is not valid JSON", Toast.LENGTH_SHORT).show()
        return
    }

    val keys = obj.keys().asSequence().toList().sorted()
    if (keys.isEmpty()) {
        Toast.makeText(this, "No fields to save", Toast.LENGTH_SHORT).show()
        return
    }

    val labels = keys.toTypedArray()

    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(48, 24, 48, 24)
    }

    val listView = ListView(this).apply {
        adapter = ArrayAdapter(
            this@MainActivity,
            android.R.layout.simple_list_item_multiple_choice,
            labels
        )
        choiceMode = ListView.CHOICE_MODE_MULTIPLE
        for (i in keys.indices) {
            setItemChecked(i, true)
        }
    }

    val includeValuesCheck = CheckBox(this).apply {
        text = "Include current values (otherwise use {{placeholders}})"
        isChecked = true
    }

    root.addView(listView)
    root.addView(includeValuesCheck)

    AlertDialog.Builder(this)
        .setTitle("Save as preset")
        .setView(root)
        .setPositiveButton("Next") { dialog, _ ->
            dialog.dismiss()
            val selectedKeys =
                keys.filterIndexed { index, _ -> listView.isItemChecked(index) }
            if (selectedKeys.isEmpty()) {
                Toast.makeText(this, "No fields selected", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val includeValues = includeValuesCheck.isChecked
            val templateObj = JSONObject()
            selectedKeys.forEach { key ->
                val value = obj.opt(key)
                if (includeValues && value != null && value !is JSONObject && value !is JSONArray) {
                    templateObj.put(key, value)
                } else {
                    val placeholder = when (key) {
                        "code" -> "{{code}}"
                        "scanQuantity" -> "{{scanQuantity}}"
                        "timestamp" -> "{{timestamp}}"
                        else -> "{{${key}}}"
                    }
                    templateObj.put(key, placeholder)
                }
            }

            showSavePresetMetaDialog(templateObj.toString())
        }
        .setNegativeButton("Back") { dialog, _ ->
            dialog.dismiss()
            showPayloadForHistory(item)
        }
        .show()
}

private fun showSavePresetMetaDialog(bodyTemplate: String) {
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(48, 24, 48, 24)
    }

    val nameInput = EditText(this).apply {
        hint = "Preset name (e.g. Default inventory update)"
    }
    val descInput = EditText(this).apply {
        hint = "Optional description"
    }
    val webhookSpinner = Spinner(this)
    val configs = WebhookConfigStore.getAll(this)
    val labels = configs.map { it.name }.toTypedArray()

    webhookSpinner.adapter = ArrayAdapter(
        this,
        android.R.layout.simple_spinner_item,
        labels
    ).apply {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    root.addView(TextView(this).apply { text = "Preset name" })
    root.addView(nameInput)
    root.addView(TextView(this).apply { text = "Description" })
    root.addView(descInput)
    root.addView(TextView(this).apply { text = "Webhook" })
    root.addView(webhookSpinner)

    AlertDialog.Builder(this)
        .setTitle("Preset details")
        .setView(root)
        .setPositiveButton("Save") { dialog, _ ->
            dialog.dismiss()
            val name = nameInput.text.toString().trim()
            val description = descInput.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(this, "Preset name is required", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (configs.isEmpty()) {
                Toast.makeText(this, "No webhook configured", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            val cfg = configs[webhookSpinner.selectedItemPosition]

            val preset = Preset(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                description = description,
                webhookUrl = cfg.url,
                bodyTemplate = bodyTemplate
            )

            PresetStore.add(this, preset)
            Toast.makeText(this, "Preset saved", Toast.LENGTH_SHORT).show()
        }
        .setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
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

    container.addView(TextView(this).apply { text = "Webhook name" })
    container.addView(nameInput)
    container.addView(TextView(this).apply { text = "Webhook URL" })
    container.addView(urlInput)

    val title = if (existing == null) "New webhook" else "Edit webhook"

    AlertDialog.Builder(this)
        .setTitle(title)
        .setView(container)
        .setPositiveButton("Save") { dialog, _ ->
            dialog.dismiss()
            val name = nameInput.text.toString().trim()
            val url = urlInput.text.toString().trim()
            if (name.isBlank() || url.isBlank()) {
                Toast.makeText(this, "Name and URL required", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val wasEmpty = WebhookConfigStore.getAll(this).isEmpty()

            if (existing == null) {
                val cfg = WebhookConfig(
                    id = System.currentTimeMillis().toString(),
                    name = name,
                    url = url
                )
                WebhookConfigStore.add(this, cfg)
            } else {
                val updated = existing.copy(name = name, url = url)
                val all = WebhookConfigStore.getAll(this).toMutableList()
                val idx = all.indexOfFirst { it.id == existing.id }
                if (idx >= 0) {
                    all[idx] = updated
                    WebhookConfigStore.saveAll(this, all)
                }
            }

            // Trigger preset seeding if this was the first webhook
            if (wasEmpty) {
                PresetStore.ensureDefaultsSeeded(this)
                Toast.makeText(this, "Created default presets!", Toast.LENGTH_SHORT).show()
            }
        }
        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        .show()
}  // ADD THIS CLOSING BRACE - closes showEditWebhookDialog

private fun showDateTimePicker(onDateTimeChosen: (String) -> Unit) {
    val c = Calendar.getInstance()
    val year = c.get(Calendar.YEAR)
    val month = c.get(Calendar.MONTH)
    val day = c.get(Calendar.DAY_OF_MONTH)
    var selectedYear = year
    var selectedMonth = month
    var selectedDay = day

    DatePickerDialog(
        this,
        { _, y, m, d ->
            selectedYear = y
            selectedMonth = m
            selectedDay = d

            android.app.TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    val hh = hourOfDay.toString().padStart(2, '0')
                    val mm = minute.toString().padStart(2, '0')
                    val monthStr = (selectedMonth + 1).toString().padStart(2, '0')
                    val dayStr = selectedDay.toString().padStart(2, '0')
                    val result = "$selectedYear-$monthStr-$dayStr $hh:$mm"
                    onDateTimeChosen(result)
                },
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE),
                TimeSettings.use24Hour
            ).show()
        },
        year,
        month,
        day
    ).show()
}
}