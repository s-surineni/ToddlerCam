package com.toddlerapps.simplecam

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A transparent overlay view that:
 * - Detects taps (quick touches) and invokes [onTapDetected]
 * - Detects circle ("O") gestures and invokes [onCircleDetected]
 */
class CircleGestureView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

  var onCircleDetected: (() -> Unit)? = null
  var onTapDetected: (() -> Unit)? = null

  private val touchPoints = mutableListOf<Pair<Float, Float>>()
  private val path = Path()
  private val paint = Paint().apply {
    isAntiAlias = true
    style = Paint.Style.STROKE
    strokeWidth = 8f
    color = 0x40FFFFFF
  }

  private var touchStartTime = 0L
  private var touchStartX = 0f
  private var touchStartY = 0f

  init {
    isClickable = true
    isFocusable = true
    isFocusableInTouchMode = true
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        touchStartTime = System.currentTimeMillis()
        touchStartX = event.x
        touchStartY = event.y
        touchPoints.clear()
        path.reset()
        path.moveTo(event.x, event.y)
        touchPoints.add(Pair(event.x, event.y))
        parent?.requestDisallowInterceptTouchEvent(true)
      }
      MotionEvent.ACTION_MOVE -> {
        path.lineTo(event.x, event.y)
        touchPoints.add(Pair(event.x, event.y))
        invalidate()
      }
      MotionEvent.ACTION_UP -> {
        path.lineTo(event.x, event.y)
        touchPoints.add(Pair(event.x, event.y))
        invalidate()

        val elapsed = System.currentTimeMillis() - touchStartTime
        val totalPathLength = totalPathLength()

        // Quick tap with minimal movement -> take photo
        if (elapsed < 300 && totalPathLength < 50f) {
          postDelayed({
            path.reset()
            touchPoints.clear()
            invalidate()
          }, 200)
          onTapDetected?.invoke()
        } else if (isCircle()) {
          // Circle gesture -> exit app
          postDelayed({
            path.reset()
            touchPoints.clear()
            invalidate()
          }, 500)
          onCircleDetected?.invoke()
        } else {
          // Not a tap or circle -> just clear
          postDelayed({
            path.reset()
            touchPoints.clear()
            invalidate()
          }, 200)
        }
      }
    }
    return true
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.drawPath(path, paint)
  }

  private fun isCircle(): Boolean {
    if (touchPoints.size < 15) return false

    val start = touchPoints.first()
    val end = touchPoints.last()

    val closureDistance = distance(start, end)
    val pathLength = totalPathLength()

    if (pathLength < 150f) return false
    if (closureDistance > pathLength * 0.30f) return false

    val centerX = touchPoints.map { it.first }.average().toFloat()
    val centerY = touchPoints.map { it.second }.average().toFloat()
    val center = Pair(centerX, centerY)

    val avgRadius = touchPoints.map { distance(it, center) }.average().toFloat()

    if (avgRadius < 20f) return false

    val variance = touchPoints.map { abs(distance(it, center) - avgRadius) / avgRadius }.average()

    return variance < 0.40
  }

  private fun distance(a: Pair<Float, Float>, b: Pair<Float, Float>): Float {
    val dx = a.first - b.first
    val dy = a.second - b.second
    return sqrt(dx * dx + dy * dy)
  }

  private fun totalPathLength(): Float {
    var length = 0f
    for (i in 1 until touchPoints.size) {
      length += distance(touchPoints[i - 1], touchPoints[i])
    }
    return length
  }
}