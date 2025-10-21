package com.dantariun.sat_demo_map.data.db.dao

import androidx.room.*
import com.dantariun.sat_demo_map.data.db.entity.TileInfo

@Dao
interface TileInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tileInfo: TileInfo)

    @Query("SELECT * FROM tile_info WHERE tileIndex = :tileIndex LIMIT 1")
    suspend fun findByTileIndex(tileIndex: Long): TileInfo?

    @Delete
    suspend fun delete(tileInfo: TileInfo)

    @Query("SELECT SUM(sizeBytes) FROM tile_info")
    suspend fun getTotalCacheSize(): Long?

    // 가장 오래되고, 현재 화면 바깥에 있는 타일을 가져오는 쿼리
    @Query("""
        SELECT * FROM tile_info 
        WHERE NOT (
            zoomLevel = :zoom 
            AND tileX >= :west AND tileX <= :east 
            AND tileY >= :north AND tileY <= :south
        )
        ORDER BY timestamp ASC 
        LIMIT :limit
    """)
    suspend fun getOldestTilesOutsideOf(
        zoom: Int,
        north: Int, south: Int, west: Int, east: Int,
        limit: Int
    ): List<TileInfo>
}