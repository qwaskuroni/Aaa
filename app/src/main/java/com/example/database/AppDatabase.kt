package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ScriptEntity::class,
        ScriptTargetEntity::class,
        AutomationProfile::class,
        ActionButton::class,
        ActionSequence::class,
        Settings::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scriptDao(): ScriptDao
    abstract fun automationProfileDao(): AutomationProfileDao
    abstract fun actionButtonDao(): ActionButtonDao
    abstract fun actionSequenceDao(): ActionSequenceDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auto_clicker_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
