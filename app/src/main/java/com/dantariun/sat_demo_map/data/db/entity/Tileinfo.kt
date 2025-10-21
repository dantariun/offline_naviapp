package com.dantariun.sat_demo_map.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tile_info")
data class TileInfo(
    @PrimaryKey
    val tileIndex: Long,      // MapTileIndex.getTileIndex()로 생성된 고유 ID
    val zoomLevel: Int,       // 줌 레벨
    val tileX: Int,           // 타일 X 좌표
    val tileY: Int,           // 타일 Y 좌표
    val timestamp: Long,      // 타일이 저장된 시간 (ms)
    val filePath: String,     // 실제 타일 이미지 파일의 절대 경로
    val sizeBytes: Long       // 타일 파일의 크기 (bytes)
)
