package com.dantariun.sat_demo_map.util.map

import android.content.Context
import android.util.Log
import com.dantariun.sat_demo_map.data.db.entity.TileInfo
import com.dantariun.sat_demo_map.data.db.repository.TileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TileCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tileRepository: TileRepository,
) {
    // 500MB 용량 제한
    private val CACHE_LIMIT_BYTES = 500 * 1024 * 1024L

    suspend fun cacheArea(mapView: MapView, boundingBox: BoundingBox, zoom: Int) {
        withContext(Dispatchers.IO) {
            val tilesToDownload = CacheManager.getTilesCoverage(boundingBox, zoom)
            Log.d("TileCacheManager", "총 ${tilesToDownload.size}개의 타일 확인 시작...")

            for (tileIndex in tilesToDownload) {
                downloadTileIfNotExists(mapView, tileIndex)
            }
            Log.d("TileCacheManager", "타일 확인 작업 완료.")
        }
    }

    /**
     * 타일이 DB에 없으면 다운로드하고 저장합니다.
     */
    private suspend fun downloadTileIfNotExists(mapView: MapView, tileIndex: Long) {
        if (tileRepository.findTile(tileIndex) != null) {
            return
        }

        Log.d("TileCacheManager", "타일($tileIndex)이 DB에 없어 다운로드를 시도합니다.")

        // 다운로드 전, 캐시 용량 확인 및 자동 정리 실행
        checkAndRunEviction(mapView)

        // 실제 타일 다운로드 및 파일 저장 로직
        val tileFile = downloadTileImage(mapView, tileIndex)

        if (tileFile != null) {
            // 다운로드 성공 시, DB에 메타데이터 저장
            val newTileInfo = TileInfo(
                tileIndex = tileIndex,
                zoomLevel = MapTileIndex.getZoom(tileIndex),
                tileX = MapTileIndex.getX(tileIndex),
                tileY = MapTileIndex.getY(tileIndex),
                timestamp = System.currentTimeMillis(),
                filePath = tileFile.absolutePath,
                sizeBytes = tileFile.length()
            )
            tileRepository.insertTile(newTileInfo)
            Log.d("TileCacheManager", "타일($tileIndex) 다운로드 및 DB 저장 완료.")
        }
    }

    /**
     * 실제 이미지를 다운로드하고 기기에 파일로 저장합니다.
     * (이전 MapViewManager의 downloadSingleTileSuspended 로직과 유사)
     */
    private suspend fun downloadTileImage(mapView: MapView, tileIndex: Long): File? {
        // osmdroid의 CacheManager를 다운로드 유틸리티로 활용
        val cacheManager = CacheManager(mapView)
        val tileSource = mapView.tileProvider.tileSource

        if (tileSource !is OnlineTileSourceBase) return null

        // loadTile은 내부적으로 다운로드 후 파일 저장을 수행하고 성공 여부를 반환
        val success = cacheManager.loadTile(tileSource, tileIndex)

        return if (success) {
            // 성공 시 저장된 파일 객체를 반환
            CacheManager.getFileName(tileSource, tileIndex)
        } else {
            null
        }
    }

    /**
     * 캐시 용량을 확인하고, 한도를 초과하면 오래된 타일을 삭제합니다.
     */
    private suspend fun checkAndRunEviction(mapView: MapView) {
        val currentSize = tileRepository.getTotalCacheSizeBytes()
        if (currentSize < CACHE_LIMIT_BYTES) {
            return
        }

        Log.d("TileCacheManager", "캐시 용량 초과. 자동 정리 시작...")
        var bytesToDelete = currentSize - CACHE_LIMIT_BYTES

        val zoom = mapView.zoomLevelDouble.toInt()
        val screenRect = mapView.getIntrinsicScreenRect(null)
        val projection = mapView.projection
        val northWestGeoPoint = projection.fromPixels(screenRect.left, screenRect.top, null)
        val southEastGeoPoint = projection.fromPixels(screenRect.right, screenRect.bottom, null)
        val tileSystem = MapView.getTileSystem()
        val northTile = tileSystem.getTileYFromLatitude(northWestGeoPoint.latitude, zoom)
        val westTile = tileSystem.getTileXFromLongitude(northWestGeoPoint.longitude, zoom)
        val southTile = tileSystem.getTileYFromLatitude(southEastGeoPoint.latitude, zoom)
        val eastTile = tileSystem.getTileXFromLongitude(southEastGeoPoint.longitude, zoom)

        while (bytesToDelete > 0) {
            val candidates = tileRepository.getEvictionCandidates(zoom, northTile, southTile, westTile, eastTile)
            if (candidates.isEmpty()) break

            for (tileInfo in candidates) {
                val file = File(tileInfo.filePath)
                if (file.exists()) file.delete()
                tileRepository.deleteTile(tileInfo)
                bytesToDelete -= tileInfo.sizeBytes
                if (bytesToDelete <= 0) break
            }
        }
        Log.d("TileCacheManager", "캐시 정리 완료.")
    }

    suspend fun preCacheTilesForRoute(
        mapView: MapView,
        route: List<GeoPoint>,
        zoomMin: Int,
        zoomMax: Int
    ) {
        if (route.isEmpty()) {
            Log.w("TileCacheManager", "경로가 비어 있어 타일을 캐시할 수 없습니다.")
            return
        }

        withContext(Dispatchers.IO) {
            // 1. osmdroid 유틸리티를 사용해 경로가 통과하는 모든 타일의 목록을 계산합니다.
            // getTilesCoverage는 ArrayList를 파라미터로 받으므로 변환해줍니다.
            val tilesToCache = CacheManager.getTilesCoverage(ArrayList(route), zoomMin, zoomMax)
            Log.d("TileCacheManager", "경로 캐싱 시작: ${tilesToCache.size}개의 타일을 확인합니다. (줌 레벨: $zoomMin-$zoomMax)")

            // 2. 각 타일에 대해 DB에 존재하는지 확인하고, 없으면 다운로드합니다.
            // 이미 만들어 둔 downloadTileIfNotExists 함수를 재사용합니다.
            for (tileIndex in tilesToCache) {
                downloadTileIfNotExists(mapView, tileIndex)
            }
            Log.d("TileCacheManager", "경로 타일 캐싱 작업이 완료되었습니다.")
        }
    }
}