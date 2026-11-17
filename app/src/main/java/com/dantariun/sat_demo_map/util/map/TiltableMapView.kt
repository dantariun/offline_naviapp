package com.dantariun.sat_demo_map.util.map
import android.content.Context
import android.graphics.Camera
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import org.osmdroid.util.TileSystem
import org.osmdroid.views.MapView

class TiltableMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : MapView(context, attrs) {

    private val tiltCamera = Camera()
    private val tiltMatrix = Matrix()

    var mapTilt: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 60f)
            invalidate()
        }

    override fun dispatchDraw(canvas: Canvas) {
        if (mapTilt > 0) {
            applyTilt(canvas)
        }
        super.dispatchDraw(canvas)
    }

    private fun applyTilt(canvas: Canvas) {
        tiltCamera.save()
        tiltCamera.rotateX(mapTilt)
        tiltCamera.getMatrix(tiltMatrix)
        tiltCamera.restore()

        tiltMatrix.preTranslate(-width / 2f, -height.toFloat())
        tiltMatrix.postTranslate(width / 2f, height.toFloat())

        canvas.concat(tiltMatrix)
    }

    fun mapTiltUp(){
        mapTilt++
        Log.d("", "mapTilt = $mapTilt")
    }

    fun mapTiltDown(){
        mapTilt--
        Log.d("", "mapTilt = $mapTilt")
    }

    private var overscanTileCount = 0

    /**
     * 화면 경계 바깥으로 몇 개의 타일을 추가로 렌더링할지 설정합니다.
     * @param tileCount 상하좌우로 추가 렌더링할 타일의 개수.
     */
    fun setOverscanTileCount(tileCount: Int) {
        this.overscanTileCount = tileCount.coerceAtLeast(0)
        // 설정이 변경되었으므로, Projection을 다시 계산하도록 요청합니다.
        this.requestLayout()
        this.invalidate()
    }

    /**
     * MapView의 '고유한' 화면 크기를 반환하는 메소드를 오버라이드합니다.
     * 이 메소드가 반환하는 Rect는 Projection 객체 생성의 기준이 됩니다.
     */
    override fun getIntrinsicScreenRect(reuse: Rect?): Rect {
        // 1. 부모 클래스의 원본 메소드를 호출하여 실제 화면 크기를 가져옵니다.
        val rect = super.getIntrinsicScreenRect(reuse)

        // 2. 오버 스캔할 타일 개수가 0보다 크면, 영역을 확장합니다.
        if (overscanTileCount > 0) {
            // 추가할 픽셀 크기를 계산합니다. (타일 개수 * 타일 크기)
            val overscanPixel = overscanTileCount * TileSystem.getTileSize()

            // Rect.inset()은 양수 값을 주면 안쪽으로 줄어들기 때문에,
            // 바깥쪽으로 확장하려면 음수 값을 사용해야 합니다.
            rect.inset(-overscanPixel, -overscanPixel)
        }

        // 3. 확장된 Rect를 반환합니다.
        return rect
    }

}