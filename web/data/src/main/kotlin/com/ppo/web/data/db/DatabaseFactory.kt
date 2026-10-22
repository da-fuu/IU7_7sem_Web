package com.ppo.web.data.db

import com.ppo.web.data.orm.CategoriesTable
import com.ppo.web.data.orm.CategoryEntity
import com.ppo.web.data.orm.RecordHistoryTable
import com.ppo.web.data.orm.TimeRecordsTable
import com.ppo.web.data.orm.UserSettingsTable
import com.ppo.web.data.orm.UsersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import javax.sql.DataSource

object DatabaseFactory {
    fun createDataSource(
        jdbcUrl: String? = null,
        user: String? = null,
        pass: String? = null
    ): DataSource {
        val host = System.getenv("DB_HOST") ?: "localhost"
        val port = System.getenv("DB_PORT") ?: "5432"
        val dbName = System.getenv("DB_NAME") ?: "timetracker"
        val defaultUrl = "jdbc:postgresql://$host:$port/$dbName"

        val effectiveUrl = jdbcUrl ?: System.getenv("DATABASE_URL") ?: defaultUrl
        val effectiveUser = user ?: System.getenv("DB_USER") ?: "timetracker"
        val effectivePass = pass ?: System.getenv("DB_PASSWORD") ?: "timetracker_pass"

        val config = HikariConfig().apply {
            this.jdbcUrl = effectiveUrl
            this.username = effectiveUser
            this.password = effectivePass
            maximumPoolSize = 10
            minimumIdle = 2
            isAutoCommit = true
            connectionTimeout = 5000
            validationTimeout = 3000
        }

        return HikariDataSource(config)
    }

    fun initSchema(dataSource: DataSource) {
        Database.connect(dataSource)
        transaction {
            SchemaUtils.create(
                UsersTable,
                UserSettingsTable,
                CategoriesTable,
                TimeRecordsTable,
                RecordHistoryTable
            )
            val cat1Id = "11111111-1111-1111-1111-111111111111"
            if (CategoryEntity.findById(cat1Id) == null) {
                CategoryEntity.new(cat1Id) {
                    this.user = null
                    this.name = "Работа / Проекты"
                    this.colorHex = "#3B82F6"
                    this.isDefault = true
                }
            }
            val cat2Id = "22222222-2222-2222-2222-222222222222"
            if (CategoryEntity.findById(cat2Id) == null) {
                CategoryEntity.new(cat2Id) {
                    this.user = null
                    this.name = "Учеба"
                    this.colorHex = "#10B981"
                    this.isDefault = true
                }
            }
        }
    }
}
