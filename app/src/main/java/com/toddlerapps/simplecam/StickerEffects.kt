package com.toddlerapps.simplecam

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.random.Random

object StickerEffects {

  private data class StickerInfo(val name: String, val draw: (Canvas, Float, Float) -> Unit)

  private val stickerSets = listOf(
    // 1. Balloons
    StickerInfo("Balloons") { canvas, w, h ->
      val colors = intArrayOf(
        0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0066FF.toInt(),
        0xFFFF00FF.toInt(), 0xFFFFFF00.toInt(), 0xFF00FFFF.toInt(),
        0xFFFF6600.toInt(), 0xFF9900FF.toInt()
      )
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      val stringPaint = Paint(Paint.ANTI_ALIAS_FLAG)

      for (i in 0..12) {
        val x = Random.nextFloat() * w * 0.8f + w * 0.1f
        val y = Random.nextFloat() * h * 0.6f + h * 0.05f
        val size = Random.nextFloat() * 40 + 50

        // Balloon body (oval)
        paint.color = colors[Random.nextInt(colors.size)]
        paint.style = Paint.Style.FILL
        canvas.drawOval(RectF(x - size * 0.4f, y - size * 0.5f, x + size * 0.4f, y + size * 0.3f), paint)

        // Balloon highlight
        paint.color = 0x44FFFFFF
        canvas.drawOval(RectF(x - size * 0.2f, y - size * 0.35f, x, y - size * 0.1f), paint)

        // String
        paint.color = 0xFF888888.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawLine(x, y + size * 0.3f, x + Random.nextFloat() * 20 - 10, y + size * 0.8f, paint)
        paint.style = Paint.Style.FILL
      }
    },

    // 2. Birthday Cake
    StickerInfo("Birthday Cake") { canvas, w, h ->
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)

      for (i in 0..4) {
        val cx = Random.nextFloat() * w * 0.7f + w * 0.15f
        val cy = Random.nextFloat() * h * 0.7f + h * 0.15f
        val size = Random.nextFloat() * 30 + 40

        // Cake base (rectangle)
        paint.color = 0xFFE8A065.toInt()
        canvas.drawRoundRect(RectF(cx - size, cy, cx + size, cy + size * 0.6f), 8f, 8f, paint)

        // Frosting (top)
        paint.color = 0xFFFF69B4.toInt()
        canvas.drawRoundRect(RectF(cx - size * 1.1f, cy - size * 0.15f, cx + size * 1.1f, cy + size * 0.1f), 6f, 6f, paint)

        // Candle
        paint.color = 0xFF4488FF.toInt()
        canvas.drawRect(cx - 3f, cy - size * 0.5f, cx + 3f, cy - size * 0.15f, paint)

        // Flame
        paint.color = 0xFFFFAA00.toInt()
        canvas.drawCircle(cx, cy - size * 0.55f, 5f, paint)
        paint.color = 0xFFFFDD00.toInt()
        canvas.drawCircle(cx, cy - size * 0.55f, 3f, paint)
      }
    },

    // 3. Rainbow Dots
    StickerInfo("Rainbow Dots") { canvas, w, h ->
      val colors = intArrayOf(
        0xFFFF0000.toInt(), 0xFFFF8800.toInt(), 0xFFFFFF00.toInt(),
        0xFF00CC00.toInt(), 0xFF0088FF.toInt(), 0xFF8800FF.toInt(),
        0xFFFF00FF.toInt(), 0xFFFF4488.toInt(), 0xFF00CCCC.toInt()
      )
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      paint.style = Paint.Style.FILL

      for (i in 0..40) {
        val x = Random.nextFloat() * w
        val y = Random.nextFloat() * h
        val radius = Random.nextFloat() * 18 + 8
        paint.color = colors[Random.nextInt(colors.size)]
        paint.alpha = Random.nextInt(100) + 155
        canvas.drawCircle(x, y, radius, paint)
      }
    },

    // 4. Stars
    StickerInfo("Stars") { canvas, w, h ->
      val colors = intArrayOf(
        0xFFFFD700.toInt(), 0xFFFFAA00.toInt(), 0xFFFFFF44.toInt(),
        0xFFFF6600.toInt(), 0xFFFF88CC.toInt()
      )
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      paint.style = Paint.Style.FILL

      for (i in 0..20) {
        val x = Random.nextFloat() * w
        val y = Random.nextFloat() * h
        val size = Random.nextFloat() * 25 + 15
        paint.color = colors[Random.nextInt(colors.size)]
        drawStar(canvas, x, y, size, 5, paint)
      }
    },

    // 5. Hearts
    StickerInfo("Hearts") { canvas, w, h ->
      val colors = intArrayOf(
        0xFFFF0066.toInt(), 0xFFFF3399.toInt(), 0xFFFF66AA.toInt(),
        0xFFFF0033.toInt(), 0xFFCC0066.toInt(), 0xFFFF99CC.toInt()
      )
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      paint.style = Paint.Style.FILL

      for (i in 0..15) {
        val x = Random.nextFloat() * w
        val y = Random.nextFloat() * h
        val size = Random.nextFloat() * 20 + 15
        paint.color = colors[Random.nextInt(colors.size)]
        drawHeart(canvas, x, y, size, paint)
      }
    },

    // 6. Emoji Stickers
    StickerInfo("Emoji Party") { canvas, w, h ->
      val emojis = arrayOf("🎈", "🎂", "🎁", "🌟", "🎵", "🦋", "🌈", "🎨", "🌸", "🍭", "🎪", "🎡")
      val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
      textPaint.textAlign = Paint.Align.CENTER

      for (i in 0..15) {
        val x = Random.nextFloat() * w
        val y = Random.nextFloat() * h
        val size = Random.nextFloat() * 30 + 30
        textPaint.textSize = size
        canvas.drawText(emojis[Random.nextInt(emojis.size)], x, y, textPaint)
      }
    },

    // 7. Polka Dots
    StickerInfo("Polka Dots") { canvas, w, h ->
      val colors = intArrayOf(
        0xFFFF4488.toInt(), 0xFF44BBFF.toInt(), 0xFFFFEE44.toInt(),
        0xFF44FF88.toInt(), 0xFFFF8844.toInt(), 0xFFBB44FF.toInt()
      )
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      paint.style = Paint.Style.FILL

      val spacing = 80f
      var row = 0
      var y = spacing / 2
      while (y < h) {
        val offsetX = if (row % 2 == 1) spacing / 2 else 0f
        var x = spacing / 2 + offsetX
        while (x < w) {
          paint.color = colors[Random.nextInt(colors.size)]
          paint.alpha = Random.nextInt(80) + 120
          canvas.drawCircle(x, y, Random.nextFloat() * 10 + 12, paint)
          x += spacing
        }
        y += spacing
        row++
      }
    }
  )

  fun applyRandomSticker(source: Bitmap): Pair<Bitmap, String> {
    val sticker = stickerSets[Random.nextInt(stickerSets.size)]
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    sticker.draw(canvas, source.width.toFloat(), source.height.toFloat())
    return Pair(result, sticker.name)
  }

  private fun drawStar(canvas: Canvas, cx: Float, cy: Float, size: Float, points: Int, paint: Paint) {
    val path = android.graphics.Path()
    val innerRadius = size * 0.4f
    val outerRadius = size
    val angle = Math.PI / points

    path.moveTo(cx, cy - outerRadius)
    for (i in 1 until points * 2) {
      val r = if (i % 2 == 0) outerRadius else innerRadius
      val a = -Math.PI / 2 + i * angle
      path.lineTo(cx + (r * Math.cos(a)).toFloat(), cy + (r * Math.sin(a)).toFloat())
    }
    path.close()
    canvas.drawPath(path, paint)
  }

  private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
    val path = android.graphics.Path()
    val w = size
    val h = size * 0.9f

    path.moveTo(cx, cy + h * 0.35f)
    path.cubicTo(cx, cy - h * 0.1f, cx - w, cy - h * 0.1f, cx - w, cy + h * 0.15f)
    path.cubicTo(cx - w, cy + h * 0.55f, cx, cy + h * 0.75f, cx, cy + h)
    path.cubicTo(cx, cy + h * 0.75f, cx + w, cy + h * 0.55f, cx + w, cy + h * 0.15f)
    path.cubicTo(cx + w, cy - h * 0.1f, cx, cy - h * 0.1f, cx, cy + h * 0.35f)
    path.close()
    canvas.drawPath(path, paint)
  }
}
