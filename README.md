# offline_naviapp

인터넷 없이 동작하는 Android 오프라인 내비게이션 앱. 대한민국 OSM 지도 데이터를 앱에 번들로 내장하고, GraphHopper 엔진으로 경로를 계산한다. 재난 위험 구역(Shadow Zone)을 회피하는 경로 탐색 기능을 포함한다.

## Stack

![Kotlin](https://img.shields.io/badge/Kotlin-Android-7F52FF)
![GraphHopper](https://img.shields.io/badge/Routing-GraphHopper-green)
![OSMDroid](https://img.shields.io/badge/Map-OSMDroid-blue)
![Hilt](https://img.shields.io/badge/DI-Hilt-orange)
![Room](https://img.shields.io/badge/DB-Room-green)

- **지도**: OSMDroid (OpenStreetMap)
- **경로 계산**: GraphHopper Core (오프라인)
- **지도 데이터**: 대한민국 OSM PBF 번들 (`south-korea-251016.osm.pbf`)
- **DI**: Hilt
- **DB**: Room (Shadow Zone, Tile 정보 저장)
- **상태 관리**: Coroutines + Flow + ViewModel
- **네트워크**: Retrofit (온라인 Geocoding 보조)
- **저장소**: DataStore
- **빌드**: compileSdk 35, minSdk 28, ViewBinding

## 주요 기능

### 오프라인 경로 탐색
- 대한민국 OSM 지도 데이터를 앱 내 번들로 내장 → 인터넷 없이 경로 계산
- GraphHopper 엔진 초기화 (`InitGraphHopperUseCase`)
- 출발지–목적지 경로 탐색 (`FindRouteUseCase`)
- 경유지 포함 다중 경로 탐색 (`FindRouteWithWaypointsUseCase`)

### 재난 위험 구역 회피
- Shadow Zone(위험 구역) DB 저장 및 관리
- 위험 구역 회피 경로 탐색 (`FindRouteAvoidingAreaUseCase`)
- 구역 타입 분류 (`ZoneType`)

### 주소 / 위치 서비스
- 주소 검색 (`SearchAddressUseCase`)
- 역지오코딩 (`ReverseGeocodeUseCase`)
- 상세 주소 조회 (`GetDetailedAddressUseCase`)
- 재난 구역 조회 (`GetDisasterZonesUseCase`)

### 경로 안내 UI
- 회전 안내 (`MajorTurnInstruction`, `TurnType`)
- 경로 목록 (`RouteAdapter`)

### 채팅
- 사용자 간 채팅 (`ChatMessage`, `ChatAdapter`)

## 프로젝트 구조

```
app/src/main/
├── assets/
│   └── south-korea-251016.osm.pbf   # 오프라인 지도 데이터
├── java/com/dantariun/sat_demo_map/
│   ├── data/
│   │   ├── db/           # Room (ShadowZone, TileInfo)
│   │   ├── model/        # Route, ChatMessage, TurnType 등
│   │   └── repository/   # RouteRepository, RouteState
│   ├── domain/
│   │   └── usecase/      # 경로·주소·재난구역 Use Case
│   ├── network/          # Retrofit (Geocoding API)
│   ├── ui/
│   │   ├── HomeActivity  # 메인 지도 화면
│   │   ├── SplashActivity
│   │   └── adapter/      # Route, Chat, Settings Adapter
│   └── di/               # Hilt Module
```

## 시작하기

```bash
git clone https://github.com/dantariun/offline_naviapp.git
```

Android Studio Hedgehog 이상, minSdk 28 (Android 9) 필요.

```bash
./gradlew assembleDebug
```

> OSM PBF 파일(`south-korea-251016.osm.pbf`)이 `app/src/main/assets/`에 포함되어 있어 최초 빌드 시 오래 걸릴 수 있다.

## 권한

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />  <!-- Geocoding API용 -->
```

## 주의사항

- 오프라인 경로 계산 엔진(GraphHopper) 초기화는 최초 1회 PBF 파싱이 필요하므로 시간이 소요된다.
- Geocoding(주소 검색)은 온라인 API를 사용하므로 인터넷 연결이 필요하다.
- Shadow Zone 데이터는 Room DB에 저장되며, API 또는 수동으로 등록 가능하다.
