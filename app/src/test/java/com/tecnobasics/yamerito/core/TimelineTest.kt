package com.tecnobasics.yamerito.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TimelineTest {
    private val zone = ZoneId.of("America/Mexico_City")
    private fun ms(y: Int, mo: Int, d: Int, h: Int, mi: Int = 0) =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    // Viernes 30 de octubre de 2026, 9:00
    private val now = ms(2026, 10, 30, 9)

    @Test
    fun buckets() {
        assertEquals(Bucket.OVERDUE, Timeline.bucket(ms(2026, 10, 29, 23), now, zone))
        assertEquals(Bucket.TODAY, Timeline.bucket(ms(2026, 10, 30, 23, 59), now, zone))
        assertEquals(Bucket.TOMORROW, Timeline.bucket(ms(2026, 10, 31, 8), now, zone))
        assertEquals(Bucket.WEEK, Timeline.bucket(ms(2026, 11, 6, 8), now, zone))
        assertEquals(Bucket.LATER, Timeline.bucket(ms(2026, 11, 7, 8), now, zone))
    }

    @Test
    fun countdownTexts() {
        assertEquals("Faltan 20 min", Timeline.countdown(now + 20 * 60_000L, now))
        assertEquals("Faltan 5 h", Timeline.countdown(now + 5 * 3_600_000L, now))
        assertEquals("Falta 1 día", Timeline.countdown(now + 30 * 3_600_000L, now))
        assertEquals("Faltan 3 días", Timeline.countdown(now + 3 * 86_400_000L, now))
        assertEquals("Venció hace 2 días", Timeline.countdown(now - 2 * 86_400_000L, now))
        assertEquals("Falta 1 h", Timeline.countdown(now + 3_600_000L, now))
    }

    @Test
    fun urgencyLevels() {
        assertEquals(Urgency.OVERDUE, Timeline.urgency(now - 1, now))
        assertEquals(Urgency.HIGH, Timeline.urgency(now + 5 * 3_600_000L, now))
        assertEquals(Urgency.MID, Timeline.urgency(now + 48 * 3_600_000L, now))
        assertEquals(Urgency.LOW, Timeline.urgency(now + 100 * 3_600_000L, now))
    }

    @Test
    fun spanishDates() {
        val due = ms(2026, 10, 31, 23, 59)
        assertEquals("sáb 31 oct", Fmt.date(due, zone))
        assertEquals("23:59", Fmt.time(due, zone, true))
        assertEquals("11:59 pm", Fmt.time(due, zone, false))
        assertEquals("12:05 am", Fmt.time(ms(2026, 11, 1, 0, 5), zone, false))
        assertEquals("mañana", Fmt.dayLabel(due, now, zone))
        assertEquals("el martes", Fmt.dayLabel(ms(2026, 11, 3, 8), now, zone))
    }
}
