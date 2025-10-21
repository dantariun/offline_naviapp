package com.dantariun.sat_demo_map.domain.usecase

import android.content.Context
import com.dantariun.sat_demo_map.util.graphHopper.GraphHopperManager
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

class GetDetailedAddressUseCase @Inject constructor(
    private val graphHopperManager: GraphHopperManager
) {
    /**
     * @param point 주소로 변환할 지리적 좌표
     * @return 성공 시 도로명(String?), 실패 시 Result.Failure
     */
    suspend operator fun invoke(context: Context, point: GeoPoint): Result<String?> {
        // runCatching을 사용하여 GraphHopperManager 호출 중 발생할 수 있는 예외를 처리하고
        // 결과를 Result 객체로 래핑하여 반환합니다.
        return runCatching {
            graphHopperManager.getDetailedAddress(context, point)
        }
    }
}