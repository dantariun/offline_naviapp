package com.dantariun.sat_demo_map.util.map

import CacheEvictionManager
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.dantariun.sat_demo_map.data.db.dao.TileInfoDao
import com.dantariun.sat_demo_map.data.db.entity.TileInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import java.io.File

class CustomTileProvider(
    private val tileInfoDao: TileInfoDao,
    private val tileSource: OnlineTileSourceBase,
    private val evictionManager: CacheEvictionManager
) : MapTileModuleProviderBase(2, 40) { // 스레드 2개, 큐 40

    private val providerScope = CoroutineScope(Dispatchers.IO)

    override fun getTileLoader(): TileLoader = CustomTileLoader()
    override fun getMinimumZoomLevel(): Int = tileSource.minimumZoomLevel
    override fun getMaximumZoomLevel(): Int = tileSource.maximumZoomLevel
    override fun setTileSource(pTileSource: ITileSource?) {}
    override fun getUsesDataConnection(): Boolean = true
    override fun getName(): String = "Custom Tile Provider"
    override fun getThreadGroupName(): String = "custom-tile-provider"

    private inner class CustomTileLoader : TileLoader() {
        override fun loadTile(pMapTileIndex: Long): Drawable? {
            // 1. DB에서 먼저 타일 정보 조회
            val existingTileInfo = findTileInDb(pMapTileIndex)
            if (existingTileInfo != null) {
                val file = File(existingTileInfo.filePath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    return bitmap?.let { BitmapDrawable(null, it) } // Context의 Resources가 없으면 null 전달
                }
            }

            // 2. DB에 없으면 네트워크에서 다운로드
            val tileFile = downloadTile(pMapTileIndex)
            if (tileFile != null && tileFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(tileFile.absolutePath)

                // 3. 다운로드 성공 시 DB에 정보 저장
                saveTileInfoToDb(pMapTileIndex, tileFile)

                return bitmap?.let { BitmapDrawable(null, it) }
            }
            return null
        }
    }

    // 비동기 DB 조회를 위한 Helper 함수
    private fun findTileInDb(tileIndex: Long): TileInfo? {
        // 실제 앱에서는 Coroutine runBlocking을 피해야 하지만,
        // osmdroid의 동기적인 loadTile 구조에 맞추기 위해 여기서는 간단하게 사용.
        // 더 나은 방법은 이 클래스 전체를 코루틴 기반으로 리팩토링하는 것.
        var result: TileInfo? = null
        val job = providerScope.launch {
            result = tileInfoDao.findByTileIndex(tileIndex)
        }.apply {
            // 이 작업이 끝날 때까지 기다려야 함
            // 실제 프로덕션에서는 이 부분을 더 정교하게 처리해야 함
        }
        // 임시방편으로 동기적으로 기다림
        Thread.sleep(50) // DB 조회 시간을 고려한 임의의 대기
        return result
    }

    private fun downloadTile(tileIndex: Long): File? {
        // 이 부분은 osmdroid의 TileDownloader 로직을 재사용하거나 직접 구현
        // 여기서는 직접 파일 경로를 만들고 저장한다고 가정
//        val file = File(context.cacheDir, "tiles/${MapTileIndex.getZoom(tileIndex)}_${MapTileIndex.getX(tileIndex)}_${MapTileIndex.getY(tileIndex)}.png")
        // ... 실제 다운로드 로직 ...
        // 다운로드 성공 시 return file
        return null // 임시
    }

    private fun saveTileInfoToDb(tileIndex: Long, tileFile: File) {
        providerScope.launch {
            val tileInfo = TileInfo(
                tileIndex = tileIndex,
                zoomLevel = MapTileIndex.getZoom(tileIndex),
                tileX = MapTileIndex.getX(tileIndex),
                tileY = MapTileIndex.getY(tileIndex),
                timestamp = System.currentTimeMillis(),
                filePath = tileFile.absolutePath,
                sizeBytes = tileFile.length()
            )
            tileInfoDao.insert(tileInfo)

            // DB 저장 후 캐시 용량 확인 및 정리 작업 트리거
            evictionManager.checkAndEvict()
        }
    }
}