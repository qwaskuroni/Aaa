package com.example.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ScriptEntity::class,
        ScriptTargetEntity::class,
        AutomationProfile::class,
        ActionButton::class,
        ActionSequence::class,
        Settings::class
    ],
    version = 10,
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

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE script_targets ADD COLUMN excelFilePath TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE script_targets ADD COLUMN excelRulesContent TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE script_targets ADD COLUMN matchThreshold REAL NOT NULL DEFAULT 0.3")
                db.execSQL("ALTER TABLE script_targets ADD COLUMN fallbackReply TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE script_targets ADD COLUMN voiceToTextDelayBeforeMs INTEGER NOT NULL DEFAULT 2000")
                db.execSQL("ALTER TABLE script_targets ADD COLUMN voiceToTextWaitAfterMs INTEGER NOT NULL DEFAULT 2000")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE script_targets ADD COLUMN aiIntentApiKey TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE script_targets ADD COLUMN voiceToTextRetryCount INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE script_targets ADD COLUMN voiceToTextRetryIntervalMs INTEGER NOT NULL DEFAULT 500")
                db.execSQL("ALTER TABLE script_targets ADD COLUMN voiceToTextSearchTimeoutMs INTEGER NOT NULL DEFAULT 3000")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auto_clicker_db"
                )
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

