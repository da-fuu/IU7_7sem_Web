package com.ppo.domain.repository

interface IPasswordHasher {
    fun hash(rawPassword: String): String
    fun verify(rawPassword: String, hashed: String): Boolean
}
