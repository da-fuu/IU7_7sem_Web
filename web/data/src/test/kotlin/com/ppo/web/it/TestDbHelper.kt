package com.ppo.web.it

import com.ppo.domain.repository.IPasswordHasher
import com.ppo.web.data.db.DatabaseFactory
import java.security.MessageDigest
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

object TestPasswordHasher : IPasswordHasher {
    override fun hash(rawPassword: String): String = MessageDigest.getInstance("SHA-256").digest(rawPassword.toByteArray()).joinToString("") { "%02x".format(it) }
    override fun verify(rawPassword: String, hashed: String): Boolean = hash(rawPassword) == hashed
}

object TestDbHelper {
    private val host: String = System.getProperty("DB_HOST") ?: System.getenv("POSTGRES_TEST_HOST") ?: "localhost"
    private val port: String = System.getProperty("DB_PORT") ?: System.getenv("POSTGRES_TEST_PORT") ?: "5434"
    private val dbName: String = System.getProperty("DB_NAME") ?: System.getenv("POSTGRES_TEST_DB") ?: "timetracker_test"
    private val user: String = System.getProperty("DB_USER") ?: System.getenv("POSTGRES_TEST_USER") ?: "timetracker_test"
    private val pass: String = System.getProperty("DB_PASSWORD") ?: System.getenv("POSTGRES_TEST_PASSWORD") ?: "timetracker_test_pass"

    val jdbcUrl: String = "jdbc:postgresql://$host:$port/$dbName"

    @Volatile
    private var sharedDataSource: DataSource? = null

    fun getDataSource(): DataSource {
        return sharedDataSource ?: synchronized(this) {
            sharedDataSource ?: createDataSource().also { sharedDataSource = it }
        }
    }

    fun createDataSource(customPoolSize: Int = 10): DataSource {
        val config = HikariConfig().apply {
            this.jdbcUrl = TestDbHelper.jdbcUrl
            this.username = user
            this.password = pass
            this.maximumPoolSize = customPoolSize
            this.minimumIdle = 2
            this.isAutoCommit = true
            this.connectionTimeout = 5000
            this.validationTimeout = 3000
        }
        return HikariDataSource(config)
    }

    fun initTestSchema() {
        val ds = getDataSource()
        DatabaseFactory.initSchema(ds)
    }

    fun resetDatabase() {
        val ds = getDataSource()
        ds.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(
                    """
                    TRUNCATE TABLE time_record_history, time_records, user_settings CASCADE;
                    DELETE FROM categories WHERE id NOT IN ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222');
                    DELETE FROM users;
                    INSERT INTO categories (id, user_id, name, color_hex, is_default)
                    VALUES 
                        ('11111111-1111-1111-1111-111111111111', NULL, 'Работа / Проекты', '#3B82F6', true),
                        ('22222222-2222-2222-2222-222222222222', NULL, 'Учеба', '#10B981', true)
                    ON CONFLICT (id) DO NOTHING;
                    """.trimIndent()
                )
            }
        }
    }
}
