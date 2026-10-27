package com.ppo.web.it

import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.UUID

@RunWith(AllureRunner::class)
class RollbackIsolationITCase {
    @Test
    fun rollsBackNestedRepositoryTransactionsAfterSuccessAndFailure() {
        verifyRollback(false)
        verifyRollback(true)
    }

    private fun verifyRollback(fail: Boolean) {
        val id = UUID.randomUUID().toString()
        val login = "rollback_$id"
        val statement = object : Statement() {
            override fun evaluate() {
                transaction {
                    exec("INSERT INTO users (id, login, name, password_hash, role) VALUES ('$id', '$login', 'Rollback probe', 'hash', 'USER')")
                }
                assertEquals(0, countUsers(login))
                if (fail) throw AssertionError("Intentional failure")
            }
        }
        val wrapped = TestDbHelper.rollbackRule().apply(statement, Description.EMPTY)
        if (fail) {
            assertThrows(AssertionError::class.java) { wrapped.evaluate() }
        } else {
            wrapped.evaluate()
        }
        assertEquals(0, countUsers(login))
    }

    private fun countUsers(login: String): Int = TestDbHelper.getDataSource().connection.use { connection ->
        connection.prepareStatement("SELECT count(*) FROM users WHERE login = ?").use { statement ->
            statement.setString(1, login)
            statement.executeQuery().use { result ->
                result.next()
                result.getInt(1)
            }
        }
    }
}
