package com.ppo.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.ppo.data.db.dao.CategoryDao
import com.ppo.data.db.dao.TimeRecordDao
import com.ppo.data.db.dao.UserDao
import com.ppo.data.entity.CategoryEntity
import com.ppo.data.entity.TimeRecordEntity
import com.ppo.data.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        TimeRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun timeRecordDao(): TimeRecordDao
}
