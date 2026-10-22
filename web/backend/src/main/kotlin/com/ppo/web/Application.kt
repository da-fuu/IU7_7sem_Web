package com.ppo.web

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ppo.domain.repository.*
import com.ppo.domain.usecase.*
import com.ppo.web.data.db.DatabaseFactory
import com.ppo.web.data.repository.*
import com.ppo.web.routes.*
import com.ppo.web.service.AutoStopWorker
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import java.io.File
import javax.sql.DataSource

fun main(args: Array<String>) {
    val port = System.getenv("PORT")?.toIntOrNull()
        ?: System.getenv("BACKEND_PORT")?.toIntOrNull()
        ?: 8080
    val logger = LoggerFactory.getLogger("TimeTrackerServer")
    logger.info("Starting TimeTracker Ktor REST API Server on port {}", port)

    val dataSource = DatabaseFactory.createDataSource()
    DatabaseFactory.initSchema(dataSource)

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        val specFile = File("web/api/openapi.yaml")
        val effectiveSpecFile = if (specFile.exists()) specFile else File("../../web/api/openapi.yaml")
        module(dataSource, effectiveSpecFile)
    }.start(wait = true)
}

fun Application.module(
    dataSource: DataSource? = null,
    specFile: File? = null
) {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            registerModule(JavaTimeModule())
            registerKotlinModule()
        }
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }

    install(CallLogging)

    val ds = dataSource ?: DatabaseFactory.createDataSource()

    val userRepository: IUserRepository = PostgresUserRepository(ds)
    val userSettingsRepository: IUserSettingsRepository = PostgresUserSettingsRepository(ds)
    val categoryRepository: ICategoryRepository = PostgresCategoryRepository(ds)
    val timeRecordRepository: ITimeRecordRepository = PostgresTimeRecordRepository(ds)
    val recordHistoryRepository: IRecordHistoryRepository = PostgresRecordHistoryRepository(ds)

    val createTimeRecordUseCase = CreateTimeRecordUseCase(timeRecordRepository, categoryRepository, userSettingsRepository)
    val updateTimeRecordUseCase = UpdateTimeRecordUseCase(timeRecordRepository, categoryRepository, userSettingsRepository, recordHistoryRepository)
    val deleteRecordUseCase = DeleteRecordUseCase(timeRecordRepository)
    val autoStopExpiredRecordsUseCase = AutoStopExpiredRecordsUseCase(timeRecordRepository, userSettingsRepository, recordHistoryRepository)
    val getRecordHistoryUseCase = GetRecordHistoryUseCase(timeRecordRepository, recordHistoryRepository)
    val rollbackRecordHistoryUseCase = RollbackRecordHistoryUseCase(timeRecordRepository, recordHistoryRepository)
    val manageUserSettingsUseCase = ManageUserSettingsUseCase(userSettingsRepository)

    val autoStopWorker = AutoStopWorker(autoStopExpiredRecordsUseCase)
    val autoStopIntervalMs = System.getenv("AUTO_STOP_INTERVAL_MS")?.toLongOrNull() ?: (10 * 60 * 1000L)
    autoStopWorker.start(this, autoStopIntervalMs)

    val spec = specFile ?: File("web/api/openapi.yaml")

    routing {
        swaggerRoutes(spec)
        authRoutes(userRepository, userSettingsRepository)
        userRoutes(userRepository, userSettingsRepository, manageUserSettingsUseCase)
        categoryRoutes(categoryRepository)
        recordRoutes(
            timeRecordRepository = timeRecordRepository,
            categoryRepository = categoryRepository,
            userSettingsRepository = userSettingsRepository,
            recordHistoryRepository = recordHistoryRepository,
            createTimeRecordUseCase = createTimeRecordUseCase,
            updateTimeRecordUseCase = updateTimeRecordUseCase,
            deleteRecordUseCase = deleteRecordUseCase,
            autoStopWorker = autoStopWorker
        )
        historyRoutes(getRecordHistoryUseCase, rollbackRecordHistoryUseCase)
    }
}
