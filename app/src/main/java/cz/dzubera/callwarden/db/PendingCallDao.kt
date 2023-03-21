package cz.dzubera.callwarden.db

import androidx.room.*

@Dao
interface PendingCallDao {

    @Query("SELECT * FROM call")
    fun getAll(): List<CallEntity>

    @Insert
    fun insert(call: CallEntity)

    @Delete
    fun delete(call: CallEntity)

    @Update
    fun update(call: CallEntity)

}