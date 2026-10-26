package com.ppo.web.data

import com.ppo.web.data.db.DatabaseFactory
import com.ppo.web.data.fixtures.DatabaseFixtureContext
import com.ppo.web.data.fixtures.WithInMemoryDatabase
import com.ppo.web.data.orm.CategoriesTable
import com.ppo.web.data.orm.CategoryEntity
import com.ppo.web.data.runner.RandomOrderRunner
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RandomOrderRunner::class)
class DatabaseFactoryTest {

    @Test
    @WithInMemoryDatabase
    fun initSchema_createsAllTablesAndDefaultBaseCategories() {
        val dataSource = DatabaseFixtureContext.dataSource

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
