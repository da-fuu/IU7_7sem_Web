package com.ppo.data.repository

import com.ppo.data.db.dao.UserDao
import com.ppo.data.mapper.toDomain
import com.ppo.data.mapper.toEntity
import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.IUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.slf4j.LoggerFactory

class UserRepositoryImpl(
    private val userDao: UserDao
) : IUserRepository {
    private val logger = LoggerFactory.getLogger(UserRepositoryImpl::class.java)

    override suspend fun findByLogin(login: String): User? = withContext(Dispatchers.IO) {
        logger.debug("Searching for user by login: {}", login)
        userDao.findByLogin(login)?.toDomain()
    }

    override suspend fun saveUser(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        logger.info("Saving new user: {}", user.login)
        try {
            userDao.insert(user.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to save user {}: {}", user.login, e.message)
            Result.failure(e)
        }
    }


    override suspend fun getUserRole(userId: UserId): UserRole? = withContext(Dispatchers.IO) {
        userDao.getUserRole(userId.value.toString())
    }

    override suspend fun getUserById(userId: UserId): User? = withContext(Dispatchers.IO) {
        userDao.findById(userId.value.toString())?.toDomain()
    }

    override suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        userDao.getAll().map { it.toDomain() }
    }
}
