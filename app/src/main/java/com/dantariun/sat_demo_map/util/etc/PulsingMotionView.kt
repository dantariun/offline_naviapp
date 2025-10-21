package com.dantariun.sat_demo_map.util.etc

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.dantariun.sat_demo_map.R
import com.dantariun.sat_demo_map.util.common.CommonUtil
import kotlin.math.min

class PulsingMotionView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private companion object {
        const val WAVE_COUNT = 3
    }

    private data class Wave(var radius: Float, var alpha: Float)

    private val waves = mutableListOf<Wave>()
    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var maxRadius = 0f

    // 중앙에 표시될 아이콘 비트맵
    private val iconBitmap: Bitmap
    private var iconSize: Int = 0
    private val animator: ValueAnimator

    init {
        // --- 1. Custom Attribute 읽어오기 ---
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.PulsingMotionView,
            0, 0
        )

        val iconDrawable = typedArray.getDrawable(R.styleable.PulsingMotionView_pmv_iconDrawable)
            ?: ContextCompat.getDrawable(context, R.drawable.voice_motion)

        iconSize = typedArray.getDimensionPixelSize(
            R.styleable.PulsingMotionView_pmv_iconSize,
            CommonUtil.dpToPx(context, 25) // 기본 아이콘 크기 100px
        )

        val waveColor = typedArray.getColor(
            R.styleable.PulsingMotionView_pmv_waveColor,
            ContextCompat.getColor(context, R.color.white) // 기본 파동 색상
        )

        val waveDuration = typedArray.getInt(
            R.styleable.PulsingMotionView_pmv_waveDuration,
            3000 // 기본 주기 3초
        ).toLong()

        typedArray.recycle() // 항상 호출해야 함

        // --- 2. 읽어온 값으로 View 설정 ---
        iconBitmap = (iconDrawable as BitmapDrawable).toBitmap(iconSize, iconSize)
        wavePaint.style = Paint.Style.FILL
        wavePaint.color = waveColor

        // 3개의 파동 객체 미리 생성
        for (i in 0 until WAVE_COUNT) {
            waves.add(Wave(0f, 0f))
        }

        // --- 3. 3중 파동 루프를 위한 애니메이터 설정 ---
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = waveDuration
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator() // 일정한 속도로 반복
            addUpdateListener {
                updateWaveStates(it.animatedFraction)
            }
        }
    }

    // 각 파동의 상태(반지름, 투명도)를 업데이트
    private fun updateWaveStates(masterFraction: Float) {
        // 3개의 파동이 순차적으로 보이도록 각 파동의 개별 진행률(fraction)을 계산
        for (i in 0 until WAVE_COUNT) {
            val delayFraction = i.toFloat() / WAVE_COUNT // 0.0, 0.33, 0.66
            var individualFraction = masterFraction - delayFraction
            if (individualFraction < 0) {
                individualFraction += 1f // 루프를 위해 1을 더해줌
            }

            val wave = waves[i]

            // 파동이 커졌다가 사라지는 효과
            wave.radius = maxRadius * individualFraction
            wave.alpha = (1f - individualFraction)
        }
        invalidate() // onDraw()를 다시 호출하여 화면을 새로 그림
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 최대 반지름을 '아이콘 바깥부터 View 끝까지'로 계산
        maxRadius = min(w, h) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f

        // 모든 파동을 먼저 그리기
        waves.forEach { wave ->
            // 알파 값은 0~255 사이 정수여야 함
            wavePaint.alpha = (255 * wave.alpha).toInt()
            // 파동의 시작 지점을 아이콘 크기에 맞춤
            val currentRadius = iconSize / 2f + (maxRadius - iconSize / 2f) * wave.radius / maxRadius
            canvas.drawCircle(centerX, centerY, currentRadius, wavePaint)
        }

        // 중앙 아이콘을 마지막에 그리기
        canvas.drawBitmap(
            iconBitmap,
            centerX - iconBitmap.width / 2f,
            centerY - iconBitmap.height / 2f,
            null
        )
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }
}