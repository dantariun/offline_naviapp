package com.dantariun.sat_demo_map.data.db

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.osmdroid.util.GeoPoint

@ProvidedTypeConverter
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromGeoPointList(points: List<GeoPoint>?): String? {
        if (points == null) {
            return null
        }
        val type = object : TypeToken<List<GeoPoint>>() {}.type
        return gson.toJson(points, type)
    }

    @TypeConverter
    fun toGeoPointList(json: String?): List<GeoPoint>? {
        if (json == null) {
            return null
        }
        val type = object : TypeToken<List<GeoPoint>>() {}.type
        return gson.fromJson(json, type)
    }
}