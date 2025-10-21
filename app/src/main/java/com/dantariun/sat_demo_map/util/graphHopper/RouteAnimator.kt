package com.dantariun.sat_demo_map.util.graphHopper

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.core.animation.doOnEnd
import com.dantariun.sat_demo_map.util.map.TiltableMapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.core.content.ContextCompat
import com.dantariun.sat_demo_map.R
import org.osmdroid.util.BoundingBox

interface OnRouteAnimationListener {
    fun onAnimationStart()
    fun onAnimationStopped(completedNaturally: Boolean)

}
interface OnRouteProgressListener {
    fun onProgress(currentPoint: GeoPoint)
    fun onRotationChanged(rotation: Float)
}

class RouteAnimator(
    private val mapView: MapView,
    private val maxZoomLevel: Double // 최대 줌 레벨을 외부에서 받음
) {
    private var isAnimating = false
    private lateinit var routePoints: List<GeoPoint>
    // ✅ 1. 준비 애니메이션과 메인 애니메이터를 분리하여 관리
    private var preparationAnimator: ValueAnimator? = null
    private var routeAnimator: ValueAnimator? = null

    private var movingMarker: Marker? = null
    private var baseRoutePolyline: Polyline? = null
    private var newPolyline:Polyline? = null

    private var isInShadowZone = false
    private var listener: OnRouteAnimationListener? = null
    private var progressListener: OnRouteProgressListener? = null
    private val manualOrientationProvider = ManualOrientationProvider()

    private val normalPuckDrawable: Drawable? = ContextCompat.getDrawable(mapView.context, R.drawable.navigation_puck)
    private val shadowPuckDrawable: Drawable? = ContextCompat.getDrawable(mapView.context, R.drawable.navigation_puck_sat)

    fun setOnRouteAnimationListener(listener: OnRouteAnimationListener) {
        this.listener = listener
    }

    fun setOnRouteProgressListener(listener: OnRouteProgressListener) {
        this.progressListener = listener
    }

    fun drawRoute(routePoints: List<GeoPoint>) {
        this.routePoints = routePoints
        removeRoute()
        baseRoutePolyline = Polyline().apply {
            setPoints(routePoints)
            outlinePaint.color = Color.BLUE
            outlinePaint.strokeWidth = POLYLINE_STROKE_WIDTH
        }
        mapView.overlays.add(baseRoutePolyline)
        mapView.invalidate()
    }

    fun notifyShadowZoneEntered() {
        isInShadowZone = true
        movingMarker?.icon = shadowPuckDrawable
        baseRoutePolyline?.outlinePaint?.color = Color.GREEN
        mapView.invalidate() // 변경된 색상을 즉시 적용하기 위해 맵을 다시 그립니다.
    }

    fun notifyShadowZoneExited() {
        isInShadowZone = false
        movingMarker?.icon = normalPuckDrawable
        baseRoutePolyline?.outlinePaint?.color = Color.BLUE
        mapView.invalidate()
    }

    fun removeRoute() {
        baseRoutePolyline?.let { mapView.overlays.remove(it) }
        baseRoutePolyline = null
        mapView.invalidate()
    }

    // ✅ 2. start() 함수 로직 변경: 준비 애니메이션을 먼저 시작하도록 수정
    fun start(withPreparation: Boolean = true) {
        if (routePoints.size < 2) return
        isAnimating = true // 상태 플래그 설정
        listener?.onAnimationStart()
        // 지도를 시작점에 맞춤
        mapView.controller.setCenter(routePoints.first())

        // 메인 애니메이션 대신 준비 애니메이션을 시작
        if (withPreparation) {
            // 준비 애니메이션을 시작합니다. (1.5초 소요)
            startPreparationAnimation()
        } else {
            // 준비 애니메이션이 하던 일을 즉시 실행
            mapView.controller.setZoom(maxZoomLevel) // 줌 즉시 적용
            (mapView as? TiltableMapView)?.mapTilt = 12f // 틸트 즉시 적용

            // 회전 즉시 적용
            val startPoint = routePoints[0]
            val nextPoint = routePoints[1]
            val targetRotation = -startPoint.bearingTo(nextPoint).toFloat()
            mapView.setMapOrientation(targetRotation)

            progressListener?.onRotationChanged(targetRotation)
            // 메인 애니메이션을 바로 시작합니다.
            startMainRouteAnimation()
        }
    }

    // ✅ 3. 사전 준비 애니메이션(회전, 틸트)을 위한 함수 추가
    // ✅ 이 함수 내부의 회전 각도 계산 로직을 수정합니다.
    private fun startPreparationAnimation() {
        val startPoint = routePoints[0]
        val nextPoint = routePoints[1]

        val startRotation = mapView.mapOrientation
        // 1. targetRotation을 var로 변경하여 수정 가능하게 합니다.
        var targetRotation = -startPoint.bearingTo(nextPoint).toFloat()

        // 2. 현재 각도와 목표 각도의 차이를 계산합니다.
        val diff = targetRotation - startRotation

        // 3. 차이가 180도보다 크면 반대 방향으로 가는 것이 더 가깝습니다. (e.g., 350도 -> 10도)
        //    이 경우 목표 각도에서 360도를 빼서 음수 각도로 만듭니다. (e.g., 10도 -> -350도)
        if (diff > 180f) {
            targetRotation -= 360f
        }
        // 4. 차이가 -180도보다 작아도 반대 방향이 더 가깝습니다. (e.g., 10도 -> 350도)
        //    이 경우 목표 각도에 360도를 더합니다.
        else if (diff < -180f) {
            targetRotation += 360f
        }
        // 위 두 경우가 아니면 이미 최단 경로이므로 변경하지 않습니다.

        val startTilt = (mapView as? TiltableMapView)?.mapTilt ?: 0f
        val targetTilt = 12f

        // ✅ 2. 줌 애니메이션을 시작하는 코드 추가
        // PREPARATION_DURATION_MS 시간 동안 maxZoomLevel까지 부드럽게 확대됩니다.
        mapView.controller.zoomTo(maxZoomLevel, PREPARATION_DURATION_MS)

        preparationAnimator?.cancel()
        preparationAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = PREPARATION_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float

                // 5. 이제 애니메이터는 조정된 targetRotation을 향해 최단 경로로 보간합니다.
                val currentRotation = startRotation + (targetRotation - startRotation) * fraction
                val currentTilt = startTilt + (targetTilt - startTilt) * fraction

                mapView.setMapOrientation(currentRotation)
                progressListener?.onRotationChanged(currentRotation)
                (mapView as? TiltableMapView)?.mapTilt = currentTilt
            }

            doOnEnd {
                startMainRouteAnimation()
            }
        }
        preparationAnimator?.start()
    }

    // ✅ 4. 기존의 경로 애니메이션 로직을 별도 함수로 분리
    private fun startMainRouteAnimation() {
        setupMovingMarker(routePoints.first())
        val totalDuration = (routePoints.size - 1) * ANIMATION_INTERVAL
        if (totalDuration <= 0) return

        routeAnimator = ValueAnimator.ofInt(0, routePoints.size - 1).apply {
//            setDuration(this@RouteAnimator.duration)
            duration = totalDuration
            interpolator = LinearInterpolator()

            addUpdateListener { animation ->
                val currentIndex = animation.animatedValue as Int
                if (currentIndex >= routePoints.size - 1) return@addUpdateListener

                val currentPoint = routePoints[currentIndex]
                progressListener?.onProgress(currentPoint)
                val nextPoint = routePoints[currentIndex + 1]
                movingMarker?.position = currentPoint
                mapView.controller.setCenter(currentPoint)

                val bearing = currentPoint.bearingTo(nextPoint).toFloat()

                manualOrientationProvider.setOrientation(bearing)
                mapView.setMapOrientation(-bearing)

                progressListener?.onRotationChanged(-bearing)
                mapView.invalidate()
            }

            doOnEnd {
                val lastIndex = (it as ValueAnimator).animatedValue as Int
                val isAnimationCompleted = lastIndex >= routePoints.size - 2 // 마지막 인덱스에 도달했는지 확인

                if (isAnimationCompleted) {
                    this@RouteAnimator.stop(true)
                }
            }
        }
        routeAnimator?.start()
    }

    fun drawRouteNew( newPoints : List<GeoPoint>){
        animationStop()
        newPolyline?.let { mapView.overlays.remove(it) }
        newPolyline = Polyline().apply {
            setPoints(newPoints)
            outlinePaint.color = Color.BLUE
            outlinePaint.strokeWidth = POLYLINE_STROKE_WIDTH
        }
        mapView.overlays.add(newPolyline)

        baseRoutePolyline?.outlinePaint?.color = Color.GRAY

        mapView.invalidate()

        // 3. 기존 경로와 새 경로의 모든 점을 합쳐서 두 경로가 모두 보이는 BoundingBox를 계산합니다.
        val oldPoints = baseRoutePolyline?.points ?: emptyList()
        val allPoints = oldPoints + newPoints
        if (allPoints.isNotEmpty()) {
            val boundingBox = BoundingBox.fromGeoPoints(allPoints)
            // 4. 계산된 BoundingBox에 맞춰 지도를 부드럽게 축소/이동합니다. (100은 padding 값)
            mapView.zoomToBoundingBox(boundingBox, true, 100)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            clearPolyLines(false)
            baseRoutePolyline = newPolyline
            newPolyline = null
            this.routePoints = newPoints
            refreshMovingMarker()
            isAnimating = false
            start(withPreparation = false)

        }, 3000L)
    }

    fun pause() {
        if (routeAnimator?.isStarted == true) {
            routeAnimator?.pause()
        }
    }

    // ✅ 2. 애니메이션을 재개하는 public 메서드 추가
    fun resume() {
        if (routeAnimator?.isPaused == true) {
            routeAnimator?.resume()
        }
    }

    fun animationStop(){
        preparationAnimator?.cancel()
        routeAnimator?.cancel()
    }

    // ✅ 5. stop() 함수에서 두 애니메이터를 모두 취소하도록 수정
    fun stop(completedNaturally: Boolean = false) {
        if (!isAnimating) return
        isAnimating = false
        animationStop()
        movingMarker?.let {
            mapView.overlays.remove(it)
            mapView.invalidate()
        }
        movingMarker = null

        clearPolyLines()

        listener?.onAnimationStopped(completedNaturally)
    }

    fun isAnimating() = routeAnimator?.isRunning ?: false

    private fun showArrivalDialog() {
        AlertDialog.Builder(mapView.context)
            .setTitle("목적지 도착")
            .setMessage("목적지에 도착했습니다. 경로 안내를 종료하시겠습니까?")
            .setPositiveButton("수락") { dialog, _ ->
                clearPolyLines(true)
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    fun clearPolyLines(deleteNew:Boolean = true){
        if(deleteNew){
            mapView.overlays.removeAll { it is Polyline }
            newPolyline = null
        }else{
            mapView.overlays.removeAll { it is Polyline && it != newPolyline }
        }

        baseRoutePolyline = null
        mapView.invalidate()
    }

    private fun refreshMovingMarker(){
        movingMarker?.let { marker ->
            mapView.overlays.remove(marker)
            mapView.overlays.add(marker)
        }
    }

    private fun setupMovingMarker(startPoint: GeoPoint) {
        if (movingMarker == null) {
            movingMarker = Marker(mapView).apply {
                // ✅ 1. 아이콘을 화살표 모양으로 설정
                icon = normalPuckDrawable
                // ✅ 2. 회전의 중심점을 마커의 중앙으로 설정
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            }
            mapView.overlays.add(movingMarker)
        }
        movingMarker?.position = startPoint
    }

    companion object {
        // ✅ 준비 애니메이션을 위한 지속 시간 상수 추가
        private const val TAG = "RouteAnimator" // ✅ 로그 TAG 추가
        private const val PREPARATION_DURATION_MS = 1500L
        private const val POLYLINE_STROKE_WIDTH = 20f
        private const val ANIMATION_INTERVAL = 70L
    }
}