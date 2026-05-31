package com.toddlerapps.simplecam

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object PhotoEffects {

  private val effectNames = listOf(
    "Pixel Art", "Cartoon", "Disco Party", "X-Ray", "Confetti",
    "Vignette", "Pop Art", "Thermal", "Fisheye", "Neon Glow"
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
      0 -> applyPixelArt(canvas, result)
      1 -> applyCartoon(canvas, result)
      2 -> applyDiscoParty(canvas, result)
      3 -> applyXRay(canvas, result)
      4 -> applyConfetti(canvas, result)
      5 -> applyVignette(canvas, result)
      6 -> applyPopArt(canvas, result)
      7 -> applyThermal(canvas, result)
      8 -> applyFisheye(canvas, result)
      9 -> applyNeonGlow(canvas, result)
    }
    return result
  }

  private fun applyPixelArt(canvas: Canvas, bitmap: Bitmap) {
    val w = bitmap.width
    val h = bitmap.height
    val pixelSize = (w / 30).coerceAtLeast(8)
    val tiny = Bitmap.createScaledBitmap(bitmap, w / pixelSize, h / pixelSize, false)
    val pixelated = Bitmap.createScaledBitmap(tiny, w, h, false)
    canvas.drawBitmap(pixelated, 0f, 0f, null)
    tiny.recycle()
  }

  private fun applyCartoon(canvas: Canvas, bitmap: Bitmap) {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    val levels = 6
    for (i in pixels.indices) {
      var r = Color.red(pixels[i])
      var g = Color.green(pixels[i])
      var b = Color.blue(pixels[i])

      r = (r / (256 / levels)) * (256 / levels) + 128 / levels
      g = (g / (256 / levels)) * (256 / levels) + 128 / levels
      b = (b / (256 / levels)) * (256 / levels) + 128 / levels

      val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
      val satBoost = 1.5f
      r = (gray + satBoost * (r - gray)).toInt().coerceIn(0, 255)
      g = (gray + satBoost * (g - gray)).toInt().coerceIn(0, 255)
      b = (gray + satBoost * (b - gray)).toInt().coerceIn(0, 255)

      pixels[i] = Color.argb(Color.alpha(pixels[i]), r, g, b)
    }
    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)

    val edgeBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val edgePixels = IntArray(w * h)
    edgeBitmap.getPixels(edgePixels, 0, w, 0, 0, w, h)

    for (y in 1 until h - 1) {
      for (x in 1 until w - 1) {
        val idx = y * w + x
        val left = edgePixels[idx - 1]
        val right = edgePixels[idx + 1]
        val top = edgePixels[idx - w]
        val bottom = edgePixels[idx + w]

        val diffH = Math.abs(Color.red(left) - Color.red(right)) +
          Math.abs(Color.green(left) - Color.green(right)) +
          Math.abs(Color.blue(left) - Color.blue(right))
        val diffV = Math.abs(Color.red(top) - Color.red(bottom)) +
          Math.abs(Color.green(top) - Color.green(bottom)) +
          Math.abs(Color.blue(top) - Color.blue(bottom))

        if (diffH + diffV > 100) {
          bitmap.setPixel(x, y, Color.BLACK)
        }
      }
    }
    edgeBitmap.recycle()
  }

  private fun applyDiscoParty(canvas: Canvas, bitmap: Bitmap) {
    val paint = Paint()
    paint.colorFilter = PorterDuffColorFilter(0x22FF00FF, PorterDuff.Mode.SRC_ATOP)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)

    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val bokehPaint = Paint()
    bokehPaint.isAntiAlias = true
    bokehPaint.style = Paint.Style.FILL

    val colors = intArrayOf(
      Color.argb(80, 255, 0, 100), Color.argb(70, 0, 200, 255),
      Color.argb(70, 255, 255, 0), Color.argb(60, 0, 255, 100),
      Color.argb(80, 255, 100, 255), Color.argb(70, 100, 200, 255),
      Color.argb(60, 255, 150, 0)
    )

    for (i in 0..20) {
      bokehPaint.color = colors[Random.nextInt(colors.size)]
      canvas.drawCircle(
        Random.nextFloat() * w, Random.nextFloat() * h,
        Random.nextFloat() * 80 + 30, bokehPaint
      )
    }

    val sparklePaint = Paint()
    sparklePaint.color = Color.WHITE
    for (i in 0..15) {
      sparklePaint.alpha = Random.nextInt(150) + 100
      canvas.drawCircle(
        Random.nextFloat() * w, Random.nextFloat() * h,
        Random.nextFloat() * 6 + 2, sparklePaint
      )
    }
  }

  private fun applyXRay(canvas: Canvas, bitmap: Bitmap) {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    for (i in pixels.indices) {
      val a = Color.alpha(pixels[i])
      val r = 255 - Color.red(pixels[i])
      val g = 255 - Color.green(pixels[i])
      val b = 255 - Color.blue(pixels[i])
      pixels[i] = Color.argb(a, r, (g * 0.8f).toInt(), (b * 1.2f).toInt().coerceAtMost(255))
    }
    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
  }

  private fun applyConfetti(canvas: Canvas, bitmap: Bitmap) {
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()

    val tintPaint = Paint()
    tintPaint.colorFilter = PorterDuffColorFilter(0x15FFFFFF, PorterDuff.Mode.SRC_ATOP)
    canvas.drawBitmap(bitmap, 0f, 0f, tintPaint)

    val confettiColors = intArrayOf(
      Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
      Color.MAGENTA, Color.CYAN, Color.rgb(255, 100, 0),
      Color.rgb(200, 0, 255), Color.rgb(0, 200, 100), Color.rgb(255, 200, 0)
    )

    val confettiPaint = Paint()
    confettiPaint.isAntiAlias = true

    for (i in 0..60) {
      confettiPaint.color = confettiColors[Random.nextInt(confettiColors.size)]
      val x = Random.nextFloat() * w
      val y = Random.nextFloat() * h
      val sizeW = Random.nextFloat() * 20 + 8
      val sizeH = Random.nextFloat() * 12 + 6

      canvas.save()
      canvas.rotate(Random.nextFloat() * 360, x, y)
      canvas.drawRoundRect(
        RectF(x - sizeW / 2, y - sizeH / 2, x + sizeW / 2, y + sizeH / 2),
        4f, 4f, confettiPaint
      )
      canvas.restore()
    }
  }

  private fun applyVignette(canvas: Canvas, bitmap: Bitmap) {
    val w = canvas.width.toFloat()
    val h = canvas.height.toFloat()
    val centerX = w / 2
    val centerY = h / 2
    val radius = min(w, h) * 0.7f

    // Dark vignette
    val gradient = RadialGradient(
      centerX, centerY, radius + 200,
      intArrayOf(Color.TRANSPARENT, Color.argb(150, 0, 0, 0)),
      floatArrayOf(0.4f, 1.0f),
      Shader.TileMode.CLAMP
    )
    val paint = Paint()
    paint.shader = gradient
    canvas.drawRect(0f, 0f, w, h, paint)

    // Warm center glow
    val glowGradient = RadialGradient(
      centerX, centerY, radius * 0.5f,
      intArrayOf(Color.argb(30, 255, 200, 100), Color.TRANSPARENT),
      floatArrayOf(0.0f, 1.0f),
      Shader.TileMode.CLAMP
    )
    val glowPaint = Paint()
    glowPaint.shader = glowGradient
    canvas.drawRect(0f, 0f, w, h, glowPaint)
  }

  private fun applyPopArt(canvas: Canvas, bitmap: Bitmap) {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    for (i in pixels.indices) {
      var r = Color.red(pixels[i])
      var g = Color.green(pixels[i])
      var b = Color.blue(pixels[i])

      r = ((r - 128) * 1.8f + 128).toInt().coerceIn(0, 255)
      g = ((g - 128) * 1.8f + 128).toInt().coerceIn(0, 255)
      b = ((b - 128) * 1.8f + 128).toInt().coerceIn(0, 255)

      val levels = 4
      r = (r / (256 / levels)) * (256 / levels) + 160 / levels
      g = (g / (256 / levels)) * (256 / levels) + 160 / levels
      b = (b / (256 / levels)) * (256 / levels) + 160 / levels

      pixels[i] = Color.argb(Color.alpha(pixels[i]),
        r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)

    val dotPaint = Paint()
    dotPaint.color = Color.BLACK
    dotPaint.alpha = 30
    for (y in 0 until canvas.height step 12) {
      for (x in 0 until canvas.width step 12) {
        canvas.drawCircle(x.toFloat(), y.toFloat(), 2f, dotPaint)
      }
    }
  }

  private fun applyThermal(canvas: Canvas, bitmap: Bitmap) {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    for (i in pixels.indices) {
      val r = Color.red(pixels[i])
      val g = Color.green(pixels[i])
      val b = Color.blue(pixels[i])
      val brightness = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0

      pixels[i] = when {
        brightness < 0.15 -> Color.rgb(0, 0, 80)
        brightness < 0.3 -> Color.rgb(0, 0, 200)
        brightness < 0.4 -> Color.rgb(0, 150, 255)
        brightness < 0.5 -> Color.rgb(0, 255, 0)
        brightness < 0.6 -> Color.rgb(255, 255, 0)
        brightness < 0.75 -> Color.rgb(255, 130, 0)
        brightness < 0.9 -> Color.rgb(255, 0, 0)
        else -> Color.rgb(255, 255, 255)
      }
    }
    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
  }

  private fun applyFisheye(canvas: Canvas, bitmap: Bitmap) {
    val w = bitmap.width
    val h = bitmap.height
    val centerX = w / 2f
    val centerY = h / 2f
    val maxRadius = min(w, h) / 2f

    val src = bitmap.copy(Bitmap.Config.ARGB_8888, false)
    val srcPixels = IntArray(w * h)
    src.getPixels(srcPixels, 0, w, 0, 0, w, h)
    val dstPixels = IntArray(w * h)

    for (y in 0 until h) {
      for (x in 0 until w) {
        val dx = (x - centerX) / maxRadius
        val dy = (y - centerY) / maxRadius
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (dist <= 1.0f) {
          val newDist = dist * dist
          val angle = Math.atan2(dy.toDouble(), dx.toDouble())
          val srcX = (centerX + (newDist * maxRadius * cos(angle).toFloat())).toInt()
          val srcY = (centerY + (newDist * maxRadius * sin(angle).toFloat())).toInt()
          if (srcX in 0 until w && srcY in 0 until h) {
            dstPixels[y * w + x] = srcPixels[srcY * w + srcX]
          } else {
            dstPixels[y * w + x] = Color.BLACK
          }
        } else {
          dstPixels[y * w + x] = Color.BLACK
        }
      }
    }
    bitmap.setPixels(dstPixels, 0, w, 0, 0, w, h)
    src.recycle()
  }

  private fun applyNeonGlow(canvas: Canvas, bitmap: Bitmap) {
    val w = bitmap.width
    val h = bitmap.height
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

    val gray = IntArray(w * h)
    for (i in pixels.indices) {
      gray[i] = (0.299 * Color.red(pixels[i]) + 0.587 * Color.green(pixels[i]) + 0.114 * Color.blue(pixels[i])).toInt()
    }

    for (i in pixels.indices) { pixels[i] = Color.BLACK }

    val neonColors = intArrayOf(
      Color.rgb(0, 255, 255), Color.rgb(255, 0, 255),
      Color.rgb(0, 255, 0), Color.rgb(255, 255, 0), Color.rgb(255, 100, 0)
    )

    for (y in 1 until h - 1) {
      for (x in 1 until w - 1) {
        val gx = -gray[(y - 1) * w + (x - 1)] - 2 * gray[y * w + (x - 1)] - gray[(y + 1) * w + (x - 1)] +
          gray[(y - 1) * w + (x + 1)] + 2 * gray[y * w + (x + 1)] + gray[(y + 1) * w + (x + 1)]
        val gy = -gray[(y - 1) * w + (x - 1)] - 2 * gray[(y - 1) * w + x] - gray[(y - 1) * w + (x + 1)] +
          gray[(y + 1) * w + (x - 1)] + 2 * gray[(y + 1) * w + x] + gray[(y + 1) * w + (x + 1)]

        val magnitude = Math.sqrt((gx * gx + gy * gy).toDouble()).toInt()
        if (magnitude > 80) {
          val neonColor = neonColors[Random.nextInt(neonColors.size)]
          val brightness = (magnitude.coerceAtMost(255).toFloat() / 255f)
          val alpha = (brightness * 255).toInt().coerceIn(100, 255)
          pixels[y * w + x] = Color.argb(alpha, Color.red(neonColor), Color.green(neonColor), Color.blue(neonColor))
        }
      }
    }
    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
  }
}
