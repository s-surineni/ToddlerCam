package com.toddlerapps.simplecam

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Shader
import kotlin.random.Random

object PhotoEffects {

  private val effectNames = listOf(
    "Sunny", "Frozen", "Fairy Pink", "Rainbow", "Underwater", "Candy", "Space"
  )

  fun applyRandomEffect(source: Bitmap): Pair<Bitmap, String> {
    val index = Random.nextInt(effectNames.size)
    val name = effectNames[index]
    return Pair(applyEffect(source, index), name)
  }

  private fun applyEffect(source: Bitmap, index: Int): Bitmap {
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)

    when (index) {
      0 -> applySunnyEffect(canvas, result)
      1 -> applyFrozenEffect(canvas, result)
      2 -> applyFairyPinkEffect(canvas, result)
      3 -> applyRainbowEffect(canvas, result)
      4 -> applyUnderwaterEffect(canvas, result)
      5 -> applyCandyEffect(canvas, result)
      6 -> applySpaceEffect(canvas, result)
    }

    return result
  }

  private fun applySunnyEffect(canvas: Canvas, bitmap: Bitmap) {
    val paint = Paint()
    paint.colorFilter = PorterDuffColorFilter(0x44FFAA00, PorterDuff.Mode.SRC_ATOP)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    val gradient = LinearGradient(
      0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(),
      Color.argb(80, 255, 200, 50),
      Color.argb(40, 255, 100, 0),
      Shader.TileMode.CLAMP
    )
    val overlayPaint = Paint()
    overlayPaint.shader = gradient
    canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), overlayPaint)
  }

  private fun applyFrozenEffect(canvas: Canvas, bitmap: Bitmap) {
    val paint = Paint()
    paint.colorFilter = PorterDuffColorFilter(0x330066FF, PorterDuff.Mode.SRC_ATOP)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    val gradient = LinearGradient(
      0f, 0f, 0f, canvas.height.toFloat(),
      Color.argb(60, 100, 200, 255),
      Color.argb(30, 0, 50, 150),
      Shader.TileMode.CLAMP
    )
    val overlayPaint = Paint()
    overlayPaint.shader = gradient
    canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), overlayPaint)
  }

  private fun applyFairyPinkEffect(canvas: Canvas, bitmap: Bitmap) {
    val paint = Paint()
    paint.colorFilter = PorterDuffColorFilter(0x33FF69B4, PorterDuff.Mode.SRC_ATOP)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    val gradient = LinearGradient(
      canvas.width.toFloat(), 0f, 0f, canvas.height.toFloat(),
      Color.argb(70, 255, 105, 180),
      Color.argb(30, 255, 182, 193),
      Shader.TileMode.CLAMP
    )
    val overlayPaint = Paint()
    overlayPaint.shader = gradient
    canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), overlayPaint)
  }

  private fun applyRainbowEffect(canvas: Canvas, bitmap: Bitmap) {
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()
    val rainbowColors = intArrayOf(
      Color.argb(50, 255, 0, 0),
      Color.argb(50, 255, 127, 0),
      Color.argb(50, 255, 255, 0),
      Color.argb(50, 0, 200, 0),
      Color.argb(50, 0, 100, 255),
      Color.argb(50, 75, 0, 130),
      Color.argb(50, 148, 0, 211)
    )
    val stripeHeight = h / rainbowColors.size
    val overlayPaint = Paint()
    rainbowColors.forEachIndexed { index, color ->
      overlayPaint.color = color
      val top = index * stripeHeight
      canvas.drawRect(0f, top, w, top + stripeHeight, overlayPaint)
    }
    val tintPaint = Paint()
    tintPaint.colorFilter = PorterDuffColorFilter(0x22FFFFFF, PorterDuff.Mode.SRC_ATOP)
    canvas.drawBitmap(bitmap, 0f, 0f, tintPaint)
  }

  private fun applyUnderwaterEffect(canvas: Canvas, bitmap: Bitmap) {
    val paint = Paint()
    paint.colorFilter = PorterDuffColorFilter(0x44006688, PorterDuff.Mode.SRC_ATOP)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    val gradient = LinearGradient(
      0f, 0f, 0f, canvas.height.toFloat(),
      Color.argb(30, 0, 200, 200),
      Color.argb(60, 0, 30, 100),
      Shader.TileMode.CLAMP
    )
    val overlayPaint = Paint()
    overlayPaint.shader = gradient
    canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), overlayPaint)
  }

  private fun applyCandyEffect(canvas: Canvas, bitmap: Bitmap) {
    val paint = Paint()
    paint.colorFilter = PorterDuffColorFilter(0x33FF44CC, PorterDuff.Mode.SRC_ATOP)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    val gradient = LinearGradient(
      0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(),
      Color.argb(60, 255, 100, 200),
      Color.argb(40, 200, 50, 255),
      Shader.TileMode.CLAMP
    )
    val overlayPaint = Paint()
    overlayPaint.shader = gradient
    canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), overlayPaint)
  }

  private fun applySpaceEffect(canvas: Canvas, bitmap: Bitmap) {
    val paint = Paint()
    paint.colorFilter = PorterDuffColorFilter(0x33220044, PorterDuff.Mode.SRC_ATOP)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    val overlayPaint = Paint()
    overlayPaint.color = Color.argb(40, 80, 0, 120)
    canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), overlayPaint)
    val starPaint = Paint()
    starPaint.color = Color.WHITE
    starPaint.alpha = 200
    val w = canvas.width
    val h = canvas.height
    for (i in 0..30) {
      val x = Random.nextFloat() * w
      val y = Random.nextFloat() * h
      val size = Random.nextFloat() * 4 + 2
      canvas.drawCircle(x, y, size, starPaint)
    }
  }
}
