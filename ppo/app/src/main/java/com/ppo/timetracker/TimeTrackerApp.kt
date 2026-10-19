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
import com.ppo.data_objectbox.entity.MyObjectBox
import com.ppo.data_objectbox.repository.CategoryRepositoryObjectBoxImpl
import com.ppo.data_objectbox.repository.TimeRecordRepositoryObjectBoxImpl
import com.ppo.data_objectbox.repository.UserRepositoryObjectBoxImpl
import com.ppo.migration.DataMigrationUseCase
import com.ppo.timetracker.migration.MigrationManager
import com.ppo.timetracker.ui.migration.MigrationViewModel
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

        val objectBoxPathStr = properties.getProperty("objectbox.path", "db/objectbox")
        val objectBoxFile = resolvePath(baseDir, objectBoxPathStr)
        try {
            if (!objectBoxFile.exists()) {
                objectBoxFile.mkdirs()
            }
        } catch (e: Exception) {
            handleFatalError("Ошибка подготовки ObjectBox БД: ${e.message}")
            return
        }

        val minDurationStr = properties.getProperty("min.duration.minutes", "1")
        val minDuration = minDurationStr.toLongOrNull() ?: 1L
        val businessConfig = BusinessConfig(minDurationMinutes = minDuration)

        val apiUrl = properties.getProperty("api.url")
        val isApiMode = properties.getProperty("storage.type", "objectbox") == "api" || apiUrl != null
        val effectiveApiUrl = apiUrl ?: "http://10.0.2.2:8080/api/v1"

        startKoin {
            androidContext(this@TimeTrackerApp)
            modules(module {
                single { businessConfig }
                single(named("db_file")) { dbFile }
                val sessionFile = File(baseDir, "session.txt")
                single(named("session_file")) { sessionFile }

                val apiClient = com.ppo.data.api.RestApiClient(effectiveApiUrl, sessionFile)
                single(named("api_client")) { apiClient }

                single {
                    val file = get<File>(named("db_file"))
                    Room.databaseBuilder(
                        androidContext(),
                        AppDatabase::class.java,
                        file.absolutePath
                    )
                        .setDriver(BundledSQLiteDriver())
                        .fallbackToDestructiveMigration(true)
                        .setQueryCoroutineContext(Dispatchers.IO)
                        .build()
                }

                single {
                    MyObjectBox.builder().directory(objectBoxFile).build()
                }

                single { get<AppDatabase>().userDao() }
                single { get<AppDatabase>().categoryDao() }
                single { get<AppDatabase>().timeRecordDao() }
                single { get<AppDatabase>().timeRecordHistoryDao() }

                single<IUserRepository>(named("room")) { UserRepositoryImpl(get()) }
                single<ICategoryRepository>(named("room")) { CategoryRepositoryImpl(get()) }
                single<ITimeRecordRepository>(named("room")) { TimeRecordRepositoryImpl(get(), get()) }

                single<IUserRepository>(named("objectbox")) { UserRepositoryObjectBoxImpl(get()) }
                single<ICategoryRepository>(named("objectbox")) { CategoryRepositoryObjectBoxImpl(get()) }
                single<ITimeRecordRepository>(named("objectbox")) { TimeRecordRepositoryObjectBoxImpl(get()) }

                single<IUserRepository>(named("api")) { com.ppo.data.api.ApiUserRepository(get(named("api_client"))) }
                single<ICategoryRepository>(named("api")) { com.ppo.data.api.ApiCategoryRepository(get(named("api_client"))) }
                single<ITimeRecordRepository>(named("api")) { com.ppo.data.api.ApiTimeRecordRepository(get(named("api_client"))) }

                single<IUserRepository> { if (isApiMode) get(named("api")) else get(named("objectbox")) }
                single<ICategoryRepository> { if (isApiMode) get(named("api")) else get(named("objectbox")) }
                single<ITimeRecordRepository> { if (isApiMode) get(named("api")) else get(named("objectbox")) }

                single<IPasswordHasher> { PasswordHasherImpl() }

                single { SessionManager(get(named("session_file"))) }

                single {
                    DataMigrationUseCase(
                        get(named("room")), get(named("objectbox")),
                        get(named("room")), get(named("objectbox")),
                        get(named("room")), get(named("objectbox"))
                    )
                }

                single {
                    val file = get<File>(named("db_file"))
                    MigrationManager(file.absolutePath, get())
                }

                factoryOf(::RegisterUserUseCase)
                factoryOf(::LoginUserUseCase)
                factoryOf(::GetUserInfoUseCase)
                factoryOf(::StartTimerUseCase)
                factoryOf(::StopTimerUseCase)
                factoryOf(::AddManualRecordUseCase)
                factoryOf(::AdminManageBaseCategoryUseCase)
                factoryOf(::ManageUserCategoryUseCase)
                factoryOf(::GetCategoriesUseCase)
                factoryOf(::GetTimeRecordsUseCase)
                factoryOf(::DeleteCategoryUseCase)
                factoryOf(::AdminDeleteBaseCategoryUseCase)
                factoryOf(::UpdateRecordDescriptionUseCase)
                factoryOf(::RollbackRecordDescriptionUseCase)
                factoryOf(::GetDescriptionHistoryUseCase)

                factory { MigrationViewModel(get()) }
                factory { AuthViewModel(get(), get(), get(), get()) }
                factory { DashboardViewModel(get(), get(), get(), get(), get()) }
                factory { CategoriesViewModel(get(), get(), get(), get(), get(), get(), get()) }
                factory { RecordsViewModel(get(), get(), get(), get(), get(), get(), get()) }
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
