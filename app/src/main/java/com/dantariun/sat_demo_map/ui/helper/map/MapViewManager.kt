package com.dantariun.sat_demo_map.ui.helper.map

import android.animation.ValueAnimator
import android.content.Context
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import androidx.core.content.ContextCompat
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.common.AppConstants
import com.dantariun.sat_demo_map.util.common.AssetUtil
import com.dantariun.sat_demo_map.util.map.MBTilesModuleProvider
import com.dantariun.sat_demo_map.util.map.TiltableMapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import kotlin.coroutines.resume

class MapViewManager(
    private val mapView: TiltableMapView
) {
    private var zoomInButton: ImageButton? = null
    private var zoomOutButton: ImageButton? = null
    private val context: Context = mapView.context
    val mapController = mapView.controller

    fun setupMap(
        defaultZoom: Double,
        zoomInButton: ImageButton? = null,
        zoomOutButton: ImageButton? = null
    ) {
        if (zoomInButton != null && zoomOutButton != null) {
            this.zoomInButton = zoomInButton
            this.zoomOutButton = zoomOutButton

            mapView.addMapListener(object : MapListener {
                override fun onZoom(event: ZoomEvent?): Boolean {
                    updateZoomButtonState()
                    return true
                }
                override fun onScroll(event: ScrollEvent?) = false
            })
        }

        mapView.setBuiltInZoomControls(false)
        mapView.setMultiTouchControls(true)
        mapView.setOverscanTileCount(2)
        mapView.isTilesScaledToDpi = true
        mapView.minZoomLevel = 2.0
        mapView.maxZoomLevel = 16.0

        mapView.setTileSource(TileSourceFactory.MAPNIK)
        // 이제 데이터 연결을 사용해야 합니다. 이 줄은 생략해도 기본값이 true입니다.
        mapView.setUseDataConnection(true)

//        mapController.setCenter(GeoPoint(40.41646546176908, -3.7051451282942387))
        mapController.setCenter(AppConstants.Location.MY_LOCATION)

        // 초기 줌 레벨과 버튼 상태 설정
        mapController.setZoom(defaultZoom)
        updateZoomButtonState()
        mapView.invalidate()
    }


    fun setupMap(mbtilesName: String, minZoom: Int, maxZoom: Int,
                 zoomInButton: ImageButton? = null,
                 zoomOutButton: ImageButton? = null
    ) {
        if (zoomInButton != null && zoomOutButton != null) {
            this.zoomInButton = zoomInButton
            this.zoomOutButton = zoomOutButton

            mapView.addMapListener(object : MapListener {
                override fun onZoom(event: ZoomEvent?): Boolean {
                    updateZoomButtonState()
                    return true
                }
                override fun onScroll(event: ScrollEvent?) = false
            })
        }

        mapView.setBuiltInZoomControls(false)
        mapView.setMultiTouchControls(true)
        mapView.setOverscanTileCount(2)
        mapView.isTilesScaledToDpi = true

        val mbtilesFile = AssetUtil.copyMapFromAssetsToStorage(context, mbtilesName)
        if (mbtilesFile != null) {
            val tileSource = XYTileSource("MBTiles", minZoom, maxZoom, 256, ".png", arrayOf())
            val mbTilesProvider = MBTilesModuleProvider(context, mbtilesFile, minZoom, maxZoom)
            val providerArray = MapTileProviderArray(tileSource, null, arrayOf(mbTilesProvider))
            mapView.tileProvider = providerArray
            mapView.setTileSource(tileSource)
            mapView.setUseDataConnection(false)
        }

        mapController.setCenter(GeoPoint(40.41646546176908, -3.7051451282942387))
//        mapView.overlays.add(MapMarker(mapView, context, GeoPoint(40.41646546176908, -3.7051451282942387)))

        // 초기 줌 레벨과 버튼 상태 설정
        mapController.setZoom(maxZoom.toDouble())
        updateZoomButtonState()
        mapView.invalidate()
    }

    fun setMapCenter(mapLocation:GeoPoint){
        mapController.setCenter(mapLocation)
        mapView.overlays.add(
            Marker(mapView).apply {
                position = mapLocation
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = ContextCompat.getDrawable(mapView.context, R.drawable.icon_my_location)
                setPanToView(false)
            }
        )
    }

    private fun updateZoomButtonState() {
        val currentZoom = mapView.zoomLevelDouble
        val minZoom = mapView.minZoomLevel
        val maxZoom = mapView.maxZoomLevel

        // --- Zoom In 버튼 로직 ---
        val canZoomIn = currentZoom < maxZoom
        zoomInButton?.isEnabled = canZoomIn
        if (canZoomIn) {
            zoomInButton?.setImageResource(R.drawable.button_zoom_up_selector)
        } else {
            zoomInButton?.setImageResource(R.drawable.pressed_up)
        }

        // --- Zoom Out 버튼 로직 ---
        val canZoomOut = currentZoom > minZoom
        zoomOutButton?.isEnabled = canZoomOut
        if (canZoomOut) {
            zoomOutButton?.setImageResource(R.drawable.button_zoom_down_selector)
        } else {
            zoomOutButton?.setImageResource(R.drawable.pressed_down)
        }
    }

    fun resetMapRotation() {
        val currentRotation = mapView.mapOrientation
        val currentTilt = (mapView as? TiltableMapView)?.mapTilt ?: 0f

        if (currentRotation == 0f && currentTilt == 0f) {
            return
        }

        val startRotation: Float = if (currentRotation > 180f) {
            currentRotation - 360f
        } else if (currentRotation < -180f) {
            currentRotation + 360f
        } else {
            currentRotation
        }

        val animator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 500L
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                mapView.mapOrientation = startRotation * progress
                (mapView as? TiltableMapView)?.mapTilt = currentTilt * progress
            }
        }
        animator.start()
    }

    suspend fun downloadAreaSuspended(boundingBox: BoundingBox, zoomLevel: Int): Boolean {
        // suspendCancellableCoroutine을 사용하여 콜백 기반 코드를 코루틴으로 변환
        return suspendCancellableCoroutine { continuation ->
            val cacheManager = CacheManager(mapView)

            val callback = object : CacheManager.CacheManagerCallback {
                override fun onTaskComplete() {
                    Log.d("CacheManager", "성공적으로 완료: $boundingBox")
                    // 작업이 성공적으로 완료되면 true를 반환하며 코루틴을 재개
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onTaskFailed(errors: Int) {
                    Log.e("CacheManager", "실패: $boundingBox, errors: $errors")
                    // 작업이 실패하면 false를 반환하며 코루틴을 재개
                    if (continuation.isActive) continuation.resume(false)
                }

                // 기타 콜백 함수들 (필요에 따라 UI 업데이트 로직 추가 가능)
                override fun updateProgress(progress: Int, currentZoomLevel: Int, zoomMin: Int, zoomMax: Int) {}
                override fun downloadStarted() {
                    Log.d("CacheManager", "다운로드 시작: $boundingBox")
                }
                override fun setPossibleTilesInArea(total: Int) {
                    Log.d("CacheManager", "다운로드할 총 타일 수: $total")
                }
            }

            // UI가 없는 비동기 다운로드 실행
            cacheManager.downloadAreaAsyncNoUI(context, boundingBox, zoomLevel, zoomLevel, callback)

            // 코루틴이 취소될 경우 CacheManager 작업도 취소하도록 등록
            continuation.invokeOnCancellation {
                cacheManager.cancelAllJobs()
            }
        }
    }

    suspend fun downloadSingleTileSuspended(tileIndex: Long): Boolean {
        // withContext를 사용하여 백그라운드 스레드에서 네트워크 작업을 수행
        return withContext(Dispatchers.IO) {
            try {
                val cacheManager = CacheManager(mapView)
                // loadTile은 타일 소스가 OnlineTileSourceBase일 때만 유효합니다.
                val tileSource = mapView.tileProvider.tileSource
                if (tileSource is org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase) {
                    // loadTile은 캐시에 없으면 다운로드하고, 있으면 true를 반환합니다.
                    // 다운로드 실패 시 false를 반환합니다.
                    cacheManager.loadTile(tileSource, tileIndex)
                } else {
                    // mbtiles 등 오프라인 소스인 경우 다운로드 불가
                    Log.w("CacheManager", "Cannot download tile for a non-online tile source.")
                    false
                }
            } catch (e: Exception) {
                Log.e("CacheManager", "Error downloading single tile: $tileIndex", e)
                false
            }
        }
    }
}