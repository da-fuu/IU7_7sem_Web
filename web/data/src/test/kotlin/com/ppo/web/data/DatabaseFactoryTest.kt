package com.ppo.web.data

import com.ppo.web.data.db.DatabaseFactory
import com.ppo.web.data.orm.CategoriesTable
import com.ppo.web.data.orm.CategoryEntity
import com.ppo.web.data.runner.RandomOrderRunner
import org.h2.jdbcx.JdbcDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(RandomOrderRunner::class)
class DatabaseFactoryTest {

    private var tempDbFile: File? = null

    @After
    fun tearDown() {
        tempDbFile?.delete()
    }

    @Test
    fun initSchema_createsAllTablesAndDefaultBaseCategories() {
        val dbName = "timetracker_factory_${UUID.randomUUID().toString().replace("-", "")}"
        val tempFile = File.createTempFile("test_factory_${UUID.randomUUID()}_", ".db")
        tempDbFile = tempFile
        val dataSource = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:$dbName;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1")
            user = "sa"
            password = ""
        }

        DatabaseFactory.initSchema(dataSource)

        Database.connect(dataSource)
        val defaultCategories = transaction {
            CategoryEntity.find { CategoriesTable.isDefault eq true }.map { it.toDomain() }
        }
        assertEquals(2, defaultCategories.size)
        assertTrue(defaultCategories.any { it.name == "Работа / Проекты" && it.colorHex == "#3B82F6" })
        assertTrue(defaultCategories.any { it.name == "Учеба" && it.colorHex == "#10B981" })
    }
}
