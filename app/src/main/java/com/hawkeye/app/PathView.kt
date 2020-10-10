package com.hawkeye.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class PathView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val paint1 = Paint()
    private val path1 = Path()
    private val paint2 = Paint()
    private val path2 = Path()
    private var isFirstDraw = true

    init {
        paint1.apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 4f
            isAntiAlias = true
        }
        paint2.apply {
            color = Color.WHITE
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
//            path1.moveTo((width / 2).toFloat(), (height / 2).toFloat())
            path2.moveTo((width / 2).toFloat(), (height / 2).toFloat())
            isFirstDraw = false
        }
//        canvas.drawPath(path1, paint1)
        canvas.drawPath(path2, paint2)
    }

    fun addNewPoint(
        x1: Float, y1: Float,
        x2: Float, y2: Float) {
//        path1.lineTo(x1 * 5 + width / 2, y1 * 5 + height / 2)
//        path1.moveTo(x1 * 5 + width / 2, y1 * 5 + height / 2)
        path2.lineTo(x2 * 5 + width / 2, y2 * 5 + height / 2)
        path2.moveTo(x2 * 5 + width / 2, y2 * 5 + height / 2)
        invalidate()
    }

    fun addNewPoint(x: Float, y: Float) {
        path2.lineTo(x * 5 + width / 2, y * 5 + height / 2)
        path2.moveTo(x * 5 + width / 2, y * 5 + height / 2)
        invalidate()
    }

    fun reset() {
//        path1.reset()
//        path1.moveTo((width / 2).toFloat(), (height / 2).toFloat())
        path2.reset()
        path2.moveTo((width / 2).toFloat(), (height / 2).toFloat())
        invalidate()
    }
}