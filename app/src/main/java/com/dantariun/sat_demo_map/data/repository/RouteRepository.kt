package com.dantariun.sat_demo_map.data.repository

import android.util.Log
import com.dantariun.sat_demo_map.data.model.RouteItem
import com.dantariun.sat_demo_map.data.model.TurnInstruction
import com.dantariun.sat_demo_map.domain.usecase.FindRouteAvoidingAreaUseCase
import com.dantariun.sat_demo_map.domain.usecase.FindRouteUseCase
import com.dantariun.sat_demo_map.domain.usecase.FindRouteWithWaypointsUseCase
import com.dantariun.sat_demo_map.domain.usecase.ReverseGeocodeUseCase
import com.dantariun.sat_demo_map.util.graphHopper.RouteAnalyzer
import com.dantariun.sat_demo_map.util.graphHopper.RouteRefiner
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.osmdroid.util.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteRepository @Inject constructor(
    private val findRouteUseCase: FindRouteUseCase,
    private val findRouteWithWaypointsUseCase: FindRouteWithWaypointsUseCase,
    private val findRouteAvoidingAreaUseCase: FindRouteAvoidingAreaUseCase,
    private val reverseGeocodeUseCase: ReverseGeocodeUseCase,
    private val routeRefiner: RouteRefiner,
    private val routeAnalyzer: RouteAnalyzer
) {
    // Repository 외부에서는 읽기만 가능하도록 private MutableStateFlow와 public StateFlow를 분리
    private val _routeState = MutableStateFlow(RouteState())
    val routeState: StateFlow<RouteState> = _routeState

    private val _selectedRouteState = MutableStateFlow<RouteState?>(null)
    val selectedRouteState: StateFlow<RouteState?> = _selectedRouteState

    // ViewModel에서 가져온 경로 상수들
//    private val startPoint = GeoPoint(40.41663904048912, -3.7038067060400697)
    private val startPoint = GeoPoint(37.501331, 127.026243)
    private val waypoints = listOf(GeoPoint(40.41236773643751, -3.6992084893409625))
//    private val endPoint = GeoPoint(40.41421779512965, -3.6956403664270385)
    private val endPoint = GeoPoint(37.510516, 127.099411)

    suspend fun fetchInitialRoute() {
        _routeState.value = RouteState(isLoading = true) // 로딩 시작
        findRouteUseCase(startPoint, endPoint)
            .onSuccess { routeDetails ->
                val refinedRoute = routeRefiner.refineRoute(routeDetails.points)
                val instructions = routeAnalyzer.analyze(refinedRoute)
                val instructionsWithAddresses = enrichInstructionsWithAddresses(instructions)
                val endPointAddress = getAddressForPoint(endPoint)

                _routeState.value = RouteState(
                    route = refinedRoute,
                    instructions = instructionsWithAddresses ,
                    isLoading = false,
                    distance = routeDetails.distance,
                    time = routeDetails.time,
                    address = endPointAddress
                )
            }
            .onFailure { error ->
                _routeState.value = RouteState(
                    isLoading = false,
                    error = "경로 탐색 실패: ${error.message}"
                )
            }
    }

    suspend fun fetchAlternateRoute(currentLocation: GeoPoint) {
        if (waypoints.isEmpty()) {
            _routeState.value = _routeState.value.copy(error = "경유지가 설정되지 않았습니다.")
            return
        }
        _routeState.value = _routeState.value.copy(
            isLoading = true,
            route= emptyList(),
            instructions = emptyList(),
            ) // 로딩 시작

        delay(4000) // 실제 네트워크처럼 보이게 딜레이

        findRouteWithWaypointsUseCase(currentLocation, endPoint, waypoints)
            .onSuccess { routeDetails ->
                val refinedRoute = routeRefiner.refineRoute(routeDetails.points)
                val instructions = routeAnalyzer.analyze(refinedRoute)
                val instructionsWithAddresses = enrichInstructionsWithAddresses(instructions)
                // ▼▼▼ [수정] 도착 지점의 주소를 가져옵니다. ▼▼▼
                val endPointAddress = getAddressForPoint(endPoint)
                _routeState.value = RouteState(
                    route = refinedRoute,
                    instructions = instructionsWithAddresses,
                    isLoading = false,
                    autoStartAnimation = true,
                    distance = routeDetails.distance,
                    time = routeDetails.time,
                    address = endPointAddress // ▼▼▼ [수정] 주소 정보 포함 ▼▼▼
                )
            }
            .onFailure { error ->
                _routeState.value = _routeState.value.copy(
                    isLoading = false,
                    error = "대체 경로 탐색 실패: ${error.message}"
                )
            }
    }

    suspend fun findRoutesByQuery(query: String): List<RouteItem> {
        if (query.isBlank()) {
            return emptyList()
        }

        var newRouteState: RouteState? = null
        findRouteUseCase(startPoint, endPoint)
            .onSuccess { routeDetails ->
                val refinedRoute = routeRefiner.refineRoute(routeDetails.points)
                val instructions = routeAnalyzer.analyze(refinedRoute)
                val instructionsWithAddresses = enrichInstructionsWithAddresses(instructions)
                val endPointAddress = getAddressForPoint(endPoint)
                newRouteState = RouteState(
                    route = refinedRoute,
                    instructions = instructionsWithAddresses,
                    distance = routeDetails.distance,
                    time = routeDetails.time,
                    address = endPointAddress // ▼▼▼ [수정] 주소 정보 포함 ▼▼▼
                )
            }
            .onFailure {
                // 경로 탐색 실패 시 에러 상태를 포함한 RouteState 생성 가능
                newRouteState = RouteState(error = "경로 탐색 실패")
            }

        // 성공적으로 경로가 생성된 경우에만 아이템 리스트 반환
        return newRouteState?.let {
            listOf(RouteItem(id = 1L, routeState = it), RouteItem(id = 2L, routeState = it), RouteItem(id = 3L, routeState = it))
        } ?: emptyList()
    }

    suspend fun findRoutesByQuery(id:Long, name: String, address: String,  endPoint:GeoPoint): RouteItem {

        var newRouteState: RouteState? = null
        findRouteUseCase(startPoint, endPoint)
            .onSuccess { routeDetails ->
                val refinedRoute = routeRefiner.refineRoute(routeDetails.points)
                val instructions = routeAnalyzer.analyze(refinedRoute)
                val instructionsWithAddresses = enrichInstructionsWithAddresses(instructions)
                newRouteState = RouteState(
                    route = refinedRoute,
                    instructions = instructionsWithAddresses,
                    distance = routeDetails.distance,
                    time = routeDetails.time,
                    address = name
                )
            }
            .onFailure {
                // 경로 탐색 실패 시 에러 상태를 포함한 RouteState 생성 가능
                newRouteState = RouteState(error = "경로 탐색 실패")
            }

        return  RouteItem(id = id, address = address,  routeState = newRouteState!!)
    }

    /**
     * 특정 지역을 회피하는 경로를 탐색하고, 결과를 routeState에 반영합니다.
     * @param avoidArea 회피할 지역의 꼭지점 리스트 (Polygon)
     */
    suspend fun fetchRouteAvoidingArea(currentLocation: GeoPoint,avoidArea: List<GeoPoint>) {
        // 입력값 검증
        if (avoidArea.size < 3) {
            _routeState.value = RouteState(isLoading = false, error = "회피 영역은 3개 이상의 지점으로 지정해야 합니다.")
            return
        }

        // 로딩 상태 시작
        _routeState.value = _routeState.value.copy(
            isLoading = true,
            route= emptyList(),
            instructions = emptyList(),
        ) // 로딩 시작


        delay(2000) // 실제 네트워크처럼 보이게 딜레이

        // UseCase를 사용하여 경로 탐색
        findRouteAvoidingAreaUseCase(currentLocation, endPoint, avoidArea)
            .onSuccess { routeDetails ->
                // 성공 시 경로를 정제하고 분석합니다.
                val refinedRoute = routeRefiner.refineRoute(routeDetails.points)
                val instructions = routeAnalyzer.analyze(refinedRoute)
                val instructionsWithAddresses = enrichInstructionsWithAddresses(instructions)
                val endPointAddress = getAddressForPoint(endPoint)
                _routeState.value = RouteState(
                    route = refinedRoute,
                    instructions = instructionsWithAddresses,
                    isLoading = false,
                    address = endPointAddress // ▼▼▼ [수정] 주소 정보 포함 ▼▼▼
                )
            }
            .onFailure { error ->
                Log.d("", "회피 경로 탐색 실패: ${error.message}")
                // 실패 시 에러 상태를 업데이트합니다.
                _routeState.value = RouteState(
                    isLoading = false,
                    error = "회피 경로 탐색 실패: ${error.message}"
                )
            }
    }

    /**
     * [추가] ViewModel에서 호출하여 선택된 아이템의 RouteState를 저장하는 함수
     */
    fun setSelectedRoute(routeState: RouteState?) {
        _selectedRouteState.value = routeState
    }

    fun updateRouteState(newState: RouteState) {
        _routeState.value = newState
    }

    /**
     * 특정 GeoPoint에 해당하는 가장 가까운 도로명을 반환합니다.
     * @param point 주소를 찾고자 하는 위치
     * @return 성공 시 도로명 String, 실패 또는 결과 없음 시 null
     */
    suspend fun getAddressForPoint(point: GeoPoint): String? {
        var address: String? = null
        reverseGeocodeUseCase(point)
            .onSuccess { streetName ->
                // Usecase가 성공적으로 도로명을 반환하면 변수에 저장
                address = streetName
                Log.d("RouteRepository", "리버스 지오코딩 streetName = $streetName")
            }
            .onFailure { error ->
                // 실패 시 로그를 남기고 null을 반환하도록 보장
                Log.e("RouteRepository", "리버스 지오코딩 실패", error)
                address = null
            }
        return address
    }

    private suspend fun enrichInstructionsWithAddresses(instructions: List<TurnInstruction>): List<TurnInstruction> {
        // 코틀린의 map 함수를 사용하여 리스트의 각 아이템을 변환합니다.
        return instructions.map { instruction ->
            // 각 instruction의 GeoPoint로 주소를 조회합니다.
            val address = getAddressForPoint(instruction.turnPoint)
            // 기존 instruction 데이터는 그대로 복사하고, 주소만 추가하여 새로운 객체를 생성합니다.
            instruction.copy(turnPointAddress = address)
        }
    }
}