package com.dantariun.sat_demo_map.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dantariun.sat_demo_map.data.db.entity.ShadowZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShadowZoneDao {

    // 새로운 음영 지역 기록을 시작 (삽입)
    @Insert
    suspend fun startShadowZone(entity: ShadowZoneEntity): Long

    // 진행 중인 음영 지역 기록을 업데이트 (경로 추가, 종료 시간 설정)
    @Update
    suspend fun updateShadowZone(entity: ShadowZoneEntity)

    // ID로 특정 기록 조회
    @Query("SELECT * FROM shadow_zone_paths WHERE id = :id")
    suspend fun getById(id: Long): ShadowZoneEntity?

    // 모든 음영 지역 기록을 Flow 형태로 조회 (UI에 실시간 반영 용이)
    @Query("SELECT * FROM shadow_zone_paths ORDER BY startTimestamp DESC")
    fun getAllShadowZones(): Flow<List<ShadowZoneEntity>>
}