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
 * A transparent overlay view that detects when the user draws a circle ("O") gesture.
 * When a circle is detected, [onCircleDetected] callback is invoked.
 */
class CircleGestureView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

  var onCircleDetected: (() -> Unit)? = null

  private val touchPoints = mutableListOf<Pair<Float, Float>>()
  private val path = Path()
  private val paint = Paint().apply {
    isAntiAlias = true
    style = Paint.Style.STROKE
    strokeWidth = 8f
    color = 0x40FFFFFF // Semi-transparent white for visual feedback
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        touchPoints.clear()
        path.reset()
        path.moveTo(event.x, event.y)
        touchPoints.add(Pair(event.x, event.y))
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
        if (isCircle()) {
          onCircleDetected?.invoke()
        }
        // Clear the drawn path after a short delay
        postDelayed({
          path.reset()
          touchPoints.clear()
          invalidate()
        }, 500)
      }
    }
    return true
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.drawPath(path, paint)
  }

  /**
   * Determines if the collected touch points form a roughly circular shape.
   * Uses these heuristics:
   * 1. Sufficient number of points
   * 2. Start and end points are close together
   * 3. The path has enough curvature (deviates from a straight line)
   * 4. The overall shape is roughly symmetric
   */
  private fun isCircle(): Boolean {
    if (touchPoints.size < 15) return false

    val start = touchPoints.first()
    val end = touchPoints.last()

    // Start and end must be close together (circle closes)
    val closureDistance = distance(start, end)
    val pathLength = totalPathLength()

    if (pathLength < 200f) return false // Too short to be a meaningful circle

    // The closure distance should be small relative to total path length
    if (closureDistance > pathLength * 0.25f) return false

    // Calculate the center and average radius
    val centerX = touchPoints.map { it.first }.average().toFloat()
    val centerY = touchPoints.map { it.second }.average().toFloat()
    val center = Pair(centerX, centerY)

    val avgRadius = touchPoints.map { distance(it, center) }.average().toFloat()

    if (avgRadius < 30f) return false // Too small

    // Check that most points are roughly the same distance from center (circular shape)
    val variance = touchPoints.map { abs(distance(it, center) - avgRadius) / avgRadius }.average()

    return variance < 0.35 // Points should be within ~35% of the average radius
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