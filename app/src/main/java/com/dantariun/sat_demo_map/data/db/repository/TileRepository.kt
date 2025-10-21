package com.dantariun.sat_demo_map.data.db.repository

import com.dantariun.sat_demo_map.data.db.dao.TileInfoDao
import com.dantariun.sat_demo_map.data.db.entity.TileInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TileRepository @Inject constructor(
    private val tileInfoDao: TileInfoDao
) {
    suspend fun findTile(tileIndex: Long): TileInfo? {
        return tileInfoDao.findByTileIndex(tileIndex)
    }

    suspend fun insertTile(tileInfo: TileInfo) {
        tileInfoDao.insert(tileInfo)
    }

    suspend fun deleteTile(tileInfo: TileInfo) {
        tileInfoDao.delete(tileInfo)
    }

    suspend fun getTotalCacheSizeBytes(): Long {
        return tileInfoDao.getTotalCacheSize() ?: 0L
    }

    suspend fun getEvictionCandidates(
        zoom: Int,
        north: Int, south: Int, west: Int, east: Int,
        limit: Int = 20 // 한번에 20개씩 가져오기
    ): List<TileInfo> {
        return tileInfoDao.getOldestTilesOutsideOf(zoom, north, south, west, east, limit)
    }
}