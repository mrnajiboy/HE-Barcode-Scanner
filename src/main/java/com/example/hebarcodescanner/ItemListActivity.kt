package com.example.hebarcodescanner

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ItemListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var btnAdd: Button
    private lateinit var spinnerType: Spinner

    private val inventoryItems = mutableListOf<InventoryItem>()
    private val packagingItems = mutableListOf<PackagingItem>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_list)

        listView = findViewById(R.id.listItems)
        btnAdd = findViewById(R.id.btnAddItem)
        spinnerType = findViewById(R.id.spinnerItemType)

        val types = listOf("Inventory", "Packaging")
        spinnerType.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            types
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        loadItems()

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                refreshList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnAdd.setOnClickListener {
            val type = spinnerType.selectedItem as String
            if (type == "Inventory") {
                showNewInventoryItemDialog()
            } else {
                showNewPackagingItemDialog()
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            val type = spinnerType.selectedItem as String
            if (type == "Inventory") {
                val item = inventoryItems[position]
                openItemDetail(item.code, "inventory")
            } else {
                val item = packagingItems[position]
                openItemDetail(item.code, "packaging")
            }
        }

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        loadItems()
        refreshList()
    }

    private fun loadItems() {
        inventoryItems.clear()
        inventoryItems.addAll(ItemStore.getAllInventory(this).values)

        packagingItems.clear()
        packagingItems.addAll(ItemStore.getAllPackaging(this).values)
    }

    private fun refreshList() {
        val labels = mutableListOf<String>()
        val type = spinnerType.selectedItem as String
        if (type == "Inventory") {
            labels.addAll(inventoryItems.map { "${it.code} – ${it.itemName ?: "(no name)"}" })
        } else {
            labels.addAll(packagingItems.map { "${it.code} – ${it.item ?: "(no name)"}" })
        }
        adapter.clear()
        adapter.addAll(labels)
        adapter.notifyDataSetChanged()
    }

    private fun openItemDetail(code: String, type: String) {
        val intent = Intent(this, ItemDetailActivity::class.java)
        intent.putExtra("code", code)
        intent.putExtra("type", type)
        startActivity(intent)
    }

    private fun showNewInventoryItemDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val codeInput = EditText(this).apply { hint = "Code" }
        val nameInput = EditText(this).apply { hint = "Item name" }

        container.addView(TextView(this).apply { text = "Code" })
        container.addView(codeInput)
        container.addView(TextView(this).apply { text = "Name" })
        container.addView(nameInput)

        AlertDialog.Builder(this)
            .setTitle("New inventory item")
            .setView(container)
            .setPositiveButton("Save") { dialog, _ ->
                dialog.dismiss()
                val code = codeInput.text.toString().trim()
                if (code.isBlank()) {
                    Toast.makeText(this, "Code is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val item = InventoryItem(
                    code = code,
                    itemName = nameInput.text.toString().trim()
                )
                inventoryItems.add(item)
                ItemStore.upsertInventory(this, item)
                refreshList()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showNewPackagingItemDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }

        val codeInput = EditText(this).apply { hint = "Code" }
        val nameInput = EditText(this).apply { hint = "Item" }

        container.addView(TextView(this).apply { text = "Code" })
        container.addView(codeInput)
        container.addView(TextView(this).apply { text = "Item" })
        container.addView(nameInput)

        AlertDialog.Builder(this)
            .setTitle("New packaging item")
            .setView(container)
            .setPositiveButton("Save") { dialog, _ ->
                dialog.dismiss()
                val code = codeInput.text.toString().trim()
                if (code.isBlank()) {
                    Toast.makeText(this, "Code is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val item = PackagingItem(
                    code = code,
                    item = nameInput.text.toString().trim()
                )
                packagingItems.add(item)
                ItemStore.upsertPackaging(this, item)
                refreshList()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
