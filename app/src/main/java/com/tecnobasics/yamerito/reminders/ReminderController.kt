package com.tecnobasics.yamerito.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tecnobasics.yamerito.core.Reminders
import com.tecnobasics.yamerito.data.Repo

/**
 * Una sola alarma programada a la vez: la del siguiente aviso.
 * Cuando suena, se mandan los avisos que tocan y se programa el siguiente.
 */
object ReminderController {
    /** Margen para que una alarma que llega unos segundos antes no se pierda. */
    private const val GRACE_MS = 30_000L
    private val lock = Any()

    fun canExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun check(ctx: Context) = synchronized(lock) {
        val d = Repo.get(ctx)
        val upTo = System.currentTimeMillis() + GRACE_MS
        val hits = Reminders.due(d.tasks, d.defaultReminders, d.lastCheckMs, upTo)
        hits.forEach { Notifs.reminder(ctx, it, d.subject(it.task.subjectId)) }
        Repo.setLastCheck(ctx, upTo)
        schedule(ctx, Reminders.next(d.tasks, d.defaultReminders, upTo))
    }

    private fun pending(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        1,
        Intent(ctx, ReminderReceiver::class.java).setAction("com.tecnobasics.yamerito.CHECK"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun schedule(ctx: Context, atMs: Long?) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val op = pending(ctx)
        am.cancel(op)
        if (atMs == null) return
        try {
            if (canExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, op)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, op)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, op)
        }
    }
}
