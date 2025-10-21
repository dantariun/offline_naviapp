package com.dantariun.sat_demo_map.ui.helper.scenario

import com.dantariun.sat_demo_map.data.model.Sender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 미리 정의된 대화 시나리오의 한 단계를 나타내는 데이터 클래스
 *
 * @param message 메시지 내용
 * @param sender 메시지를 보낸 주체
 * @param delayMillis 이전 메시지 이후 이 메시지를 표시할 때까지의 지연 시간 (밀리초)
 */
data class ScenarioStep(
    val message: String,
    val sender: Sender,
    val delayMillis: Long
)

/**
 * 정의된 시나리오에 따라 채팅을 자동으로 진행하는 헬퍼 클래스.
 *
 * @param chatHelper 메시지를 UI에 추가하는 역할을 하는 기존 ChatHelper
 * @param scope 시나리오 진행을 위한 코루틴 스코프 (일반적으로 Fragment의 viewLifecycleOwner.lifecycleScope)
 */
class ChatScenarioHelper(
    private val chatHelper: ChatHelper,
    private val scope: CoroutineScope
) {
    // 현재 진행 중인 시나리오 코루틴을 제어하기 위한 Job
    private var scenarioJob: Job? = null

    // 미리 정의된 대화 시나리오
    private val conversationScenario = listOf(
        ScenarioStep("위성 통신망에 연결되었습니다. 메시지를 보내주세요.", Sender.OTHER, 1000),
        ScenarioStep("현재 위치와 목적지를 알려주세요.", Sender.OTHER, 2000),
        ScenarioStep("네, 현재 제 위치는 마드리드 왕궁 근처입니다. 목적지는 프라도 미술관입니다.", Sender.ME, 3000),
        ScenarioStep("경로를 탐색합니다. 약 15분 소요됩니다.", Sender.OTHER, 2500),
        ScenarioStep("감사합니다. 바로 출발하겠습니다.", Sender.ME, 2000)
    )

    private val emergencyScenario = listOf(
        // 첫 번째 보고 및 응답
        ScenarioStep("좌표 37.0836N, -113.5827에서 심각한 충돌이 발생했습니다. 차량 전면부가 크게 파손되고 에어백이 전개되었습니다. 운전자는 의식 불명, 움직임이 없으며 출혈이 보입니다. 긴급 구조를 요청합니다.",
            Sender.ME, 2500),
        ScenarioStep("운전자 상태 변화에 주의를 기울이고 생체 신호를 계속 모니터링하세요. 출혈 증가나 의식 변화를 추적하고, 새로운 정보가 발생하면 전달해 주십시오. 구조팀이 출동했으며 곧 도착할 것입니다. 구조를 진행하겠습니다. 정보 감사합니다.",
            Sender.OTHER, 3000),

        // 두 번째 보고 및 응답
        ScenarioStep("운전자는 의식 불명 상태이며 출혈과 움직임 없음이 감지되었습니다. 에어백이 전개되었고 차량 전면 손상이 심각합니다. 긴급 구조 서비스에 연락했으며 구조팀이 오고 있습니다. 구조를 기다리겠습니다. 감사합니다.",
            Sender.ME, 2500),
        ScenarioStep("운전자 상태를 계속 모니터링하고 생체 신호 변화를 즉시 보고해 주십시오. 차량 내 감지된 모든 위험 요소를 확인하세요. 구조대가 지원을 위해 가고 있습니다. 지속적인 업데이트에 감사드립니다. 구조를 진행하겠습니다. 정보 감사합니다.",
            Sender.OTHER, 3000),

        // 마지막 메시지
        ScenarioStep("구조를 기다리겠습니다. 감사합니다.", Sender.ME, 2000)
    )

    private val emergencyScenarioSatellite = listOf(
        // 첫 번째 메시지 시도
        ScenarioStep("차량 위치 36.9533N, 113.7987W, 차량 전면 손상, 에어백 전개됨. 운전자 의식 있으나 움직이지 못함, 출혈 없음, 호흡 정상. 119에 긴급 구조를 요청합니다.",
            Sender.ME, 1000),
        ScenarioStep("[System] 위성 신호가 약해 구조 요청 전송에 실패했습니다. 메시지를 압축하여 재시도합니다…",
            Sender.SYSTEM, 2000),
        ScenarioStep("36.9533N, 113.7987W, 전면 손상, 에어백 전개, 운전자 의식 있음",
            Sender.ME, 5000),
        ScenarioStep("호흡 및 의식 변화를 계속 확인하고, 운전자를 진정시키세요. 구조팀에 통보되었으며, 구조를 진행할 것입니다.",
            Sender.OTHER, 3000),

        // 두 번째 메시지 시도
        ScenarioStep("운전자는 의식이 있으며 호흡과 출혈은 정상입니다. 차량 전면부의 심각한 손상으로 에어백이 전개되었습니다. 긴급 구조 서비스에 통보되었으며, 구조를 기다리겠습니다.",
            Sender.ME, 3000),
        ScenarioStep("[System] 위성 신호가 약해 구조 요청 전송에 실패했습니다. 메시지를 압축하여 재시도합니다…",
            Sender.SYSTEM, 2000),
        ScenarioStep("운전자 의식 있음, 호흡 정상, 출혈 없음, 에어백 전개, 차량 전면 손상",
            Sender.ME, 5000),
        ScenarioStep("상황 모니터링 중. 운전자 상태 확인 및 안정적 환경 유지. 구조팀 이동 중. 호흡을 확인하세요.",
            Sender.OTHER, 3000),

        // 세 번째 메시지 시도
        ScenarioStep("운전자는 의식이 있고 호흡은 정상이며 출혈의 징후는 없습니다. 차량 전면 손상으로 에어백이 전개되었습니다. 긴급 구조 서비스에 통보되었으며 구조대가 오고 있습니다. 구조를 기다리겠습니다.",
            Sender.ME, 3000),
        ScenarioStep("[System] 위성 신호가 약해 구조 요청 전송에 실패했습니다. 메시지를 압축하여 재시도합니다…",
            Sender.SYSTEM, 2000),
        ScenarioStep("운전자 의식 있음, 호흡 정상, 에어백 전개, 긴급 구조 통보 완료",
            Sender.ME, 5000),
        ScenarioStep("상황 모니터링 중. 안전 확보 및 운전자 안정 유지. 구조팀 도착 예정. 구조 진행.",
            Sender.OTHER, 3000)
    )


    /**
     * 정의된 대화 시나리오를 시작합니다.
     * 이미 실행 중인 시나리오가 있다면 취소하고 새로 시작합니다.
     */
    fun startScenario(isSatComMode:Boolean = false) {
        // 이전 시나리오가 실행 중이면 취소
        cancelScenario()

        // 코루틴을 사용하여 시나리오 진행
        scenarioJob = scope.launch {
            delay(2000L)

            if(isSatComMode) {
                emergencyScenarioSatellite.forEach { step ->
                    // 각 단계별로 정해진 시간만큼 대기
                    delay(step.delayMillis)

                    // ChatHelper를 통해 메시지 추가
                    chatHelper.addMessage(step.message, step.sender)
                }
            }else{
                emergencyScenario.forEach { step ->
                    // 각 단계별로 정해진 시간만큼 대기
                    delay(step.delayMillis)

                    // ChatHelper를 통해 메시지 추가
                    chatHelper.addMessage(step.message, step.sender)
                }
            }
        }
    }

    /**
     * 현재 진행 중인 시나리오를 중단합니다.
     */
    fun cancelScenario() {
        chatHelper.clearAllMessages()
        scenarioJob?.cancel()
        scenarioJob = null
    }
}