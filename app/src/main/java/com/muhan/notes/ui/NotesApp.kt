package com.muhan.notes.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.muhan.notes.NoteViewModel
import com.muhan.notes.data.Note
import com.muhan.notes.ui.edit.NoteEditScreen
import com.muhan.notes.ui.list.NoteListScreen

/** 路由定义 */
object Routes {
    const val LIST = "list"
    const val EDIT = "edit/{noteId}"

    /** 新建传 -1，编辑传真实 id */
    fun edit(id: Long?): String = if (id == null || id <= 0) "edit/-1" else "edit/$id"
}

@Composable
fun NotesApp(viewModel: NoteViewModel) {
    val navController = rememberSwipeDismissableNavController()
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Routes.LIST
    ) {
        composable(Routes.LIST) {
            NoteListScreen(
                notes = notes,
                onOpenNote = { id -> navController.navigate(Routes.edit(id)) },
                onNewNote = { navController.navigate(Routes.edit(null)) },
                onDelete = viewModel::deleteNote
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("noteId") { type = NavType.LongType; defaultValue = -1L })
        ) { entry ->
            val noteId = entry.arguments?.getLong("noteId") ?: -1L
            NoteEditScreen(
                noteId = noteId,
                loadNote = viewModel::getNote,
                onSave = { existing: Note?, title: String, content: String, color: Long, pinned: Boolean ->
                    if (existing == null) {
                        viewModel.addNote(title, content, color, pinned)
                    } else {
                        viewModel.updateNote(existing, title, content, color, pinned)
                    }
                    navController.popBackStack()
                },
                onDelete = { id ->
                    viewModel.deleteNote(id)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
