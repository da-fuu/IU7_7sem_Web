package com.ppo.web.data.repository

import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.web.data.TestDatabaseFactory
import com.ppo.web.data.runner.RandomOrderRunner
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
class PostgresUserRepositoryTest {

    private lateinit var tempDbFile: File
    private lateinit var repository: PostgresUserRepository

    @Before
    fun setUp() {
        val (dataSource, file) = TestDatabaseFactory.createTestDataSource()
        tempDbFile = file
        repository = PostgresUserRepository(dataSource)
    }

    @After
    fun tearDown() {
        tempDbFile.delete()
    }

    @Test
    fun saveUser_newEntity_persistsInDatabase() = runTest {
        val user = User(
            id = UserId(UUID.randomUUID()),
            login = "test_user_${UUID.randomUUID()}",
            name = "Test User",
            passwordHash = "hash123",
            role = UserRole.USER
        )

        val result = repository.saveUser(user)

        assertTrue(result.isSuccess)
        val loaded = repository.getUserById(user.id)
        assertNotNull(loaded)
        assertEquals(user.login, loaded?.login)
        assertEquals(user.name, loaded?.name)
        assertEquals(user.role, loaded?.role)
    }

    @Test
    fun saveUser_existingEntity_updatesFields() = runTest {
        val userId = UserId(UUID.randomUUID())
        val user = User(
            id = userId,
            login = "initial_login",
            name = "Initial Name",
            passwordHash = "initial_hash",
            role = UserRole.USER
        )
        repository.saveUser(user)
        val updatedUser = user.copy(
            name = "Updated Name",
            role = UserRole.ADMIN
        )

        val result = repository.saveUser(updatedUser)

        assertTrue(result.isSuccess)
        val loaded = repository.getUserById(userId)
        assertEquals("Updated Name", loaded?.name)
        assertEquals(UserRole.ADMIN, loaded?.role)
    }

    @Test
    fun findByLogin_existingLogin_returnsUser() = runTest {
        val login = "unique_login_${UUID.randomUUID()}"
        val user = User(
            id = UserId(UUID.randomUUID()),
            login = login,
            name = "Login User",
            passwordHash = "secret_hash",
            role = UserRole.USER
        )
        repository.saveUser(user)

        val found = repository.findByLogin(login)

        assertNotNull(found)
        assertEquals(user.id, found?.id)
        assertEquals(login, found?.login)
    }

    @Test
    fun findByLogin_missingLogin_returnsNull() = runTest {
        val missingLogin = "non_existent_login_${UUID.randomUUID()}"

        val found = repository.findByLogin(missingLogin)

        assertNull(found)
    }

    @Test
    fun getUserRole_existingUser_returnsRoleEnum() = runTest {
        val user = User(
            id = UserId(UUID.randomUUID()),
            login = "admin_user_${UUID.randomUUID()}",
            name = "Admin",
            passwordHash = "admin_hash",
            role = UserRole.ADMIN
        )
        repository.saveUser(user)

        val role = repository.getUserRole(user.id)

        assertEquals(UserRole.ADMIN, role)
    }

    @Test
    fun getUserRole_missingUser_returnsNull() = runTest {
        val missingUserId = UserId(UUID.randomUUID())

        val role = repository.getUserRole(missingUserId)

        assertNull(role)
    }

    @Test
    fun getAllUsers_populatedTable_returnsAllEntities() = runTest {
        val user1 = User(UserId(UUID.randomUUID()), "user1_${UUID.randomUUID()}", "User 1", "hash", UserRole.USER)
        val user2 = User(UserId(UUID.randomUUID()), "user2_${UUID.randomUUID()}", "User 2", "hash", UserRole.ADMIN)
        repository.saveUser(user1)
        repository.saveUser(user2)

        val users = repository.getAllUsers()

        assertTrue(users.size >= 2)
        assertTrue(users.any { it.id == user1.id })
        assertTrue(users.any { it.id == user2.id })
    }
}
