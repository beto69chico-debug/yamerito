package com.tecnobasics.yamerito.core

/** Una materia: nombre, profesor y el color con el que se agrupan sus tareas. */
data class Subject(
    val id: String,
    val name: String,
    val teacher: String,
    val color: Int, // índice en Palette.COLORS
)

/**
 * Una tarea que dejó el profe.
 *
 * [reminders]: minutos antes de la entrega en que se avisa; null = usar los avisos de siempre.
 * [armedMs]: momento de la última vez que se cambió la fecha o los avisos. Los avisos que
 * caían antes de ese momento ya no se mandan (si no, al crear una tarea para mañana sonaría
 * de golpe el aviso de "faltan 3 días").
 */
data class Task(
    val id: String,
    val subjectId: String,
    val title: String,
    val notes: String,
    val dueMs: Long,
    val reminders: List<Int>?,
    val createdMs: Long,
    val armedMs: Long,
    val deliveredMs: Long?,
) {
    val delivered: Boolean get() = deliveredMs != null
}

/** Todo lo que guarda la app, en el propio teléfono. */
data class AppData(
    val subjects: List<Subject> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val defaultReminders: List<Int> = Reminders.DEFAULT,
    /** Hasta qué momento ya se revisaron y mandaron los avisos. */
    val lastCheckMs: Long = 0L,
) {
    fun subject(id: String): Subject? = subjects.firstOrNull { it.id == id }
}

/** Colores para las materias (ARGB). */
object Palette {
    val COLORS: List<Long> = listOf(
        0xFF0B6E48, // verde Ya Merito
        0xFF1E6FD9, // azul
        0xFF7B3FE4, // morado
        0xFFE0457B, // rosa
        0xFFF26B1D, // naranja
        0xFFD9A100, // mostaza
        0xFF0097A7, // turquesa
        0xFFD93025, // rojo
        0xFF5D6B7A, // gris pizarra
        0xFF8D5524, // café
    )

    fun argb(index: Int): Long = COLORS[Math.floorMod(index, COLORS.size)]

    /** El siguiente color que todavía nadie usa, para que cada materia nueva se distinga. */
    fun nextFree(used: Collection<Int>): Int =
        COLORS.indices.firstOrNull { it !in used } ?: (used.size % COLORS.size)
}
