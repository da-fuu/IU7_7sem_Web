package com.ppo.data_objectbox.repository

import com.ppo.data_objectbox.entity.UserBoxEntity
import com.ppo.data_objectbox.entity.UserBoxEntity_
import com.ppo.data_objectbox.mapper.toBoxEntity
import com.ppo.data_objectbox.mapper.toDomain
import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.IUserRepository
import io.objectbox.BoxStore
import io.objectbox.query.QueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class UserRepositoryObjectBoxImpl(
    boxStore: BoxStore
) : IUserRepository {
    private val userBox = boxStore.boxFor(UserBoxEntity::class.java)
    private val logger = LoggerFactory.getLogger(UserRepositoryObjectBoxImpl::class.java)

    override suspend fun findByLogin(login: String): User? = withContext(Dispatchers.IO) {
        userBox.query(UserBoxEntity_.login.equal(login, QueryBuilder.StringOrder.CASE_SENSITIVE)).build().findFirst()?.toDomain()
    }

    override suspend fun saveUser(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existingUser = userBox.query(UserBoxEntity_.uid.equal(user.id.value.toString(), QueryBuilder.StringOrder.CASE_SENSITIVE)).build().findFirst()
            userBox.put(user.toBoxEntity(existingUser?.boxId ?: 0))
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to save user {}: {}", user.login, e.message)
            Result.failure(e)
        }
    }

    override suspend fun getUserRole(userId: UserId): UserRole? = withContext(Dispatchers.IO) {
        userBox.query(UserBoxEntity_.uid.equal(userId.value.toString(), QueryBuilder.StringOrder.CASE_SENSITIVE)).build().findFirst()?.toDomain()?.role
    }

    override suspend fun getUserById(userId: UserId): User? = withContext(Dispatchers.IO) {
        userBox.query(UserBoxEntity_.uid.equal(userId.value.toString(), QueryBuilder.StringOrder.CASE_SENSITIVE)).build().findFirst()?.toDomain()
    }

    override suspend fun getAllUsers(): List<User> = withContext(Dispatchers.IO) {
        userBox.all.map { it.toDomain() }
    }
}
