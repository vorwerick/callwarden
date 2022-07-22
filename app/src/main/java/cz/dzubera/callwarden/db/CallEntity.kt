package cz.dzubera.callwarden.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call")
data class CallEntity(
    @PrimaryKey val uid: Long,
    @ColumnInfo(name = "type") val type: String?,
    @ColumnInfo(name = "direction") val direction: String?,
    @ColumnInfo(name = "phoneNumber") val phoneNumber: String?,
    @ColumnInfo(name = "callStarted") val callStarted: Long?,
    @ColumnInfo(name = "callEnded") val callEnded: Long?,
    @ColumnInfo(name = "callAccepted") val callAccepted: Long?
) {


}