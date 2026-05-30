package com.ppo.timetracker.session

import com.ppo.domain.model.UserId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

class SessionManager(private val sessionFile: File) {
    private val _currentUserId = MutableStateFlow<UserId?>(null)
    val currentUserIdFlow: StateFlow<UserId?> = _currentUserId.asStateFlow()

    val currentUserId: UserId? get() = _currentUserId.value

    init {
        loadSession()
    }

    private fun loadSession() {
        if (sessionFile.exists()) {
            try {
                val idStr = sessionFile.readText().trim()
                if (idStr.isNotEmpty()) {
                    _currentUserId.value = UserId(UUID.fromString(idStr))
                }
            } catch (e: Exception) {
                sessionFile.delete()
            }
        }
    }

    fun login(userId: UserId) {
        _currentUserId.value = userId
        sessionFile.writeText(userId.value.toString())
    }

    fun logout() {
        _currentUserId.value = null
        if (sessionFile.exists()) {
            sessionFile.delete()
        }
    }
}
