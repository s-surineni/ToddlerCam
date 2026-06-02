package com.toddlerapps.simplecam

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.random.Random

object StickerEffects {

  private data class StickerInfo(val name: String, val draw: (Canvas, Float, Float) -> Unit)

  private val stickerSets = listOf(
    // 1. Balloons - colorful balloon emoji
    StickerInfo("Balloons") { canvas, w, h ->
      val emojis = arrayOf("🎈", "🎈", "🎈", "🎈", "🎈", "🎈", "🎈", "🎈")
      drawEmojiScatter(canvas, w, h, emojis, 12, 35f, 55f)
    },

    // 2. Birthday Party - cake, candles, party
    StickerInfo("Birthday Party") { canvas, w, h ->
      val emojis = arrayOf("🎂", "🍰", "🧁", "🎉", "🎁", "🎀", "🎈", "🎵")
      drawEmojiScatter(canvas, w, h, emojis, 10, 40f, 65f)
    },

    // 3. Rainbow Magic
    StickerInfo("Rainbow Magic") { canvas, w, h ->
      val emojis = arrayOf("🌈", "⭐", "✨", "💫", "🌟", "🦄", "🦋", "🌸")
      drawEmojiScatter(canvas, w, h, emojis, 14, 30f, 55f)
    },

    // 4. Love Hearts
    StickerInfo("Love Hearts") { canvas, w, h ->
      val emojis = arrayOf("❤️", "💕", "💖", "💗", "💘", "💝", "😍", "🥰")
      drawEmojiScatter(canvas, w, h, emojis, 12, 35f, 55f)
    },

    // 5. Ocean Fun
    StickerInfo("Ocean Fun") { canvas, w, h ->
      val emojis = arrayOf("🐠", "🐟", "🐙", "🐚", "🌊", "🐬", "🦈", "🐡")
      drawEmojiScatter(canvas, w, h, emojis, 10, 40f, 65f)
    },

    // 6. Nature & Animals
    StickerInfo("Nature") { canvas, w, h ->
      val emojis = arrayOf("🦋", "🐝", "🌻", "🌺", "🍀", "🐛", "🐞", "🌿")
      drawEmojiScatter(canvas, w, h, emojis, 12, 35f, 55f)
    },

    // 7. Space Adventure
    StickerInfo("Space") { canvas, w, h ->
      val emojis = arrayOf("🚀", "🌙", "⭐", "🌍", "👽", "🛸", "🪐", "☄️")
      drawEmojiScatter(canvas, w, h, emojis, 10, 38f, 60f)
    },

    // 8. Candy Land
    StickerInfo("Candy Land") { canvas, w, h ->
      val emojis = arrayOf("🍭", "🍬", "🍫", "🍩", "🍪", "🎂", "🧁", "🍰")
      drawEmojiScatter(canvas, w, h, emojis, 12, 35f, 55f)
    },

    // 9. Music & Dance
    StickerInfo("Music") { canvas, w, h ->
      val emojis = arrayOf("🎵", "🎶", "🎤", "🎸", "🥁", "🎺", "🪘", "🪗")
      drawEmojiScatter(canvas, w, h, emojis, 10, 38f, 58f)
    },

    // 10. Playground
    StickerInfo("Playground") { canvas, w, h ->
      val emojis = arrayOf("⚽", "🏀", "🎾", "🎯", "🎪", "🎠", "🎡", "🗿")
      drawEmojiScatter(canvas, w, h, emojis, 10, 40f, 62f)
    },

    // 11. Flowers Garden
    StickerInfo("Flower Garden") { canvas, w, h ->
      val emojis = arrayOf("🌸", "🌺", "🌹", "🌷", "🌻", "💐", "🌼", "🪻")
      drawEmojiScatter(canvas, w, h, emojis, 14, 32f, 50f)
    },

    // 12. Emoji Explosion
    StickerInfo("Emoji Explosion") { canvas, w, h ->
      val emojis = arrayOf("😀", "😂", "🥳", "😎", "🤩", "😺", "🐶", "🐼")
      drawEmojiScatter(canvas, w, h, emojis, 10, 40f, 65f)
    }
  )

  /**
   * Draw emoji stickers scattered across the canvas.
   * Each emoji is rendered as large text using the system emoji font.
   */
  private fun drawEmojiScatter(
    canvas: Canvas, w: Float, h: Float,
    emojis: Array<String>, count: Int,
    minSize: Float, maxSize: Float
  ) {
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    textPaint.textAlign = Paint.Align.CENTER

    for (i in 0 until count) {
      val x = Random.nextFloat() * w * 0.85f + w * 0.075f
      val y = Random.nextFloat() * h * 0.85f + h * 0.075f
      val size = Random.nextFloat() * (maxSize - minSize) + minSize
      val rotation = Random.nextFloat() * 40 - 20 // -20 to +20 degrees

      textPaint.textSize = size

      canvas.save()
      canvas.rotate(rotation, x, y)
      canvas.drawText(emojis[Random.nextInt(emojis.size)], x, y, textPaint)
      canvas.restore()
    }
  }

  fun applyRandomSticker(source: Bitmap): Pair<Bitmap, String> {
    val sticker = stickerSets[Random.nextInt(stickerSets.size)]
    val result = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    sticker.draw(canvas, source.width.toFloat(), source.height.toFloat())
    return Pair(result, sticker.name)
  }
}
