package cz.dzubera.callwarden.service.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


@Database(
    entities = [CallEntity::class, PendingCallEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {


    abstract fun taskCalls(): CallDao
    abstract fun pendingCalls(): PendingCallDao
}
