package com.dantariun.sat_demo_map.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dantariun.sat_demo_map.data.db.dao.ShadowZoneDao
import com.dantariun.sat_demo_map.data.db.dao.TileInfoDao
import com.dantariun.sat_demo_map.data.db.entity.ShadowZoneEntity

import com.dantariun.sat_demo_map.data.db.entity.TileInfo

@Database(entities = [ShadowZoneEntity::class, TileInfo::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shadowZoneDao(): ShadowZoneDao
    abstract fun tileInfoDao(): TileInfoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addTypeConverter(Converters()) // TypeConverter 제공
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}