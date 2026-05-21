package com.ppo.tech_ui

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ppo.data.db.AppDatabase
import com.ppo.data.PasswordHasherImpl
import com.ppo.data.repository.*
import com.ppo.domain.usecase.*
import java.io.File
import kotlinx.coroutines.Dispatchers

fun main(args: Array<String>) {
    val appDir = File(System.getProperty("user.home"), ".timetracker")
    if (!appDir.exists()) {
        appDir.mkdirs()
    }

    val dbFile = File(appDir, "timetracker.db")
    val sessionFile = File(appDir, "session.txt")

    val db = Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    val userRepository = UserRepositoryImpl(db.userDao())
    val categoryRepository = CategoryRepositoryImpl(db.categoryDao())
    val timeRecordRepository = TimeRecordRepositoryImpl(db.timeRecordDao())
    val passwordHasher = PasswordHasherImpl()

    val registerUseCase = RegisterUserUseCase(userRepository, passwordHasher)
    val loginUseCase = LoginUserUseCase(userRepository, passwordHasher)
    val startTimerUseCase = StartTimerUseCase(timeRecordRepository)
    val stopTimerUseCase = StopTimerUseCase(timeRecordRepository)
    val addManualRecordUseCase = AddManualRecordUseCase(timeRecordRepository)
    val adminManageBaseCategoryUseCase = AdminManageBaseCategoryUseCase(categoryRepository, userRepository)
    val manageUserCategoryUseCase = ManageUserCategoryUseCase(categoryRepository)
    val getCategoriesUseCase = GetCategoriesUseCase(categoryRepository)
    val getTimeRecordsUseCase = GetTimeRecordsUseCase(timeRecordRepository)
    val deleteCategoryUseCase = DeleteCategoryUseCase(categoryRepository)
    val adminDeleteBaseCategoryUseCase = AdminDeleteBaseCategoryUseCase(categoryRepository, userRepository)

    val controller = CliController(
        sessionFile = sessionFile,
        registerUseCase = registerUseCase,
        loginUseCase = loginUseCase,
        startTimerUseCase = startTimerUseCase,
        stopTimerUseCase = stopTimerUseCase,
        addManualRecordUseCase = addManualRecordUseCase,
        adminManageBaseCategoryUseCase = adminManageBaseCategoryUseCase,
        manageUserCategoryUseCase = manageUserCategoryUseCase,
        getCategoriesUseCase = getCategoriesUseCase,
        getTimeRecordsUseCase = getTimeRecordsUseCase,
        deleteCategoryUseCase = deleteCategoryUseCase,
        adminDeleteBaseCategoryUseCase = adminDeleteBaseCategoryUseCase
    )

    controller.handleArgs(args)

    db.close()
}
