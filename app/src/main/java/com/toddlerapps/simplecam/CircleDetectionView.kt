package com.toddlerapps.simplecam

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.pow
import kotlin.math.sqrt

class CircleDetectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val touchPoints = mutableListOf<Pair<Float, Float>>()
    private val paint = Paint().apply {
        color = 0xFF00FF00.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private var circleDetectionCallback: (() -> Unit)? = null
    private var tapCallback: (() -> Unit)? = null
    private val minPointsForCircle = 20
    private val minCircleRadius = 100f

    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f

    fun setCircleDetectionCallback(callback: () -> Unit) {
        circleDetectionCallback = callback
    }

    fun setTapCallback(callback: () -> Unit) {
        tapCallback = callback
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downTime = System.currentTimeMillis()
                downX = event.x
                downY = event.y
                touchPoints.clear()
                touchPoints.add(Pair(event.x, event.y))
            }
            MotionEvent.ACTION_MOVE -> {
                touchPoints.add(Pair(event.x, event.y))
                invalidate()
                
                // Check if circle is detected
                if (touchPoints.size >= minPointsForCircle) {
                    if (isCircleDetected()) {
                        circleDetectionCallback?.invoke()
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                var isCircle = false
                if (touchPoints.size >= minPointsForCircle) {
                    if (isCircleDetected()) {
                        circleDetectionCallback?.invoke()
                        isCircle = true
                    }
                }
                
                if (!isCircle) {
                    val upTime = System.currentTimeMillis()
                    val upX = event.x
                    val upY = event.y
                    val distance = sqrt((upX - downX).pow(2) + (upY - downY).pow(2))
                    val duration = upTime - downTime
                    
                    if (distance < 50f && duration < 500) {
                        tapCallback?.invoke()
                    }
                }
                touchPoints.clear()
                invalidate()
            }
        }
        return true
    }

    private fun isCircleDetected(): Boolean {
        if (touchPoints.size < minPointsForCircle) return false

        // Calculate center of all points
        val centerX = touchPoints.map { it.first }.average().toFloat()
        val centerY = touchPoints.map { it.second }.average().toFloat()

        // Calculate average distance from center (radius)
        val distances = touchPoints.map { point ->
            sqrt((point.first - centerX).pow(2) + (point.second - centerY).pow(2))
        }
        val avgRadius = distances.average()

        if (avgRadius < minCircleRadius) return false

        // Check if all points are roughly equidistant from center (circularity)
        val variance = distances.map { (it - avgRadius).pow(2) }.average()
        val stdDev = sqrt(variance)

        // If standard deviation is small relative to radius, it's a circle
        return stdDev < avgRadius * 0.3
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val rects = listOf(Rect(0, 0, width, height))
            systemGestureExclusionRects = rects
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw touch path
        if (touchPoints.size > 1) {
            for (i in 0 until touchPoints.size - 1) {
                val p1 = touchPoints[i]
                val p2 = touchPoints[i + 1]
                canvas.drawLine(p1.first, p1.second, p2.first, p2.second, paint)
            }
        }
    }
}
