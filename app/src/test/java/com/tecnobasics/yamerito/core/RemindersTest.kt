package com.tecnobasics.yamerito.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemindersTest {
    private val min = 60_000L
    private val hour = 60 * min
    private val day = 24 * hour
    private val t0 = 1_800_000_000_000L // un instante fijo cualquiera

    private fun task(
        id: String = "t",
        due: Long = t0 + 10 * day,
        reminders: List<Int>? = null,
        armed: Long = t0,
        delivered: Long? = null,
    ) = Task(id, "s", "Tarea $id", "", due, reminders, armed, armed, delivered)

    private val defaults = Reminders.DEFAULT // 3 días, 1 día, 3 horas

    @Test
    fun nextIsTheEarliestReminder() {
        val t = task()
        assertEquals(t.dueMs - 3 * day, Reminders.next(listOf(t), defaults, t0))
    }

    @Test
    fun customRemindersReplaceDefaults() {
        val t = task(reminders = listOf(Reminders.HOUR))
        assertEquals(t.dueMs - hour, Reminders.next(listOf(t), defaults, t0))
    }

    @Test
    fun remindersBeforeCreationAreSkipped() {
        // Tarea creada para dentro de 2 días: el aviso de "3 días antes" ya no aplica.
        val t = task(due = t0 + 2 * day)
        assertEquals(t.dueMs - day, Reminders.next(listOf(t), defaults, t0))
    }

    @Test
    fun deliveredTasksDoNotRemind() {
        assertNull(Reminders.next(listOf(task(delivered = t0)), defaults, t0))
        assertTrue(Reminders.due(listOf(task(delivered = t0)), defaults, t0, t0 + 20 * day).isEmpty())
    }

    @Test
    fun dueReturnsReminderInWindow() {
        val t = task()
        val at = t.dueMs - 3 * day
        val hits = Reminders.due(listOf(t), defaults, at - min, at + min)
        assertEquals(1, hits.size)
        assertEquals(3 * Reminders.DAY, hits[0].minutesBefore)
    }

    @Test
    fun missedRemindersCollapseToTheLatest() {
        // El teléfono estuvo apagado y se saltó los avisos de 3 días y de 1 día: solo llega uno.
        val t = task()
        val hits = Reminders.due(listOf(t), defaults, t0, t.dueMs - 20 * hour)
        assertEquals(1, hits.size)
        assertEquals(Reminders.DAY, hits[0].minutesBefore)
    }

    @Test
    fun noRemindersAfterTheDeadline() {
        val t = task()
        assertTrue(Reminders.due(listOf(t), defaults, t0, t.dueMs + hour).isEmpty())
    }

    @Test
    fun windowIsExclusiveAtStart() {
        val t = task()
        val at = t.dueMs - 3 * day
        assertTrue(Reminders.due(listOf(t), defaults, at, at + min).isEmpty())
        assertEquals(t.dueMs - day, Reminders.next(listOf(t), defaults, at))
    }

    @Test
    fun summaryReadsNaturally() {
        assertEquals("3 días, 1 día y 3 horas antes", Reminders.summary(defaults))
        assertEquals("1 semana antes", Reminders.summary(listOf(7 * Reminders.DAY)))
        assertEquals("Sin avisos", Reminders.summary(emptyList()))
    }
}
