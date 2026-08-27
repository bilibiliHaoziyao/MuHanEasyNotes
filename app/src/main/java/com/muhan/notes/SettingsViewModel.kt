package com.muhan.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.muhan.notes.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.security.MessageDigest

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val uiScale: StateFlow<Float> = repository.uiScale.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 1f
    )

    val fontScale: StateFlow<Float> = repository.fontScale.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 1f
    )

    val autoSave: StateFlow<Boolean> = repository.autoSave.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    /** 隐私中心密码哈希；null 表示未设置 */
    val privacyPassword: StateFlow<String?> = repository.privacyPassword.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    val webdavUrl: StateFlow<String> = repository.webdavUrl.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )

    val webdavUser: StateFlow<String> = repository.webdavUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )

    val webdavPass: StateFlow<String> = repository.webdavPass.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ""
    )

    fun setUiScale(value: Float) {
        viewModelScope.launch { repository.setUiScale(value.coerceIn(MIN_SCALE, MAX_SCALE)) }
    }

    fun setFontScale(value: Float) {
        viewModelScope.launch { repository.setFontScale(value.coerceIn(MIN_SCALE, MAX_SCALE)) }
    }

    fun setAutoSave(value: Boolean) {
        viewModelScope.launch { repository.setAutoSave(value) }
    }

    /** 设置/修改隐私密码（保存 SHA-256 哈希） */
    fun setPrivacyPassword(password: String, onDone: (Boolean) -> Unit = {}) {
        val hash = hashPassword(password)
        viewModelScope.launch {
            repository.setPrivacyPassword(hash)
            onDone(true)
        }
    }

    fun clearPrivacyPassword() {
        viewModelScope.launch { repository.clearPrivacyPassword() }
    }

    /** 校验密码是否正确 */
    suspend fun verifyPassword(input: String): Boolean {
        val stored = privacyPassword.value ?: return false
        return hashPassword(input) == stored
    }

    fun setWebdav(url: String, user: String, pass: String) {
        viewModelScope.launch { repository.setWebdav(url, user, pass) }
    }

    companion object {
        const val MIN_SCALE = 0.8f
        const val MAX_SCALE = 1.5f

        /** 明文密码 -> SHA-256 十六进制 */
        fun hashPassword(password: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                SettingsViewModel(SettingsRepository(app))
            }
        }
    }
}
