package com.tecnobasics.yamerito.core

import org.junit.Assert.assertEquals
import org.junit.Test

class CodecTest {
    @Test
    fun roundTripKeepsEverything() {
        val d = AppData(
            subjects = listOf(Subject("s1", "Microeconomía", "Prof. Ruiz", 2), Subject("s2", "Estadística", "", 5)),
            tasks = listOf(
                Task("t1", "s1", "Ensayo \"inflación\"", "5 cuartillas\nAPA", 1_800_000_000_000L, null, 1L, 2L, null),
                Task("t2", "s2", "Ejercicios", "", 1_800_100_000_000L, listOf(60, 1440), 3L, 4L, 5L),
            ),
            defaultReminders = listOf(4320, 180),
            lastCheckMs = 99L,
        )
        assertEquals(d, Codec.decode(Codec.encode(d)))
    }

    @Test
    fun missingFieldsGetDefaults() {
        val d = Codec.decode("""{"tasks":[{"id":"x","dueMs":10,"createdMs":7}]}""")
        assertEquals(Reminders.DEFAULT, d.defaultReminders)
        assertEquals(7L, d.tasks[0].armedMs)
        assertEquals(null, d.tasks[0].reminders)
        assertEquals(null, d.tasks[0].deliveredMs)
    }
}
