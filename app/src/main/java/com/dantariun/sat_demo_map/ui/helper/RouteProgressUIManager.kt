package com.dantariun.sat_demo_map.ui.helper

import android.widget.ImageView
import android.widget.TextView
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.data.model.TurnInstruction
import com.dantariun.sat_demo_map.data.model.TurnType
import org.osmdroid.util.GeoPoint
import java.text.DecimalFormat

interface OnTotalDistanceUpdatedListener {
    fun onUpdate(distanceText: String)
}

class RouteProgressUIManager(
    private val turnIcon: ImageView,
    private val nextTurnDistanceText: TextView,
    private val distanceUnitText: TextView,
    private val nextAddressText: TextView,
) {
    private lateinit var instructions: List<TurnInstruction>
    private var currentInstructionIndex = 0
    private var totalDistanceListener: OnTotalDistanceUpdatedListener? = null
    fun setOnTotalDistanceUpdatedListener(listener: OnTotalDistanceUpdatedListener) {
        this.totalDistanceListener = listener
    }
    fun reset() {
        this.currentInstructionIndex = 0
        if (instructions.isNotEmpty()) {
            val totalDistance = instructions.sumOf { it.distanceToNextTurnMeters.toInt() }
            updateTotalDistanceUI(totalDistance)
            getIconForTurnTypeIcon(TurnType.START, turnIcon)
            nextTurnDistanceText.text = ""
        }else {
            // 경로가 없을 경우 빈 문자열로 초기화
            totalDistanceListener?.onUpdate("")
        }
    }

    /**
     * (선택사항) 새로운 경로 정보로 교체하고 리셋하는 메소드
     */
    fun setNewRoute(newInstructions: List<TurnInstruction>) {
        this.instructions = newInstructions
        reset()
    }

    fun update(currentLocation: GeoPoint) {
        if (currentInstructionIndex >= instructions.size - 1) {
            displayFinish()
            return
        }

        val nextInstruction = instructions[currentInstructionIndex + 1]

        // 다음 회전 지점까지 남은 거리 (m)
        val distanceToNextTurn = currentLocation.distanceToAsDouble(nextInstruction.turnPoint)

        // ✅ 2. 총 남은 거리 계산 로직 추가
        // (현재 위치 ~ 다음 회전 지점까지의 거리) + (그 이후 모든 구간의 거리 합)
        var remainingSegmentsDistance = 0.0
        // 다음다음 안내부터 마지막 안내까지의 모든 구간 길이를 더함
        for (i in (currentInstructionIndex + 2) until instructions.size) {
            remainingSegmentsDistance += instructions[i].distanceToNextTurnMeters
        }
        val totalRemainingDistance = distanceToNextTurn + remainingSegmentsDistance

        // 다음 회전 지점에 거의 도착하면, 다음 안내로 전환
        if (distanceToNextTurn < 10) {
            currentInstructionIndex++
            update(currentLocation) // UI를 즉시 다시 계산하여 업데이트
            return
        }

        // ✅ 3. UI 업데이트 함수 호출
        updateNextTurnUI(nextInstruction, distanceToNextTurn.toInt())
        updateTotalDistanceUI(totalRemainingDistance.toInt())
    }

    // 다음 회전 안내 UI 업데이트
    private fun updateNextTurnUI(instruction: TurnInstruction, distance: Int) {
        if (distance <= 1000) {
            nextTurnDistanceText.text = "$distance"
            distanceUnitText.text = "m"
        } else {
            val distanceInKm = distance / 1000.0
            nextTurnDistanceText.text = String.format("%.1f", distanceInKm)
            distanceUnitText.text = "km"
        }

        nextAddressText.text = instruction.turnPointAddress

        getIconForTurnTypeIcon(instruction.turnType, turnIcon)
    }

    // ✅ 4. 총 남은 거리 UI를 업데이트하는 함수 추가
    private fun updateTotalDistanceUI(distanceMeters: Int) {
        val km = distanceMeters / 1000.0
        val df = DecimalFormat("#.#")
        val distanceText = df.format(km)
        totalDistanceListener?.onUpdate(distanceText)
    }

    private fun displayFinish() {
        currentInstructionIndex = 0
        nextTurnDistanceText.text = "도착"
        getIconForTurnTypeIcon(TurnType.FINISH, turnIcon)
        // 도착 시 남은 거리 0으로 표시
        totalDistanceListener?.onUpdate("0m")
    }

    private fun getIconForTurnTypeIcon(turnType: TurnType, turnIcon: ImageView): ImageView {
        turnIcon.setImageResource(getIconForTurnType(turnType))
//        turnIcon.imageTintList = ColorStateList.valueOf(Color.BLUE)
        return turnIcon
    }

    private fun getIconForTurnType(turnType: TurnType): Int {
        return when (turnType) {
            TurnType.LEFT -> R.drawable.icon_turn_left
            TurnType.RIGHT -> R.drawable.icon_turn_right
            TurnType.U_TURN -> R.drawable.icon_u_turn
            TurnType.STRAIGHT -> R.drawable.icon_straight
            TurnType.START -> R.drawable.icon_flag
            TurnType.FINISH -> R.drawable.icon_flag
        }
    }
}