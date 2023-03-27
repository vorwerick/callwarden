package cz.dzubera.callwarden.service.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_call")
data class PendingCallEntity(
    @PrimaryKey val uid: Long,
)