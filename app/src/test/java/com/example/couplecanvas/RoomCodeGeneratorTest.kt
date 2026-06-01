package com.example.couplecanvas

import com.example.couplecanvas.util.RoomCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomCodeGeneratorTest {
    @Test
    fun generatedCodesUseSafeAlphabet() {
        repeat(100) {
            val code = RoomCodeGenerator.generate()
            assertEquals(6, code.length)
            assertTrue(RoomCodeGenerator.isValid(code))
            assertFalse(code.any { it in listOf('O', '0', 'I', '1') })
        }
    }
}
