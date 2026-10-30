package com.tecnobasics.yamerito.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tecnobasics.yamerito.data.Repo

/** Llega a la hora del siguiente aviso. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        ReminderController.check(ctx)
    }
}

/** Botón "Ya la entregué" de la notificación. */
class DeliveredReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_TASK = "task_id"
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_TASK) ?: return
        Repo.setDelivered(ctx, id, true)
    }
}

/** Android borra las alarmas al apagar el teléfono o cambiar la hora: aquí se vuelven a poner. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        ReminderController.check(ctx)
    }
}
