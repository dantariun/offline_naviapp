package com.dantariun.sat_demo_map.data.db.repository


import com.dantariun.sat_demo_map.data.db.dao.ShadowZoneDao
import com.dantariun.sat_demo_map.data.db.entity.ShadowZoneEntity
import kotlinx.coroutines.flow.Flow
import org.osmdroid.util.GeoPoint

class ShadowZoneRepository(private val dao: ShadowZoneDao) {

    // 진행 중인 음영 지역의 ID. null이면 진행 중인 기록이 없음.
    private var currentShadowZoneId: Long? = null

    // 모든 기록을 가져오는 Flow
    val allShadowZones: Flow<List<ShadowZoneEntity>> = dao.getAllShadowZones()

    /**
     * 음영 지역에 진입했을 때 호출됩니다.
     * 새로운 기록을 생성하고, 진행 중인 ID를 저장합니다.
     */
    suspend fun startLoggingShadowZone(startPoint: GeoPoint) {
        // 이미 다른 음영 지역을 기록 중이면 무시
        if (currentShadowZoneId != null) return

        val newEntity = ShadowZoneEntity(
            startTimestamp = System.currentTimeMillis(),
            pathPoints = listOf(startPoint) // 시작점만 포함된 리스트
        )
        currentShadowZoneId = dao.startShadowZone(newEntity)
    }

    /**
     * 음영 지역을 벗어났을 때 호출됩니다.
     * 진행 중인 기록에 마지막 지점을 추가하고 종료 시간을 업데이트합니다.
     */
    suspend fun stopLoggingShadowZone(endPoint: GeoPoint) {
        val zoneId = currentShadowZoneId ?: return // 기록 중인 ID가 없으면 무시

        val currentEntity = dao.getById(zoneId)
        if (currentEntity != null) {
            // 기존 경로에 마지막 지점을 추가하여 새로운 리스트 생성
            val updatedPath = currentEntity.pathPoints.toMutableList().apply {
                add(endPoint)
            }
            // 종료 시간과 업데이트된 경로로 엔티티 업데이트
            val updatedEntity = currentEntity.copy(
                endTimestamp = System.currentTimeMillis(),
                pathPoints = updatedPath
            )
            dao.updateShadowZone(updatedEntity)
        }
        // 기록 완료 후 ID 초기화
        currentShadowZoneId = null
    }

    /**
     * 음영 지역 내에서 이동할 때마다 호출됩니다.
     * 현재 경로에 중간 지점을 추가합니다.
     */
    suspend fun addPointToCurrentShadowZone(point: GeoPoint) {
        val zoneId = currentShadowZoneId ?: return // 기록 중인 ID가 없으면 무시

        val currentEntity = dao.getById(zoneId)
        if (currentEntity != null) {
            val updatedPath = currentEntity.pathPoints.toMutableList().apply {
                add(point)
            }
            val updatedEntity = currentEntity.copy(pathPoints = updatedPath)
            dao.updateShadowZone(updatedEntity)
        }
    }
}