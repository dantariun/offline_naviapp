package com.dantariun.sat_demo_map.util.etc

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import com.dantariun.sat_demo_map.R // 자신의 R 클래스 경로로 수정하세요.
import kotlin.math.PI
import kotlin.math.sin

class DotElasticLoader @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private companion object {
        const val DOT_COUNT = 3
        const val DELAY_FACTOR = 0.15f // 옆 점들이 얼마나 늦게 반응할지 (0.0 ~ 1.0)
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var dotRadius: Float
    private var dotSpacing: Float
    private var elasticScale: Float

    // 각 점의 현재 Y 스케일과 X 위치 오프셋을 저장
    private val dotYScale = FloatArray(DOT_COUNT) { 1f }
    private val dotXOffsets = FloatArray(DOT_COUNT) { 0f }

    private val animator: ValueAnimator

    init {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.DotElasticLoader, 0, 0
        )
        dotRadius = typedArray.getDimension(R.styleable.DotElasticLoader_del_dotRadius, 12f)
        dotSpacing = typedArray.getDimension(R.styleable.DotElasticLoader_del_dotSpacing, 8f)
        elasticScale = typedArray.getFloat(R.styleable.DotElasticLoader_del_elasticScale, 1.7f)
        val animationDuration = typedArray.getInt(
            R.styleable.DotElasticLoader_del_animationDuration, 600
        ).toLong()
        dotPaint.color = typedArray.getColor(
            R.styleable.DotElasticLoader_del_dotColor, 0xFF3B3B3B.toInt()
        )
        typedArray.recycle()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = animationDuration
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                updateDotStates(it.animatedFraction)
            }
        }
    }

    private fun updateDotStates(masterFraction: Float) {
        for (i in 0 until DOT_COUNT) {
            val individualFraction = if (i == 1) { // 가운데 점
                masterFraction
            } else { // 양 옆 점
                var fraction = masterFraction - DELAY_FACTOR
                if (fraction < 0) fraction += 1f
                fraction
            }

            // sin 함수를 이용해 0 -> 1 -> 0 으로 변하는 '바운스' 값 생성
            val bounce = sin(individualFraction * PI).toFloat()

            if (i == 1) { // 가운데 점은 Y축으로 스케일 변경
                dotYScale[i] = 1f + (elasticScale - 1f) * bounce
                dotXOffsets[i] = 0f
            } else { // 양 옆 점은 X축으로 이동
                dotYScale[i] = 1f
                val translation = (dotRadius * 2 + dotSpacing) * bounce
                dotXOffsets[i] = if (i == 0) -translation else translation
            }
        }
        invalidate()
    }

    // View의 크기를 내용물에 맞게 계산
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalWidth = (dotRadius * 2 * DOT_COUNT) + (dotSpacing * (DOT_COUNT - 1))
        val totalHeight = dotRadius * 2 * elasticScale
        setMeasuredDimension(totalWidth.toInt(), totalHeight.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2f

        for (i in 0 until DOT_COUNT) {
            canvas.save()
            val x = (width / 2f) - (dotRadius * 2 + dotSpacing) + (i * (dotRadius * 2 + dotSpacing))
            val y = centerY

            // 각 점의 X 위치 오프셋 적용
            canvas.translate(x + dotXOffsets[i], y)
            // 가운데 점의 Y 스케일 적용
            canvas.scale(1f, dotYScale[i])

            canvas.drawCircle(0f, 0f, dotRadius, dotPaint)
            canvas.restore()
        }
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