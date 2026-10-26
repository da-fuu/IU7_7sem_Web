package com.ppo.web.data.fixtures

import com.ppo.web.data.TestDatabaseFactory
import org.junit.runners.model.FrameworkMethod
import java.io.File
import javax.sql.DataSource

object DatabaseFixtureContext {
    private val currentDataSource = ThreadLocal<DataSource?>()
    private val currentDbFile = ThreadLocal<File?>()

    val dataSource: DataSource
        get() = currentDataSource.get() ?: createAndSet().first

    private fun createAndSet(): Pair<DataSource, File> {
        val (ds, file) = TestDatabaseFactory.createTestDataSource()
        currentDataSource.set(ds)
        currentDbFile.set(file)
        return Pair(ds, file)
    }

    fun setup(method: FrameworkMethod) {
        if (method.getAnnotation(WithInMemoryDatabase::class.java) != null) {
            createAndSet()
        }
    }

    fun clear() {
        currentDbFile.get()?.delete()
        currentDbFile.remove()
        currentDataSource.remove()
    }
}
