package com.dantariun.sat_demo_map.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dantariun.sat_demo_map.data.model.Screens
import com.dantariun.sat_demo_map.databinding.FragmentRouteSearchBinding // 달라진 점
import com.dantariun.sat_demo_map.ui.adapter.RouteAdapter
import com.dantariun.sat_demo_map.ui.viewmodel.MapViewModel
import com.dantariun.sat_demo_map.ui.viewmodel.RouteSearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RouteSearchFragment : Fragment() { // 달라진 점

    private var _binding: FragmentRouteSearchBinding? = null // 달라진 점
    private val binding get() = _binding!!

    private val routeViewModel: RouteSearchViewModel by viewModels()
    private val mapViewModel: MapViewModel by activityViewModels()
    private lateinit var routeAdapter: RouteAdapter

//    private lateinit var mapViewManager: MapViewManager
//    private lateinit var mapView: TiltableMapView
//    private lateinit var routeOverlayManager: RouteOverlayManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        observeViewModel()
    }

    private fun initView() {
        binding.startRouteButton.setOnClickListener {
            val selectedRoute = routeViewModel.selectedRouteState
            if (selectedRoute != null) {
                parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(BUNDLE_KEY to selectedRoute))
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "경로를 선택해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnRouteSearch.setOnClickListener {
            hideKeyboard()
            performSearch(binding.searchEditText.text.toString())
        }

        binding.searchEditText.post {
            showKeyboard()
        }
//        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.recycler_item_line_spacing)
//        val itemDecoration = VerticalSpaceDrawItemDecoration(spacingInPixels, requireContext().getColor(R.color.gray_300))
//        binding.routeListRecyclerview.addItemDecoration(itemDecoration)

        setupRecyclerView()
        setupMapView()
        setupSearchEditTextListener()
    }

    private fun setupMapView() {
//        mapView = binding.mapViewSearch // 레이아웃에 추가한 MapView ID
//        mapViewManager = MapViewManager(mapView)
//        mapViewManager.setupMap(15.0, null, null)
//        routeOverlayManager = RouteOverlayManager(mapView)
    }

    private fun observeViewModel() {

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                routeViewModel.searchResultsFlow.collect {routes ->
                    routeAdapter.submitList(routes)
                    if (routes.isNotEmpty() && routes.none { it.isSelected }) {
                        routeViewModel.toggleRouteSelection(routes.first())
                    }
                    val isItemSelected = routes.any { it.isSelected }
                    binding.startRouteButton.visibility = if (isItemSelected) View.VISIBLE else View.GONE
                    binding.cancelButton.visibility = if (isItemSelected) View.VISIBLE else View.GONE
                }
            }
        }

//        routeViewModel.searchResults.observe(viewLifecycleOwner) { routes ->
//            routeAdapter.submitList(routes)
//            if (routes.isNotEmpty() && routes.none { it.isSelected }) {
//                routeViewModel.toggleRouteSelection(routes.first())
//            }
//            val isItemSelected = routes.any { it.isSelected }
//            binding.startRouteButton.visibility = if (isItemSelected) View.VISIBLE else View.GONE
//            binding.cancelButton.visibility = if (isItemSelected) View.VISIBLE else View.GONE

//            val selectedRouteState = routes.find { it.isSelected }
//            if (selectedRouteState != null && selectedRouteState.routeState.route.isNotEmpty()) {
//                binding.mapViewSearch.visibility = View.VISIBLE
//                routeOverlayManager.drawRouteAndZoom(selectedRouteState.routeState.route)
//            } else {
//                binding.mapViewSearch.visibility = View.INVISIBLE
//                routeOverlayManager.clearRoute()
//            }
//        }
    }

    private fun setupRecyclerView() {
        routeAdapter = RouteAdapter { routeItem ->
            routeViewModel.toggleRouteSelection(routeItem)
        }
        binding.routeListRecyclerview.apply {
            // [변경] requireContext() 사용
            layoutManager = LinearLayoutManager(requireContext())
            adapter = routeAdapter
        }
    }

    private fun setupSearchEditTextListener() {
        binding.searchEditText.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard()
                performSearch(textView.text.toString())
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun performSearch(query: String) {
        if (query.isNotEmpty()) {
            binding.searchEditText.text.clear()
            binding.searchEditText.clearFocus()
            routeViewModel.findRoutesByQuery(query)
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }

    private fun showKeyboard() {
        binding.searchEditText.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onResume() {
        super.onResume()
        mapViewModel.setCurrentFragmentTitle(Screens.SEARCH.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "route_search_request"
        const val BUNDLE_KEY = "selected_route_state"

        private const val MIN_ZOOM_LEVEL = 1
        private const val MAX_ZOOM_LEVEL = 17
        private const val MBTILES_NAME = "spain_lv_$MAX_ZOOM_LEVEL.mbtiles"
    }
}