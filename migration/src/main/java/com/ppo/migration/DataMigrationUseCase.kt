package com.ppo.migration

import com.ppo.domain.repository.ICategoryRepository
import com.ppo.domain.repository.ITimeRecordRepository
import com.ppo.domain.repository.IUserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataMigrationUseCase(
    private val oldUserRepository: IUserRepository,
    private val newUserRepository: IUserRepository,
    private val oldCategoryRepository: ICategoryRepository,
    private val newCategoryRepository: ICategoryRepository,
    private val oldTimeRecordRepository: ITimeRecordRepository,
    private val newTimeRecordRepository: ITimeRecordRepository
) {
    suspend fun migrate(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val users = oldUserRepository.getAllUsers()
            for (user in users) {
                val result = newUserRepository.saveUser(user)
                if (result.isFailure) throw result.exceptionOrNull()!!
            }

            val categories = oldCategoryRepository.getAllCategories()
            for (category in categories) {
                val result = newCategoryRepository.saveCategory(category)
                if (result.isFailure) throw result.exceptionOrNull()!!
            }

            val timeRecords = oldTimeRecordRepository.getAllTimeRecords()
            for (record in timeRecords) {
                val result = newTimeRecordRepository.saveRecord(record)
                if (result.isFailure) throw result.exceptionOrNull()!!

                val history = oldTimeRecordRepository.getDescriptionHistory(record.id)
                for (version in history) {
                    val hResult = newTimeRecordRepository.addDescriptionVersion(version)
                    if (hResult.isFailure) throw hResult.exceptionOrNull()!!
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
