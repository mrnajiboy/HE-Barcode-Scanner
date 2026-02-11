package com.example.hebarcodescanner

data class ScanPreset(
    val id: String,
    val name: String,
    val defaultItemType: String?,        // "INVENTORY", "PACKAGING", etc.
    val usePayloadPresetId: String?,     // links to Preset.id
    val inventoryFieldKeys: List<String> = emptyList(),   // InventoryField.key
    val packagingFieldKeys: List<String> = emptyList()    // PackagingField.key
)
