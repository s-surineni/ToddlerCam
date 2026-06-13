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

    // Multi-touch state: tracks each finger by pointerId
    private val activePointers = mutableMapOf<Int, Pair<Float, Float>>()
    private var tapDownTime = 0L
    private var tapDownX = 0f
    private var tapDownY = 0f
    private var singleFingerDown = false

    fun setCircleDetectionCallback(callback: () -> Unit) {
        circleDetectionCallback = callback
    }

    fun setTapCallback(callback: () -> Unit) {
        tapCallback = callback
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val id = event.getPointerId(idx)
                val x = event.getX(idx)
                val y = event.getY(idx)
                if (activePointers.isEmpty()) {
                    touchPoints.clear()
                    tapDownTime = System.currentTimeMillis()
                    tapDownX = x
                    tapDownY = y
                    singleFingerDown = true
                } else {
                    singleFingerDown = false
                }
                activePointers[id] = Pair(x, y)
                touchPoints.add(Pair(x, y))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)
                    activePointers[id] = Pair(x, y)
                    touchPoints.add(Pair(x, y))
                }
                invalidate()
                if (touchPoints.size >= minPointsForCircle && isCircleDetected()) {
                    circleDetectionCallback?.invoke()
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                val id = event.getPointerId(idx)
                activePointers.remove(id)
            }
            MotionEvent.ACTION_UP -> {
                val idx = event.actionIndex
                val upX = event.getX(idx)
                val upY = event.getY(idx)
                activePointers.clear()
                if (touchPoints.size >= minPointsForCircle && isCircleDetected()) {
                    circleDetectionCallback?.invoke()
                } else if (singleFingerDown) {
                    val dist = sqrt((upX - tapDownX).pow(2) + (upY - tapDownY).pow(2))
                    val dur = System.currentTimeMillis() - tapDownTime
                    if (dist < 50f && dur < 500) {
                        tapCallback?.invoke()
                    }
                }
                touchPoints.clear()
                singleFingerDown = false
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                activePointers.clear()
                touchPoints.clear()
                singleFingerDown = false
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
