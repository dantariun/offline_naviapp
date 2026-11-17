package com.dantariun.sat_demo_map.util.graphHopper

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.dantariun.sat_demo_map.common.AppConstants
import com.dantariun.sat_demo_map.data.model.RouteDetails
import com.graphhopper.GHRequest
import com.graphhopper.GraphHopper
import com.graphhopper.config.Profile
import com.graphhopper.json.Statement
import com.graphhopper.json.Statement.Op.MULTIPLY
import com.graphhopper.routing.util.EdgeFilter
import com.graphhopper.util.CustomModel
import com.graphhopper.util.JsonFeature
import com.graphhopper.util.Parameters
import com.graphhopper.util.shapes.GHPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.osmdroid.util.GeoPoint
import java.io.File
import java.io.FileOutputStream

class GraphHopperManager(private val context: Context) {

//    private val ASSET_PBF_FILE_NAME = "madrid-latest.osm.pbf"
    private val ASSET_PBF_FILE_NAME = AppConstants.NavigationSettings.PBF_NAME

    private val TARGET_PBF_FILE_PATH = "${context.getExternalFilesDir(null)?.absolutePath}/$ASSET_PBF_FILE_NAME"
    private val graphFolder: File = File(context.getExternalFilesDir(null)?.absolutePath, "graphhopper/map")

    private var hopper: GraphHopper? = null
    private var isLoading = false // 동시 초기화 방지 플래그

    val isReady: Boolean
        get() = hopper != null && !isLoading

    // 2. 초기화 로직을 담당하는 public suspend 함수로 변경
    /**
     * GraphHopper 엔진을 비동기적으로 초기화합니다.
     * I/O 작업을 포함하므로 반드시 코루틴 내에서 호출해야 합니다.
     * @return 성공 시 true, 실패 시 Exception 발생
     */
    suspend fun init() {
        if (isReady) return
        if (isLoading) throw IllegalStateException("초기화가 이미 진행 중입니다.")

        // 3. I/O 작업을 Dispatchers.IO에서 실행하고 결과를 반환
        return withContext(Dispatchers.IO) {
            try {
                isLoading = true
                Log.d("GraphHopperManager", "GraphHopper 초기화 시작...")

                // 3. Asset에서 PBF 파일을 복사하는 로직 추가
                copyPbfFromAssetsIfNeeded()
                // 폴더가 없으면 생성
                if (!graphFolder.exists()) {
                    graphFolder.mkdirs()
                }

                val gh = GraphHopper().apply {
                    osmFile = TARGET_PBF_FILE_PATH
                    graphHopperLocation = graphFolder.absolutePath


                    // 1. 두 프로필 모두 일반 'Profile'로, weighting은 'fastest'로 정의합니다.
                    val carProfile = Profile("car").setWeighting("fastest")
//                    val carFlexibleProfile = CustomProfile("car_flexible")
//                    carFlexibleProfile.setCustomModel(CustomModel())
//                    carFlexibleProfile.weighting = "custom"
                    setProfiles(carProfile)
//                    setProfiles(carProfile, carFlexibleProfile)
//                    chPreparationHandler.setCHProfiles(CHProfile("car"))
                }

                gh.importOrLoad()
                hopper = gh

                isLoading = false
                Log.d("GraphHopperManager", "GraphHopper 초기화 완료.")

            } catch (e: Exception) {
                isLoading = false
                hopper = null // 실패 시 hopper 인스턴스 null 처리
                Log.e("GraphHopperManager", "GraphHopper 초기화 실패", e)
                // 4. 콜백 대신 Exception을 던져서 실패를 알림
                throw e
            }
        }
    }

    /**
     * 지정된 경로에 PBF 파일이 없으면 Asset에서 복사합니다.
     */
    private fun copyPbfFromAssetsIfNeeded() {
        val targetFile = File(TARGET_PBF_FILE_PATH)
        if (!targetFile.exists()) {
            Log.d("GraphHopperManager", "PBF 파일이 존재하지 않아 Asset에서 복사를 시작합니다.")
            try {
                context.assets.open(ASSET_PBF_FILE_NAME).use { inputStream ->
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d("GraphHopperManager", "PBF 파일 복사 완료: ${targetFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("GraphHopperManager", "Asset에서 PBF 파일 복사 실패", e)
                throw e // 복사 실패 시 초기화 중단
            }
        } else {
            Log.d("GraphHopperManager", "PBF 파일이 이미 존재합니다. 복사를 건너뜁니다.")
        }
    }

    // 5. 경로 탐색 함수를 suspend 함수로 변경
    /**
     * 지정된 시작점과 도착점 사이의 경로를 찾습니다.
     * @return 성공 시 경로 좌표 목록(List<GeoPoint>)
     * @throws Exception 경로 탐색 실패 시
     */
    suspend fun findRoute(startPoint: GeoPoint, endPoint: GeoPoint): RouteDetails  {
        if (!isReady || hopper == null) {
            throw IllegalStateException("GraphHopper is not initialized yet.")
        }

        // 6. 결과를 직접 반환하거나 Exception을 던짐
        return withContext(Dispatchers.IO) {
            val request = GHRequest(
                startPoint.latitude, startPoint.longitude,
                endPoint.latitude, endPoint.longitude
            ).setProfile("car")

            val response = hopper!!.route(request)

            if (response.hasErrors()) {
                throw Exception(response.errors.firstOrNull()?.message ?: "Unknown routing error")
            }


            // 성공 시 경로 포인트 리스트를 직접 반환
            response.best.points.map { GeoPoint(it.lat, it.lon) }

            RouteDetails(
                points = response.best.points.map { GeoPoint(it.lat, it.lon) },
                distance = response.best.distance,
                time = response.best.time
            )
        }
    }

    suspend fun findRouteWithWaypoints(
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        waypoints: List<GeoPoint>
    ): RouteDetails {
        if (!isReady || hopper == null) {
            throw IllegalStateException("GraphHopper is not initialized yet.")
        }

        return withContext(Dispatchers.IO) {
            // GHRequest에 모든 지점을 순서대로 추가합니다.
            // 시작점 -> 경유지1 -> 경유지2 -> ... -> 도착점
            val request = GHRequest().setProfile("car")

            // 1. 시작점 추가
            request.addPoint(GHPoint(startPoint.latitude, startPoint.longitude))

            // 2. 경유지 목록 추가
            waypoints.forEach { waypoint ->
                request.addPoint(GHPoint(waypoint.latitude, waypoint.longitude))
            }

            // 3. 도착점 추가
            request.addPoint(GHPoint(endPoint.latitude, endPoint.longitude))

            val response = hopper!!.route(request)

            if (response.hasErrors()) {
                throw Exception(response.errors.firstOrNull()?.message ?: "Unknown routing error")
            }

            // 성공 시 경로 포인트 리스트를 직접 반환
            response.best.points.map { GeoPoint(it.lat, it.lon) }

            RouteDetails(
                points = response.best.points.map { GeoPoint(it.lat, it.lon) },
                distance = response.best.distance,
                time = response.best.time
            )
        }
    }

    /**
     * 특정 지역(Polygon)을 회피하여 경로를 찾습니다.
     * @param startPoint 시작점
     * @param endPoint 도착점
     * @param avoidArea 회피할 지역의 꼭지점 리스트 (최소 3개 이상)
     * @return 성공 시 경로 좌표 목록(List<GeoPoint>)
     * @throws Exception 경로 탐색 실패 시
     */
    suspend fun findRouteAvoidingArea(
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        avoidArea: List<GeoPoint>
    ): RouteDetails {
        if (!isReady || hopper == null) throw IllegalStateException("GraphHopper is not initialized yet.")
        if (avoidArea.size < 3) throw IllegalArgumentException("회피 지역은 최소 3개 이상의 꼭지점을 가져야 합니다.")

        return withContext(Dispatchers.IO) {
            val customModel = CustomModel()

            val areaId = "avoid_area"

            val geometryFactory = GeometryFactory()
            val coordinates = avoidArea.map { Coordinate(it.longitude, it.latitude) }.toMutableList()
            if (coordinates.first() != coordinates.last()) {
                coordinates.add(coordinates.first())
            }
            val polygon = geometryFactory.createPolygon(coordinates.toTypedArray())
            val jsonFeature = JsonFeature(areaId, "Polygon", null, polygon, HashMap<String, Any>())
            customModel.areas[areaId] = jsonFeature

            val condition = "area_$areaId"
            val statement = Statement.If(condition, MULTIPLY, "0.0")
            customModel.priority.add(statement)

            val request = GHRequest(
                startPoint.latitude, startPoint.longitude,
                endPoint.latitude, endPoint.longitude
            ).setProfile("car_flexible") // CH가 준비되지 않은 '유연한' 프로필 사용

            request.customModel = customModel
            request.putHint(Parameters.CH.DISABLE, true)

            val response = hopper!!.route(request)

            if (response.hasErrors()) {
                Log.e("GraphHopperManager", "Error finding route with avoidance: " + response.errors)
                throw Exception(response.errors.firstOrNull()?.message ?: "Unknown routing error with avoidance area")
            }
            response.best.points.map { GeoPoint(it.lat, it.lon) }


            RouteDetails(
                points = response.best.points.map { GeoPoint(it.lat, it.lon) },
                distance = response.best.distance,
                time = response.best.time
            )
        }
    }

    /**
     * 지정된 GeoPoint에서 가장 가까운 도로명을 찾아 반환합니다. (리버스 지오코딩)
     *
     * 참고: 이 기능은 완전한 주소가 아닌, OSM 데이터에 기록된 가장 가까운 도로명을 반환합니다.
     *
     * @param point 주소로 변환할 지점
     * @return 도로명을 찾은 경우 해당 String, 찾지 못한 경우 null
     * @throws IllegalStateException GraphHopper가 초기화되지 않았을 경우
     */
    suspend fun getAddressFromGeoPoint(point: GeoPoint): String? {
        if (!isReady || hopper == null) {
            throw IllegalStateException("GraphHopper is not initialized yet.")
        }

        return withContext(Dispatchers.IO) {
            // LocationIndex를 사용하여 가장 가까운 지점을 찾습니다.
            val locationIndex = hopper!!.locationIndex
            val result = locationIndex.findClosest(point.latitude, point.longitude, EdgeFilter.ALL_EDGES)

            if (!result.isValid) {
                // 유효한 결과가 없는 경우 null 반환
                return@withContext null
            }

            // 가장 가까운 도로(Edge)의 이름을 가져옵니다.
            val edge = result.closestEdge
            val streetName = edge.name

            // 도로명이 비어있지 않은 경우에만 반환합니다.
            if (streetName.isNullOrEmpty()) {
                null
            } else {
                streetName
            }
        }
    }

    suspend fun getDetailedAddress(context: Context, point: GeoPoint): String? {
        return withContext(Dispatchers.IO) {
            val geocoder = Geocoder(context)
            try {
                val addresses: List<Address>? = geocoder.getFromLocation(point.latitude, point.longitude, 1)

                if (addresses.isNullOrEmpty()) {
                    return@withContext "주소 정보 없음"
                }

                val address = addresses[0]
                // Address 객체에서 원하는 주소 정보를 조합하여 반환
                // 예: "대한민국 서울특별시 중구 태평로1가 31"
                return@withContext address.getAddressLine(0)

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "주소 변환 실패"
            }
        }
    }
}