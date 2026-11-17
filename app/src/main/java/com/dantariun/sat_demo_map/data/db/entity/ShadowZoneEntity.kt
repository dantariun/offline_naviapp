package com.dantariun.sat_demo_map.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.osmdroid.util.GeoPoint

@Entity(tableName = "shadow_zone_paths")
data class ShadowZoneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTimestamp: Long,
    var endTimestamp: Long? = null,
    // GeoPoint 리스트는 TypeConverter를 통해 JSON 문자열로 변환되어 저장됩니다.
    val pathPoints: List<GeoPoint>
)