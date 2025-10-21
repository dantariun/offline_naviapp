import android.util.Log
import com.dantariun.sat_demo_map.data.db.dao.TileInfoDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.views.MapView
import java.io.File

class CacheEvictionManager(
    private val tileInfoDao: TileInfoDao,
    private val mapView: MapView // 현재 보이는 영역을 알아야 함
) {
    private val evictionScope = CoroutineScope(Dispatchers.IO)

    // 500MB 용량 제한
    private val CACHE_LIMIT_BYTES = 500 * 1024 * 1024L

    fun checkAndEvict() {
        evictionScope.launch {
            val currentSize = tileInfoDao.getTotalCacheSize() ?: 0L
            if (currentSize < CACHE_LIMIT_BYTES) {
                return@launch // 용량 제한 미만이면 아무것도 안함
            }

            var bytesToDelete = currentSize - CACHE_LIMIT_BYTES
            Log.d("CacheEvictionManager", "캐시 용량 초과. 정리 시작...")

            // 1. 현재 화면에 보이는 타일 영역 계산 (수정된 부분)
            val zoom = mapView.zoomLevelDouble.toInt()

            // mapView.intrinsicScreenRect -> mapView.getIntrinsicScreenRect(null)로 변경
            val screenRect = mapView.getIntrinsicScreenRect(null)
            val projection = mapView.projection

            // 화면의 좌상단과 우하단 픽셀을 GeoPoint로 변환
            val northWestGeoPoint = projection.fromPixels(screenRect.left, screenRect.top, null)
            val southEastGeoPoint = projection.fromPixels(screenRect.right, screenRect.bottom, null)

            // GeoPoint를 Tile System을 이용해 타일 좌표로 변환
            val tileSystem = MapView.getTileSystem()
            val northTile = tileSystem.getTileYFromLatitude(northWestGeoPoint.latitude, zoom)
            val westTile = tileSystem.getTileXFromLongitude(northWestGeoPoint.longitude, zoom)
            val southTile = tileSystem.getTileYFromLatitude(southEastGeoPoint.latitude, zoom)
            val eastTile = tileSystem.getTileXFromLongitude(southEastGeoPoint.longitude, zoom)

            // 2. DB에서 삭제할 타일 목록을 가져옴 (오래되고, 화면 밖에 있는 타일)
            while (bytesToDelete > 0) {
                // 수정된 타일 좌표 변수를 쿼리에 전달
                val tilesToRemove = tileInfoDao.getOldestTilesOutsideOf(
                    zoom = zoom,
                    north = northTile,
                    south = southTile,
                    west = westTile,
                    east = eastTile,
                    limit = 20 // 한번에 20개씩 처리
                )

                if (tilesToRemove.isEmpty()) {
                    Log.d("CacheEvictionManager", "삭제할 타일 없음. 정리 중단.")
                    break // 삭제할 타일이 더 이상 없음
                }

                Log.d("CacheEvictionManager", "${tilesToRemove.size}개의 타일 삭제 시도...")

                for (tileInfo in tilesToRemove) {
                    // 3. 실제 파일 삭제
                    val file = File(tileInfo.filePath)
                    if (file.exists()) {
                        file.delete()
                    }

                    // 4. DB에서 레코드 삭제
                    tileInfoDao.delete(tileInfo)
                    bytesToDelete -= tileInfo.sizeBytes
                }
            }
            Log.d("CacheEvictionManager", "캐시 정리 완료.")
        }
    }
}