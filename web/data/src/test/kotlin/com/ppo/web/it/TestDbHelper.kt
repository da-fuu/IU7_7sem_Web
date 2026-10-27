package com.ppo.web.it

import com.ppo.domain.repository.IPasswordHasher
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.security.MessageDigest
import javax.sql.DataSource

object TestPasswordHasher : IPasswordHasher {
    override fun hash(rawPassword: String): String = MessageDigest.getInstance("SHA-256").digest(rawPassword.toByteArray()).joinToString("") { "%02x".format(it) }
    override fun verify(rawPassword: String, hashed: String): Boolean = hash(rawPassword) == hashed
}

object TestDbHelper {
    private val host = System.getProperty("DB_HOST") ?: System.getenv("POSTGRES_TEST_HOST") ?: "localhost"
    private val port = System.getProperty("DB_PORT") ?: System.getenv("POSTGRES_TEST_PORT") ?: "55432"
    private val dbName = System.getProperty("DB_NAME") ?: System.getenv("POSTGRES_TEST_DB") ?: "timetracker_test"
    private val user = System.getProperty("DB_USER") ?: System.getenv("POSTGRES_TEST_USER") ?: "timetracker_test"
    private val pass = System.getProperty("DB_PASSWORD") ?: System.getenv("POSTGRES_TEST_PASSWORD") ?: "timetracker_test_pass"

    private val dataSource: HikariDataSource by lazy {
        HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://$host:$port/$dbName"
            username = user
            password = pass
            maximumPoolSize = 2
            minimumIdle = 0
            connectionTimeout = 5000
        })
    }

    private val database: Database by lazy {
        Database.connect(dataSource, databaseConfig = DatabaseConfig { useNestedTransactions = true })
    }

    fun getDataSource(): DataSource = dataSource

    fun rollbackRule(): TestRule = TestRule { base: Statement, _: Description ->
        object : Statement() {
            override fun evaluate() {
                transaction(database) {
                    maxAttempts = 1
                    try {
                        base.evaluate()
                    } finally {
                        rollback()
                    }
                }
            }
        }
    }
}
