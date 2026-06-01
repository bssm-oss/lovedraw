package com.example.couplecanvas

import com.example.couplecanvas.data.model.ModelFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ModelFactoryTest {
    @Test
    fun dateKeyDefaultsToUtcForSharedDailySparkDay() {
        val instant = Instant.parse("2026-05-31T23:30:00Z")

        assertEquals("2026-05-31", ModelFactory.dateKey(instant))
        assertEquals("2026-06-01", ModelFactory.dateKey(instant, ZoneId.of("Asia/Seoul")))
    }
}
