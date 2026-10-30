package com.tecnobasics.yamerito.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecnobasics.yamerito.core.AppData
import com.tecnobasics.yamerito.core.Subject
import com.tecnobasics.yamerito.data.Repo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(data: AppData, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var editing by remember { mutableStateOf<Subject?>(null) }
    var adding by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopAppBar(
                title = { Text("Mis materias", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Materia", fontWeight = FontWeight.SemiBold) },
                containerColor = Green,
                contentColor = Color.White,
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = pad.calculateTopPadding() + 4.dp,
                bottom = pad.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (data.subjects.isEmpty()) {
                item {
                    Text(
                        "Agrega cada materia que llevas este semestre, con su profe y un color. " +
                            "Así tus tareas se ven agrupadas y las encuentras rápido.",
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            items(data.subjects, key = { it.id }) { s ->
                val pending = data.tasks.count { it.subjectId == s.id && !it.delivered }
                Card(
                    onClick = { editing = s },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, Line),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Dot(subjectColor(s.color), 18)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name, fontWeight = FontWeight.SemiBold, color = Ink, style = MaterialTheme.typography.titleMedium)
                            if (s.teacher.isNotBlank()) {
                                Text(s.teacher, color = Muted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Pill(
                            when (pending) {
                                0 -> "Al día"
                                1 -> "1 pendiente"
                                else -> "$pending pendientes"
                            },
                            if (pending == 0) Green else subjectColor(s.color),
                        )
                    }
                }
            }
        }
    }

    if (adding) {
        SubjectDialog(
            initial = null,
            usedColors = data.subjects.map { it.color },
            taskCount = 0,
            onSave = {
                Repo.saveSubject(ctx, it)
                adding = false
            },
            onDelete = null,
            onDismiss = { adding = false },
        )
    }

    editing?.let { s ->
        SubjectDialog(
            initial = s,
            usedColors = data.subjects.filter { it.id != s.id }.map { it.color },
            taskCount = data.tasks.count { it.subjectId == s.id },
            onSave = {
                Repo.saveSubject(ctx, it)
                editing = null
            },
            onDelete = {
                Repo.deleteSubject(ctx, s.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}
