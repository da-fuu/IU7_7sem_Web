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
        val backupFile = File("$oldDbPath.backup")

        if (!oldDbFile.exists()) {
            logger.info("Old DB not found. Migration not needed.")
            return@withContext MigrationState.NOT_NEEDED
        }

        if (backupFile.exists()) {
            logger.info("Backup file exists. Migration already performed.")
            return@withContext MigrationState.NOT_NEEDED
        }

        logger.info("Starting data migration...")
        val result = migrationUseCase.migrate()

        if (result.isSuccess) {
            logger.info("Migration successful. Renaming old DB to backup.")
            val renameSuccess = oldDbFile.renameTo(backupFile)
            if (!renameSuccess) {
                logger.error("Failed to rename old DB file.")
            }
            File("$oldDbPath-wal").renameTo(File("$oldDbPath.backup-wal"))
            File("$oldDbPath-shm").renameTo(File("$oldDbPath.backup-shm"))

            MigrationState.SUCCESS
        } else {
            logger.error("Migration failed.", result.exceptionOrNull())
            MigrationState.ERROR
        }
    }
}
