package com.example.hebarcodescanner

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class TypeListActivity : AppCompatActivity() {

    private lateinit var listTypes: ListView
    private lateinit var btnNewType: Button

    private val types = mutableListOf<ItemType>()
    private lateinit var adapter: ArrayAdapter<String>
    private var selectedType: ItemType? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ItemTypeStore.ensureSeeded(this)

        setContentView(R.layout.activity_type_list)

        btnNewType = findViewById(R.id.btnNewType)
        listTypes = findViewById(R.id.listTypes)

        btnNewType.setOnClickListener {
            showAddTypeDialog()
        }

        refreshTypes()

        val focusId = intent.getStringExtra("focus_type_id")
        if (!focusId.isNullOrBlank()) {
            val idx = types.indexOfFirst { it.id == focusId }
            if (idx >= 0) {
                listTypes.post {
                    listTypes.setSelection(idx)
                    selectedType = types[idx]
                    showTypeDetailDialog(types[idx])
                }
            }
        }

        listTypes.setOnItemClickListener { _, _, position, _ ->
            val type = types[position]
            selectedType = type
            showTypeDetailDialog(type)
        }

        listTypes.setOnItemLongClickListener { _, _, position, _ ->
            val type = types[position]
            AlertDialog.Builder(this)
                .setTitle("Delete type")
                .setMessage("Delete \"${type.name}\"?")
                .setPositiveButton("Delete") { _, _ ->
                    ItemTypeStore.delete(this, type.id)
                    refreshTypes()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 1, "Add field")
        menu.add(0, 2, 2, "Remove fields")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val type = selectedType
        return when (item.itemId) {
            1 -> {
                if (type == null) {
                    Toast.makeText(this, "Tap a type first", Toast.LENGTH_SHORT).show()
                } else {
                    showAddFieldDialog(type)
                }
                true
            }
            2 -> {
                if (type == null) {
                    Toast.makeText(this, "Tap a type first", Toast.LENGTH_SHORT).show()
                } else {
                    showRemoveFieldsDialog(type)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshTypes() {
        types.clear()
        types.addAll(ItemTypeStore.getAll(this))
        val names = types.map { type ->
            if (type.name.isBlank()) type.id else type.name
        }
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        listTypes.adapter = adapter
        selectedType = null
    }

    private fun showTypeDetailDialog(type: ItemType) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 0, 24, 24)
        }

        val labels = if (type.fields.isEmpty()) {
            listOf("(no fields yet)")
        } else {
            type.fields.map { f ->
                val req = if (f.required) " (required)" else ""
                "${f.label} [${f.key}] : ${f.type}$req"
            }
        }

        val listView = ListView(this).apply {
            adapter = ArrayAdapter(
                this@TypeListActivity,
                android.R.layout.simple_list_item_1,
                labels
            )
            setOnItemClickListener { _, _, position, _ ->
                if (type.fields.isEmpty()) return@setOnItemClickListener
                val field = type.fields[position]
                showEditFieldDialog(type, field)
            }
        }

        root.addView(listView)

        val titleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(48, 32, 24, 16)
        }
        val titleView = TextView(this).apply {
            text = type.name
            textSize = 20f
            setPadding(0, 0, 24, 0)
        }
        val btnNewField = Button(this).apply {
            text = "NEW FIELD"
            setOnClickListener {
                showAddFieldDialog(type)
            }
        }
        titleLayout.addView(
            titleView,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )
        titleLayout.addView(btnNewField)

        AlertDialog.Builder(this)
            .setCustomTitle(titleLayout)
            .setView(root)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showAddTypeDialog() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val nameInput = EditText(this).apply {
            hint = "Type name (e.g. Inventory)"
        }
        val idInput = EditText(this).apply {
            hint = "Type id (e.g. inventory)"
        }

        root.addView(TextView(this).apply { text = "Name" })
        root.addView(nameInput)
        root.addView(TextView(this).apply { text = "Id" })
        root.addView(idInput)

        AlertDialog.Builder(this)
            .setTitle("Add type")
            .setView(root)
            .setPositiveButton("Add") { dialog, _ ->
                dialog.dismiss()
                val name = nameInput.text.toString().trim()
                val id = idInput.text.toString().trim()
                if (name.isBlank() || id.isBlank()) {
                    Toast.makeText(this, "Name and id are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newType = ItemType(
                    id = id,
                    name = name,
                    fields = emptyList()
                )
                ItemTypeStore.addOrUpdate(this, newType)
                refreshTypes()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun showAddFieldDialog(type: ItemType) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val keyInput = EditText(this).apply {
            hint = "Key (e.g. costPerUnitLocal)"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val labelInput = EditText(this).apply {
            hint = "Label (e.g. Cost Per Unit)"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val typeSpinner = Spinner(this)
        val fieldTypes = FieldType.entries.toTypedArray()
        typeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            fieldTypes.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val requiredCheck = CheckBox(this).apply {
            text = "Required"
        }

        root.addView(TextView(this).apply { text = "Field key" })
        root.addView(keyInput)
        root.addView(TextView(this).apply { text = "Field label" })
        root.addView(labelInput)
        root.addView(TextView(this).apply { text = "Field type" })
        root.addView(typeSpinner)
        root.addView(requiredCheck)

        AlertDialog.Builder(this)
            .setTitle("Add field to ${type.name}")
            .setView(root)
            .setPositiveButton("Add") { dialog, _ ->
                dialog.dismiss()
                val key = keyInput.text.toString().trim()
                val label = labelInput.text.toString().trim()
                if (key.isBlank()) {
                    Toast.makeText(this, "Key is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val chosenType = fieldTypes[typeSpinner.selectedItemPosition]
                val newField = ItemField(
                    key = key,
                    label = label.ifBlank { key },
                    type = chosenType,
                    required = requiredCheck.isChecked
                )

                val updated = type.copy(fields = type.fields + newField)
                ItemTypeStore.addOrUpdate(this, updated)
                refreshTypes()

                val idx = types.indexOfFirst { it.id == updated.id }
                if (idx >= 0) {
                    selectedType = types[idx]
                    showTypeDetailDialog(types[idx])
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun showEditFieldDialog(type: ItemType, field: ItemField) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val keyInput = EditText(this).apply {
            hint = "Key"
            setText(field.key)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val labelInput = EditText(this).apply {
            hint = "Label"
            setText(field.label)
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val typeSpinner = Spinner(this)
        val fieldTypes = FieldType.entries.toTypedArray()
        typeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            fieldTypes.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val initialIndex = fieldTypes.indexOfFirst { it == field.type }.coerceAtLeast(0)
        typeSpinner.setSelection(initialIndex)

        val requiredCheck = CheckBox(this).apply {
            text = "Required"
            isChecked = field.required
        }

        root.addView(TextView(this).apply { text = "Field key" })
        root.addView(keyInput)
        root.addView(TextView(this).apply { text = "Field label" })
        root.addView(labelInput)
        root.addView(TextView(this).apply { text = "Field type" })
        root.addView(typeSpinner)
        root.addView(requiredCheck)

        AlertDialog.Builder(this)
            .setTitle("Edit field in ${type.name}")
            .setView(root)
            .setPositiveButton("Save") { dialog, _ ->
                dialog.dismiss()
                val newKey = keyInput.text.toString().trim()
                val newLabel = labelInput.text.toString().trim()
                if (newKey.isBlank()) {
                    Toast.makeText(this, "Key is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val chosenType = fieldTypes[typeSpinner.selectedItemPosition]
                val updatedField = field.copy(
                    key = newKey,
                    label = newLabel.ifBlank { newKey },
                    type = chosenType,
                    required = requiredCheck.isChecked
                )

                val updatedFields = type.fields.map {
                    if (it.key == field.key) updatedField else it
                }

                val updatedType = type.copy(fields = updatedFields)
                ItemTypeStore.addOrUpdate(this, updatedType)
                refreshTypes()

                val idx = types.indexOfFirst { it.id == updatedType.id }
                if (idx >= 0) {
                    selectedType = types[idx]
                    showTypeDetailDialog(types[idx])
                }

                val refreshed = types.firstOrNull { it.id == updatedType.id } ?: updatedType
                selectedType = refreshed
                showTypeDetailDialog(refreshed)

            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveFieldsDialog(type: ItemType) {
        if (type.fields.isEmpty()) {
            Toast.makeText(this, "No fields to remove", Toast.LENGTH_SHORT).show()
            return
        }

        val labels = type.fields.map { "${it.label} [${it.key}]" }.toTypedArray()
        val checked = BooleanArray(type.fields.size)

        AlertDialog.Builder(this)
            .setTitle("Remove fields from ${type.name}")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("Remove") { dialog, _ ->
                dialog.dismiss()
                val toRemoveKeys = type.fields
                    .mapIndexedNotNull { idx, field -> if (checked[idx]) field.key else null }
                    .toSet()
                if (toRemoveKeys.isEmpty()) return@setPositiveButton

                val remaining = type.fields.filterNot { it.key in toRemoveKeys }
                val updated = type.copy(fields = remaining)
                ItemTypeStore.addOrUpdate(this, updated)
                refreshTypes()

                val idx = types.indexOfFirst { it.id == updated.id }
                if (idx >= 0) {
                    selectedType = types[idx]
                    showTypeDetailDialog(types[idx])
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
