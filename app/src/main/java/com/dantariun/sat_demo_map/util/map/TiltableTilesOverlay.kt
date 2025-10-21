package com.dantariun.sat_demo_map.util.map

import android.content.Context
import android.graphics.Canvas
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.util.RectL
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import kotlin.math.tan

/**
 * 기울기(Tilt)에 따라 동적으로 오버 스캔 영역을 계산하여 타일을 그리는 커스텀 TilesOverlay.
 */
class TiltableTilesOverlay(
    aTileProvider: MapTileProviderBase,
    aContext: Context
) : TilesOverlay(aTileProvider, aContext) {

    // TiltableMapView로부터 현재 기울기 값을 받기 위한 변수
    var mapTilt: Float = 0f

    /**
     * 오버레이를 그리는 핵심 메소드를 오버라이드합니다.
     * @param c 캔버스
     * @param pMapView MapView 객체
     * @param shadow 그림자 여부 (타일 오버레이에서는 사용 안 함)
     */
    override fun draw(c: Canvas, pMapView: MapView, shadow: Boolean) {
        if (shadow) {
            return // 타일 오버레이는 그림자를 그리지 않습니다.
        }

        // MapView로부터 현재 프로젝션(좌표 변환 객체)을 가져옵니다.
        val projection = pMapView.projection

        // 프로젝션의 기본 화면 영역을 복사하여 수정할 준비를 합니다.
        val extendedViewPort = RectL(
            projection.screenRect.left.toLong(),
            projection.screenRect.top.toLong(),
            projection.screenRect.right.toLong(),
            projection.screenRect.bottom.toLong()
        )
        // 기울기가 적용되었을 때만 오버 스캔 영역을 계산합니다.
        if (mapTilt > 0 && projection.height > 0) {
            // tan 함수를 이용해 상단에 추가로 필요한 수직 픽셀을 계산합니다.
            val verticalOverscan = (projection.height * tan(Math.toRadians(mapTilt.toDouble()))).toInt()
            // 좌우 스크롤을 대비해 수평 버퍼도 추가합니다. (타일 1장 크기)
            val horizontalOverscan = 256

            // 계산된 값을 적용하여 그리기 영역(viewport)을 비대칭적으로 확장합니다.
            extendedViewPort.top -= verticalOverscan
            extendedViewPort.left -= horizontalOverscan
            extendedViewPort.right += horizontalOverscan
        }

        // 확장된 viewport를 사용하여 타일을 그리는 내부 메소드를 호출합니다.
        drawTiles(c, projection, projection.zoomLevel, extendedViewPort)
    }
}
