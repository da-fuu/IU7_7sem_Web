package com.ppo.domain.model

data class User(
    val id: UserId,
    val login: String,
    val name: String,
    val passwordHash: String,
    val role: UserRole
)
