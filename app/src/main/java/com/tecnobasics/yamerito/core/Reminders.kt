package com.tecnobasics.yamerito.core

/** Cuándo avisar de cada tarea. Todo en minutos antes de la entrega. */
object Reminders {
    const val HOUR = 60
    const val DAY = 24 * 60

    /** Opciones que puede escoger cada quien. */
    val OPTIONS: List<Int> = listOf(1 * HOUR, 3 * HOUR, 12 * HOUR, 1 * DAY, 2 * DAY, 3 * DAY, 7 * DAY)

    /** Los avisos "de siempre" con los que arranca la app. */
    val DEFAULT: List<Int> = listOf(3 * DAY, 1 * DAY, 3 * HOUR)

    fun short(min: Int): String = when {
        min % (7 * DAY) == 0 -> plural(min / (7 * DAY), "semana", "semanas")
        min % DAY == 0 -> plural(min / DAY, "día", "días")
        min % HOUR == 0 -> plural(min / HOUR, "hora", "horas")
        else -> "$min min"
    }

    fun label(min: Int): String = "${short(min)} antes"

    /** "3 días, 1 día y 3 horas antes" */
    fun summary(list: List<Int>): String {
        val s = normalize(list).map { short(it) }
        return when (s.size) {
            0 -> "Sin avisos"
            1 -> "${s[0]} antes"
            else -> s.dropLast(1).joinToString(", ") + " y " + s.last() + " antes"
        }
    }

    /** Sin repetidos, del más lejano al más cercano. */
    fun normalize(list: List<Int>): List<Int> = list.filter { it > 0 }.distinct().sortedDescending()

    fun offsetsFor(task: Task, defaults: List<Int>): List<Int> = normalize(task.reminders ?: defaults)

    /** Un aviso concreto: de qué tarea, cuántos minutos antes y a qué hora suena. */
    data class Hit(val task: Task, val minutesBefore: Int, val atMs: Long)

    /** Todos los avisos válidos de las tareas pendientes. */
    fun all(tasks: List<Task>, defaults: List<Int>): List<Hit> =
        tasks.asSequence()
            .filter { !it.delivered }
            .flatMap { t -> offsetsFor(t, defaults).asSequence().map { Hit(t, it, t.dueMs - it * 60_000L) } }
            .filter { it.atMs > it.task.armedMs }
            .toList()

    /**
     * Los avisos que tocan en el intervalo (afterMs, upToMs].
     * Si a una misma tarea le tocaban varios (por ejemplo, el teléfono estuvo apagado),
     * solo se manda el más reciente, y nunca de tareas cuya entrega ya pasó.
     */
    fun due(tasks: List<Task>, defaults: List<Int>, afterMs: Long, upToMs: Long): List<Hit> =
        all(tasks, defaults)
            .filter { it.atMs > afterMs && it.atMs <= upToMs && it.task.dueMs > upToMs }
            .groupBy { it.task.id }
            .map { (_, hits) -> hits.maxBy { it.atMs } }
            .sortedBy { it.task.dueMs }

    /** El siguiente momento en que hay que avisar de algo, o null si no hay nada. */
    fun next(tasks: List<Task>, defaults: List<Int>, afterMs: Long): Long? =
        all(tasks, defaults).asSequence().map { it.atMs }.filter { it > afterMs }.minOrNull()

    private fun plural(n: Int, one: String, many: String) = if (n == 1) "1 $one" else "$n $many"
}
