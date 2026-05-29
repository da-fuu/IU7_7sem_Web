package com.ppo.timetracker

import android.app.Application
import android.os.Environment
import android.widget.Toast
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ppo.data.PasswordHasherImpl
import com.ppo.data.db.AppDatabase
import com.ppo.data.repository.CategoryRepositoryImpl
import com.ppo.data.repository.TimeRecordRepositoryImpl
import com.ppo.data.repository.UserRepositoryImpl
import com.ppo.domain.model.BusinessConfig
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.IPasswordHasher
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserRepository
import com.ppo.domain.usecase.*
import com.ppo.timetracker.session.SessionManager
import com.ppo.timetracker.ui.auth.AuthViewModel
import com.ppo.timetracker.ui.categories.CategoriesViewModel
import com.ppo.timetracker.ui.dashboard.DashboardViewModel
import com.ppo.timetracker.ui.records.RecordsViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.Properties
import kotlin.system.exitProcess

class TimeTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val baseDir =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TimeTracker")

        if (!baseDir.exists() && !baseDir.mkdirs()) {
            handleFatalError("Не удалось создать директорию приложения в Downloads")
            return
        }

        val configFile = File(baseDir, "config.properties")
        val properties = Properties()
        if (configFile.exists()) {
            try {
                configFile.inputStream().use { properties.load(it) }
            } catch (e: Exception) {
            }
        }

        val logPathStr = properties.getProperty("log.path", "logs/app.log")
        val logFile = resolvePath(baseDir, logPathStr)

        try {
            val parent = logFile.parentFile
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) throw Exception("Не удалось создать папку для логов")
            }
            if (!logFile.exists()) {
                if (!logFile.createNewFile()) throw Exception("Не удалось создать файл логов")
            }
            if (!logFile.canWrite()) throw Exception("Нет прав на запись в файл логов")

            val logStream = PrintStream(FileOutputStream(logFile, true))
            System.setErr(logStream)
            System.setOut(logStream)
        } catch (e: Exception) {
            handleFatalError("Ошибка инициализации логов: ${e.message}\nПуть: ${logFile.absolutePath}")
            return
        }

        val logLevel = properties.getProperty("log.level", "INFO")
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", logLevel)
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss")

        val dbPathStr = properties.getProperty("db.path", "db/database.db")
        val dbFile = resolvePath(baseDir, dbPathStr)
        try {
            val parent = dbFile.parentFile
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) throw Exception("Не удалось создать папку для БД")
            }
        } catch (e: Exception) {
            handleFatalError("Ошибка подготовки БД: ${e.message}")
            return
        }

        val minDuration = properties.getProperty("min.duration.minutes", "1").toLong()
        val businessConfig = BusinessConfig(minDurationMinutes = minDuration)

        startKoin {
            androidContext(this@TimeTrackerApp)
            modules(module {
                single { businessConfig }
                single(named("db_file")) { dbFile }
                single(named("session_file")) { File(baseDir, "session.txt") }

                single {
                    val file = get<File>(named("db_file"))
                    Room.databaseBuilder<AppDatabase>(
                        androidContext(),
                        AppDatabase::class.java,
                        file.absolutePath
                    )
                        .setDriver(BundledSQLiteDriver())
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

                single { SessionManager(get(named("session_file"))) }

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

                factory { AuthViewModel(get(), get(), get()) }
                factory { DashboardViewModel(get(), get(), get(), get(), get()) }
                factory { CategoriesViewModel(get(), get(), get(), get(), get(), get(), get()) }
                factory { RecordsViewModel(get(), get(), get(), get(), get()) }
            })
        }
    }

    private fun resolvePath(baseDir: File, path: String): File {
        return if (path.startsWith("~/")) {
            File(Environment.getExternalStorageDirectory(), path.substring(2))
        } else if (path == "~") {
            Environment.getExternalStorageDirectory()
        } else {
            File(baseDir, path)
        }
    }

    private fun handleFatalError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Thread.sleep(2000)
        exitProcess(1)
    }
}
