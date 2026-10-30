package com.tecnobasics.yamerito.ui

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecnobasics.yamerito.Perm
import com.tecnobasics.yamerito.Perms
import com.tecnobasics.yamerito.R
import com.tecnobasics.yamerito.core.AppData
import com.tecnobasics.yamerito.core.Bucket
import com.tecnobasics.yamerito.core.Fmt
import com.tecnobasics.yamerito.core.Subject
import com.tecnobasics.yamerito.core.Task
import com.tecnobasics.yamerito.core.Timeline
import com.tecnobasics.yamerito.data.Repo
import java.time.ZoneId

@Composable
fun HomeScreen(
    data: AppData,
    now: Long,
    resumeTick: Int,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onSubjects: () -> Unit,
    onSettings: () -> Unit,
) {
    val ctx = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val h24 = DateFormat.is24HourFormat(ctx)
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var showDelivered by rememberSaveable { mutableStateOf(false) }
    var permTick by remember { mutableIntStateOf(0) }
    val missing = remember(resumeTick, permTick, data) { Perms.missingEssential(ctx) }

    val askNotifs = rememberAskNotifications(settingsIfDenied = true) { permTick++ }
    val askOnce = rememberAskNotifications(settingsIfDenied = false) { permTick++ }
    // La primera vez que se abre la app se piden las notificaciones directo.
    LaunchedEffect(Unit) { if (!Perms.isGranted(ctx, Perm.NOTIFICATIONS)) askOnce() }

    // Si se borró la materia del filtro, se regresa a "Todas".
    val activeFilter = filter?.takeIf { id -> data.subjects.any { it.id == id } }
    val visible = data.tasks.filter { activeFilter == null || it.subjectId == activeFilter }
    val pending = visible.filter { !it.delivered }
    val delivered = visible.filter { it.delivered }.sortedByDescending { it.deliveredMs ?: 0L }
    val next = pending.filter { it.dueMs > now }.minByOrNull { it.dueMs }

    Scaffold(
        containerColor = Bg,
        floatingActionButton = {
            if (data.subjects.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Tarea", fontWeight = FontWeight.SemiBold) },
                    containerColor = Green,
                    contentColor = Color.White,
                )
            }
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = pad.calculateTopPadding() + 8.dp,
                bottom = pad.calculateBottomPadding() + 96.dp,
            ),
        ) {
            item(key = "header") { Header(onSettings) }

            if (missing.isNotEmpty()) {
                item(key = "perm") {
                    PermBanner(
                        missing = missing,
                        onFix = { p -> if (p == Perm.NOTIFICATIONS) askNotifs() else Perms.open(ctx, p) },
                    )
                }
            }

            if (data.subjects.isEmpty()) {
                item(key = "welcome") { Welcome(onSubjects) }
                return@LazyColumn
            }

            item(key = "filter") {
                SubjectFilter(data.subjects, activeFilter, onSelect = { filter = it }, onEdit = onSubjects)
            }
            item(key = "tabs") {
                Tabs(
                    pending = pending.size,
                    delivered = delivered.size,
                    showDelivered = showDelivered,
                    onChange = { showDelivered = it },
                )
            }

            if (!showDelivered) {
                if (next != null) {
                    item(key = "next") { NextUp(next, data.subject(next.subjectId), now, zone, h24) { onOpen(next.id) } }
                }
                if (pending.isEmpty()) {
                    item(key = "empty") {
                        EmptyCard("Nada pendiente", "¡Bien ahí! Cuando te dejen tarea, agrégala con el botón verde.")
                    }
                }
                Timeline.group(pending, now, zone).forEach { (bucket, list) ->
                    item(key = "b_${bucket.name}") { BucketHeader(bucket, list.size) }
                    items(list, key = { it.id }) { t ->
                        TaskRow(
                            task = t,
                            subject = data.subject(t.subjectId),
                            now = now,
                            zone = zone,
                            h24 = h24,
                            onToggle = { Repo.setDelivered(ctx, t.id, true) },
                            onClick = { onOpen(t.id) },
                        )
                    }
                }
            } else {
                if (delivered.isEmpty()) {
                    item(key = "empty_d") {
                        EmptyCard("Aún no hay entregadas", "Palomea una tarea cuando la entregues y aparecerá aquí.")
                    }
                }
                items(delivered, key = { it.id }) { t ->
                    TaskRow(
                        task = t,
                        subject = data.subject(t.subjectId),
                        now = now,
                        zone = zone,
                        h24 = h24,
                        onToggle = { Repo.setDelivered(ctx, t.id, false) },
                        onClick = { onOpen(t.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            Image(
                painter = painterResource(R.drawable.logo_yamerito),
                contentDescription = "Ya Merito",
                modifier = Modifier.height(46.dp),
            )
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = GreenDark)
        }
    }
}

@Composable
private fun PermBanner(missing: List<Perm>, onFix: (Perm) -> Unit) {
    val p = missing.first()
    Surface(
        color = AmberBg,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = AmberU)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Activa: ${p.title}", fontWeight = FontWeight.SemiBold, color = Ink)
                Text(p.desc, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Button(
                onClick = { onFix(p) },
                colors = ButtonDefaults.buttonColors(containerColor = AmberU),
            ) { Text("Activar") }
        }
    }
}

@Composable
private fun Welcome(onSubjects: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.icono_yamerito),
                contentDescription = null,
                modifier = Modifier.size(84.dp),
            )
            Text("¡Bienvenid@!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
            Text(
                "Primero agrega tus materias. Luego anota cada tarea con su fecha de entrega " +
                    "y Ya Merito te avisa antes de que se te pase.",
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
            )
            Button(onClick = onSubjects, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Text("Agregar mis materias")
            }
        }
    }
}

@Composable
private fun SubjectFilter(subjects: List<Subject>, selected: String?, onSelect: (String?) -> Unit, onEdit: () -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("Todas") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Green,
                    selectedLabelColor = Color.White,
                ),
            )
        }
        items(subjects, key = { it.id }) { s ->
            val c = subjectColor(s.color)
            FilterChip(
                selected = selected == s.id,
                onClick = { onSelect(if (selected == s.id) null else s.id) },
                label = { Text(s.name) },
                leadingIcon = { Dot(c) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = c.copy(alpha = 0.16f),
                    selectedLabelColor = Ink,
                ),
            )
        }
        item {
            AssistChip(
                onClick = onEdit,
                label = { Text("Materias") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
            )
        }
    }
}

@Composable
private fun Tabs(pending: Int, delivered: Int, showDelivered: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFE6ECE8))
            .padding(4.dp),
    ) {
        listOf(false to "Pendientes ($pending)", true to "Entregadas ($delivered)").forEach { (value, label) ->
            val on = showDelivered == value
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) CardBg else Color.Transparent)
                    .clickable { onChange(value) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) GreenDark else Muted,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun NextUp(task: Task, subject: Subject?, now: Long, zone: ZoneId, h24: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GreenDark),
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("LO QUE SIGUE", style = MaterialTheme.typography.labelMedium, color = Color(0xFFA9D9C2), fontWeight = FontWeight.Bold)
            Text(
                task.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subject != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Dot(subjectColor(subject.color))
                    Text(subject.name, color = Color(0xFFD5EDE1), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                Timeline.countdown(task.dueMs, now),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Text(
                "Se entrega ${Fmt.dayLabel(task.dueMs, now, zone)} a las ${Fmt.time(task.dueMs, zone, h24)}",
                color = Color(0xFFD5EDE1),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun BucketHeader(bucket: Bucket, count: Int) {
    val color = if (bucket == Bucket.OVERDUE) RedU else GreenDark
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(22.dp), contentAlignment = Alignment.Center) { Dot(color, 8) }
        Spacer(Modifier.width(8.dp))
        Text(bucket.title, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(6.dp))
        Text("· $count", color = Muted, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun TaskRow(
    task: Task,
    subject: Subject?,
    now: Long,
    zone: ZoneId,
    h24: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val color = subject?.let { subjectColor(it.color) } ?: Muted
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Riel del timeline: línea continua con un punto del color de la materia.
        Box(Modifier.width(22.dp).fillMaxHeight()) {
            Box(Modifier.align(Alignment.TopCenter).width(2.dp).fillMaxHeight().background(Line))
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, Bg, CircleShape),
            )
        }
        Spacer(Modifier.width(8.dp))
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, Line),
            modifier = Modifier.weight(1f).padding(vertical = 5.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(Modifier.width(6.dp).fillMaxHeight().background(color))
                Column(
                    Modifier.weight(1f).padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.delivered) Muted else Ink,
                        textDecoration = if (task.delivered) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subject != null) {
                        Text(
                            if (subject.teacher.isBlank()) subject.name else "${subject.name} · ${subject.teacher}",
                            style = MaterialTheme.typography.bodySmall,
                            color = color,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "${Fmt.date(task.dueMs, zone)} · ${Fmt.time(task.dueMs, zone, h24)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Muted,
                        )
                        if (task.delivered) {
                            Pill("Entregada", Green)
                        } else {
                            Pill(Timeline.countdown(task.dueMs, now), urgencyColor(Timeline.urgency(task.dueMs, now)))
                        }
                    }
                }
                Checkbox(
                    checked = task.delivered,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = Green, uncheckedColor = Muted),
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
        }
    }
}

@Composable
private fun EmptyCard(title: String, body: String) {
    Surface(
        color = CardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = Ink)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
    }
}
