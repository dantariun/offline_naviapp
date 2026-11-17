package com.dantariun.sat_demo_map.ui.helper.ui

import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class UserLocationManager(private val mapView: MapView) {
    private var myLocationMarker: Marker? = null
    private val mapController = mapView.controller

    fun updateLocation(currentPoint: GeoPoint) {
        if (myLocationMarker == null) {
            myLocationMarker = Marker(mapView).apply {
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                // icon = ...
            }
            mapView.overlays.add(myLocationMarker)
        }
        myLocationMarker?.position = currentPoint
        mapController.animateTo(currentPoint)
        mapView.invalidate()
    }
}