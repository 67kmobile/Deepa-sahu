package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConnectionLogEntity::class, CustomTunnelEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VpnDatabase : RoomDatabase() {
    abstract fun vpnDao(): VpnDao

    companion object {
        @Volatile
        private var instance: VpnDatabase? = null

        fun getInstance(context: Context): VpnDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VpnDatabase::class.java,
                    "violet_tunnel.db"
                ).build().also { instance = it }
            }
        }
    }
}
