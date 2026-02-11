package com.example.hebarcodescanner

data class ScanHistoryItem(
    val code: String,
    val timestamp: Long,
    val presetId: String?,
    val presetName: String?,
    val webhookUrl: String?,
    val webhookName: String?,
    val sent: Boolean,
    val payload: String? = null  // NEW: Store actual payload sent
)

