package cz.dzubera.callwarden.db

import androidx.room.*

@Dao
interface PendingCallDao {

    @Query("SELECT * FROM pending_call")
    fun getAll(): List<PendingCallEntity>

    @Insert
    fun insert(call: PendingCallEntity)

    @Delete
    fun delete(call: PendingCallEntity)

    @Query("DELETE FROM pending_call")
    fun deleteAll()

    @Update
    fun update(call: PendingCallEntity)

}