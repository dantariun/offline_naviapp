package com.dantariun.sat_demo_map.util.etc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class OutlineTextView : AppCompatTextView {
    private var strokeWidth = 0.0f
    private var strokeColor = 0

    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(context, attrs)
    }

    private fun initView(context: Context, attrs: AttributeSet?) {
//        val type = context.obtainStyledAttributes(attrs, R.styleable.OutlineTextView)
//        stroke = type.getBoolean(R.styleable.OutlineTextView_textStroke, false) // 아웃라인 유무
//        strokeWidth = type.getFloat(R.styleable.OutlineTextView_textStrokeWidth, 0.0f) // 아웃라인 두께
//        strokeColor = type.getColor(R.styleable.OutlineTextView_textStrokeColor, -0x1) // 아웃라인 컬러

        strokeWidth = 10f
        strokeColor = Color.WHITE
    }

    override fun onDraw(canvas: Canvas) {
        val states = textColors
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        setTextColor(strokeColor)
        super.onDraw(canvas)
        paint.style = Paint.Style.FILL
        setTextColor(states)

        super.onDraw(canvas)
    }
}