package com.dantariun.sat_demo_map.util.map

import android.content.Context
import androidx.core.content.ContextCompat
import com.dantariun.sat_demo_map.R
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * @param mapView 마커를 표시할 MapView
 * @param context 애플리케이션 Context
 * @param geoPoint 마커를 표시할 위치 (GeoPoint)
 */
class MapMarker(mapView: MapView, context: Context, geoPoint: GeoPoint) : Marker(mapView) {
    init {
        position = geoPoint
        setAnchor(ANCHOR_CENTER, ANCHOR_BOTTOM)
        icon = ContextCompat.getDrawable(context, R.drawable.marker_default_focused_base)
//        title = "내 위치"
        setInfoWindow(null) //info window 지우기
    }
}
