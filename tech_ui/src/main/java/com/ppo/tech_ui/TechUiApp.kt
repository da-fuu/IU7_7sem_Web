package com.ppo.tech_ui

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ppo.data.PasswordHasherImpl
import com.ppo.data.db.AppDatabase
import com.ppo.data.repository.*
import com.ppo.domain.repository.*
import com.ppo.domain.usecase.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

val appModule = module {
    single {
        val appDir = File(System.getProperty("user.home"), ".timetracker")
        if (!appDir.exists()) appDir.mkdirs()
        appDir
    }

    single { File(get<File>(), "session.txt") }

    single {
        val dbFile = File(get<File>(), "timetracker.db")
        Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
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

    single {
        CliController(
            sessionFile = get(),
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

fun main(args: Array<String>) {
    val koinApp = startKoin {
        modules(appModule)
    }

    val controller = koinApp.koin.get<CliController>()
    controller.handleArgs(args)

    koinApp.koin.get<AppDatabase>().close()
}
