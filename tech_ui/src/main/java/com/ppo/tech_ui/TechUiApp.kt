package com.ppo.tech_ui

import androidx.room.Room
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

import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val configFile = File("config.properties")
    val properties = Properties()
    if (configFile.exists()) {
        configFile.inputStream().use { properties.load(it) }
    }

    val envLogPath = System.getenv("TIMETRACKER_LOG_PATH")
    val propLogPath = properties.getProperty("log.path")
    var logPath = envLogPath ?: propLogPath ?: "logs/app.log"

    if (logPath.startsWith("~/")) {
        logPath = System.getProperty("user.home") + logPath.substring(1)
    }

    try {
        val logFile = File(logPath)
        val parent = logFile.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            System.err.println("Error: Не удалось создать директорию для логов по пути ${parent.absolutePath}")
            return
        }
        val logStream = java.io.PrintStream(java.io.FileOutputStream(logFile, true))
        System.setErr(logStream)
    } catch (e: Exception) {
        System.err.println("Error: Не удалось инициализировать файл логов $logPath: ${e.message}")
        return
    }

    val envLogLevel = System.getenv("TIMETRACKER_LOG_LEVEL")
    val propLogLevel = properties.getProperty("log.level")
    val logLevel = envLogLevel ?: propLogLevel ?: "INFO"

    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", logLevel)
    System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
    System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss")

    if (System.getProperty("java.vm.name") == "Dalvik") {
        try {
            val libPath = "/data/local/tmp/timetracker/lib/arm64-v8a/libsqliteJni.so"
            if (File(libPath).exists()) {
                System.load(libPath)
            }
        } catch (e: Throwable) {
        }
    }

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
            val dbFile = get<File>(named("db_file"))
            val driver = createSQLiteDriver()

            Room.databaseBuilder<AppDatabase>(
                name = dbFile.absolutePath,
                factory = {
                    Class.forName("com.ppo.data.db.AppDatabase_Impl")
                        .getDeclaredConstructor()
                        .newInstance() as AppDatabase
                }
            )
                .setDriver(driver)
                .fallbackToDestructiveMigration(true)
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

    try {
        val db = koinApp.koin.get<AppDatabase>()
        runBlocking {
            db.userDao().findByLogin("dummy")
        }
    } catch (e: Exception) {
    }

    val controller = koinApp.koin.get<CliController>()
    controller.handleArgs(args)

    koinApp.koin.get<AppDatabase>().close()
}

fun createSQLiteDriver(): SQLiteDriver {
    val vmName = System.getProperty("java.vm.name")
    val isAndroid = vmName == "Dalvik"

    try {
        val clazz = Class.forName("androidx.sqlite.driver.bundled.BundledSQLiteDriver")
        return clazz.getDeclaredConstructor().newInstance() as SQLiteDriver
    } catch (e: Throwable) {
    }

    if (isAndroid) {
        val possibleDrivers = listOf(
            "androidx.sqlite.driver.AndroidSQLiteDriver",
            "androidx.sqlite.driver.framework.FrameworkSQLiteDriver"
        )

        for (driverClassName in possibleDrivers) {
            try {
                val clazz = Class.forName(driverClassName)
                return clazz.getDeclaredConstructor().newInstance() as SQLiteDriver
            } catch (e: Throwable) {
            }
        }
    }

    throw RuntimeException("Could not load any SQLite driver")
}
