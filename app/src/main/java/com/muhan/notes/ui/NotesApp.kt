package com.muhan.notes.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.muhan.notes.NoteViewModel
import com.muhan.notes.SettingsViewModel
import com.muhan.notes.ui.about.AboutScreen
import com.muhan.notes.ui.backup.BackupScreen
import com.muhan.notes.ui.edit.NoteEditScreen
import com.muhan.notes.ui.list.NoteListScreen
import com.muhan.notes.ui.privacy.PrivacyCenterScreen
import com.muhan.notes.ui.settings.SettingsScreen
import com.muhan.notes.ui.sync.SyncScreen
import com.muhan.notes.ui.trash.RecycleBinScreen

/** 路由定义 */
object Routes {
    const val LIST = "list"
    const val EDIT = "edit/{noteId}"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val PRIVACY = "privacy"
    const val TRASH = "trash"
    const val BACKUP = "backup"
    const val SYNC = "sync"

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
    val privateNotes by noteViewModel.privateNotes.collectAsStateWithLifecycle()
    val trashedNotes by noteViewModel.trashedNotes.collectAsStateWithLifecycle()
    val syncStatus by noteViewModel.syncStatus.collectAsStateWithLifecycle()

    // 应用「缩放大小」与「字体大小」设置：整体界面按 uiScale 缩放，文字再按 fontScale 缩放
    val uiScale by settingsViewModel.uiScale.collectAsStateWithLifecycle()
    val fontScale by settingsViewModel.fontScale.collectAsStateWithLifecycle()
    val autoSave by settingsViewModel.autoSave.collectAsStateWithLifecycle()
    val isRecording by noteViewModel.isRecording.collectAsStateWithLifecycle()
    val hasPrivacyPassword by settingsViewModel.privacyPassword.collectAsStateWithLifecycle()
    val webdavUrl by settingsViewModel.webdavUrl.collectAsStateWithLifecycle()
    val webdavUser by settingsViewModel.webdavUser.collectAsStateWithLifecycle()
    val webdavPass by settingsViewModel.webdavPass.collectAsStateWithLifecycle()
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density * uiScale,
        fontScale = fontScale
    )

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        // 兼容手机屏幕：内容限宽并居中，手机上呈现居中的竖列布局
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.fillMaxSize().widthIn(max = 480.dp)) {
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
                            onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                            onSetPrivate = { id, isPrivate ->
                                noteViewModel.setPrivate(id, isPrivate)
                            }
                        )
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            uiScale = uiScale,
                            fontScale = fontScale,
                            autoSave = autoSave,
                            hasPrivacyPassword = hasPrivacyPassword != null,
                            onUiScaleChange = settingsViewModel::setUiScale,
                            onFontScaleChange = settingsViewModel::setFontScale,
                            onAutoSaveChange = settingsViewModel::setAutoSave,
                            onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                            onOpenTrash = { navController.navigate(Routes.TRASH) },
                            onOpenBackup = { navController.navigate(Routes.BACKUP) },
                            onOpenSync = { navController.navigate(Routes.SYNC) },
                            onOpenAbout = { navController.navigate(Routes.ABOUT) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.ABOUT) {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Routes.PRIVACY) {
                        PrivacyCenterScreen(
                            privateNotes = privateNotes,
                            hasPassword = hasPrivacyPassword != null,
                            onVerify = settingsViewModel::verifyPassword,
                            onSetPassword = settingsViewModel::setPrivacyPassword,
                            onClearPassword = settingsViewModel::clearPrivacyPassword,
                            onOpenNote = { id -> navController.navigate(Routes.edit(id)) },
                            onSetPrivate = noteViewModel::setPrivate,
                            onDelete = noteViewModel::deleteNote,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.TRASH) {
                        RecycleBinScreen(
                            trashedNotes = trashedNotes,
                            onRestore = noteViewModel::restoreFromTrash,
                            onPurge = noteViewModel::purgeNote,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.BACKUP) {
                        BackupScreen(
                            webdavUrl = webdavUrl,
                            webdavUser = webdavUser,
                            webdavPass = webdavPass,
                            onSaveWebdav = settingsViewModel::setWebdav,
                            onExport = noteViewModel::exportBackup,
                            onListLocal = noteViewModel::listLocalBackups,
                            onRestoreLocal = noteViewModel::restoreLocalBackup,
                            onExportToUri = noteViewModel::writeExportZipToUri,
                            onRestoreUri = noteViewModel::restoreFromUri,
                            onWebdavUpload = noteViewModel::webdavUpload,
                            onWebdavDownload = noteViewModel::webdavDownload,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Routes.SYNC) {
                        SyncScreen(
                            syncStatus = syncStatus,
                            onResetStatus = noteViewModel::resetSyncStatus,
                            onLanHost = noteViewModel::lanHost,
                            onLanConnect = noteViewModel::lanConnect,
                            onBtHost = noteViewModel::bluetoothHost,
                            onBtConnect = noteViewModel::bluetoothConnect,
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
                            onBack = { navController.popBackStack() },
                            loadAttachments = noteViewModel::getAttachments,
                            isRecording = isRecording,
                            onStartRecording = noteViewModel::startRecording,
                            onStopRecording = noteViewModel::stopRecording,
                            onAddAttachment = noteViewModel::addAttachment,
                            onAddLocalAttachment = noteViewModel::addLocalAttachment,
                            onDeleteAttachment = noteViewModel::deleteAttachment,
                            onTouchNote = noteViewModel::touchNote
                        )
                    }
                }
            }
        }
    }
}
