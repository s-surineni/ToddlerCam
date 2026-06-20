package com.toddlerapps.simplecam

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
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
    private val minCircleRadius = 150f

    // Multi-touch state: tracks each finger by pointerId
    private val activePointers = mutableMapOf<Int, Pair<Float, Float>>()
    private val pointerDownTime = mutableMapOf<Int, Long>()
    private val pointerDownX = mutableMapOf<Int, Float>()
    private val pointerDownY = mutableMapOf<Int, Float>()
    private var tapConsumed = false

    // Cap at 200 points to prevent unbounded growth
    private val maxTouchPoints = 200

    // Cached for onDraw path
    private val touchPath = android.graphics.Path()

    // Cached for onLayout
    private val exclusionRect = Rect()
    private val exclusionRects = mutableListOf<Rect>()

    fun setCircleDetectionCallback(callback: () -> Unit) {
        circleDetectionCallback = callback
    }

    fun setTapCallback(callback: () -> Unit) {
        tapCallback = callback
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val id = event.getPointerId(idx)
                val x = event.getX(idx)
                val y = event.getY(idx)
                if (activePointers.isEmpty()) {
                    touchPoints.clear()
                    tapConsumed = false
                }
                pointerDownTime[id] = System.currentTimeMillis()
                pointerDownX[id] = x
                pointerDownY[id] = y
                activePointers[id] = Pair(x, y)
                if (touchPoints.size < maxTouchPoints) {
                    touchPoints.add(Pair(x, y))
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)
                    activePointers[id] = Pair(x, y)
                    if (touchPoints.size < maxTouchPoints) {
                        touchPoints.add(Pair(x, y))
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_POINTER_UP -> {
                val idx = event.actionIndex
                val id = event.getPointerId(idx)
                checkTap(id, event.getX(idx), event.getY(idx))
                cleanupPointer(id)
            }
            MotionEvent.ACTION_UP -> {
                val idx = event.actionIndex
                val id = event.getPointerId(idx)
                if (touchPoints.size >= minPointsForCircle && isCircleDetected()) {
                    circleDetectionCallback?.invoke()
                } else {
                    checkTap(id, event.getX(idx), event.getY(idx))
                }
                cleanupAll()
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                cleanupAll()
                invalidate()
            }
        }
        return true
    }

    private fun checkTap(id: Int, upX: Float, upY: Float) {
        if (tapConsumed) return
        val downTime = pointerDownTime[id] ?: return
        val downX = pointerDownX[id] ?: return
        val downY = pointerDownY[id] ?: return
        val dist = sqrt((upX - downX).pow(2) + (upY - downY).pow(2))
        val dur = System.currentTimeMillis() - downTime
        if (dist < 50f && dur < 500) {
            tapConsumed = true
            tapCallback?.invoke()
        }
    }

    private fun cleanupPointer(id: Int) {
        activePointers.remove(id)
        pointerDownTime.remove(id)
        pointerDownX.remove(id)
        pointerDownY.remove(id)
    }

    private fun cleanupAll() {
        activePointers.clear()
        pointerDownTime.clear()
        pointerDownX.clear()
        pointerDownY.clear()
        touchPoints.clear()
        tapConsumed = false
    }

    private fun isCircleDetected(): Boolean {
        val n = touchPoints.size
        if (n < minPointsForCircle) return false

        // Single pass: compute centroid
        var sumX = 0.0
        var sumY = 0.0
        for ((x, y) in touchPoints) {
            sumX += x
            sumY += y
        }
        val cx = (sumX / n).toFloat()
        val cy = (sumY / n).toFloat()

        // Second pass: compute average radius and variance
        var sumDist = 0.0
        var sumSqDiff = 0.0
        for ((x, y) in touchPoints) {
            val dx = x - cx
            val dy = y - cy
            val dist = sqrt((dx * dx + dy * dy).toDouble())
            sumDist += dist
            sumSqDiff += dist * dist
        }
        val avgRadius = sumDist / n
        if (avgRadius < minCircleRadius) return false

        val variance = max(0.0, sumSqDiff / n - avgRadius * avgRadius)
        val stdDev = sqrt(variance)
        return stdDev < avgRadius * 0.3
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            exclusionRect.set(0, 0, width, height)
            exclusionRects.clear()
            exclusionRects.add(exclusionRect)
            systemGestureExclusionRects = exclusionRects
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (touchPoints.size > 1) {
            touchPath.reset()
            touchPath.moveTo(touchPoints[0].first, touchPoints[0].second)
            for (i in 1 until touchPoints.size) {
                touchPath.lineTo(touchPoints[i].first, touchPoints[i].second)
            }
            canvas.drawPath(touchPath, paint)
        }
    }
}
