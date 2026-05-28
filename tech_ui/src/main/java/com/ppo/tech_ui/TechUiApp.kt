package com.ppo.tech_ui

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ppo.data.PasswordHasherImpl
import com.ppo.data.db.AppDatabase
import com.ppo.data.repository.*
import com.ppo.domain.repository.*
import com.ppo.domain.usecase.*
import com.ppo.domain.model.BusinessConfig
import java.io.File
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.koin.core.qualifier.named

import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    val configFile = File("config.properties")
    val properties = Properties()
    if (configFile.exists()) {
        configFile.inputStream().use { properties.load(it) }
    }

    // Logging config
    val envLogPath = System.getenv("TIMETRACKER_LOG_PATH")
    val propLogPath = properties.getProperty("log.path")
    val defaultLogPath = "logs/app.log"
    val logPath = envLogPath ?: propLogPath ?: defaultLogPath
    System.setProperty("LOG_FILE", logPath)

    val envLogLevel = System.getenv("TIMETRACKER_LOG_LEVEL")
    val propLogLevel = properties.getProperty("log.level")
    val defaultLogLevel = "INFO"
    val logLevel = envLogLevel ?: propLogLevel ?: defaultLogLevel
    System.setProperty("LOG_LEVEL", logLevel)

    val logger = LoggerFactory.getLogger("TechUiApp")
    logger.info("Starting TimeTracker CLI with log level: {} and path: {}", logLevel, logPath)

    val envDbPath = System.getenv("TIMETRACKER_DB_PATH")
    val propDbPath = properties.getProperty("db.path")
    val defaultDbPath = File(System.getProperty("user.home"), ".timetracker/timetracker.db").absolutePath
    val dbPath = envDbPath ?: propDbPath ?: defaultDbPath

    val envMinDuration = System.getenv("TIMETRACKER_MIN_DURATION")
    val propMinDuration = properties.getProperty("min.duration.minutes")
    val defaultMinDuration = "1"
    val minDuration = (envMinDuration ?: propMinDuration ?: defaultMinDuration).toLong()

    val businessConfig = BusinessConfig(minDurationMinutes = minDuration)

    val dynamicModule = module {
        single { businessConfig }

        single(named("db_file")) {
            val dbFile = File(dbPath)
            val appDir = dbFile.parentFile
            if (appDir != null && !appDir.exists()) appDir.mkdirs()
            dbFile
        }

        single(named("session_file")) { File(get<File>(named("db_file")).parentFile, "session.txt") }

        single {
            Room.databaseBuilder<AppDatabase>(name = get<File>(named("db_file")).absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }

        single { get<AppDatabase>().userDao() }
        single { get<AppDatabase>().categoryDao() }
        single { get<AppDatabase>().timeRecordDao() }

        single<IUserRepository> { UserRepositoryImpl(get()) }
        single<ICategoryRepository> { CategoryRepositoryImpl(get()) }
        single<ITimeRecordRepository> { TimeRecordRepositoryImpl(get()) }
        single<IPasswordHasher> { PasswordHasherImpl() }

        factoryOf(::RegisterUserUseCase)
        factoryOf(::LoginUserUseCase)
        factoryOf(::StartTimerUseCase)
        factoryOf(::StopTimerUseCase)
        factoryOf(::AddManualRecordUseCase)
        factoryOf(::AdminManageBaseCategoryUseCase)
        factoryOf(::ManageUserCategoryUseCase)
        factoryOf(::GetCategoriesUseCase)
        factoryOf(::GetTimeRecordsUseCase)
        factoryOf(::DeleteCategoryUseCase)
        factoryOf(::AdminDeleteBaseCategoryUseCase)
        factoryOf(::EditTimeRecordUseCase)

        single {
            CliController(
                sessionFile = get(named("session_file")),
                userRepository = get(),
                registerUseCase = get(),
                loginUseCase = get(),
                startTimerUseCase = get(),
                stopTimerUseCase = get(),
                addManualRecordUseCase = get(),
                adminManageBaseCategoryUseCase = get(),
                manageUserCategoryUseCase = get(),
                getCategoriesUseCase = get(),
                getTimeRecordsUseCase = get(),
                deleteCategoryUseCase = get(),
                adminDeleteBaseCategoryUseCase = get()
            )
        }
    }

    val koinApp = startKoin {
        modules(dynamicModule)
    }

    val controller = koinApp.koin.get<CliController>()
    controller.handleArgs(args)

    koinApp.koin.get<AppDatabase>().close()
}
