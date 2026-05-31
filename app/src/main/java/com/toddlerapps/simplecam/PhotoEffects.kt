package com.toddlerapps.simplecam

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageColorMatrixFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageExposureFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageRGBFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSobelEdgeDetectionFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageVignetteFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageWhiteBalanceFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageZoomBlurFilter
import kotlin.random.Random

object PhotoEffects {

  private data class EffectInfo(val name: String, val apply: (GPUImage) -> Unit)

  private fun createEffects(): List<EffectInfo> = listOf(
    // 1. Retro Sepia - warm vintage tone
    EffectInfo("Retro Sepia") { gpu ->
      gpu.setFilter(GPUImageColorMatrixFilter().apply {
        // Standard sepia matrix
        setColorMatrix(floatArrayOf(
          0.393f, 0.769f, 0.189f, 0f, 0f,
          0.349f, 0.686f, 0.168f, 0f, 0f,
          0.272f, 0.534f, 0.131f, 0f, 0f,
          0f, 0f, 0f, 1f, 0f
        ))
      })
    },

    // 2. Dreamy - soft warm glow
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

    // 3. Frozen - cool blue tint
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

    // 4. Neon Pop - super saturated
    EffectInfo("Neon Pop") { gpu ->
      gpu.setFilter(GPUImageSaturationFilter(2.2f))
    },

    // 5. Vintage Film - light sepia
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

    // 6. Dramatic - high contrast
    EffectInfo("Dramatic") { gpu ->
      gpu.setFilter(GPUImageContrastFilter(1.8f))
    },

    // 7. Pastel - soft colors
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

    // 8. Bright - boosted exposure
    EffectInfo("Bright") { gpu ->
      gpu.setFilter(GPUImageExposureFilter(1.2f))
    },

    // 9. Warm Glow - warm white balance
    EffectInfo("Warm Glow") { gpu ->
      gpu.setFilter(GPUImageWhiteBalanceFilter(3500f, 0.5f))
    },

    // 10. Cool Breeze - cool white balance
    EffectInfo("Cool Breeze") { gpu ->
      gpu.setFilter(GPUImageWhiteBalanceFilter(8000f, 0.2f))
    },

    // 11. Edge Sketch - neon outline effect
    EffectInfo("Edge Sketch") { gpu ->
      gpu.setFilter(GPUImageSobelEdgeDetectionFilter())
    },

    // 12. Motion Blur - zoom blur
    EffectInfo("Motion Blur") { gpu ->
      gpu.setFilter(GPUImageZoomBlurFilter(PointF(0.5f, 0.5f), 1.5f))
    },

    // 13. Retro Red - warm red tone
    EffectInfo("Retro Red") { gpu ->
      gpu.setFilter(GPUImageRGBFilter(1.5f, 0.8f, 0.7f))
    },

    // 14. Emerald - green tone
    EffectInfo("Emerald") { gpu ->
      gpu.setFilter(GPUImageRGBFilter(0.7f, 1.4f, 0.8f))
    },

    // 15. Deep Blue - cool blue tone
    EffectInfo("Deep Blue") { gpu ->
      gpu.setFilter(GPUImageRGBFilter(0.6f, 0.7f, 1.6f))
    },

    // 16. Sunset - warm hue shift
    EffectInfo("Sunset") { gpu ->
      gpu.setFilter(GPUImageHueFilter(20f))
    },

    // 17. Noir - black and white
    EffectInfo("Noir") { gpu ->
      gpu.setFilter(GPUImageGrayscaleFilter())
    },

    // 18. Spotlight - vignette effect
    EffectInfo("Spotlight") { gpu ->
      gpu.setFilter(GPUImageVignetteFilter(
        PointF(0.5f, 0.5f),
        floatArrayOf(0f, 0f, 0f),
        0.4f,
        1.0f
      ))
    },

    // 19. Sharp - sharpened details
    EffectInfo("Sharp") { gpu ->
      gpu.setFilter(GPUImageSharpenFilter(1.0f))
    },

    // 20. High Key - bright + low contrast
    EffectInfo("High Key") { gpu ->
      gpu.setFilter(GPUImageBrightnessFilter(0.25f))
    }
  )

  fun applyRandomEffect(context: Context, source: Bitmap): Pair<Bitmap, String> {
    val effects = createEffects()
    val effect = effects[Random.nextInt(effects.size)]
    val gpuImage = GPUImage(context)
    gpuImage.setImage(source)
    effect.apply(gpuImage)
    val result = gpuImage.getBitmapWithFilterApplied()
    return Pair(result, effect.name)
  }
}
