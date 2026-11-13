package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedProductEntity::class,
        CachedCustomerEntity::class,
        CachedOrderEntity::class,
        CachedReplenishmentEntity::class,
        CachedExpenseEntity::class,
        CachedShipmentEntity::class,
        CachedActivityEntity::class,
        CachedSettingsEntity::class,
        CachedStaffEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun swrDao(): SwrDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bralirwa_pos_swr.db"
                ).fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
