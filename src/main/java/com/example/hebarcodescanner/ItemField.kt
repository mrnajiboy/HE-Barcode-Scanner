package com.example.hebarcodescanner

data class ItemField(
    val key: String,
    val label: String,
    val type: FieldType,
    val required: Boolean = false
)
