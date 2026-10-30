package com.tecnobasics.yamerito.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecnobasics.yamerito.Perm
import com.tecnobasics.yamerito.Perms
import com.tecnobasics.yamerito.R
import com.tecnobasics.yamerito.core.AppData
import com.tecnobasics.yamerito.core.Reminders
import com.tecnobasics.yamerito.data.Repo
import com.tecnobasics.yamerito.reminders.Notifs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(data: AppData, resumeTick: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var permTick by remember { mutableIntStateOf(0) }
    val granted = remember(resumeTick, permTick) { Perm.entries.associateWith { Perms.isGranted(ctx, it) } }
    val askNotifs = rememberAskNotifications(settingsIfDenied = true) { permTick++ }
    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Block {
                SectionTitle("Avisos de siempre")
                Text(
                    "Se usan en todas las tareas, a menos que a alguna le pongas avisos propios.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Reminders.OPTIONS.forEach { m ->
                        val on = m in data.defaultReminders
                        FilterChip(
                            selected = on,
                            onClick = {
                                val next = if (on) data.defaultReminders - m else data.defaultReminders + m
                                Repo.setDefaultReminders(ctx, Reminders.normalize(next))
                            },
                            label = { Text(Reminders.label(m)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenSoft,
                                selectedLabelColor = GreenDark,
                            ),
                        )
                    }
                }
                Text(
                    Reminders.summary(data.defaultReminders),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenDark,
                )
            }

            Block {
                SectionTitle("Para que los avisos sí lleguen")
                Perm.entries.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                if (p.essential) p.title else "${p.title} (recomendado)",
                                fontWeight = FontWeight.SemiBold,
                                color = Ink,
                            )
                            Text(p.desc, style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                        if (granted[p] == true) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Listo", tint = Green)
                        } else {
                            Button(
                                onClick = { if (p == Perm.NOTIFICATIONS) askNotifs() else Perms.open(ctx, p) },
                                colors = ButtonDefaults.buttonColors(containerColor = Green),
                            ) { Text("Activar") }
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        if (!Notifs.test(ctx)) {
                            Toast.makeText(ctx, "Primero activa las notificaciones", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Probar un aviso") }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.icono_yamerito),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Ya Merito $version", fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(
                        "Tus tareas se guardan solo en este teléfono. Hecha por Tecnobasics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Block(content: @Composable () -> Unit) {
    Surface(
        color = CardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}
