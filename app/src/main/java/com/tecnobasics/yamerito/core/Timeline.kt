package com.tecnobasics.yamerito.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** En qué parte del timeline cae cada tarea. */
enum class Bucket(val title: String) {
    OVERDUE("Ya se te pasó"),
    TODAY("Hoy"),
    TOMORROW("Mañana"),
    WEEK("Esta semana"),
    LATER("Más adelante"),
}

/** Qué tan encima está la entrega; decide el color de la cuenta regresiva. */
enum class Urgency { OVERDUE, HIGH, MID, LOW }

object Timeline {
    private const val HOUR_MS = 3_600_000L

    fun date(ms: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    fun bucket(dueMs: Long, nowMs: Long, zone: ZoneId): Bucket {
        if (dueMs <= nowMs) return Bucket.OVERDUE
        val today = date(nowMs, zone)
        val d = date(dueMs, zone)
        return when {
            d == today -> Bucket.TODAY
            d == today.plusDays(1) -> Bucket.TOMORROW
            !d.isAfter(today.plusDays(7)) -> Bucket.WEEK
            else -> Bucket.LATER
        }
    }

    fun urgency(dueMs: Long, nowMs: Long): Urgency {
        val left = dueMs - nowMs
        return when {
            left <= 0 -> Urgency.OVERDUE
            left < 24 * HOUR_MS -> Urgency.HIGH
            left < 72 * HOUR_MS -> Urgency.MID
            else -> Urgency.LOW
        }
    }

    /** "Faltan 3 días", "Faltan 5 h", "Faltan 20 min", "Venció hace 2 días". */
    fun countdown(dueMs: Long, nowMs: Long): String {
        val diff = dueMs - nowMs
        val past = diff <= 0
        val min = kotlin.math.abs(diff) / 60_000L
        if (min < 1) return if (past) "Venció justo ahora" else "Es ya"
        val text = when {
            min < 60 -> "$min min"
            min < 24 * 60 -> "${min / 60} h"
            else -> {
                val days = min / (24 * 60)
                if (days == 1L) "1 día" else "$days días"
            }
        }
        return when {
            past -> "Venció hace $text"
            text.startsWith("1 ") -> "Falta $text"
            else -> "Faltan $text"
        }
    }

    /** Pendientes agrupadas y ordenadas por fecha de entrega. */
    fun group(tasks: List<Task>, nowMs: Long, zone: ZoneId): List<Pair<Bucket, List<Task>>> =
        tasks.sortedBy { it.dueMs }
            .groupBy { bucket(it.dueMs, nowMs, zone) }
            .toList()
            .sortedBy { it.first.ordinal }
}

/** Fechas en español, a mano, para no depender de cómo cada teléfono traduce. */
object Fmt {
    private val DAYS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
    private val DAYS_LONG = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
    private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    private fun at(ms: Long, zone: ZoneId): LocalDateTime = Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime()

    /** "vie 31 oct" */
    fun date(ms: Long, zone: ZoneId): String {
        val d = at(ms, zone)
        return "${DAYS[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${MONTHS[d.monthValue - 1]}"
    }

    /** "23:59" o "11:59 pm" */
    fun time(ms: Long, zone: ZoneId, h24: Boolean): String {
        val t = at(ms, zone)
        val mm = t.minute.toString().padStart(2, '0')
        if (h24) return "${t.hour.toString().padStart(2, '0')}:$mm"
        val h = if (t.hour % 12 == 0) 12 else t.hour % 12
        return "$h:$mm ${if (t.hour < 12) "am" else "pm"}"
    }

    /** "hoy", "mañana", "el viernes" o "el vie 7 nov" */
    fun dayLabel(ms: Long, nowMs: Long, zone: ZoneId): String {
        val d = Timeline.date(ms, zone)
        val today = Timeline.date(nowMs, zone)
        return when {
            d == today -> "hoy"
            d == today.plusDays(1) -> "mañana"
            d == today.minusDays(1) -> "ayer"
            d.isAfter(today) && !d.isAfter(today.plusDays(6)) -> "el ${DAYS_LONG[d.dayOfWeek.value - 1]}"
            else -> "el ${date(ms, zone)}"
        }
    }
}
