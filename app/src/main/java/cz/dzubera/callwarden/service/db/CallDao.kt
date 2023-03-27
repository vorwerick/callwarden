package cz.dzubera.callwarden.service.db

import android.telecom.Call
import androidx.room.*

@Dao
interface CallDao {

    @Query("SELECT * FROM call_record")
    fun getAll(): List<CallEntity>

    @Insert
    fun insert(call: CallEntity)

    @Delete
    fun delete(call: CallEntity)

    @Update
    fun update(call: CallEntity)

    @Query("SELECT * FROM call_record WHERE uid=:uid ")
    fun get(uid: Long): CallEntity?

}