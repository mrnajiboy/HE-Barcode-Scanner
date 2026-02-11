package com.example.hebarcodescanner

data class ShipmentItem(
    val code: String,
    val buyerName: String? = null,
    val buyerCountry: String? = null,
    val shippedDate: String? = null,
    val estDeliveryDate: String? = null,
    val trackingNumber: String? = null,
    val fulfillmentLocation: String? = null,
    val lastHandledBy: String? = null,
    val scanReason: String? = null,
    val notes: String? = null,
    val weight: MeasurementValue? = null,
    val height: MeasurementValue? = null,
    val width: MeasurementValue? = null,
    val depth: MeasurementValue? = null,
    val currencyFields: Map<String, CurrencyValue> = emptyMap()
)
