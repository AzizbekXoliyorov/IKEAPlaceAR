package com.azizbek.ikeaplace

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {
    @Test fun catalogHasAtLeastTenUniqueItems() {
        assertTrue(Catalog.items.size >= 10)
        assertEquals(Catalog.items.size, Catalog.items.map { it.id }.toSet().size)
    }

    @Test fun everyModelFileExistsAndIsValidGlb() {
        Catalog.items.forEach {
            val f = File("src/main/assets/${it.asset}")
            assertTrue("missing ${f.path}", f.exists())
            val head = f.inputStream().use { s -> ByteArray(4).also { b -> s.read(b) } }
            assertEquals("glTF", String(head))
        }
    }

    @Test fun dimensionsAreRealistic() {
        Catalog.items.forEach { assertTrue(it.widthCm in 20..300 && it.depthCm in 20..300 && it.heightCm in 20..250) }
    }
}
