package com.example.shiftalarmmvp.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryGuideCatalogTest {

    @Test
    fun `manufacturer aliases map to expected battery guide keys`() {
        assertEquals("samsung", batteryGuideManufacturerKey("Samsung"))
        assertEquals("xiaomi", batteryGuideManufacturerKey("Redmi"))
        assertEquals("xiaomi", batteryGuideManufacturerKey("POCO"))
        assertEquals("huawei", batteryGuideManufacturerKey("HONOR"))
        assertEquals("oneplus", batteryGuideManufacturerKey("OnePlus"))
        assertEquals("vivo", batteryGuideManufacturerKey("iQOO"))
        assertEquals("realme", batteryGuideManufacturerKey("realme"))
        assertEquals("google_pixel", batteryGuideManufacturerKey("Google"))
        assertNull(batteryGuideManufacturerKey("unknown"))
    }

    @Test
    fun `brand aliases are used when manufacturer is ambiguous`() {
        assertEquals("xiaomi", batteryGuideManufacturerKey("Android", "POCO"))
        assertEquals("huawei", batteryGuideManufacturerKey("Android", "HONOR"))
        assertEquals("vivo", batteryGuideManufacturerKey("Android", "iQOO"))
        assertEquals("realme", batteryGuideManufacturerKey("Android", "realme"))
        assertEquals("google_pixel", batteryGuideManufacturerKey("Android", "Pixel"))
    }

    @Test
    fun `catalog lookup uses normalized manufacturer and brand keys`() {
        val catalog = BatteryGuideCatalog(
            manufacturers = mapOf(
                "xiaomi" to BatteryGuideEntry(
                    title = "Autostart and battery saver",
                    shortDescription = "Allow autostart and remove battery limits",
                    steps = listOf("Enable autostart")
                )
            )
        )

        val guide = catalog.findGuideForManufacturer("Android", "Redmi")

        assertEquals("Autostart and battery saver", guide?.title)
        assertEquals(1, guide?.steps?.size)
    }

    @Test
    fun `catalog lookup falls back to shared guide when manufacturer is unsupported`() {
        val catalog = BatteryGuideCatalog(
            fallback = BatteryGuideEntry(
                title = "Generic Android battery settings",
                steps = listOf("Open app battery settings")
            )
        )

        val guide = catalog.findGuideForManufacturer("unknown")

        assertEquals("Generic Android battery settings", guide?.title)
        assertEquals(1, guide?.steps?.size)
    }
}