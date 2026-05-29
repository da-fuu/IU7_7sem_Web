package com.ppo.timetracker

import android.app.Application
import android.os.Environment
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
import com.ppo.timetracker.ui.categories.CategoriesScreen
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

class TimeTrackerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val baseDir =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TimeTracker")
        if (!baseDir.exists()) baseDir.mkdirs()

        val configFile = File(baseDir, "config.properties")
        val properties = Properties()
        if (configFile.exists()) {
            try {
                configFile.inputStream().use { properties.load(it) }
            } catch (e: Exception) {
            }
        }

        // 1. Logging setup
        val logRelPath = properties.getProperty("log.path", "logs/app.log")
        val logFile = File(baseDir, logRelPath)
        if (!logFile.parentFile.exists()) logFile.parentFile.mkdirs()

        val logLevel = properties.getProperty("log.level", "INFO")
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", logLevel)
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
        System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss")

        try {
            val logStream = PrintStream(FileOutputStream(logFile, true))
            System.setErr(logStream)
            System.setOut(logStream)
        } catch (e: Exception) {
        }

        // 2. Database setup
        val dbRelPath = properties.getProperty("db.path", "db/database.db")
        val dbFile = File(baseDir, dbRelPath)
        if (!dbFile.parentFile.exists()) dbFile.parentFile.mkdirs()

        // 3. Business config
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
}
