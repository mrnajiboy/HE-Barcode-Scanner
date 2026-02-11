package com.example.hebarcodescanner

data class MeasurementUnit(
    val value: Double?,
    val unit: String,
    val symbol: String
)

data class MeasurementValue(
    val metric: MeasurementUnit?,
    val imperial: MeasurementUnit?
)
