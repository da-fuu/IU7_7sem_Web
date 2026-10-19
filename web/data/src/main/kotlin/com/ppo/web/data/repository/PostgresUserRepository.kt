package com.ppo.web.data.repository

import com.ppo.domain.model.User
import com.ppo.domain.model.UserId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.IUserRepository
import com.ppo.web.data.orm.UserEntity
import com.ppo.web.data.orm.UsersTable
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

class PostgresUserRepository(private val dataSource: DataSource) : IUserRepository {
    override suspend fun findByLogin(login: String): User? = transaction {
        UserEntity.find { UsersTable.login eq login }.firstOrNull()?.toDomain()
    }

    override suspend fun saveUser(user: User): Result<Unit> = runCatching {
        transaction {
            val existing = UserEntity.findById(user.id.value.toString())
            if (existing != null) {
                existing.login = user.login
                existing.name = user.name
                existing.passwordHash = user.passwordHash
                existing.role = user.role.name
            } else {
                UserEntity.new(user.id.value.toString()) {
                    this.login = user.login
                    this.name = user.name
                    this.passwordHash = user.passwordHash
                    this.role = user.role.name
                }
            }
        }
    }

    override suspend fun getUserRole(userId: UserId): UserRole? = transaction {
        UserEntity.findById(userId.value.toString())?.let { UserRole.valueOf(it.role) }
    }

    override suspend fun getUserById(userId: UserId): User? = transaction {
        UserEntity.findById(userId.value.toString())?.toDomain()
    }

    override suspend fun getAllUsers(): List<User> = transaction {
        UserEntity.all().map { it.toDomain() }
    }
}
