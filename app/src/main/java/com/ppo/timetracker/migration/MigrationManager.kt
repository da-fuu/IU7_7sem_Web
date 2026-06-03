package com.ppo.timetracker.migration

import com.ppo.migration.DataMigrationUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File

enum class MigrationState {
    NOT_NEEDED,
    IN_PROGRESS,
    SUCCESS,
    ERROR
}

class MigrationManager(
    private val oldDbPath: String,
    private val migrationUseCase: DataMigrationUseCase
) {
    private val logger = LoggerFactory.getLogger(MigrationManager::class.java)

    suspend fun checkAndMigrate(): MigrationState = withContext(Dispatchers.IO) {
        val oldDbFile = File(oldDbPath)
        val markerFile = File(oldDbFile.parent, "migration_done.txt")

        if (!oldDbFile.exists()) {
            logger.info("Old DB not found. Migration not needed.")
            return@withContext MigrationState.NOT_NEEDED
        }

        if (markerFile.exists()) {
            logger.info("Migration marker file exists. Migration already performed.")
            return@withContext MigrationState.NOT_NEEDED
        }

        logger.info("Starting data migration...")
        val result = migrationUseCase.migrate()

        if (result.isSuccess) {
            logger.info("Migration successful. Creating marker file.")
            try {
                markerFile.createNewFile()
            } catch (e: Exception) {
                logger.error("Failed to create migration marker file", e)
            }

            MigrationState.SUCCESS
        } else {
            logger.error("Migration failed.", result.exceptionOrNull())
            MigrationState.ERROR
        }
    }
}
