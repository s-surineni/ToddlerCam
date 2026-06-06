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
      val top = rect.top - rect.height() * 0.25f
      val crownWidth = rect.width() * 0.8f
      val crownHeight = rect.height() * 0.4f

      val path = android.graphics.Path()
      path.moveTo(cx - crownWidth / 2, top + crownHeight)
      path.lineTo(cx - crownWidth / 2, top)
      path.lineTo(cx - crownWidth / 4, top + crownHeight * 0.6f)
      path.lineTo(cx, top - crownHeight * 0.2f)
      path.lineTo(cx + crownWidth / 4, top + crownHeight * 0.6f)
      path.lineTo(cx + crownWidth / 2, top)
      path.lineTo(cx + crownWidth / 2, top + crownHeight)
      path.close()
      canvas.drawPath(path, paint)

      // Jewels
      paint.color = Color.RED
      canvas.drawCircle(cx, top + crownHeight * 0.5f, 8f, paint)
      paint.color = 0xFF0066FF.toInt()
      canvas.drawCircle(cx - crownWidth / 4, top + crownHeight * 0.6f, 6f, paint)
      canvas.drawCircle(cx + crownWidth / 4, top + crownHeight * 0.6f, 6f, paint)
    },

    // Sunglasses
    FaceSticker { canvas, rect ->
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      paint.color = Color.BLACK
      paint.style = Paint.Style.FILL

      val eyeY = rect.centerY() - rect.height() * 0.05f
      val eyeSpacing = rect.width() * 0.25f
      val lensWidth = rect.width() * 0.25f
      val lensHeight = rect.height() * 0.18f

      // Left lens
      canvas.drawRoundRect(
        RectF(rect.centerX() - eyeSpacing - lensWidth, eyeY - lensHeight,
              rect.centerX() - eyeSpacing + lensWidth, eyeY + lensHeight),
        12f, 12f, paint
      )
      // Right lens
      canvas.drawRoundRect(
        RectF(rect.centerX() + eyeSpacing - lensWidth, eyeY - lensHeight,
              rect.centerX() + eyeSpacing + lensWidth, eyeY + lensHeight),
        12f, 12f, paint
      )
      // Bridge
      paint.strokeWidth = 6f
      paint.style = Paint.Style.STROKE
      canvas.drawLine(rect.centerX() - eyeSpacing + lensWidth, eyeY,
                      rect.centerX() + eyeSpacing - lensWidth, eyeY, paint)
      // Temples
      canvas.drawLine(rect.centerX() - eyeSpacing - lensWidth, eyeY,
                      rect.centerX() - rect.width() * 0.5f, eyeY - 8f, paint)
      canvas.drawLine(rect.centerX() + eyeSpacing + lensWidth, eyeY,
                      rect.centerX() + rect.width() * 0.5f, eyeY - 8f, paint)

      // Lens shine
      paint.color = Color.WHITE
      paint.alpha = 80
      paint.style = Paint.Style.FILL
      canvas.drawOval(
        RectF(rect.centerX() - eyeSpacing - lensWidth * 0.4f, eyeY - lensHeight * 0.7f,
              rect.centerX() - eyeSpacing + lensWidth * 0.4f, eyeY),
        paint
      )
    },

    // Funny mustache
    FaceSticker { canvas, rect ->
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      paint.color = 0xFF4A2800.toInt()
      paint.style = Paint.Style.FILL

      val mustacheY = rect.centerY() + rect.height() * 0.2f
      val mustacheWidth = rect.width() * 0.45f

      val path = android.graphics.Path()
      path.moveTo(rect.centerX(), mustacheY)
      path.cubicTo(
        rect.centerX() - mustacheWidth * 0.3f, mustacheY - 20f,
        rect.centerX() - mustacheWidth, mustacheY - 15f,
        rect.centerX() - mustacheWidth, mustacheY + 8f
      )
      path.cubicTo(
        rect.centerX() - mustacheWidth, mustacheY + 25f,
        rect.centerX() - mustacheWidth * 0.6f, mustacheY + 20f,
        rect.centerX(), mustacheY + 5f
      )
      path.cubicTo(
        rect.centerX() + mustacheWidth * 0.6f, mustacheY + 20f,
        rect.centerX() + mustacheWidth, mustacheY + 25f,
        rect.centerX() + mustacheWidth, mustacheY + 8f
      )
      path.cubicTo(
        rect.centerX() + mustacheWidth, mustacheY - 15f,
        rect.centerX() + mustacheWidth * 0.3f, mustacheY - 20f,
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
      val eyeSpacing = rect.width() * 0.25f
      val starSize = rect.width() * 0.12f

      drawStar(canvas, rect.centerX() - eyeSpacing, eyeY, starSize, paint)
      drawStar(canvas, rect.centerX() + eyeSpacing, eyeY, starSize, paint)
    },

    // Pig nose
    FaceSticker { canvas, rect ->
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      paint.color = 0xFFFAC8C8.toInt()
      paint.style = Paint.Style.FILL

      val noseCenterX = rect.centerX()
      val noseCenterY = rect.centerY() + rect.height() * 0.1f
      val noseRadius = rect.width() * 0.15f

      canvas.drawCircle(noseCenterX, noseCenterY, noseRadius, paint)

      // Nostrils
      paint.color = 0xFF663300.toInt()
      canvas.drawCircle(noseCenterX - noseRadius * 0.5f, noseCenterY, noseRadius * 0.4f, paint)
      canvas.drawCircle(noseCenterX + noseRadius * 0.5f, noseCenterY, noseRadius * 0.4f, paint)
    },

    // Butterfly wings
    FaceSticker { canvas, rect ->
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      val wingColor = 0xFFFF69B4.toInt()
      paint.color = wingColor
      paint.style = Paint.Style.FILL

      val cx = rect.centerX()
      val wingY = rect.centerY()
      val wingWidth = rect.width() * 0.6f
      val wingHeight = rect.height() * 0.5f

      // Left wing
      val leftPath = android.graphics.Path()
      leftPath.moveTo(cx, wingY)
      leftPath.quadTo(cx - wingWidth / 2, wingY - wingHeight, cx - wingWidth, wingY)
      leftPath.quadTo(cx - wingWidth / 2, wingY + wingHeight, cx, wingY)
      leftPath.close()
      canvas.drawPath(leftPath, paint)

      // Right wing
      val rightPath = android.graphics.Path()
      rightPath.moveTo(cx, wingY)
      rightPath.quadTo(cx + wingWidth / 2, wingY - wingHeight, cx + wingWidth, wingY)
      rightPath.quadTo(cx + wingWidth / 2, wingY + wingHeight, cx, wingY)
      rightPath.close()
      canvas.drawPath(rightPath, paint)

      // Wing details
      paint.color = Color.WHITE
      paint.strokeWidth = 3f
      paint.style = Paint.Style.STROKE
      canvas.drawLine(cx - wingWidth * 0.3f, wingY, cx - wingWidth * 0.7f, wingY - wingHeight * 0.5f, paint)
      canvas.drawLine(cx - wingWidth * 0.5f, wingY, cx - wingWidth * 0.8f, wingY + wingHeight * 0.5f, paint)
      canvas.drawLine(cx + wingWidth * 0.3f, wingY, cx + wingWidth * 0.7f, wingY - wingHeight * 0.5f, paint)
      canvas.drawLine(cx + wingWidth * 0.5f, wingY, cx + wingWidth * 0.8f, wingY + wingHeight * 0.5f, paint)
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
        detector.close() // Close detector to free resources

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
          4 -> "Pig Nose"
          5 -> "Butterfly Wings"
          else -> "Face Sticker"
        }
        onResult(result, "Face $stickerName")
      }
      .addOnFailureListener {
        detector.close() // Close detector on failure too
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
