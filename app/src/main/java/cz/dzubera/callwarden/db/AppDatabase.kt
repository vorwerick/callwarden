package cz.dzubera.callwarden.db

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [CallEntity::class, PendingCallEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskCalls(): CallDao
    abstract fun pendingCalls(): PendingCallDao
}