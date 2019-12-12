package com.firefly.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class PathView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint = Paint()
    private val path = Path()
    private var isFirstDraw = true

    init {
        paint.apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 4f
            isAntiAlias = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isFirstDraw) {
            path.moveTo((width / 2).toFloat(), (height / 2).toFloat())
            isFirstDraw = false
        }
        canvas.drawPath(path, paint)
    }

    public fun addNewPoint(x: Float, y: Float) {
        path.lineTo(x * 5 + width / 2, y * 5 + height / 2)
        path.moveTo(x * 5 + width / 2, y * 5 + height / 2)
        invalidate()
    }

    public fun reset() {
        path.reset()
        path.moveTo((width / 2).toFloat(), (height / 2).toFloat())
        invalidate()
    }
}