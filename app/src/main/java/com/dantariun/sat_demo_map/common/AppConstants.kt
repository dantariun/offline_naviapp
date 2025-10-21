package com.dantariun.sat_demo_map.common

import org.osmdroid.util.GeoPoint

object AppConstants {
    object Location {
        val MY_LOCATION = GeoPoint(37.501331, 127.026243)  // 강남역
    }

    object MapSettings{
        const val MIN_ZOOM_LEVEL = 3
        const val MAX_ZOOM_LEVEL = 17
    }

    object NavigationSettings {
        const val PBF_NAME ="south-korea-251016.osm.pbf"
    }

    object Network{
        const val BASE_URL = "https://nominatim.openstreetmap.org/"   // search?q=서울%20강남역&format=json
    }
}