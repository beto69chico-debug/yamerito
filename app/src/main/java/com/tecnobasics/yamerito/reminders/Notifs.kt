package com.tecnobasics.yamerito.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tecnobasics.yamerito.MainActivity
import com.tecnobasics.yamerito.R
import com.tecnobasics.yamerito.core.Fmt
import com.tecnobasics.yamerito.core.Palette
import com.tecnobasics.yamerito.core.Reminders
import com.tecnobasics.yamerito.core.Subject
import com.tecnobasics.yamerito.core.Timeline
import com.tecnobasics.yamerito.core.Urgency
import java.time.ZoneId

object Notifs {
    const val CHANNEL = "ym_entregas_v1"
    private const val ID_TEST = 9001

    fun ensureChannel(ctx: Context) {
        val ch = NotificationChannel(CHANNEL, "Entregas próximas", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisos antes de que se venza cada tarea"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun canPost(ctx: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    private fun notifId(taskId: String) = taskId.hashCode()

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun deliveredAction(ctx: Context, taskId: String): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        notifId(taskId),
        Intent(ctx, DeliveredReceiver::class.java)
            .setAction("com.tecnobasics.yamerito.DELIVERED.$taskId")
            .putExtra(DeliveredReceiver.EXTRA_TASK, taskId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    @SuppressLint("MissingPermission")
    fun reminder(ctx: Context, hit: Reminders.Hit, subject: Subject?) {
        if (!canPost(ctx)) return
        ensureChannel(ctx)
        val t = hit.task
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val h24 = DateFormat.is24HourFormat(ctx)
        val title = when (Timeline.urgency(t.dueMs, now)) {
            Urgency.HIGH, Urgency.OVERDUE -> "¡Ya merito! ${t.title}"
            Urgency.MID -> "¡Aguas! ${t.title}"
            Urgency.LOW -> "Pa' que no se te pase: ${t.title}"
        }
        val countdown = Timeline.countdown(t.dueMs, now)
        val whenText = "Se entrega ${Fmt.dayLabel(t.dueMs, now, zone)} a las ${Fmt.time(t.dueMs, zone, h24)}"
        val subjectLine = subject?.let { s -> if (s.teacher.isBlank()) s.name else "${s.name} · ${s.teacher}" }
        val body = listOfNotNull(subjectLine, "$countdown. $whenText.").joinToString("\n")

        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_yamerito)
            .setColor((subject?.let { Palette.argb(it.color) } ?: Palette.argb(0)).toInt())
            .setContentTitle(title)
            .setContentText("$countdown · ${subject?.name ?: "Tarea"}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .addAction(0, "Ya la entregué", deliveredAction(ctx, t.id))
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(notifId(t.id), n)
        } catch (_: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    fun test(ctx: Context): Boolean {
        if (!canPost(ctx)) return false
        ensureChannel(ctx)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_yamerito)
            .setColor(Palette.argb(0).toInt())
            .setContentTitle("¡Así te van a llegar los avisos!")
            .setContentText("Ya Merito te avisa antes de cada entrega.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .build()
        return try {
            NotificationManagerCompat.from(ctx).notify(ID_TEST, n)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancel(ctx: Context, taskId: String) {
        NotificationManagerCompat.from(ctx).cancel(notifId(taskId))
    }
}
