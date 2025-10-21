package com.dantariun.sat_demo_map.util.etc

import android.database.sqlite.SQLiteDatabase
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Log
import org.osmdroid.tileprovider.ReusableBitmapDrawable
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase
import org.osmdroid.util.MapTileIndex
import java.io.File

class CustomMBTilesTileSource(mbtilesFilePath: String, name: String, minZoom: Int, maxZoom: Int) :
    BitmapTileSourceBase(name, minZoom, maxZoom, 256, ".png") {

    private var db: SQLiteDatabase? = null
    private val mbtilesFile = File(mbtilesFilePath)

    private val MBTILES_SCHEME = "mbtiles://"

    init {
        if (mbtilesFile.exists()) {
            try {
                db = SQLiteDatabase.openDatabase(mbtilesFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                Log.i("CustomMBTiles", "Database opened successfully from ${mbtilesFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("CustomMBTiles", "Error opening database", e)
            }
        } else {
            Log.e("CustomMBTiles", "MBTiles file not found at ${mbtilesFile.absolutePath}")
        }
    }

    /**
     * 타일의 고유 식별자(파일 경로 대신 사용될 키)를 생성합니다.
     * 예: "mbtiles://12/1234/5678"
     */
    override fun getTileRelativeFilenameString(pMapTileIndex: Long): String {
        val zoom = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "$MBTILES_SCHEME$zoom/$x/$y"
    }

    /**
     * 타일 로더가 이 함수를 호출할 때, aFilePath 인자로 위에서 생성한 키가 들어옵니다.
     * 이 키를 파싱하여 DB에서 타일 데이터를 가져옵니다.
     */
    override fun getDrawable(aFilePath: String?): Drawable? {
        if (aFilePath == null || !aFilePath.startsWith(MBTILES_SCHEME)) {
            return super.getDrawable(aFilePath) // 우리 스킴이 아니면 부모 클래스에 위임
        }

        if (db == null || !db!!.isOpen) {
            Log.e("CustomMBTiles", "Database is not open.")
            return null
        }

        try {
            // "mbtiles://" 부분을 제외하고 "zoom/x/y" 부분만 추출
            val parts = aFilePath.substring(MBTILES_SCHEME.length).split("/")
            if (parts.size != 3) return null

            val zoom = parts[0].toInt()
            val x = parts[1].toInt()
            val y = parts[2].toInt()

            // MBTiles는 TMS 타일 스키마를 사용하므로 Y좌표 변환
            val tmsY = (1 shl zoom) - 1 - y

            val columns = arrayOf("tile_data")
            val selection = "zoom_level = ? AND tile_column = ? AND tile_row = ?"
            val selectionArgs = arrayOf(zoom.toString(), x.toString(), tmsY.toString())

            db!!.query("tiles", columns, selection, selectionArgs, null, null, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    val blob = cursor.getBlob(0)
                    if (blob != null) {
                        val bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.size)
                        return ReusableBitmapDrawable(bitmap)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CustomMBTiles", "Error querying tile for path: $aFilePath", e)
        }

        return null
    }
}