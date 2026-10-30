package com.tecnobasics.yamerito.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.tecnobasics.yamerito.core.AppData
import com.tecnobasics.yamerito.core.Fmt
import com.tecnobasics.yamerito.core.Reminders
import com.tecnobasics.yamerito.core.Task
import com.tecnobasics.yamerito.core.Timeline
import com.tecnobasics.yamerito.data.Repo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTaskScreen(data: AppData, taskId: String?, now: Long, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val h24 = DateFormat.is24HourFormat(ctx)
    val original = remember(taskId) { taskId?.let { id -> data.tasks.firstOrNull { it.id == id } } }

    var title by rememberSaveable { mutableStateOf(original?.title ?: "") }
    var notes by rememberSaveable { mutableStateOf(original?.notes ?: "") }
    var subjectId by rememberSaveable { mutableStateOf(original?.subjectId ?: data.subjects.firstOrNull()?.id) }
    var dueMs by rememberSaveable { mutableLongStateOf(original?.dueMs ?: defaultDue(zone)) }
    var useDefaults by rememberSaveable { mutableStateOf(original?.reminders == null) }
    var custom by remember { mutableStateOf(original?.reminders ?: data.defaultReminders) }
    var showNewSubject by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun pickDate() {
        val d = Instant.ofEpochMilli(dueMs).atZone(zone)
        DatePickerDialog(
            ctx,
            { _, y, m, day -> dueMs = d.with(LocalDate.of(y, m + 1, day)).toInstant().toEpochMilli() },
            d.year,
            d.monthValue - 1,
            d.dayOfMonth,
        ).show()
    }

    fun pickTime() {
        val d = Instant.ofEpochMilli(dueMs).atZone(zone)
        TimePickerDialog(
            ctx,
            { _, h, min -> dueMs = d.with(LocalTime.of(h, min)).toInstant().toEpochMilli() },
            d.hour,
            d.minute,
            h24,
        ).show()
    }

    fun save() {
        val t = title.trim()
        val sid = subjectId?.takeIf { id -> data.subjects.any { it.id == id } }
        when {
            t.isEmpty() -> error = "Escribe qué hay que entregar."
            sid == null -> error = "Escoge la materia."
            original == null && dueMs <= System.currentTimeMillis() -> error = "Esa fecha y hora ya pasaron."
            else -> {
                val stamp = System.currentTimeMillis()
                Repo.saveTask(
                    ctx,
                    Task(
                        id = original?.id ?: Repo.newId(),
                        subjectId = sid,
                        title = t,
                        notes = notes.trim(),
                        dueMs = dueMs,
                        reminders = if (useDefaults) null else Reminders.normalize(custom),
                        createdMs = original?.createdMs ?: stamp,
                        armedMs = original?.armedMs ?: stamp,
                        deliveredMs = original?.deliveredMs,
                    ),
                )
                onDone()
            }
        }
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text(if (original == null) "Nueva tarea" else "Editar tarea", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    if (original != null) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar tarea", tint = RedU)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            )
        },
        bottomBar = {
            Surface(color = Bg) {
                Button(
                    onClick = { save() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                        .height(52.dp),
                ) { Text("Guardar tarea", fontWeight = FontWeight.SemiBold) }
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    error = null
                },
                label = { Text("¿Qué hay que entregar?") },
                placeholder = { Text("Ej. Ensayo sobre inflación") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Materia")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    data.subjects.forEach { s ->
                        val c = subjectColor(s.color)
                        FilterChip(
                            selected = subjectId == s.id,
                            onClick = {
                                subjectId = s.id
                                error = null
                            },
                            label = { Text(s.name) },
                            leadingIcon = { Dot(c) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = c.copy(alpha = 0.18f),
                                selectedLabelColor = Ink,
                            ),
                        )
                    }
                    AssistChip(
                        onClick = { showNewSubject = true },
                        label = { Text("Nueva materia") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Entrega")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { pickDate() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(Fmt.date(dueMs, zone))
                    }
                    OutlinedButton(onClick = { pickTime() }, modifier = Modifier.weight(1f)) {
                        Text(Fmt.time(dueMs, zone, h24))
                    }
                }
                Text(
                    Timeline.countdown(dueMs, now),
                    color = urgencyColor(Timeline.urgency(dueMs, now)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Avisos")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Usar mis avisos de siempre", color = Ink)
                        Text(
                            Reminders.summary(data.defaultReminders),
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }
                    Switch(
                        checked = useDefaults,
                        onCheckedChange = {
                            useDefaults = it
                            if (!it && custom.isEmpty()) custom = data.defaultReminders
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Green),
                    )
                }
                if (!useDefaults) {
                    Text("Avísame:", style = MaterialTheme.typography.bodySmall, color = Muted)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Reminders.OPTIONS.forEach { m ->
                            FilterChip(
                                selected = m in custom,
                                onClick = { custom = if (m in custom) custom - m else custom + m },
                                label = { Text(Reminders.label(m)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenSoft,
                                    selectedLabelColor = GreenDark,
                                ),
                            )
                        }
                    }
                }
                val offsets = Reminders.normalize(if (useDefaults) data.defaultReminders else custom)
                val upcoming = offsets.map { dueMs - it * 60_000L }.filter { it > now }
                Text(
                    when {
                        offsets.isEmpty() -> "No te voy a avisar de esta tarea."
                        upcoming.isEmpty() -> "Ya no da tiempo para esos avisos."
                        else -> "Te aviso: " + upcoming.joinToString(", ") { "${Fmt.date(it, zone)} ${Fmt.time(it, zone, h24)}" }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                placeholder = { Text("Páginas, formato, si es en equipo…") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            if (original != null) {
                OutlinedButton(
                    onClick = {
                        Repo.setDelivered(ctx, original.id, !original.delivered)
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (original.delivered) "Regresar a pendientes" else "Ya la entregué")
                }
            }

            error?.let { Text(it, color = RedU, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showNewSubject) {
        SubjectDialog(
            initial = null,
            usedColors = data.subjects.map { it.color },
            taskCount = 0,
            onSave = { s ->
                Repo.saveSubject(ctx, s)
                subjectId = s.id
                showNewSubject = false
            },
            onDelete = null,
            onDismiss = { showNewSubject = false },
        )
    }

    if (confirmDelete && original != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = CardBg,
            title = { Text("¿Borrar esta tarea?") },
            text = { Text("\"${original.title}\" se borrará y ya no te avisaré de ella.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    Repo.deleteTask(ctx, original.id)
                    onDone()
                }) { Text("Borrar", color = RedU) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

/** Por defecto: mañana a las 23:59. */
private fun defaultDue(zone: ZoneId): Long =
    ZonedDateTime.now(zone).plusDays(1).with(LocalTime.of(23, 59)).toInstant().toEpochMilli()
