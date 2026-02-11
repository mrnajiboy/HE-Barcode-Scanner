package com.example.hebarcodescanner

data class PackagingItem(
    val code: String,
    val item: String? = null,
    val supplier: String? = null,
    val scanReason: String? = null,
    val quantityPerUnit: Int? = null,
    val unitQuantityAdded: Int? = null,
    val unitQuantityRemoved: Int? = null,
    val lastOrdered: String? = null,
    val supplierLink: String? = null,
    val notes: String? = null,
    val currencyFields: Map<String, CurrencyValue> = emptyMap()
)
