package cz.dzubera.callwarden.service.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_record")
data class CallEntity(
    @PrimaryKey val uid: Long,
    @ColumnInfo(name = "userId") val userId: String?,
    @ColumnInfo(name = "domainId") val domainId: String?,
    @ColumnInfo(name = "projectId") val projectId: String?,
    @ColumnInfo(name = "projectName") val projectName: String?,
    @ColumnInfo(name = "type") val type: String?,
    @ColumnInfo(name = "direction") val direction: String?,
    @ColumnInfo(name = "phoneNumber") val phoneNumber: String?,
    @ColumnInfo(name = "callStarted") val callStarted: Long?,
    @ColumnInfo(name = "callEnded") val callEnded: Long?,
    @ColumnInfo(name = "callAccepted") val callAccepted: Long?,
    @ColumnInfo(name = "callDuration") val callDuration: Int?
) {


}