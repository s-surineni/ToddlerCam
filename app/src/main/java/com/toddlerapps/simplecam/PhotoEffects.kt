package com.toddlerapps.simplecam

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBulgeDistortionFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorMatrixFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageExposureFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageRGBFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSobelEdgeDetectionFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSwirlFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageToneCurveFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageVignetteFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageWhiteBalanceFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageZoomBlurFilter
import com.toddlerapps.simplecam.FaceStickerHelper
import kotlin.random.Random

object PhotoEffects {

  private data class EffectInfo(val name: String, val apply: (GPUImage) -> Unit)

  // Cache the effects list to avoid recreating it every time
  private val EFFECTS_LIST: List<EffectInfo> = listOf(
    // 1. Retro Sepia
    EffectInfo("Retro Sepia") { gpu ->
      gpu.setFilter(GPUImageColorMatrixFilter().apply {
        setColorMatrix(floatArrayOf(
          0.393f, 0.769f, 0.189f, 0f, 0f,
          0.349f, 0.686f, 0.168f, 0f, 0f,
          0.272f, 0.534f, 0.131f, 0f, 0f,
          0f, 0f, 0f, 1f, 0f
        ))
      })
    },

    // 2. Dreamy
    EffectInfo("Dreamy") { gpu ->
      gpu.setFilter(GPUImageColorMatrixFilter().apply {
        setColorMatrix(floatArrayOf(
          1.2f, 0.1f, 0.0f, 0f, 0.05f,
          0.0f, 1.1f, 0.1f, 0f, 0.03f,
          0.0f, 0.05f, 1.0f, 0f, 0.0f,
          0.0f, 0.0f, 0.0f, 1.0f, 0f
        ))
      })
    },

    // 3. Frozen
    EffectInfo("Frozen") { gpu ->
      gpu.setFilter(GPUImageColorMatrixFilter().apply {
        setColorMatrix(floatArrayOf(
          0.8f, 0.0f, 0.2f, 0f, 0.0f,
          0.0f, 0.9f, 0.3f, 0f, 0.0f,
          0.1f, 0.1f, 1.4f, 0f, 0.05f,
          0.0f, 0.0f, 0.0f, 1.0f, 0f
        ))
      })
    },

    // 4. Neon Pop
    EffectInfo("Neon Pop") { gpu ->
      gpu.setFilter(GPUImageSaturationFilter(2.5f))
    },

    // 5. Vintage Film
    EffectInfo("Vintage Film") { gpu ->
      gpu.setFilter(GPUImageColorMatrixFilter().apply {
        setColorMatrix(floatArrayOf(
          0.6f, 0.3f, 0.1f, 0f, 0.05f,
          0.2f, 0.6f, 0.2f, 0f, 0.03f,
          0.1f, 0.2f, 0.5f, 0f, 0.02f,
          0.0f, 0.0f, 0.0f, 1.0f, 0f
        ))
      })
    },

    // 6. Dramatic
    EffectInfo("Dramatic") { gpu ->
      gpu.setFilter(GPUImageContrastFilter(2.0f))
    },

    // 7. Pastel
    EffectInfo("Pastel") { gpu ->
      gpu.setFilter(GPUImageColorMatrixFilter().apply {
        setColorMatrix(floatArrayOf(
          1.1f, 0.1f, 0.1f, 0f, 0.1f,
          0.1f, 1.1f, 0.1f, 0f, 0.1f,
          0.1f, 0.1f, 1.1f, 0f, 0.1f,
          0.0f, 0.0f, 0.0f, 1.0f, 0f
        ))
      })
    },

    // 8. Bright
    EffectInfo("Bright") { gpu ->
      gpu.setFilter(GPUImageExposureFilter(1.5f))
    },

    // 9. Warm Glow
    EffectInfo("Warm Glow") { gpu ->
      gpu.setFilter(GPUImageWhiteBalanceFilter(3500f, 0.5f))
    },

    // 10. Cool Breeze
    EffectInfo("Cool Breeze") { gpu ->
      gpu.setFilter(GPUImageWhiteBalanceFilter(8000f, 0.3f))
    },

    // 11. Edge Sketch
    EffectInfo("Edge Sketch") { gpu ->
      gpu.setFilter(GPUImageSobelEdgeDetectionFilter())
    },

    // 12. Motion Blur
    EffectInfo("Motion Blur") { gpu ->
      gpu.setFilter(GPUImageZoomBlurFilter(PointF(0.5f, 0.5f), 1.8f))
    },

    // 13. Retro Red
    EffectInfo("Retro Red") { gpu ->
      gpu.setFilter(GPUImageRGBFilter(1.5f, 0.8f, 0.7f))
    },

    // 14. Emerald
    EffectInfo("Emerald") { gpu ->
      gpu.setFilter(GPUImageRGBFilter(0.7f, 1.4f, 0.8f))
    },

    // 15. Deep Blue
    EffectInfo("Deep Blue") { gpu ->
      gpu.setFilter(GPUImageRGBFilter(0.6f, 0.7f, 1.6f))
    },

    // 16. Sunset
    EffectInfo("Sunset") { gpu ->
      gpu.setFilter(GPUImageHueFilter(25f))
    },

    // 17. Noir
    EffectInfo("Noir") { gpu ->
      gpu.setFilter(GPUImageGrayscaleFilter())
    },

    // 18. Spotlight
    EffectInfo("Spotlight") { gpu ->
      gpu.setFilter(GPUImageVignetteFilter(
        PointF(0.5f, 0.5f), floatArrayOf(0f, 0f, 0f), 0.3f, 1.0f
      ))
    },

    // 19. Sharp
    EffectInfo("Sharp") { gpu ->
      gpu.setFilter(GPUImageSharpenFilter(1.5f))
    },

    // 20. High Key
    EffectInfo("High Key") { gpu ->
      gpu.setFilter(GPUImageExposureFilter(1.2f))
    },

    // 21. Bulge - fish-eye distortion on center
    EffectInfo("Bulge") { gpu ->
      gpu.setFilter(GPUImageBulgeDistortionFilter(0.7f, 1.2f, PointF(0.5f, 0.5f)))
    },

    // 22. Swirl - spiral distortion
    EffectInfo("Swirl") { gpu ->
      gpu.setFilter(GPUImageSwirlFilter(1.0f, 0.3f, PointF(0.5f, 0.5f)))
    },

    // 23. S-Curve - cinematic tone curve
    EffectInfo("Cinematic") { gpu ->
      val curve = GPUImageToneCurveFilter()
      curve.setBlueControlPoints(arrayOf(
        android.graphics.PointF(0f, 0f),
        android.graphics.PointF(0.3f, 0.1f),
        android.graphics.PointF(0.7f, 0.9f),
        android.graphics.PointF(1f, 1f)
      ))
      curve.setGreenControlPoints(arrayOf(
        android.graphics.PointF(0f, 0f),
        android.graphics.PointF(0.5f, 0.45f),
        android.graphics.PointF(1f, 1f)
      ))
      gpu.setFilter(curve)
    },

    // 24. Lomo - cross-processed look
    EffectInfo("Lomo") { gpu ->
      gpu.setFilter(GPUImageColorMatrixFilter().apply {
        setColorMatrix(floatArrayOf(
          1.4f, 0.0f, 0.0f, 0f, -0.05f,
          0.0f, 1.2f, 0.0f, 0f, 0.0f,
          0.0f, 0.0f, 0.9f, 0f, 0.1f,
          0.0f, 0.0f, 0.0f, 1.0f, 0f
        ))
      })
    },

    // 25. Dramatic B&W - high contrast grayscale
    EffectInfo("Dramatic B&W") { gpu ->
      gpu.setFilter(GPUImageColorMatrixFilter().apply {
        setColorMatrix(floatArrayOf(
          0.3f, 0.3f, 0.3f, 0f, 0f,
          0.3f, 0.3f, 0.3f, 0f, 0f,
          0.3f, 0.3f, 0.3f, 0f, 0f,
          0.0f, 0.0f, 0.0f, 1.5f, -0.1f
        ))
      })
    },

    // 26. Warm Cross - cross-processed warm
    EffectInfo("Warm Cross") { gpu ->
      gpu.setFilter(GPUImageColorMatrixFilter().apply {
        setColorMatrix(floatArrayOf(
          1.3f, 0.1f, -0.1f, 0f, 0.05f,
          -0.1f, 1.2f, 0.1f, 0f, 0.02f,
          0.0f, -0.1f, 1.0f, 0f, 0.0f,
          0.0f, 0.0f, 0.0f, 1.0f, 0f
        ))
      })
    },

    // 27. Cool Cross - cross-processed cool
    EffectInfo("Cool Cross") { gpu ->
      gpu.setFilter(GPUImageColorMatrixFilter().apply {
        setColorMatrix(floatArrayOf(
          0.9f, 0.0f, 0.2f, 0f, 0.0f,
          0.0f, 1.0f, 0.2f, 0f, 0.0f,
          0.1f, 0.0f, 1.3f, 0f, 0.05f,
          0.0f, 0.0f, 0.0f, 1.2f, 0f
        ))
      })
    },

    // 28. Low Key - dark and moody
    EffectInfo("Low Key") { gpu ->
      gpu.setFilter(GPUImageExposureFilter(-0.5f))
    },

    // 29. Faded - washed out vintage
    EffectInfo("Faded") { gpu ->
      gpu.setFilter(GPUImageColorMatrixFilter().apply {
        setColorMatrix(floatArrayOf(
          0.9f, 0.1f, 0.1f, 0f, 0.12f,
          0.1f, 0.9f, 0.1f, 0f, 0.12f,
          0.1f, 0.1f, 0.9f, 0f, 0.12f,
          0.0f, 0.0f, 0.0f, 1.0f, 0f
        ))
      })
    },

    // 30. Vibrant - extremely saturated and vivid
    EffectInfo("Vibrant") { gpu ->
      gpu.setFilter(GPUImageSaturationFilter(3.0f))
    }
  )

  /**
   * Apply a random effect. First tries face detection (30% chance).
   * If face detected, applies face sticker + optional filter.
   * Otherwise falls back to filter/sticker/combined effects.
   */
  fun applyRandomEffect(
    context: Context,
    source: Bitmap,
    onResult: (Bitmap, String) -> Unit
  ) {
    // Add null safety checks
    if (context == null || source == null) {
      onResult(source, "Error: Invalid input")
      return
    }

    val roll = Random.nextFloat()

    // 30% chance: try face detection first
    if (roll < 0.3f) {
      FaceStickerHelper.applyRandomFaceSticker(context, source) { faceBitmap, faceName ->
        if (faceName != null) {
          // Face detected! Optionally add a filter on top
          if (Random.nextFloat() < 0.5f) {
            val effect = EFFECTS_LIST[Random.nextInt(EFFECTS_LIST.size)]
            val gpuImage = GPUImage(context)
            gpuImage.setImage(faceBitmap)
            effect.apply(gpuImage)
            onResult(gpuImage.getBitmapWithFilterApplied(), "$faceName + ${effect.name}")
          } else {
            onResult(faceBitmap, faceName)
          }
        } else {
          // No faces found, fall back to regular effect
          applyRegularEffect(context, source, onResult)
        }
      }
    } else {
      applyRegularEffect(context, source, onResult)
    }
  }

  private fun applyRegularEffect(
    context: Context,
    source: Bitmap,
    onResult: (Bitmap, String) -> Unit
  ) {
    // Add null safety checks
    if (context == null || source == null) {
      onResult(source, "Error: Invalid input")
      return
    }

    val roll = Random.nextFloat()

    // Always apply emoji stickers; optionally add a filter on top
    if (roll < 0.4f) {
      // Filter + stickers
      val effect = EFFECTS_LIST[Random.nextInt(EFFECTS_LIST.size)]
      val gpuImage = GPUImage(context)
      gpuImage.setImage(source)
      effect.apply(gpuImage)
      val filtered = gpuImage.getBitmapWithFilterApplied()
      val (stickerBitmap, stickerName) = StickerEffects.applyRandomSticker(filtered)
      onResult(stickerBitmap, "${effect.name} + $stickerName")
    } else {
      // Stickers only
      val (bitmap, name) = StickerEffects.applyRandomSticker(source)
      onResult(bitmap, name)
    }
  }
}
