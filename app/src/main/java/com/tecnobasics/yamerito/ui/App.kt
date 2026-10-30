package com.tecnobasics.yamerito.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tecnobasics.yamerito.data.Repo
import kotlinx.coroutines.delay

sealed interface Screen {
    data object Home : Screen
    data class EditTask(val id: String?) : Screen
    data object Subjects : Screen
    data object Settings : Screen
}

@Composable
fun YaMeritoApp(resumeTick: Int) {
    val data = Repo.data.collectAsState().value ?: return
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    // La hora "de ahora" se refresca cada 30 s para que las cuentas regresivas avancen solas.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(resumeTick) { now = System.currentTimeMillis() }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            data = data,
            now = now,
            resumeTick = resumeTick,
            onAdd = { screen = Screen.EditTask(null) },
            onOpen = { screen = Screen.EditTask(it) },
            onSubjects = { screen = Screen.Subjects },
            onSettings = { screen = Screen.Settings },
        )
        is Screen.EditTask -> EditTaskScreen(data = data, taskId = s.id, now = now, onDone = { screen = Screen.Home })
        Screen.Subjects -> SubjectsScreen(data = data, onBack = { screen = Screen.Home })
        Screen.Settings -> SettingsScreen(data = data, resumeTick = resumeTick, onBack = { screen = Screen.Home })
    }
}
