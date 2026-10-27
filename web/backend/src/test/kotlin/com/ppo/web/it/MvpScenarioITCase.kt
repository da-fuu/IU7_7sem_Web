package com.ppo.web.it

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.ppo.web.dto.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AllureRunner::class)
class MvpScenarioITCase {

    @Test
    fun executeMvpDemonstrationScenario() = runBlocking {
        val client = HttpClient(CIO) {
            defaultRequest { url(System.getProperty("SERVER_URL") ?: "http://localhost:18081") }
            install(ContentNegotiation) {
                jackson { registerModule(JavaTimeModule()) }
            }
        }
        try {
        val ui = UiActionHelper(client)
        val iterations = System.getProperty("e2e.iterations")?.toIntOrNull()
            ?: System.getenv("E2E_ITERATIONS")?.toIntOrNull()
            ?: 1

        val runId = System.getenv("E2E_RUN_ID")
        require(runId != null && runId.matches(Regex("[0-9a-f]{32}"))) {
            "Run E2E through deploy/test/scripts/e2e_control.py"
        }
        require(iterations in 1..9999)
        var successCount = 0
        for (iteration in 1..iterations) {
            val demoLogin = "lab2e2e_${runId}_$iteration"
            val demoPassword = "MvpSecurePassword2026"

            val (healthStatus, healthLatency) = ui.clickHealthCheck()
            assertEquals(HttpStatusCode.OK, healthStatus)


            val (regStatus, authResult) = ui.clickRegisterButton(demoLogin, "MVP Demo Presenter", demoPassword, "USER")
            assertEquals(HttpStatusCode.Created, regStatus)
            val demoUserId = authResult.user.id
            val authToken = authResult.token
            assertNotNull(authToken)

            val (settingsStatus, _) = ui.clickSaveSettingsButton(
                authToken,
                demoUserId,
                UserSettingsUpdateRequestDto(
                    autoStopEnabled = true,
                    maxActiveDurationValue = 4,
                    maxActiveDurationUnit = "hours",
                    minDurationEnabled = false,
                    minDurationValue = 1,
                    minDurationUnit = "minutes"
                )
            )
            assertEquals(HttpStatusCode.OK, settingsStatus)

            val (categoryStatus, mvpCategory) = ui.clickCreateCategoryButton(authToken, "Разработка MVP", "#6366F1")
            assertEquals(HttpStatusCode.Created, categoryStatus)
            val mvpCategoryId = mvpCategory.id

            val (startStatus, activeRecord, startLatency) = ui.clickStartTimerButton(
                authToken,
                mvpCategoryId,
                "Кодирование демо-сценария"
            )
            assertEquals(HttpStatusCode.Created, startStatus)
            assertNull(activeRecord.endTime)


            val (queryStatus, activeRecords) = ui.clickQueryActiveRecords(authToken)
            assertEquals(HttpStatusCode.OK, queryStatus)
            assertEquals(1, activeRecords.items.size)
            assertEquals(activeRecord.id, activeRecords.items.first().id)

            val (stopStatus, stoppedRecord) = ui.clickStopTimerButton(
                authToken,
                activeRecord.id,
                Instant.now().plusSeconds(180).toString()
            )
            assertEquals(HttpStatusCode.OK, stopStatus)
            assertNotNull(stoppedRecord.endTime)

            val (updateStatus, _) = ui.clickUpdateRecordButton(
                authToken,
                activeRecord.id,
                mvpCategoryId,
                stoppedRecord.startTime,
                stoppedRecord.endTime,
                "Доработка спецификации перед релизом"
            )
            assertEquals(HttpStatusCode.OK, updateStatus)

            val (historyStatus, history) = ui.clickInspectHistoryButton(authToken, activeRecord.id)
            assertEquals(HttpStatusCode.OK, historyStatus)
            assertTrue(history.items.isNotEmpty())
            val initialRevision = history.items.first()
            assertEquals("Кодирование демо-сценария", initialRevision.description)

            val (rollbackStatus, rolledBackRecord) = ui.clickRollbackHistoryButton(
                authToken,
                activeRecord.id,
                initialRevision.id
            )
            assertEquals(HttpStatusCode.OK, rollbackStatus)
            assertEquals("Кодирование демо-сценария", rolledBackRecord.description)

            val deleteStatus = ui.clickDeleteRecordButton(authToken, activeRecord.id)
            assertEquals(HttpStatusCode.NoContent, deleteStatus)

            successCount++
        }

        assertEquals(iterations, successCount)
        } finally { client.close() }
    }
}
