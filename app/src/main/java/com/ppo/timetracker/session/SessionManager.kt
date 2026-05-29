package com.ppo.timetracker.session

import com.ppo.domain.model.UserId
import java.io.File
import java.util.UUID

class SessionManager(private val sessionFile: File) {
    var currentUserId: UserId? = null
        private set

    init {
        loadSession()
    }

    private fun loadSession() {
        if (sessionFile.exists()) {
            try {
                val idStr = sessionFile.readText().trim()
                if (idStr.isNotEmpty()) {
                    currentUserId = UserId(UUID.fromString(idStr))
                }
            } catch (e: Exception) {
                sessionFile.delete()
            }
        }
    }

    fun login(userId: UserId) {
        currentUserId = userId
        sessionFile.writeText(userId.value.toString())
    }

    fun logout() {
        currentUserId = null
        if (sessionFile.exists()) {
            sessionFile.delete()
        }
    }
}
