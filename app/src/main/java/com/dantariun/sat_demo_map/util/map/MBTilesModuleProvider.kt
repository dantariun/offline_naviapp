package com.dantariun.sat_demo_map.util.map

import android.content.Context
import android.content.res.Resources
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.Drawable
import android.util.Log
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.util.MapTileIndex
import java.io.File
import androidx.core.graphics.drawable.toDrawable

class MBTilesModuleProvider(context: Context, mbtilesFile: File, minZoomLevel:Int =1 , maxZoomLevel:Int =1 ) :
// 스레드 2개, 큐 사이즈 40으로 모듈 제공자 초기화
    MapTileModuleProviderBase(2, 40) {

    private val appResources: Resources = context.resources
    private val minLevel: Int = minZoomLevel
    private val maxLevel: Int = maxZoomLevel
    private var db: SQLiteDatabase? = null
    private val mbtilesFile = mbtilesFile

    init {
        if (this.mbtilesFile.exists()) {
            try {
                db = SQLiteDatabase.openDatabase(this.mbtilesFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                Log.i("MBTilesModuleProvider", "Database opened: ${this.mbtilesFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("MBTilesModuleProvider", "Error opening database", e)
            }
        }
    }

    override fun getName(): String = "MBTiles Module Provider"

    override fun getThreadGroupName(): String  = ""

    // 타일을 로드하는 실제 작업을 수행할 TileLoader를 반환
    override fun getTileLoader(): TileLoader = MBTilesTileLoader()

    override fun getUsesDataConnection(): Boolean = false
    override fun getMinimumZoomLevel(): Int  = minLevel

    override fun getMaximumZoomLevel(): Int  = maxLevel

    override fun setTileSource(pTileSource: ITileSource?) {
        // 이 제공자는 특정 타일소스에 의존하지 않으므로 아무 작업도 하지 않음
    }

    // 내부 클래스로 실제 타일 로딩 로직을 구현
    private inner class MBTilesTileLoader : TileLoader() {
        override fun loadTile(pMapTileIndex: Long): Drawable? {
            if (db == null || !db!!.isOpen) {
                return null
            }

            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)

            // TMS Y좌표 변환
            val tmsY = (1 shl zoom) - 1 - y

            val columns = arrayOf("tile_data")
            val selection = "zoom_level = ? AND tile_column = ? AND tile_row = ?"
            val selectionArgs = arrayOf(zoom.toString(), x.toString(), tmsY.toString())

            try {
                db!!.query("tiles", columns, selection, selectionArgs, null, null, null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val blob = cursor.getBlob(0)
                        if (blob != null) {
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(blob, 0, blob.size)

                            return bitmap.toDrawable(appResources)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MBTilesModuleProvider", "Error loading tile: $zoom/$x/$y", e)
            }
            return null // 타일이 없거나 오류 발생 시 null 반환
        }
    }
}