# GraphHopper & osmdroid 기반 오프라인 내비게이션 프로토타입
본 프로젝트는 네트워크 연결 없이도 지도를 표시하고 경로를 탐색할 수 있는 안드로이드 오프라인 내비게이션 앱의 핵심 기능을 구현한 프로토타입입니다. osmdroid를 통해 오프라인 지도 타일(MBTiles)을 렌더링하고, GraphHopper 라우팅 엔진을 연동하여 경로 계산 및 기본 주행 안내 기능을 구현했습니다.

## 주요 기능
- 온라인/오프라인 지도 렌더링: OpenstreetMap 기반의 지도 타일을 로컬에 저장하여 인터넷 연결이 불안정한 상황에서도 선명한 지도를 표시합니다.

- 오프라인 경로 탐색: GraphHopper 엔진과 사전 빌드된 라우팅 데이터를 사용해 출발지와 목적지 사이의 최적 경로를 오프라인으로 계산합니다.

- 경로 시각화 및 주행 안내: 계산된 경로를 지도 위에 Polyline으로 그리고, 사용자의 이동에 따라 지도 중심과 방향이 자동으로 따라오는 기본적인 내비게이션 기능을 제공합니다.

- 특수 구역 표시: 화재, 침수 등 재난 상황을 가정한 특정 구역을 지도 위에 반투명 오버레이로 표시합니다.

## 기술 스택 및 주요 라이브러리
- Platform: Android (Kotlin)

- Map Rendering: [osmdroid](https://github.com/graphhopper/graphhopper)

- Map source : [openstreetmap](https://tile.openstreetmap.org/)

- Routing Engine: [GraphHopper](https://github.com/graphhopper/graphhopper) with [Geofabrik](https://download.geofabrik.de/)

- Location Services: Google FusedLocationProviderClient

- Architecture: MVVM, UseCase Pattern

- Asynchronous Processing: Kotlin Coroutines (Flow)

- Dependency Injection: Hilt
