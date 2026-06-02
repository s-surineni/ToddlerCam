package com.toddlerapps.simplecam

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.random.Random

/**
 * Detects faces in a bitmap and draws fun stickers on them.
 * Uses ML Kit Face Detection for real-time face detection.
 */
object FaceStickerHelper {

  private data class FaceSticker(val draw: (Canvas, RectF) -> Unit)

  private val faceStickers = listOf(
    // Crown on top of face
    FaceSticker { canvas, rect ->
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      paint.color = 0xFFFFD700.toInt()
      paint.style = Paint.Style.FILL

      val cx = rect.centerX()
      val top = rect.top - rect.height() * 0.15f
      val crownWidth = rect.width() * 0.6f
      val crownHeight = rect.height() * 0.25f

      val path = android.graphics.Path()
      path.moveTo(cx - crownWidth / 2, top + crownHeight)
      path.lineTo(cx - crownWidth / 2, top)
      path.lineTo(cx - crownWidth / 4, top + crownHeight * 0.6f)
      path.lineTo(cx, top - crownHeight * 0.1f)
      path.lineTo(cx + crownWidth / 4, top + crownHeight * 0.6f)
      path.lineTo(cx + crownWidth / 2, top)
      path.lineTo(cx + crownWidth / 2, top + crownHeight)
      path.close()
      canvas.drawPath(path, paint)

      // Jewels
      paint.color = Color.RED
      canvas.drawCircle(cx, top + crownHeight * 0.4f, 4f, paint)
      paint.color = 0xFF0066FF.toInt()
      canvas.drawCircle(cx - crownWidth / 4, top + crownHeight * 0.5f, 3f, paint)
      canvas.drawCircle(cx + crownWidth / 4, top + crownHeight * 0.5f, 3f, paint)
    },

    // Sunglasses
    FaceSticker { canvas, rect ->
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      paint.color = Color.BLACK
      paint.style = Paint.Style.FILL

      val eyeY = rect.centerY() - rect.height() * 0.05f
      val eyeSpacing = rect.width() * 0.2f
      val lensWidth = rect.width() * 0.18f
      val lensHeight = rect.height() * 0.12f

      // Left lens
      canvas.drawRoundRect(
        RectF(rect.centerX() - eyeSpacing - lensWidth, eyeY - lensHeight,
              rect.centerX() - eyeSpacing + lensWidth, eyeY + lensHeight),
        8f, 8f, paint
      )
      // Right lens
      canvas.drawRoundRect(
        RectF(rect.centerX() + eyeSpacing - lensWidth, eyeY - lensHeight,
              rect.centerX() + eyeSpacing + lensWidth, eyeY + lensHeight),
        8f, 8f, paint
      )
      // Bridge
      paint.strokeWidth = 4f
      paint.style = Paint.Style.STROKE
      canvas.drawLine(rect.centerX() - eyeSpacing + lensWidth, eyeY,
                      rect.centerX() + eyeSpacing - lensWidth, eyeY, paint)
      // Temples
      canvas.drawLine(rect.centerX() - eyeSpacing - lensWidth, eyeY,
                      rect.centerX() - rect.width() * 0.4f, eyeY - 5f, paint)
      canvas.drawLine(rect.centerX() + eyeSpacing + lensWidth, eyeY,
                      rect.centerX() + rect.width() * 0.4f, eyeY - 5f, paint)

      // Lens shine
      paint.color = Color.WHITE
      paint.alpha = 60
      paint.style = Paint.Style.FILL
      canvas.drawOval(
        RectF(rect.centerX() - eyeSpacing - lensWidth * 0.3f, eyeY - lensHeight * 0.6f,
              rect.centerX() - eyeSpacing + lensWidth * 0.3f, eyeY),
        paint
      )
    },

    // Funny mustache
    FaceSticker { canvas, rect ->
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      paint.color = 0xFF4A2800.toInt()
      paint.style = Paint.Style.FILL

      val mustacheY = rect.centerY() + rect.height() * 0.15f
      val mustacheWidth = rect.width() * 0.35f

      val path = android.graphics.Path()
      path.moveTo(rect.centerX(), mustacheY)
      path.cubicTo(
        rect.centerX() - mustacheWidth * 0.3f, mustacheY - 15f,
        rect.centerX() - mustacheWidth, mustacheY - 10f,
        rect.centerX() - mustacheWidth, mustacheY + 5f
      )
      path.cubicTo(
        rect.centerX() - mustacheWidth, mustacheY + 15f,
        rect.centerX() - mustacheWidth * 0.5f, mustacheY + 12f,
        rect.centerX(), mustacheY + 3f
      )
      path.cubicTo(
        rect.centerX() + mustacheWidth * 0.5f, mustacheY + 12f,
        rect.centerX() + mustacheWidth, mustacheY + 15f,
        rect.centerX() + mustacheWidth, mustacheY + 5f
      )
      path.cubicTo(
        rect.centerX() + mustacheWidth, mustacheY - 10f,
        rect.centerX() + mustacheWidth * 0.3f, mustacheY - 15f,
        rect.centerX(), mustacheY
      )
      path.close()
      canvas.drawPath(path, paint)
    },

    // Star eyes
    FaceSticker { canvas, rect ->
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      paint.color = 0xFFFFD700.toInt()
      paint.style = Paint.Style.FILL

      val eyeY = rect.centerY() - rect.height() * 0.05f
      val eyeSpacing = rect.width() * 0.2f
      val starSize = rect.width() * 0.08f

      drawStar(canvas, rect.centerX() - eyeSpacing, eyeY, starSize, paint)
      drawStar(canvas, rect.centerX() + eyeSpacing, eyeY, starSize, paint)
    }
  )

  /**
   * Detect faces and apply a random sticker to each face.
   * Returns the modified bitmap with face stickers.
   * If no faces found, returns the original bitmap.
   */
  fun applyRandomFaceSticker(
    context: Context,
    source: Bitmap,
    onResult: (Bitmap, String?) -> Unit
  ) {
    val image = InputImage.fromBitmap(source, 0)
    val options = FaceDetectorOptions.Builder()
      .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
      .setMinFaceSize(0.1f)
      .build()

    val detector = FaceDetection.getClient(options)

    detector.process(image)
      .addOnSuccessListener { faces ->
        if (faces.isEmpty()) {
          onResult(source, null)
          return@addOnSuccessListener
        }

        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val sticker = faceStickers[Random.nextInt(faceStickers.size)]

        for (face in faces) {
          val bounds = face.boundingBox
          // Expand bounds slightly for better sticker placement
          val expanded = RectF(
            bounds.left.toFloat() - 10f,
            bounds.top.toFloat() - bounds.height() * 0.3f,
            bounds.right.toFloat() + 10f,
            bounds.bottom.toFloat() + 10f
          )
          sticker.draw(canvas, expanded)
        }

        val stickerName = when (faceStickers.indexOf(sticker)) {
          0 -> "Crown"
          1 -> "Sunglasses"
          2 -> "Mustache"
          3 -> "Star Eyes"
          else -> "Face Sticker"
        }
        onResult(result, "Face $stickerName")
      }
      .addOnFailureListener {
        onResult(source, null)
      }
  }

  private fun drawStar(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
    val path = android.graphics.Path()
    val points = 5
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
}
