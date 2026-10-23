package com.ppo.web.data.repository

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.web.data.TestDatabaseFactory
import com.ppo.web.data.runner.RandomOrderRunner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(RandomOrderRunner::class)
class PostgresCategoryRepositoryTest {

    private lateinit var tempDbFile: File
    private lateinit var userRepo: PostgresUserRepository
    private lateinit var repository: PostgresCategoryRepository

    @Before
    fun setUp() {
        val (dataSource, file) = TestDatabaseFactory.createTestDataSource()
        tempDbFile = file
        userRepo = PostgresUserRepository(dataSource)
        repository = PostgresCategoryRepository(dataSource)
    }

    @After
    fun tearDown() {
        tempDbFile.delete()
    }

    @Test
    fun saveCategory_newBaseAndCustomCategories_persistsSuccessfully() = runTest {
        val user = User(UserId(UUID.randomUUID()), "user_${UUID.randomUUID()}", "Name", "hash", UserRole.USER)
        userRepo.saveUser(user)
        val customCategory = Category(
            id = CategoryId(UUID.randomUUID()),
            userId = user.id,
            name = "Personal Projects",
            colorHex = "#FF5733",
            isDefault = false
        )

        val result = repository.saveCategory(customCategory)

        assertTrue(result.isSuccess)
        val loaded = repository.findCategoryById(customCategory.id)
        assertNotNull(loaded)
        assertEquals("Personal Projects", loaded?.name)
        assertEquals("#FF5733", loaded?.colorHex)
        assertEquals(user.id, loaded?.userId)
    }

    @Test
    fun findCategoryByName_forUserAndDefault_returnsMatchingCategory() = runTest {
        val user = User(UserId(UUID.randomUUID()), "user_${UUID.randomUUID()}", "Name", "hash", UserRole.USER)
        userRepo.saveUser(user)
        val userCategory = Category(
            id = CategoryId(UUID.randomUUID()),
            userId = user.id,
            name = "Workout",
            colorHex = "#33FF57",
            isDefault = false
        )
        repository.saveCategory(userCategory)

        val foundCustom = repository.findCategoryByName(user.id, "Workout")

        assertNotNull(foundCustom)
        assertEquals(userCategory.id, foundCustom?.id)
    }

    @Test
    fun findCategoryByName_missingName_returnsNull() = runTest {
        val userId = UserId(UUID.randomUUID())

        val found = repository.findCategoryByName(userId, "NonExistentCategoryName")

        assertNull(found)
    }

    @Test
    fun getCategoriesFlow_filtersUserAndDefaultCategories() = runTest {
        val userA = User(UserId(UUID.randomUUID()), "userA_${UUID.randomUUID()}", "User A", "hash", UserRole.USER)
        val userB = User(UserId(UUID.randomUUID()), "userB_${UUID.randomUUID()}", "User B", "hash", UserRole.USER)
        userRepo.saveUser(userA)
        userRepo.saveUser(userB)
        val catA = Category(CategoryId(UUID.randomUUID()), userA.id, "Cat A", "#111111", false)
        val catB = Category(CategoryId(UUID.randomUUID()), userB.id, "Cat B", "#222222", false)
        repository.saveCategory(catA)
        repository.saveCategory(catB)

        val categoriesA = repository.getCategoriesFlow(userA.id).first()

        assertTrue(categoriesA.any { it.id == catA.id })
        assertTrue(categoriesA.none { it.id == catB.id })
        assertTrue(categoriesA.any { it.isDefault })
    }

    @Test
    fun deleteCategory_existingId_removesFromDatabase() = runTest {
        val category = Category(
            id = CategoryId(UUID.randomUUID()),
            userId = null,
            name = "ToDelete",
            colorHex = "#999999",
            isDefault = true
        )
        repository.saveCategory(category)

        val result = repository.deleteCategory(category.id)

        assertTrue(result.isSuccess)
        val loaded = repository.findCategoryById(category.id)
        assertNull(loaded)
    }

    @Test
    fun deleteCategory_missingId_returnsFailure() = runTest {
        val missingId = CategoryId(UUID.randomUUID())

        val result = repository.deleteCategory(missingId)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun saveCategory_existingCategory_updatesFields() = runTest {
        val category = Category(
            id = CategoryId(UUID.randomUUID()),
            userId = null,
            name = "Initial Cat",
            colorHex = "#111111",
            isDefault = true
        )
        repository.saveCategory(category)
        val updated = category.copy(name = "Updated Cat", colorHex = "#222222")

        val result = repository.saveCategory(updated)

        assertTrue(result.isSuccess)
        val loaded = repository.findCategoryById(category.id)
        assertEquals("Updated Cat", loaded?.name)
        assertEquals("#222222", loaded?.colorHex)
    }

    @Test
    fun findCategoryByName_nullUser_findsDefaultCategory() = runTest {
        val baseCat = Category(
            id = CategoryId(UUID.randomUUID()),
            userId = null,
            name = "System Global",
            colorHex = "#333333",
            isDefault = true
        )
        repository.saveCategory(baseCat)

        val found = repository.findCategoryByName(null, "System Global")

        assertNotNull(found)
        assertEquals(baseCat.id, found?.id)
    }

    @Test
    fun getAllCategories_populatedTable_returnsAllCategories() = runTest {
        val cat = Category(
            id = CategoryId(UUID.randomUUID()),
            userId = null,
            name = "Unique All",
            colorHex = "#444444",
            isDefault = true
        )
        repository.saveCategory(cat)

        val all = repository.getAllCategories()

        assertTrue(all.any { it.id == cat.id })
    }
}
