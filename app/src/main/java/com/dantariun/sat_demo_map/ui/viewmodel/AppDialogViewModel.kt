package com.dantariun.sat_demo_map.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dantariun.sat_demo_map.data.model.DialogScreen
import com.dantariun.sat_demo_map.ui.dialog.AppCenterDialog
import com.dantariun.sat_demo_map.ui.dialog.AppDialogResult

class AppDialogViewModel : ViewModel() {
    private var listener: AppCenterDialog.AppDialogListener? = null
    private var resultSent = false // 결과가 이미 전송되었는지 확인하는 플래그
    private val _currentScreen = MutableLiveData<DialogScreen>()
    val currentScreen: LiveData<DialogScreen> = _currentScreen

    val cancelReason = MutableLiveData<String>()

    fun setListener(listener: AppCenterDialog.AppDialogListener?) {
        this.listener = listener
    }

    fun setInitialScreen(screen: DialogScreen) {
        if (_currentScreen.value == null) {
            _currentScreen.value = screen
        }
    }

    fun onRescueConfirmed() {
        // 1. 리스너를 통해 결과 전달
        sendResult(AppDialogResult.RescueConfirmed)
    }

    fun onCancelReasonSelected(reason: String) {
        // 2. 데이터와 함께 결과 전달
        cancelReason.value = reason
        sendResult(AppDialogResult.RescueCancelled(reason))
    }

    fun onDangerDetected() {
        sendResult(AppDialogResult.DangerDetected)
    }

    // 다이얼로그가 그냥 닫혔을 때 호출될 함수
    fun notifyDismiss() {
        sendResult(AppDialogResult.Dismissed)
    }

    // 결과를 전송하는 공통 함수
    private fun sendResult(result: AppDialogResult) {
        if (!resultSent) { // 결과는 한 번만 보내도록 처리
            listener?.onDialogResult(result)
            resultSent = true
        }
    }

    fun onRescueCancelled() {
        _currentScreen.value = DialogScreen.CANCEL_REASON
    }
}