package com.dantariun.sat_demo_map.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dantariun.sat_demo_map.data.model.RouteItem
import com.dantariun.sat_demo_map.data.repository.RouteRepository
import com.dantariun.sat_demo_map.data.repository.RouteState
import com.dantariun.sat_demo_map.domain.usecase.SearchAddressUseCase
import com.dantariun.sat_demo_map.network.model.AddressModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

@HiltViewModel
class RouteSearchViewModel @Inject constructor(
    private val routeRepository: RouteRepository,
    private val searchAddressUseCase: SearchAddressUseCase
):ViewModel() {

    private val _searchResultsFlow = MutableStateFlow(ArrayList<RouteItem>())
    val searchResultsFlow: StateFlow<List<RouteItem>> = _searchResultsFlow

    val selectedRouteState: RouteState?
        get() = searchResultsFlow.value.find { it.isSelected }?.routeState

    fun findRoutesByQuery(query: String) {
        val dummylist = listOf(
            AddressModel(place_id = 1L, lat = "37.4913590", lon = "126.9074246",name = "한림대학교 강남성심병원", display_name = "한림대학교 강남성심병원, 신길로, 대림1동, 영등포구, 서울특별시, 07065, 대한민국"),
            AddressModel(place_id = 2L, lat = "37.4856234", lon = "126.9568109",name = "강남고려병원", display_name = "강남고려병원, 관악로, 행운동, 관악구, 서울특별시, 08737, 대한민국"),
            AddressModel(place_id = 3L, lat = "37.4908321", lon = "126.9079712",name = "강남성심병원.대림성모병원", display_name = "강남성심병원.대림성모병원, 시흥대로, 대림1동, 영등포구, 서울특별시, 07069, 대한민국"),
        )

        viewModelScope.launch {
            val result = searchAddressUseCase(query).body()

            var results =  emptyList<RouteItem>()

            if(result != null){
                results = result.map {
                    routeRepository.findRoutesByQuery(
                        it.place_id,
                        it.name,
                        it.display_name,
                        GeoPoint(
                            it.lat.toDouble(),
                            it.lon.toDouble()
                        )
                    )
                }
            }

//            val results = dummylist.map {
//                routeRepository.findRoutesByQuery(
//                    it.place_id,
//                    it.name,
//                    GeoPoint(
//                        it.lat.toDouble(),
//                        it.lon.toDouble()
//                    )
//                )
//            }
            _searchResultsFlow.value = results as ArrayList<RouteItem>
        }
    }

    fun toggleRouteSelection(selectedItem: RouteItem) {
        val currentList = _searchResultsFlow.value
        var newlySelectedState: RouteState? = null

        val newList = currentList.map { item ->
            val isNowSelected = item.id == selectedItem.id && !item.isSelected
            if (isNowSelected) {
                newlySelectedState = item.routeState
            }
            item.copy(isSelected = isNowSelected)
        }

        routeRepository.setSelectedRoute(newlySelectedState)

        _searchResultsFlow.value = newList as ArrayList<RouteItem>
    }
}