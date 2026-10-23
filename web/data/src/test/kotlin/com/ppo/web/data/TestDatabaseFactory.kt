package com.ppo.web.data

import com.ppo.web.data.db.DatabaseFactory
import org.h2.jdbcx.JdbcDataSource
import java.io.File
import java.util.UUID
import javax.sql.DataSource

object TestDatabaseFactory {
    fun createTestDataSource(): Pair<DataSource, File> {
        val dbName = "timetracker_test_${UUID.randomUUID().toString().replace("-", "")}"
        val tempFile = File.createTempFile("test_timetracker_${UUID.randomUUID()}_", ".db")
        tempFile.deleteOnExit()
        val dataSource = JdbcDataSource().apply {
            setURL("jdbc:h2:mem:$dbName;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1")
            user = "sa"
            password = ""
        }
        DatabaseFactory.initSchema(dataSource)
        return Pair(dataSource, tempFile)
    }
}
