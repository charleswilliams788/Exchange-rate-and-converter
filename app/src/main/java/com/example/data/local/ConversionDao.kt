package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ConversionRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionDao {
    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC LIMIT 20")
    fun getRecentConversions(): Flow<List<ConversionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversion(record: ConversionRecord)

    @Query("DELETE FROM conversion_history")
    suspend fun clearHistory()
}
