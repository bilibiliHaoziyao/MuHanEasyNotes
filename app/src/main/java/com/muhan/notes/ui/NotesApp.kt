package com.muhan.notes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.muhan.notes.NoteViewModel
import com.muhan.notes.SettingsViewModel
import com.muhan.notes.ui.edit.NoteEditScreen
import com.muhan.notes.ui.list.NoteListScreen
import com.muhan.notes.ui.settings.SettingsScreen

/** 路由定义 */
object Routes {
    const val LIST = "list"
    const val EDIT = "edit/{noteId}"
    const val SETTINGS = "settings"

    /** 新建传 -1，编辑传真实 id */
    fun edit(id: Long?): String = if (id == null || id <= 0) "edit/-1" else "edit/$id"
}

@Composable
fun NotesApp(
    noteViewModel: NoteViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberSwipeDismissableNavController()
    val notes by noteViewModel.notes.collectAsStateWithLifecycle()

    // 应用「缩放大小」与「字体大小」设置：整体界面按 uiScale 缩放，文字再按 fontScale 缩放
    val uiScale by settingsViewModel.uiScale.collectAsStateWithLifecycle()
    val fontScale by settingsViewModel.fontScale.collectAsStateWithLifecycle()
    val autoSave by settingsViewModel.autoSave.collectAsStateWithLifecycle()
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density * uiScale,
        fontScale = fontScale
    )

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = Routes.LIST
        ) {
            composable(Routes.LIST) {
                NoteListScreen(
                    notes = notes,
                    onOpenNote = { id -> navController.navigate(Routes.edit(id)) },
                    onNewNote = { navController.navigate(Routes.edit(null)) },
                    onDelete = noteViewModel::deleteNote,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    uiScale = uiScale,
                    fontScale = fontScale,
                    autoSave = autoSave,
                    onUiScaleChange = settingsViewModel::setUiScale,
                    onFontScaleChange = settingsViewModel::setFontScale,
                    onAutoSaveChange = settingsViewModel::setAutoSave,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.EDIT,
                arguments = listOf(navArgument("noteId") { type = NavType.LongType; defaultValue = -1L })
            ) { entry ->
                val noteId = entry.arguments?.getLong("noteId") ?: -1L
                NoteEditScreen(
                    noteId = noteId,
                    loadNote = noteViewModel::getNote,
                    autoSave = autoSave,
                    onSave = { existing, title, content, color, pinned ->
                        noteViewModel.saveNote(existing, title, content, color, pinned)
                    },
                    onDelete = { id ->
                        noteViewModel.deleteNote(id)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
