package com.example.shiftalarmmvp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun observeAll(): Flow<List<AlarmRuleEntity>>

    @Query("SELECT * FROM alarms")
    suspend fun getAll(): List<AlarmRuleEntity>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AlarmRuleEntity?

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    suspend fun getAllEnabled(): List<AlarmRuleEntity>

    @Query("DELETE FROM alarms")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmRuleEntity): Long

    @Update
    suspend fun update(alarm: AlarmRuleEntity)

    @Delete
    suspend fun delete(alarm: AlarmRuleEntity)
}