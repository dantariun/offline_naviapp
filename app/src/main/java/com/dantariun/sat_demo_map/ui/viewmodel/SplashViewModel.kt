package com.dantariun.sat_demo_map.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dantariun.sat_demo_map.domain.usecase.InitGraphHopperUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel@Inject constructor(
    private val initGraphHopperUseCase: InitGraphHopperUseCase,
    ):ViewModel() {

    sealed class GraphHopperState {
        object Loading : GraphHopperState()
        object Success : GraphHopperState()
        data class Error(val message: String) : GraphHopperState()
    }

    // LiveData를 통해 Activity에 상태를 알립니다.
    // 외부에서는 수정 불가능한 LiveData로, 내부에서는 MutableLiveData로 관리합니다.
    private val _graphHopperState = MutableLiveData<GraphHopperState>()
    val graphHopperState: LiveData<GraphHopperState> = _graphHopperState

    init {
        // ViewModel이 생성되자마자 GraphHopper 초기화를 시작합니다.
        viewModelScope.launch {
            // 초기 상태는 '로딩 중'
            _graphHopperState.value = GraphHopperState.Loading

            initGraphHopperUseCase()
                .onSuccess {
                    // 성공 시 'Success' 상태로 변경
                    _graphHopperState.value = GraphHopperState.Success
                }
                .onFailure { error ->
                    // 실패 시 'Error' 상태로 변경하고 에러 메시지를 전달
                    _graphHopperState.value = GraphHopperState.Error(error.message ?: "알 수 없는 오류 발생")
                }
        }
    }
}