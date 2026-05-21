package com.ppo.tech_ui

import com.ppo.domain.model.*
import com.ppo.domain.usecase.*
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking

class CliController(
    private val sessionFile: File,
    private val registerUseCase: RegisterUserUseCase,
    private val loginUseCase: LoginUserUseCase,
    private val startTimerUseCase: StartTimerUseCase,
    private val stopTimerUseCase: StopTimerUseCase,
    private val addManualRecordUseCase: AddManualRecordUseCase,
    private val adminManageBaseCategoryUseCase: AdminManageBaseCategoryUseCase
) {

    fun handleArgs(args: Array<String>) {
        if (args.isEmpty()) {
            printHelp()
            return
        }

        val command = args[0]
        runBlocking {
            when (command) {
                "register" -> handleRegister(args.drop(1))
                "login" -> handleLogin(args.drop(1))
                "logout" -> handleLogout()
                "start" -> handleStartTimer(args.drop(1))
                "stop" -> handleStopTimer()
                "add-manual" -> handleAddManual(args.drop(1))
                "create-base-category" -> handleCreateBaseCategory(args.drop(1))
                else -> println("Unknown command")
            }
        }
    }

    private suspend fun handleRegister(args: List<String>) {
        if (args.size < 3) {
            println("Usage: register <login> <name> <password> [USER/ADMIN]")
            return
        }
        val role = if (args.getOrNull(3)?.uppercase() == "ADMIN") UserRole.ADMIN else UserRole.USER
        val result = registerUseCase(args[0], args[1], args[2], role)

        result.onSuccess { println("Success: User registered") }
            .onFailure { println("Error: ${it.message}") }
    }

    private suspend fun handleLogin(args: List<String>) {
        if (args.size < 2) {
            println("Usage: login <login> <password>")
            return
        }
        val result = loginUseCase(args[0], args[1])
        result.onSuccess { user ->
            sessionFile.writeText(user.id.value.toString())
            println("Success: Welcome, ${user.name}")
        }.onFailure {
            println("Error: ${it.message}")
        }
    }

    private fun handleLogout() {
        if (sessionFile.exists()) {
            sessionFile.delete()
            println("Success: Logged out")
        } else {
            println("Not authenticated")
        }
    }

    private suspend fun handleStartTimer(args: List<String>) {
        val userId = getAuthenticatedUserOrNull() ?: return
        if (args.isEmpty()) {
            println("Usage: start <category_uuid>")
            return
        }
        val categoryId = CategoryId(UUID.fromString(args[0]))
        val result = startTimerUseCase(userId, categoryId, Instant.now())

        result.onSuccess { println("Success: Timer started") }
            .onFailure { println("Error: ${it.message}") }
    }

    private suspend fun handleStopTimer() {
        val userId = getAuthenticatedUserOrNull() ?: return
        val result = stopTimerUseCase(userId, Instant.now())

        result.onSuccess { println("Success: Timer stopped") }
            .onFailure { println("Error: ${it.message}") }
    }

    private suspend fun handleAddManual(args: List<String>) {
        val userId = getAuthenticatedUserOrNull() ?: return
        if (args.size < 4) {
            println("Usage: add-manual <category_uuid> <start_iso> <end_iso> <description>")
            return
        }
        val categoryId = CategoryId(UUID.fromString(args[0]))
        val start = Instant.parse(args[1])
        val end = Instant.parse(args[2])
        val record = TimeRecord(
            id = RecordId(UUID.randomUUID()),
            userId = userId,
            categoryId = categoryId,
            startTime = start,
            endTime = end,
            description = args[3]
        )
        val result = addManualRecordUseCase(record)
        result.onSuccess { println("Success: Record saved") }
            .onFailure { println("Error: ${it.message}") }
    }

    private suspend fun handleCreateBaseCategory(args: List<String>) {
        val userId = getAuthenticatedUserOrNull() ?: return
        if (args.size < 2) {
            println("Usage: create-base-category <name> <color_hex>")
            return
        }
        val category = Category(
            id = CategoryId(UUID.randomUUID()),
            userId = null,
            name = args[0],
            colorHex = args[1],
            isDefault = true
        )
        val result = adminManageBaseCategoryUseCase(category, userId)
        result.onSuccess { println("Success: Base category created") }
            .onFailure { println("Error: ${it.message}") }
    }

    private fun getAuthenticatedUserOrNull(): UserId? {
        if (!sessionFile.exists()) {
            println("Error: Authentication required")
            return null
        }
        return UserId(UUID.fromString(sessionFile.readText().trim()))
    }

    private fun printHelp() {
        println(
            """
            === TimeTracker CLI ===
            Available commands:
              register <login> <name> <password> [USER/ADMIN]
              login <login> <password>
              logout
              start <category_uuid>
              stop
              add-manual <category_uuid> <start_iso> <end_iso> <description>
              create-base-category <name> <color_hex>
        """.trimIndent()
        )
    }
}
