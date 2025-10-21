package com.dantariun.sat_demo_map.domain.usecase

import com.dantariun.sat_demo_map.domain.ZoneType
import org.osmdroid.util.GeoPoint

// 실제 앱에서는 Repository를 주입받아 데이터를 가져와야 합니다.
class GetDisasterZonesUseCase {
    operator fun invoke(): List<Pair<ZoneType, List<GeoPoint>>> {
        // 지금은 예시로 하드코딩된 데이터를 반환
        val fireZonePoints = listOf(
            GeoPoint(40.416530, -3.698836),
            GeoPoint(40.41518476798395, -3.698956909442931),
            GeoPoint(40.414829433974745, -3.6988281634243805),
            GeoPoint(40.4157236514043, -3.696059374259474),
            GeoPoint(40.416878843380715, -3.695513151162866),
            GeoPoint(40.418864782980805, -3.6958205705170033),
            GeoPoint(40.41811610676968, -3.698581253186683),
            GeoPoint(40.41811610676968, -3.698581253186683),
            GeoPoint(40.416584385443926, -3.6983827727333076)
        )
        return listOf(Pair(ZoneType.FIRE, fireZonePoints))
    }
}