package com.tecnobasics.yamerito

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import com.tecnobasics.yamerito.data.Repo
import com.tecnobasics.yamerito.reminders.Notifs
import com.tecnobasics.yamerito.reminders.ReminderController
import com.tecnobasics.yamerito.ui.YaMeritoApp
import com.tecnobasics.yamerito.ui.YaMeritoTheme

class MainActivity : ComponentActivity() {

    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app siempre es clara: íconos oscuros en las barras aunque el teléfono esté en modo oscuro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        Notifs.ensureChannel(this)
        Repo.get(this)
        ReminderController.check(this) // re-sincroniza la alarma por si el sistema la borró
        setContent {
            YaMeritoTheme {
                YaMeritoApp(resumeTick = resumeTick.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }
}
