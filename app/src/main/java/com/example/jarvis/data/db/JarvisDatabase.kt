package com.example.jarvis.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.jarvis.data.db.entities.FirewallRuleEntity
import com.example.jarvis.data.db.entities.NetworkTrafficEntity
import com.example.jarvis.data.db.entities.PhishingHistoryEntity
import com.example.jarvis.data.db.entities.ScanResultEntity
import com.example.jarvis.data.db.entities.SecurityEventEntity
import com.example.jarvis.data.db.entities.TrustedAppEntity
import com.example.jarvis.data.db.entities.TrustedDeviceEntity

@Database(
    entities = [
        ScanResultEntity::class,
        SecurityEventEntity::class,
        FirewallRuleEntity::class,
        NetworkTrafficEntity::class,
        TrustedAppEntity::class,
        PhishingHistoryEntity::class,
        TrustedDeviceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun scanResultDao(): ScanResultDao
    abstract fun securityEventDao(): SecurityEventDao
    abstract fun firewallRuleDao(): FirewallRuleDao
    abstract fun networkTrafficDao(): NetworkTrafficDao
    abstract fun trustedAppDao(): TrustedAppDao
    abstract fun phishingHistoryDao(): PhishingHistoryDao
    abstract fun trustedDeviceDao(): TrustedDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure trusted_devices table exists if migrating from v1
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `trusted_devices` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `contactName` TEXT NOT NULL,
                        `phoneNumber` TEXT NOT NULL,
                        `addedTimestamp` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ensure network_traffic table has required index and retention structure
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `network_traffic` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `appName` TEXT NOT NULL,
                        `destinationHost` TEXT NOT NULL,
                        `destinationIp` TEXT NOT NULL,
                        `port` INTEGER NOT NULL,
                        `protocol` TEXT NOT NULL,
                        `isBlocked` INTEGER NOT NULL,
                        `category` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_security.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
