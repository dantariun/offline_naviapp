package com.dantariun.sat_demo_map.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dantariun.sat_demo_map.data.SettingsManager
import com.dantariun.sat_demo_map.data.db.repository.ShadowZoneRepository
import com.dantariun.sat_demo_map.data.model.RouteItem
import com.dantariun.sat_demo_map.data.model.UserStatus
import com.dantariun.sat_demo_map.data.repository.RouteRepository
import com.dantariun.sat_demo_map.data.repository.RouteState
import com.dantariun.sat_demo_map.domain.ZoneType
import com.dantariun.sat_demo_map.domain.usecase.GetDisasterZonesUseCase
import com.dantariun.sat_demo_map.domain.usecase.InitGraphHopperUseCase
import com.dantariun.sat_demo_map.domain.usecase.SearchAddressUseCase
import com.dantariun.sat_demo_map.network.model.AddressModel
import com.dantariun.sat_demo_map.ui.data.MapUiState
import com.dantariun.sat_demo_map.ui.data.SettingItem
import com.dantariun.sat_demo_map.ui.data.ShadowZoneUiState
import com.dantariun.sat_demo_map.util.graphHopper.SpeedCalculator
import com.dantariun.sat_demo_map.ui.helper.animation.LayoutState
import com.dantariun.sat_demo_map.util.common.NetworkStatus
import com.dantariun.sat_demo_map.util.common.NetworkStatusListener
import com.dantariun.sat_demo_map.util.etc.SettingItems
import com.dantariun.sat_demo_map.util.event.SingleLiveEvent
import com.dantariun.sat_demo_map.util.map.GpsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import retrofit2.Response
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel
class MapViewModel @Inject constructor(
    private val initGraphHopperUseCase: InitGraphHopperUseCase,
    private val routeRepository: RouteRepository,
    private val getDisasterZonesUseCase: GetDisasterZonesUseCase,
    private val gpsManager: GpsManager,
    private val repository: ShadowZoneRepository,
    private val networkStatusListener: NetworkStatusListener,
    private val speedCalculator: SpeedCalculator,
    private val settingsManager: SettingsManager,
    private val searchAddressUseCase: SearchAddressUseCase
) : ViewModel() {

    private val _currentFragmentTitle = MutableLiveData<String>()
    val currentFragmentTitle: LiveData<String> = _currentFragmentTitle

    private val _currentTime = MutableLiveData<String>()
    val currentTime: LiveData<String> = _currentTime

    // UI 상태를 관리하는 LiveData
    private val _uiState = MutableLiveData<MapUiState>()
    val uiState: LiveData<MapUiState> = _uiState

    // Toast 메시지 등 1회성 이벤트를 위한 LiveData
    private val _userMessage = MutableLiveData<SingleLiveEvent<String>>()
    val userMessage: LiveData<SingleLiveEvent<String>> = _userMessage

    // 현재 사용자 위치를 관리하는 LiveData 추가
    private val _currentUserLocation = MutableLiveData<GeoPoint>()
    val currentUserLocation: LiveData<GeoPoint> = _currentUserLocation

    // ✅ 2. 음영 지역 상태를 관리할 새로운 LiveData 추가
    private val _shadowZoneState = MutableLiveData<ShadowZoneUiState>(ShadowZoneUiState.Default)
    val shadowZoneState: LiveData<ShadowZoneUiState> = _shadowZoneState

    private val simulatedLocationFlow = MutableSharedFlow<GeoPoint>()
    // ✅ 2. SpeedCalculator의 StateFlow를 ViewModel의 StateFlow로 직접 노출
    val currentSpeed: StateFlow<Float> = speedCalculator.speedFlow

    private val _disasterZones =  MutableLiveData<List<Pair<ZoneType, List<GeoPoint>>>>()
    val disasterZones : LiveData<List<Pair<ZoneType, List<GeoPoint>>>> = _disasterZones

    // ✅ 1. 재해 알림 팝업 표시를 위한 LiveData 추가
    private val _showDisasterAlertEvent = MutableLiveData<SingleLiveEvent<Unit>>()
    val showDisasterAlertEvent: LiveData<SingleLiveEvent<Unit>> = _showDisasterAlertEvent

    private val _mapResetEvent = MutableLiveData<SingleLiveEvent<Unit>>()
    val mapResetEvent: LiveData<SingleLiveEvent<Unit>> = _mapResetEvent

    private val _showEmergencyAlertEvent = MutableLiveData<SingleLiveEvent<Unit>>()
    val showEmergencyAlertEvent: LiveData<SingleLiveEvent<Unit>> = _showEmergencyAlertEvent

    private val _isSatComMode  = MutableLiveData<Boolean>(false)
    val isSatComMode :LiveData<Boolean> = _isSatComMode

    private val _isChatMode  = MutableLiveData<Boolean>(false)
    val isChatMode :LiveData<Boolean> = _isChatMode

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val _stopNavigationEvent = MutableLiveData<SingleLiveEvent<Unit>>()
    val stopNavigationEvent: LiveData<SingleLiveEvent<Unit>> = _stopNavigationEvent

    private val _totalRemainingDistance = MutableLiveData<String>()
    val totalRemainingDistance: LiveData<String> = _totalRemainingDistance

    private val _isToggleHiddenShow = MutableLiveData<Boolean>(false)
    val isToggleHiddenShow: LiveData<Boolean> = _isToggleHiddenShow

    private var lastKnownSimulatedLocation: GeoPoint? = GeoPoint(40.416775, -3.703790)

    private val _userStatus = MutableLiveData<UserStatus>(UserStatus.GOOD)
    val userStatus: LiveData<UserStatus> = _userStatus


    //Route search

    private val _startNavigationEvent = MutableSharedFlow<RouteState>()
    val startNavigationEvent = _startNavigationEvent.asSharedFlow()

    private val _searchResults = MutableLiveData<List<RouteItem>>()
    val searchResults: LiveData<List<RouteItem>> = _searchResults

    private val _selectedRouteState = MutableLiveData<RouteState?>()
    val selectedRouteState: LiveData<RouteState?> = _selectedRouteState

    private val _addressResult = MutableLiveData<Response<List<AddressModel>>>()
    val addressResult: LiveData<Response<List<AddressModel>>> = _addressResult


    init {
        initialize()
        viewModelScope.launch {
            routeRepository.routeState.collect { routeState ->
                routeState.error?.let {
                    _userMessage.value = SingleLiveEvent(it)
                }

                // Repository의 상태를 기반으로 MapUiState 업데이트
                _uiState.value = _uiState.value?.copy(
                    isLoading = routeState.isLoading,
                    route = routeState.route,
                    instructions = routeState.instructions,
                    autoStartAnimation = routeState.autoStartAnimation,
                    address = routeState.address
                )
            }
        }
    }

    private fun initialize() {
        _uiState.value = MapUiState(isLoading = true)

        //현재 시간
        viewModelScope.launch {
            val timeFormat = SimpleDateFormat("h:mm a", Locale.KOREAN)
            while (isActive) {
                _currentTime.value = timeFormat.format(Date())

                val currentTimeMillis = System.currentTimeMillis()
                val delayMillis = 60000 - (currentTimeMillis % 60000)
                delay(delayMillis)
            }
        }

        // 네트워크 상태 변화 감지 시작
        viewModelScope.launch {
            networkStatusListener.networkStatus.collect { status ->
                when (status) {
                    is NetworkStatus.Available -> onNetworkAvailable()
                    is NetworkStatus.Unavailable -> onNetworkUnavailable()
                }
            }
        }

//        _currentUserLocation.value = GeoPoint(40.416775, -3.703790)
        //현재 위치 및 속도 계산 - 실제 주행 테스트 시 활성
//        startGpsTracking()
    }

    fun updateSimulatedLocation(point: GeoPoint) {
        // ✅ 3. 새로운 위치 정보를 SpeedCalculator에 직접 전달
        // 이제 복잡한 Flow 체인이나 Coroutine launch가 필요 없습니다.
        lastKnownSimulatedLocation = point
        speedCalculator.addGeoPoint(point)
    }

    // GPS 추적을 시작하는 함수
    fun startGpsTracking() {
        viewModelScope.launch {
            val locationFlow = gpsManager.getLocationFlow()

            launch {
                locationFlow.collect { location ->
                    val newGeoPoint = GeoPoint(location.latitude, location.longitude)
                    _currentUserLocation.value = newGeoPoint
                }
            }

            // 속력 계산
        }
    }

    fun setCurrentFragmentTitle(title: String) {
        _currentFragmentTitle.value = title
    }

    // 경로 탐색
    fun findRoute() {
        viewModelScope.launch {
            routeRepository.fetchInitialRoute()
        }
    }

    fun findAlternateRoute() {
        lastKnownSimulatedLocation?.let {
            viewModelScope.launch {
                routeRepository.fetchAlternateRoute(it)
            }
        } ?: run {
            _userMessage.value = SingleLiveEvent("현재 위치를 알 수 없어 대체 경로를 탐색할 수 없습니다.")
        }
    }

    fun findRouteAvoidingArea() {
        lastKnownSimulatedLocation?.let { _location ->
            viewModelScope.launch {
                _disasterZones.value?.first()?.second?.let { zone ->
                    routeRepository.fetchRouteAvoidingArea(_location, zone)
                }
            }
        } ?: run {
            _userMessage.value = SingleLiveEvent("현재 위치를 알 수 없어 대체 경로를 탐색할 수 없습니다.")
        }
    }


    // 재난 구역 데이터 로딩 (예시)
    fun loadDisasterZones() {
        // ✅ UseCase를 호출하여 재난 구역 데이터를 가져옵니다.
        _disasterZones.value = getDisasterZonesUseCase()
    }

    fun onNetworkAvailable() {
        // ✅ 6. 상태가 '진행 중'일 때만 상태 변경 및 DB 로깅
        if (_shadowZoneState.value is ShadowZoneUiState.InProgress) {
            _shadowZoneState.value = ShadowZoneUiState.Default

//            currentUserLocation.value?.let { endPoint ->
//                viewModelScope.launch(Dispatchers.IO) {
//                    repository.stopLoggingShadowZone(endPoint)
//                }
//            }
        }
    }

    fun onNetworkUnavailable() {
        // ✅ 7. 상태가 '기본'일 때만 상태 변경 및 DB 로깅
        if (_shadowZoneState.value is ShadowZoneUiState.Default) {
            currentUserLocation.value?.let { startPoint ->
                _shadowZoneState.value = ShadowZoneUiState.InProgress(startPoint)

//                viewModelScope.launch(Dispatchers.IO) {
//                    repository.startLoggingShadowZone(startPoint)
//                }
            }
        }
    }

    fun toggleDataStatus(){
        if (_shadowZoneState.value is ShadowZoneUiState.InProgress) {
            _shadowZoneState.value = ShadowZoneUiState.Default
        }else{
            _shadowZoneState.value = lastKnownSimulatedLocation?.let {
                ShadowZoneUiState.InProgress(
                    it
                )
            }
        }
    }

    fun toggleSatComMode(){
        _isSatComMode.value = _isSatComMode.value != true
    }

    fun toggleSatComMode(mode:Boolean){
        _isSatComMode.value = mode
    }

    fun toggleChatMode(){
        _isChatMode.value = _isChatMode.value != true
    }

    fun startRouteWithState(routeState: RouteState) {
        _isNavigating.value = true
        // 애니메이션이 바로 시작되도록 autoStartAnimation 플래그를 true로 설정
        val newState = routeState.copy()
        routeRepository.updateRouteState(newState)
    }

    fun stopNavigation() {
        _stopNavigationEvent.value = SingleLiveEvent(Unit)
    }

    fun finishNavigation() {
        _isNavigating.value = false
        routeRepository.updateRouteState(RouteState())
    }

    // ✅ 2. 재해 발생을 시뮬레이션하기 위한 트리거 함수
    fun triggerDisasterAlert() {
        _showDisasterAlertEvent.value = SingleLiveEvent(Unit) // 이벤트 발생
    }

    fun triggerMapReset(){
        _mapResetEvent.value = SingleLiveEvent(Unit)
    }

    fun triggerEmergencyAlert(){
        _showEmergencyAlertEvent.value = SingleLiveEvent(Unit)
    }

    fun updateTotalRemainingDistance(distanceText: String) {
        _totalRemainingDistance.postValue(distanceText)
    }

    fun toggleHiddenView(){
        _isToggleHiddenShow.value = !_isToggleHiddenShow.value!!
    }

    fun toggleHiddenView(bool:Boolean){
        _isToggleHiddenShow.value = bool
    }

    private val _layoutState = MutableStateFlow(LayoutState.FULL_MAP)
    val layoutState = _layoutState.asStateFlow()

    // MapFragment에서 이 함수를 호출하여 현재 상태를 ViewModel에 알립니다.
    fun updateLayoutState(newState: LayoutState) {
        _layoutState.value = newState
    }

    fun cycleUserStatus() {
        val currentStatus = _userStatus.value ?: UserStatus.GOOD

        val allStatuses = UserStatus.values()
        val nextOrdinal = (currentStatus.ordinal + 1) % allStatuses.size

        _userStatus.value = allStatuses[nextOrdinal]
    }

    // route search


    fun searchAddress(query: String) {
        viewModelScope.launch {
            val result = searchAddressUseCase(query)
            _addressResult.postValue(result)
        }
    }

    // findRoute -> findRoutesByQuery 로 함수명 및 로직 변경
    fun findRoutesByQuery(query: String) {
        viewModelScope.launch {
            val results = routeRepository.findRoutesByQuery(query)
            _searchResults.postValue(results)
        }
    }

    // 아이템 선택/해제 로직
    fun toggleRouteSelection(selectedItem: RouteItem) {
        val currentList = _searchResults.value ?: return
        var newlySelectedState: RouteState? = null

        val newList = currentList.map { item ->
            val isNowSelected = item.id == selectedItem.id && !item.isSelected
            if (isNowSelected) {
                newlySelectedState = item.routeState
            }
            item.copy(isSelected = isNowSelected)
        }

        // Repository에 선택된 RouteState 업데이트 (선택 해제 시 null)
        routeRepository.setSelectedRoute(newlySelectedState)

        _searchResults.value = newList
    }

    // settings

    val settingsList: StateFlow<List<SettingItem>> = settingsManager.settingsFlow
        .map { preferences ->
            SettingItems.createSettingItems(preferences)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSettingChanged(key: String, isEnabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setBoolean(key, isEnabled) // SettingsManager에 키와 값을 전달
        }
    }
}