package com.example.hebarcodescanner

data class CurrencyUnit(
    val value: Double?,
    val currencyCode: String,
    val symbol: String
)

data class CurrencyValue(
    val local: CurrencyUnit?,
    val global: CurrencyUnit?
)

data class InventoryItem(
    val code: String,
    val itemName: String? = null,
    val imageUrl: String? = null,
    val category: String? = null,
    val version: String? = null,
    val group: String? = null,
    val scanReason: String? = null,
    val storageLocations: String? = null,
    val notes: String? = null,
    val quantityAdded: Int? = null,
    val quantityRemoved: Int? = null,
    val currencyFields: Map<String, CurrencyValue> = emptyMap()
)
