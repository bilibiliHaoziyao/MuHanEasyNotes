package com.muhan.notes.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * 应用设置（基于 Preferences DataStore 持久化）：
 * - uiScale          界面整体缩放倍率（同时缩放尺寸与文字）
 * - fontScale        字体大小倍率（仅影响文字）
 * - autoSave         编辑页自动保存开关
 * - privacyPassword  隐私中心密码（SHA-256 十六进制；null 表示未设置）
 * - webdavUrl / webdavUser / webdavPass  WebDAV 备份配置
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val UI_SCALE = floatPreferencesKey("ui_scale")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val PRIVACY_PASSWORD = stringPreferencesKey("privacy_password")
        val WEBDAV_URL = stringPreferencesKey("webdav_url")
        val WEBDAV_USER = stringPreferencesKey("webdav_user")
        val WEBDAV_PASS = stringPreferencesKey("webdav_pass")
    }

    val uiScale: Flow<Float> = context.settingsDataStore.data.map { it[Keys.UI_SCALE] ?: 1f }
    val fontScale: Flow<Float> = context.settingsDataStore.data.map { it[Keys.FONT_SCALE] ?: 1f }
    val autoSave: Flow<Boolean> = context.settingsDataStore.data.map { it[Keys.AUTO_SAVE] ?: false }
    val privacyPassword: Flow<String?> = context.settingsDataStore.data.map { it[Keys.PRIVACY_PASSWORD] }
    val webdavUrl: Flow<String> = context.settingsDataStore.data.map { it[Keys.WEBDAV_URL] ?: "" }
    val webdavUser: Flow<String> = context.settingsDataStore.data.map { it[Keys.WEBDAV_USER] ?: "" }
    val webdavPass: Flow<String> = context.settingsDataStore.data.map { it[Keys.WEBDAV_PASS] ?: "" }

    suspend fun setUiScale(value: Float) {
        context.settingsDataStore.edit { it[Keys.UI_SCALE] = value }
    }

    suspend fun setFontScale(value: Float) {
        context.settingsDataStore.edit { it[Keys.FONT_SCALE] = value }
    }

    suspend fun setAutoSave(value: Boolean) {
        context.settingsDataStore.edit { it[Keys.AUTO_SAVE] = value }
    }

    suspend fun setPrivacyPassword(hashed: String) {
        context.settingsDataStore.edit { it[Keys.PRIVACY_PASSWORD] = hashed }
    }

    suspend fun clearPrivacyPassword() {
        context.settingsDataStore.edit { it.remove(Keys.PRIVACY_PASSWORD) }
    }

    suspend fun setWebdav(url: String, user: String, pass: String) {
        context.settingsDataStore.edit {
            it[Keys.WEBDAV_URL] = url.trim()
            it[Keys.WEBDAV_USER] = user.trim()
            it[Keys.WEBDAV_PASS] = pass
        }
    }
}
