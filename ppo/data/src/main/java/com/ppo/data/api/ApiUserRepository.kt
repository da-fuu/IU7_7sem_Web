package com.ppo.data.api

import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.IUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ApiUserRepository(
    private val client: RestApiClient
) : IUserRepository {
    override suspend fun findByLogin(login: String): User? = withContext(Dispatchers.IO) {
        null
    }

    override suspend fun saveUser(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        val json = """{"login":"${user.login}","name":"${user.name}","password":"${user.passwordHash}","role":"${user.role.name}"}"""
        val res = client.request("POST", "/auth/register", json)
        if (res.statusCode in 200..299) {
            val map = JsonMini.parseObject(res.body)
            val token = map["token"] as? String
            if (token != null) client.saveToken(token)
            Result.success(Unit)
        } else {
            Result.failure(Exception(res.body))
        }
    }

    override suspend fun getUserRole(userId: UserId): UserRole? = withContext(Dispatchers.IO) {
        getUserById(userId)?.role
    }

    override suspend fun getUserById(userId: UserId): User? = withContext(Dispatchers.IO) {
        val res = client.request("GET", "/users/${userId.value}")
        if (res.statusCode == 200) {
            val map = JsonMini.parseObject(res.body)
            val idStr = map["id"] as? String ?: return@withContext null
            val login = map["login"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val roleStr = map["role"] as? String ?: "USER"
            User(
                id = UserId(UUID.fromString(idStr)),
                login = login,
                name = name,
                passwordHash = "",
                role = UserRole.valueOf(roleStr)
            )
        } else null
    }

    override suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        emptyList()
    }
}
