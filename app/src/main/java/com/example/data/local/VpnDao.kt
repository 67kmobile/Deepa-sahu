package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnDao {

    @Query("SELECT * FROM connection_logs ORDER BY timestampMs DESC, id DESC LIMIT 300")
    fun observeLogs(): Flow<List<ConnectionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ConnectionLogEntity)

    @Query("DELETE FROM connection_logs")
    suspend fun clearLogs()

    @Query("SELECT * FROM custom_tunnels ORDER BY createdAtMs DESC")
    fun observeCustomTunnels(): Flow<List<CustomTunnelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomTunnel(tunnel: CustomTunnelEntity)

    @Query("DELETE FROM custom_tunnels WHERE id = :id")
    suspend fun deleteCustomTunnel(id: String)
}
