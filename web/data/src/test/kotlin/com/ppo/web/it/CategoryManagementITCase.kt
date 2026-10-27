package com.ppo.web.it

import com.ppo.domain.model.*
import com.ppo.domain.usecase.*
import com.ppo.web.data.repository.PostgresCategoryRepository
import com.ppo.web.data.repository.PostgresUserRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CategoryManagementITCase {

    private val dataSource = TestDbHelper.getDataSource()
    private val categoryRepository = PostgresCategoryRepository(dataSource)
    private val userRepository = PostgresUserRepository(dataSource)

    private val getCategoriesUseCase = GetCategoriesUseCase(categoryRepository)
    private val manageUserCategoryUseCase = ManageUserCategoryUseCase(categoryRepository)
    private val deleteCategoryUseCase = DeleteCategoryUseCase(categoryRepository)
    private val adminManageBaseCategoryUseCase = AdminManageBaseCategoryUseCase(categoryRepository, userRepository)
    private val adminDeleteBaseCategoryUseCase = AdminDeleteBaseCategoryUseCase(categoryRepository, userRepository)

    @Before
    fun setUp() {
        TestDbHelper.initTestSchema()
        TestDbHelper.resetDatabase()
    }

    @After
    fun tearDown() {
        TestDbHelper.resetDatabase()
    }

    @Test
    fun executeCategoryManagementFunctionalityScenario(): Unit = runBlocking {
        val regularUserId = UserId(UUID.randomUUID())
        val adminUserId = UserId(UUID.randomUUID())

        userRepository.saveUser(
            User(regularUserId, "regular_cat_user", "Regular Tester", "hash", UserRole.USER)
        )
        userRepository.saveUser(
            User(adminUserId, "admin_cat_user", "Admin Tester", "hash", UserRole.ADMIN)
        )

        val initialCategories = getCategoriesUseCase(regularUserId)
        assertTrue(initialCategories.any { it.name == "Работа / Проекты" && it.isDefault })
        assertTrue(initialCategories.any { it.name == "Учеба" && it.isDefault })

        val customCategoryId = CategoryId(UUID.randomUUID())
        val customCategory = Category(
            id = customCategoryId,
            userId = regularUserId,
            name = "Спорт и фитнес",
            colorHex = "#EF4444",
            isDefault = false
        )
        val createCustomResult = manageUserCategoryUseCase(customCategory)
        assertTrue(createCustomResult.isSuccess)

        val categoriesAfterCreation = getCategoriesUseCase(regularUserId)
        assertTrue(categoriesAfterCreation.any { it.id == customCategoryId && it.name == "Спорт и фитнес" })

        val maliciousDefaultCategory = Category(
            id = CategoryId(UUID.randomUUID()),
            userId = regularUserId,
            name = "Поддельная базовая",
            colorHex = "#10B981",
            isDefault = true
        )
        val createDefaultByUserResult = manageUserCategoryUseCase(maliciousDefaultCategory)
        assertTrue(createDefaultByUserResult.isFailure)
        assertTrue(createDefaultByUserResult.exceptionOrNull() is IllegalStateException)

        val newBaseCategoryId = CategoryId(UUID.randomUUID())
        val newBaseCategory = Category(
            id = newBaseCategoryId,
            userId = null,
            name = "Чтение книг",
            colorHex = "#8B5CF6",
            isDefault = true
        )
        val adminCreateBaseResult = adminManageBaseCategoryUseCase(newBaseCategory, adminUserId)
        assertTrue(adminCreateBaseResult.isSuccess)

        val visibleCategoriesForRegular = getCategoriesUseCase(regularUserId)
        assertTrue(visibleCategoriesForRegular.any { it.id == newBaseCategoryId && it.isDefault })

        val unauthorizedBaseCategory = Category(
            id = CategoryId(UUID.randomUUID()),
            userId = null,
            name = "Взлом базы",
            colorHex = "#3B82F6",
            isDefault = true
        )
        val regularUserCreateBaseResult = adminManageBaseCategoryUseCase(unauthorizedBaseCategory, regularUserId)
        assertTrue(regularUserCreateBaseResult.isFailure)
        assertTrue(regularUserCreateBaseResult.exceptionOrNull() is IllegalStateException)

        val updatedCustomCategory = customCategory.copy(name = "Фитнес и бег", colorHex = "#10B981")
        val updateResult = manageUserCategoryUseCase(updatedCustomCategory)
        assertTrue(updateResult.isSuccess)

        val categoryAfterUpdate = categoryRepository.findCategoryById(customCategoryId)
        assertNotNull(categoryAfterUpdate)
        assertEquals("Фитнес и бег", categoryAfterUpdate?.name)
        assertEquals("#10B981", categoryAfterUpdate?.colorHex)

        val regularDeleteBaseResult = adminDeleteBaseCategoryUseCase(newBaseCategoryId, regularUserId)
        assertTrue(regularDeleteBaseResult.isFailure)
        assertTrue(regularDeleteBaseResult.exceptionOrNull() is IllegalStateException)

        val deleteCustomResult = deleteCategoryUseCase(customCategoryId)
        assertTrue(deleteCustomResult.isSuccess)

        val categoriesAfterDeletion = getCategoriesUseCase(regularUserId)
        assertFalse(categoriesAfterDeletion.any { it.id == customCategoryId })

        val adminDeleteBaseResult = adminDeleteBaseCategoryUseCase(newBaseCategoryId, adminUserId)
        assertTrue(adminDeleteBaseResult.isSuccess)

        val finalCategories = getCategoriesUseCase(regularUserId)
        assertFalse(finalCategories.any { it.id == newBaseCategoryId })
    }
}
