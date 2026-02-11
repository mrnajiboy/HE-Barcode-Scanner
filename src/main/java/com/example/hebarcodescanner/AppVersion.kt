package com.example.hebarcodescanner

import android.content.Context

object AppVersion {
    private const val PREF_NAME = "app_version"
    private const val KEY_VERSION = "version"
    private const val CURRENT_VERSION = 2

    fun getCurrentVersion(ctx: Context): Int {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_VERSION, 0)
    }

    fun setCurrentVersion(ctx: Context, version: Int) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_VERSION, version)
            .apply()
    }

    fun runMigrations(ctx: Context) {
        val currentVersion = getCurrentVersion(ctx)

        if (currentVersion < 2) {
            migrateToV2(ctx)
        }

        setCurrentVersion(ctx, CURRENT_VERSION)
    }

    private fun migrateToV2(ctx: Context) {
        // Add shipment type if missing
        val types = ItemTypeStore.getAll(ctx).toMutableList()
        val hasShipment = types.any { it.id == "shipment" }

        if (!hasShipment) {
            val shipment = ItemType(
                id = "shipment",
                name = "Shipment",
                fields = listOf(
                    ItemField("trackingNumber", "Tracking Number", FieldType.STRING, true),
                    ItemField("buyerName", "Buyer Name", FieldType.STRING, false),
                    ItemField("buyerCountry", "Buyer Country", FieldType.STRING, false),
                    ItemField("shippedDate", "Shipped Date", FieldType.DATE_TIME, false),
                    ItemField("estDeliveryDate", "Est. Delivery Date", FieldType.DATE_TIME, false),
                    ItemField("fulfillmentLocation", "Fulfillment Location", FieldType.STRING, false),
                    ItemField("lastHandledBy", "Last Handled By", FieldType.STRING, false),
                    ItemField("scanReason", "Scan Reason", FieldType.STRING, false),
                    ItemField("weight", "Weight (KG/LBS)", FieldType.STRING, false),
                    ItemField("height", "Height (CM/Inches)", FieldType.STRING, false),
                    ItemField("width", "Width (CM/Inches)", FieldType.STRING, false),
                    ItemField("depth", "Depth (CM/Inches)", FieldType.STRING, false),
                    ItemField("shippingCost", "Shipping Cost", FieldType.CURRENCY, false),
                    ItemField("declaredCustomsValue", "Declared Customs Value", FieldType.CURRENCY, false),
                    ItemField("notes", "Notes", FieldType.STRING, false),
                )
            )
            types.add(shipment)
            ItemTypeStore.saveAll(ctx, types)
        }

        // Remove old default presets and recreate all with new schema
        val presets = PresetStore.getAll(ctx).toMutableList()
        presets.removeAll {
            it.name.startsWith("Inventory - ") ||
                    it.name.startsWith("Packaging - ") ||
                    it.name.startsWith("Shipment - ")
        }
        PresetStore.saveAll(ctx, presets)

        // Force recreate all default presets with new schema
        PresetStore.ensureDefaultsSeeded(ctx)
    }
}
