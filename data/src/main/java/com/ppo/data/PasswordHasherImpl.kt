package com.ppo.data

import com.ppo.domain.repository.IPasswordHasher
import java.security.MessageDigest
import java.util.Base64

class PasswordHasherImpl : IPasswordHasher {
    override fun hash(rawPassword: String): String {
        val bytes = rawPassword.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return Base64.getEncoder().encodeToString(digest)
    }

    override fun verify(rawPassword: String, hashed: String): Boolean {
        return hash(rawPassword) == hashed
    }
}
