package com.example.hebarcodescanner

data class Preset(
    val id: String,
    val name: String,
    val description: String,
    val webhookUrl: String,
    val bodyTemplate: String,
    val requiresQuantity: Boolean = false
)
