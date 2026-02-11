package com.example.hebarcodescanner

data class GenericItem(
    val code: String,
    val typeId: String,
    val stringFields: Map<String, String> = emptyMap(),
    val numberFields: Map<String, Double> = emptyMap(),
    val dateTimeFields: Map<String, String> = emptyMap(),
    val booleanFields: Map<String, Boolean> = emptyMap(),
    val currencyFields: Map<String, CurrencyValue> = emptyMap()
)
