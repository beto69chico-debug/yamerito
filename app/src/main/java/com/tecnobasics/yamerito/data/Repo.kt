package com.tecnobasics.yamerito.data

import android.content.Context
import android.util.AtomicFile
import com.tecnobasics.yamerito.core.AppData
import com.tecnobasics.yamerito.core.Codec
import com.tecnobasics.yamerito.core.Subject
import com.tecnobasics.yamerito.core.Task
import com.tecnobasics.yamerito.reminders.Notifs
import com.tecnobasics.yamerito.reminders.ReminderController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.UUID

/** Todo vive en un archivo dentro del teléfono: sin internet y sin cuentas. */
object Repo {
    private val lock = Any()
    private val state = MutableStateFlow<AppData?>(null)
    val data: StateFlow<AppData?> = state

    private fun file(ctx: Context) = AtomicFile(File(ctx.applicationContext.filesDir, "yamerito.json"))

    fun get(ctx: Context): AppData = synchronized(lock) {
        state.value ?: load(ctx).also { state.value = it }
    }

    private fun load(ctx: Context): AppData {
        val f = file(ctx)
        val loaded = try {
            if (f.baseFile.exists()) Codec.decode(String(f.readFully(), Charsets.UTF_8)) else null
        } catch (_: Exception) {
            null
        }
        // Primera vez: se empieza a contar desde ahora para no mandar avisos viejos.
        return loaded ?: AppData(lastCheckMs = System.currentTimeMillis())
    }

    private fun save(ctx: Context, d: AppData) {
        val f = file(ctx)
        val out = f.startWrite()
        try {
            out.write(Codec.encode(d).toByteArray(Charsets.UTF_8))
            f.finishWrite(out)
        } catch (e: Exception) {
            f.failWrite(out)
            throw e
        }
    }

    /** Cambia los datos, los guarda y vuelve a acomodar los avisos. */
    fun update(ctx: Context, change: (AppData) -> AppData) {
        synchronized(lock) {
            val next = change(get(ctx))
            save(ctx, next)
            state.value = next
        }
        ReminderController.check(ctx)
    }

    /** Solo para el revisor de avisos: no vuelve a disparar la revisión. */
    internal fun setLastCheck(ctx: Context, ms: Long) = synchronized(lock) {
        val next = get(ctx).copy(lastCheckMs = ms)
        save(ctx, next)
        state.value = next
    }

    fun newId(): String = UUID.randomUUID().toString()

    // ---- Materias ----

    fun saveSubject(ctx: Context, s: Subject) = update(ctx) { d ->
        val exists = d.subjects.any { it.id == s.id }
        d.copy(subjects = if (exists) d.subjects.map { if (it.id == s.id) s else it } else d.subjects + s)
    }

    /** Borra la materia junto con sus tareas. */
    fun deleteSubject(ctx: Context, id: String) {
        get(ctx).tasks.filter { it.subjectId == id }.forEach { Notifs.cancel(ctx, it.id) }
        update(ctx) { d -> d.copy(subjects = d.subjects.filterNot { it.id == id }, tasks = d.tasks.filterNot { it.subjectId == id }) }
    }

    // ---- Tareas ----

    fun saveTask(ctx: Context, t: Task) = update(ctx) { d ->
        val old = d.tasks.firstOrNull { it.id == t.id }
        // Si cambió la fecha o los avisos, los avisos que ya pasaron no se mandan de golpe.
        val rearm = old == null || old.dueMs != t.dueMs || old.reminders != t.reminders
        val fixed = if (rearm) t.copy(armedMs = System.currentTimeMillis()) else t
        d.copy(tasks = if (old != null) d.tasks.map { if (it.id == t.id) fixed else it } else d.tasks + fixed)
    }

    fun deleteTask(ctx: Context, id: String) {
        Notifs.cancel(ctx, id)
        update(ctx) { d -> d.copy(tasks = d.tasks.filterNot { it.id == id }) }
    }

    fun setDelivered(ctx: Context, id: String, delivered: Boolean) {
        if (delivered) Notifs.cancel(ctx, id)
        val now = System.currentTimeMillis()
        update(ctx) { d ->
            d.copy(tasks = d.tasks.map {
                when {
                    it.id != id -> it
                    delivered -> it.copy(deliveredMs = now)
                    // Al regresarla a pendiente, los avisos que pasaron mientras estaba entregada no suenan.
                    else -> it.copy(deliveredMs = null, armedMs = now)
                }
            })
        }
    }

    fun setDefaultReminders(ctx: Context, list: List<Int>) = update(ctx) { d ->
        // Cambiar los avisos de siempre no debe disparar avisos atrasados de golpe.
        val now = System.currentTimeMillis()
        d.copy(
            defaultReminders = list,
            tasks = d.tasks.map { if (it.reminders == null && !it.delivered) it.copy(armedMs = now) else it },
        )
    }
}
