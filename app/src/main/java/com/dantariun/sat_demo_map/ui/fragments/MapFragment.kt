package com.dantariun.sat_demo_map.ui.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dantariun.sat_demo_map.data.model.DialogScreen
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.data.model.Screens
import com.dantariun.sat_demo_map.data.repository.RouteState
import com.dantariun.sat_demo_map.databinding.FragmentMapBinding
import com.dantariun.sat_demo_map.domain.ZoneType
import com.dantariun.sat_demo_map.ui.adapter.ChatAdapter
import com.dantariun.sat_demo_map.ui.data.ShadowZoneUiState
import com.dantariun.sat_demo_map.ui.dialog.AppCenterDialog
import com.dantariun.sat_demo_map.ui.dialog.AppDialog
import com.dantariun.sat_demo_map.ui.dialog.AppDialogResult
import com.dantariun.sat_demo_map.ui.dialog.DialogChangeRoute
import com.dantariun.sat_demo_map.ui.dialog.DialogSatManager
import com.dantariun.sat_demo_map.ui.dialog.OnSatDialogInteractionListener
import com.dantariun.sat_demo_map.ui.dialog.OnRouteDialogInteractionListener
import com.dantariun.sat_demo_map.ui.helper.*
import com.dantariun.sat_demo_map.ui.helper.animation.LayoutState
import com.dantariun.sat_demo_map.ui.helper.animation.MapPopAnimationHelper
import com.dantariun.sat_demo_map.ui.helper.map.MapViewManager
import com.dantariun.sat_demo_map.ui.helper.scenario.ChatHelper
import com.dantariun.sat_demo_map.ui.helper.scenario.ChatScenarioHelper
import com.dantariun.sat_demo_map.ui.helper.ui.TargetViewGradientAnimator
import com.dantariun.sat_demo_map.ui.helper.ui.UserLocationManager
import com.dantariun.sat_demo_map.ui.viewmodel.MapViewModel
import com.dantariun.sat_demo_map.util.graphHopper.OnRouteAnimationListener
import com.dantariun.sat_demo_map.util.graphHopper.OnRouteProgressListener
import com.dantariun.sat_demo_map.util.graphHopper.RouteAnimator
import com.dantariun.sat_demo_map.util.map.MapSensorController
import com.dantariun.sat_demo_map.util.map.TileCacheManager
import com.dantariun.sat_demo_map.util.map.TiltableMapView
import com.dantariun.sat_demo_map.util.map.ZoneOverlayManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject


@AndroidEntryPoint
class MapFragment : Fragment(), AppCenterDialog.AppDialogListener {

    @Inject
    lateinit var tileCacheManager: TileCacheManager

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val mapViewModel: MapViewModel by activityViewModels()
    private lateinit var mapView: TiltableMapView

    // Helper 클래스 (Manager)
    private lateinit var mapViewManager: MapViewManager
    private lateinit var routeUIManager: RouteUIManager
    private lateinit var userLocationManager: UserLocationManager
    private lateinit var sensorController: MapSensorController
    private var turnByTurnManager: RouteProgressUIManager? = null
    private lateinit var gradientAnimator: TargetViewGradientAnimator
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatHelper: ChatHelper
    private lateinit var chatScenarioHelper: ChatScenarioHelper
    private lateinit var mapPopAnimationHelper: MapPopAnimationHelper

    private lateinit var dialogSatManager: DialogSatManager
    private lateinit var dialogChangeRoute: DialogChangeRoute

    private var routeAnimator: RouteAnimator? = null
    private var appDialog : AppDialog? = null

    private var appCenterDialog: AppCenterDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFragmentResultListener()
        initViewsAndManagers()
        observeViewModel()
    }

    private fun setupFragmentResultListener() {
        parentFragmentManager.setFragmentResultListener(RouteSearchFragment.REQUEST_KEY, this) { requestKey, bundle ->
            val routeState: RouteState? = if (Build.VERSION.SDK_INT >= 33) {
                bundle.getParcelable(RouteSearchFragment.BUNDLE_KEY, RouteState::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelable(RouteSearchFragment.BUNDLE_KEY)
            }

            routeState?.let {
                mapViewModel.startRouteWithState(it)
                Toast.makeText(requireContext(), "경로 안내를 시작합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initiateAreaCaching() {
        viewLifecycleOwner.lifecycleScope.launch {
            val boundingBox = mapView.boundingBox
            val zoomLevel = mapView.zoomLevelDouble.toInt()

            tileCacheManager.cacheArea(mapView, boundingBox, zoomLevel)
        }
    }

    private fun initViewsAndManagers() {
        mapView = binding.mapView

        gradientAnimator = TargetViewGradientAnimator(binding.animatedGradientView)

//        binding.leftButtonMenu.btnCompass.setOnClickListener {
//            mapViewManager.resetMapRotation()
//            initiateAreaCaching()
//        }

        binding.btnNewRoute.setOnClickListener {
            mapViewModel.loadDisasterZones()
            mapViewModel.triggerDisasterAlert()
        }

        binding.btnEmergency.setOnClickListener {
            mapViewModel.triggerEmergencyAlert()
        }

        binding.btnAiChat.setOnClickListener {
            mapViewModel.updateLayoutState(LayoutState.MAP_WITH_CHAT)
        }

        binding.btnFind.setOnClickListener{
            findNavController().navigate(R.id.action_mapFragment_to_routeSearchFragment)
        }

        binding.btnSetting.setOnClickListener {
            findNavController().navigate(R.id.action_mapFragment_to_settingsFragment)
        }
        binding.viewChatLayout.btnChatClose.setOnClickListener {
            mapViewModel.updateLayoutState(LayoutState.FULL_MAP)
            mapViewModel.toggleChatMode()
        }

        routeUIManager = RouteUIManager(binding.topNextTurnInfo.root)
        userLocationManager = UserLocationManager(mapView)
        sensorController = MapSensorController(requireActivity()) // 달라진 점: this -> requireActivity()

        turnByTurnManager = RouteProgressUIManager(
            binding.topNextTurnInfo.iconTurn,
            binding.topNextTurnInfo.textDistanceNumber,
            binding.topNextTurnInfo.textDistanceUnit,
            binding.topNextTurnInfo.textCurrentRoad
        )
        turnByTurnManager?.setOnTotalDistanceUpdatedListener(object :OnTotalDistanceUpdatedListener{
            override fun onUpdate(distanceText: String) {
                mapViewModel.updateTotalRemainingDistance(distanceText)
            }
        })

        setMap()
        setupChat()
        setAnimator()

        binding.btnZoomIn.setOnClickListener { mapViewManager.mapController.zoomIn() }
        binding.btnZoomOut.setOnClickListener { mapViewManager.mapController.zoomOut() }

        sensorController.setOnSensorUpdateListener(object : MapSensorController.OnSensorUpdateListener {
            override fun onRotationUpdate(rotationAngle: Float) {}
            override fun onTiltUpdate(tiltAngle: Float) { mapView.mapTilt = tiltAngle }
        })

        mapPopAnimationHelper = MapPopAnimationHelper(binding.viewContents)
        mapPopAnimationHelper.initConstraintSets()

        dialogSatManager = DialogSatManager(
            context = requireContext(),
            questionBinding = binding.viewDialogSat,
            completeBinding = binding.viewDialogSatComplete,
            scope = viewLifecycleOwner.lifecycleScope,
            listener = object : OnSatDialogInteractionListener{
                override fun onAgreementAction() {
                    mapViewModel.onSettingChanged("satellite_communication_agree", true)
                    mapViewModel.updateLayoutState(LayoutState.FULL_MAP)
                }

                override fun onDisagreementAction() {
                    mapViewModel.updateLayoutState(LayoutState.FULL_MAP)
                }
            }
        )
        dialogChangeRoute = DialogChangeRoute(
            context = requireContext(),
            binding = binding.viewDialogRouteChange,
            scope = viewLifecycleOwner.lifecycleScope,
            listener = object : OnRouteDialogInteractionListener{
                override fun onAgreementAction() {
                    Toast.makeText(requireContext(), "새로운 경로를 탐색합니다.", Toast.LENGTH_SHORT).show()
                    mapViewModel.findAlternateRoute()
                }

                override fun onDisagreementAction() {
                    mapViewModel.updateLayoutState(LayoutState.FULL_MAP)
                }
            }
        )
    }

    private fun setMap() {
        mapViewManager = MapViewManager(mapView)
        mapViewManager.setupMap(15.0, binding.btnZoomIn, binding.btnZoomOut)
    }

    private fun setupChat() {
        chatAdapter = ChatAdapter()
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true // 키보드가 올라올 때 리스트가 위로 밀려나도록 함
        }

        binding.viewChatLayout.recyclerChatMessages.apply {
            this.layoutManager = layoutManager
            adapter = chatAdapter
        }

        chatHelper = ChatHelper(binding.viewChatLayout.recyclerChatMessages, chatAdapter, binding.viewChatLayout.viewChatProgressbar)
        chatScenarioHelper = ChatScenarioHelper(chatHelper, viewLifecycleOwner.lifecycleScope)

    }

    private fun setAnimator() {
        routeAnimator = RouteAnimator(mapView, MAX_ZOOM_LEVEL.toDouble())
        routeAnimator?.setOnRouteProgressListener(object : OnRouteProgressListener {
            override fun onProgress(currentPoint: GeoPoint) {
                turnByTurnManager?.update(currentPoint)
                mapViewModel.updateSimulatedLocation(currentPoint)
            }
            override fun onRotationChanged(rotation: Float) {
                binding.leftButtonMenu.btnCompass.rotation = rotation
            }
        })

        routeAnimator?.setOnRouteAnimationListener(object : OnRouteAnimationListener {
            override fun onAnimationStart() {
                routeUIManager.showViews()
            }

            override fun onAnimationStopped(completedNaturally: Boolean) {
                handleNavigationStop(completedNaturally)
            }
        })
    }

    private fun observeViewModel() {
        mapViewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.loadingProgressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            if (!state.route.isNullOrEmpty() && !state.instructions.isNullOrEmpty()) {

                preCacheRouteTiles(state.route)

                if (state.autoStartAnimation ) {
                    turnByTurnManager?.setNewRoute(state.instructions)
                    routeAnimator?.drawRouteNew(state.route)
                    return@observe
                }
                routeAnimator?.stop()
                turnByTurnManager?.setNewRoute(state.instructions)
                routeAnimator?.drawRoute(state.route)
                mapViewManager.mapController.setCenter(state.route.first())
                routeAnimator?.start()
            }
        }

        mapViewModel.userMessage.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }

        mapViewModel.currentUserLocation.observe(viewLifecycleOwner) { currentPoint ->
            userLocationManager.updateLocation(currentPoint)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mapViewModel.currentSpeed
                    .filter { speed -> speed > 0.5f }
                    .debounce(100L)
                    .collect { speed ->
                        // UI 업데이트
                    }
            }
        }

        mapViewModel.mapResetEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                mapViewManager.resetMapRotation()
            }
        }
        mapViewModel.showDisasterAlertEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                mapViewModel.updateLayoutState(LayoutState.MAP_WITH_ROUTE)
            }
        }

        mapViewModel.showEmergencyAlertEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                showConflictDetected()
            }
        }

        mapViewModel.disasterZones.observe(viewLifecycleOwner) { disasterZones ->
            disasterZones.forEach { (zoneType, points) ->
                ZoneOverlayManager.drawZone(mapView, points, zoneType)
            }
        }

        mapViewModel.shadowZoneState.observe(viewLifecycleOwner) { state ->
            updateSatComRelatedUi()
        }


        mapViewModel.isChatMode.observe(viewLifecycleOwner) {
            if (mapViewModel.isChatMode.value == false) {
                mapViewModel.updateLayoutState(LayoutState.FULL_MAP)
                if(mapViewModel.isNavigating.value) {
                    routeAnimator?.resume()
                }
            } else {
                mapViewModel.updateLayoutState(LayoutState.MAP_WITH_CHAT)
                if(mapViewModel.isNavigating.value) {
                    routeAnimator?.pause()
                }
                chatScenarioHelper.startScenario(mapViewModel.isSatComMode.value ?: false)
            }
        }

        mapViewModel.stopNavigationEvent.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                navigationStop()
            }
        }

        mapViewModel.isToggleHiddenShow.observe(viewLifecycleOwner) {
            if(it){
                binding.viewHidden.visibility = View.VISIBLE
            }else{
                binding.viewHidden.visibility = View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            mapViewModel.layoutState.collect { state ->
                mapPopAnimationHelper.toggleChatVisibility(state)
                binding.viewPopChat.visibility = if (state == LayoutState.MAP_WITH_CHAT) View.VISIBLE else View.GONE

                when(state) {
                    LayoutState.FULL_MAP -> {
                        dialogSatManager.cleanup()
                        dialogChangeRoute.cleanup()
                    }
                    LayoutState.MAP_WITH_CHAT -> {
                        dialogSatManager.cleanup()
                        dialogChangeRoute.cleanup()
                    }
                    LayoutState.MAP_WITH_DIALOG -> {
                        dialogChangeRoute.cleanup()
                        dialogSatManager.showQuestionView()
                    }
                    LayoutState.MAP_WITH_ROUTE -> {
                        dialogSatManager.cleanup()
                        dialogChangeRoute.showChangeRoute(ZoneType.FIRE)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            mapViewModel.settingsList.collect { settings ->
                // settingsList Flow가 새 값을 발행할 때마다 UI 업데이트 함수 호출
                // DataStore 저장이 완료되면 이 부분이 실행됩니다.
                updateSatComRelatedUi()
            }
        }
    }

    private fun updateSatComRelatedUi() {

        val isNetworkState = mapViewModel.shadowZoneState.value ?: ShadowZoneUiState.Default  // 네트워크 상태

        val isSatComSettingEnabled = mapViewModel.settingsList.value
            .find { it.key == "satellite_communication_agree" }?.isEnabled ?: false // 위성통신 사용 권한

        when (isNetworkState) {
            is ShadowZoneUiState.InProgress -> {  // 데이터 상태 악화
                routeAnimator?.notifyShadowZoneEntered()
                binding.btnStatusData.setImageResource(R.drawable.data_lvl_0)

                if (isSatComSettingEnabled) { //
                    mapViewModel.toggleSatComMode(true)
                    gradientAnimator.show()
                    binding.btnStatusData.setImageResource(R.drawable.icon_network_satellite)
                }else{
                    mapViewModel.toggleSatComMode(false)
                    mapViewModel.updateLayoutState(LayoutState.MAP_WITH_DIALOG)
                }
            }
            is ShadowZoneUiState.Default -> { // 데이터 상태 양호
                mapViewModel.toggleSatComMode(false)
                gradientAnimator.hide()
                routeAnimator?.notifyShadowZoneExited()
                binding.btnStatusData.setImageResource(R.drawable.data_lvl_4)
            }
        }
    }

    private fun preCacheRouteTiles(route: List<GeoPoint>) {
        viewLifecycleOwner.lifecycleScope.launch {
            tileCacheManager.preCacheTilesForRoute(mapView, route, MAX_ZOOM_LEVEL, MAX_ZOOM_LEVEL)
        }
    }

    private fun showConflictDetected(){
        if (appCenterDialog?.isAdded == true) {
            appCenterDialog?.dismiss()
        }
        appCenterDialog = AppCenterDialog.newInstance(DialogScreen.DANGER_DETECTED)
        appCenterDialog?.setDialogListener(this)
        appCenterDialog?.show(childFragmentManager , "AppCenterDialog")
    }

    private fun navigationStop(){
        routeAnimator?.stop(false) // 사용자가 직접 중단했으므로 false 전달
    }

    private fun handleNavigationStop(isCompleted: Boolean) {
        if (isCompleted) {
            Toast.makeText(requireContext(), "목적지에 도착했습니다. 안내를 종료합니다.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(requireContext(), "경로 안내를 종료합니다.", Toast.LENGTH_SHORT).show()
        }

        routeUIManager.hideViews()
        mapViewManager.resetMapRotation()
        binding.leftButtonMenu.btnCompass.rotation = 0f

         mapViewModel.finishNavigation()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        mapViewModel.setCurrentFragmentTitle(Screens.MAP.toString())
        updateSatComRelatedUi()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
//        routeAnimator?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapViewModel.toggleHiddenView(false)
        chatScenarioHelper.cancelScenario()
        routeAnimator?.stop()
        mapView.onDetach()
        _binding = null
    }

    companion object {
        private const val MIN_ZOOM_LEVEL = 3
        private const val MAX_ZOOM_LEVEL = 17
        private const val MBTILES_NAME = "spain_lv_$MAX_ZOOM_LEVEL.mbtiles"
    }

    override fun onDialogResult(result: AppDialogResult) {
        when (result) {
            is AppDialogResult.RescueConfirmed -> {

            }
            is AppDialogResult.RescueCancelled -> {

            }
            is AppDialogResult.DangerDetected -> {
                mapViewModel.toggleChatMode()
            }
            is AppDialogResult.Dismissed -> {

            }
        }
    }
}