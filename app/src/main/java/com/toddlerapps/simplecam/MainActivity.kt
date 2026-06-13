/*
 * Copyright (c) 2024 Kodeco Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 * 
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.toddlerapps.simplecam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.view.WindowManager
import android.content.Intent
import android.app.ActivityManager
import androidx.camera.core.AspectRatio
import androidx.activity.OnBackPressedCallback
import android.content.ContentValues
import android.provider.MediaStore
import android.media.MediaActionSound
import android.widget.ImageView
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.media.ExifInterface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Main Screen - Camera app with circle gesture to exit
 */
class MainActivity : AppCompatActivity(), SensorEventListener {

  // Sensor properties for device tilt
  private lateinit var sensorManager: SensorManager
  private var gameRotation: Sensor? = null
  // Orientation angles from game rotation vector (radians)
  private var tiltRoll = 0f // positive = right side down → ball rolls right
  private var tiltPitch = 0f // positive = top tilting away/forward → ball rolls down

  // Sticker data model (static position, no physics)
  class Sticker(
    val emoji: String,
    var x: Float, // Normalized (0.0 to 1.0)
    var y: Float, // Normalized (0.0 to 1.0)
    val sizePercent: Float = 0.08f
  ) {
    fun update() {}
  }

  private val currentStickers = mutableListOf<Sticker>()

  enum class Mode { NONE, SEASONS, ANIMALS }
  enum class Season { NONE, SPRING, SUMMER, AUTUMN, WINTER }
  enum class Animal { NONE, CAT, DOG, BIRD, COW, DUCK }

  private var currentMode = Mode.NONE
  private var currentSeason = Season.NONE
  private var currentAnimal = Animal.NONE

  private lateinit var themeOverlayView: ThemeOverlayView
  private lateinit var tvCameraBanner: TextView
  private lateinit var btnChangeMode: TextView
  private lateinit var layoutSelectionScreen: View
  private lateinit var containerModeSelection: View
  private lateinit var containerSubSelection: View
  private lateinit var tvSubSelectionTitle: TextView
  private lateinit var btnBackToModes: View
  private lateinit var layoutOptionRow3: View

  private lateinit var btnOption1: TextView
  private lateinit var btnOption2: TextView
  private lateinit var btnOption3: TextView
  private lateinit var btnOption4: TextView
  private lateinit var btnOption5: TextView
  private lateinit var btnExitAppMain: View
  private lateinit var btnExitAppSub: View
  private lateinit var btnInstructions: View
  private lateinit var btnSettings: View
  private var savePhotosToGallery = true

  private lateinit var previewView: PreviewView
  private lateinit var circleDetectionView: CircleDetectionView
  private lateinit var photoPreviewView: ImageView
  private lateinit var btnSwitchCamera: View
  private lateinit var cameraExecutor: ExecutorService
  private val cameraPermissionRequestCode = 100
  private val storagePermissionRequestCode = 101
  private var imageCapture: ImageCapture? = null
  private var cameraProvider: ProcessCameraProvider? = null
  private var preview: Preview? = null
  private var useFrontCamera = false
  private var showStickers = true
  private val mediaActionSound = MediaActionSound()

  // Play time limit fields
  private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
  private var playTimeLimitMinutes = 30
  private var playTimeLimitRunnable: Runnable? = null
  private var isTimeUp = false
  private lateinit var layoutTimeUp: View

  private val hidePreviewRunnable = Runnable {
    photoPreviewView.visibility = View.GONE
    photoPreviewView.setImageURI(null)
    circleDetectionView.isEnabled = true
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    // Switch to AppTheme for displaying the activity
    setTheme(R.style.AppTheme)

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Enable immersive sticky mode to hide navigation and prevent gestures
    hideSystemUI()

    previewView = findViewById(R.id.preview_view)
    circleDetectionView = findViewById(R.id.circle_detection_view)
    photoPreviewView = findViewById(R.id.photo_preview_view)
    btnSwitchCamera = findViewById(R.id.btn_switch_camera)
    layoutTimeUp = findViewById(R.id.layout_time_up)
    cameraExecutor = Executors.newSingleThreadExecutor()

    // Initialize accelerometer sensor
    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    gameRotation = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    // Bind selection views
    tvCameraBanner = findViewById(R.id.tv_camera_banner)
    btnChangeMode = findViewById(R.id.btn_change_mode)
    layoutSelectionScreen = findViewById(R.id.layout_selection_screen)
    containerModeSelection = findViewById(R.id.container_mode_selection)
    containerSubSelection = findViewById(R.id.container_sub_selection)
    tvSubSelectionTitle = findViewById(R.id.tv_sub_selection_title)
    btnBackToModes = findViewById(R.id.btn_back_to_modes)
    layoutOptionRow3 = findViewById(R.id.layout_option_row_3)

    btnOption1 = findViewById(R.id.btn_option_1)
    btnOption2 = findViewById(R.id.btn_option_2)
    btnOption3 = findViewById(R.id.btn_option_3)
    btnOption4 = findViewById(R.id.btn_option_4)
    btnOption5 = findViewById(R.id.btn_option_5)
    btnExitAppMain = findViewById(R.id.btn_exit_app_main)
    btnExitAppSub = findViewById(R.id.btn_exit_app_sub)
    btnInstructions = findViewById(R.id.btn_instructions)
    btnSettings = findViewById(R.id.btn_settings)

    // Load saved preferences
    val prefs = getSharedPreferences("GiggleCamPrefs", Context.MODE_PRIVATE)
    savePhotosToGallery = prefs.getBoolean("save_photos_to_gallery", true)
    playTimeLimitMinutes = prefs.getInt("play_time_limit_minutes", 30)
    useFrontCamera = prefs.getBoolean("use_front_camera", false)
    showStickers = prefs.getBoolean("show_stickers", true)

    // Clean up temporary cache photos in background
    Executors.newSingleThreadExecutor().execute {
      try {
        cacheDir.listFiles()?.forEach { file ->
          if (file.name.startsWith("GiggleCam_")) {
            file.delete()
          }
        }
      } catch (e: Exception) {
        // Ignore
      }
    }

    // Instantiate and add the custom overlay drawing view
    themeOverlayView = ThemeOverlayView(this)
    findViewById<FrameLayout>(R.id.layout_decorations_overlay).addView(themeOverlayView)

    // Set up menu clicks
    setupMenuClicks()

    // Switch between front and back camera
    btnSwitchCamera.setOnClickListener {
      useFrontCamera = !useFrontCamera
      bindCameraUseCases()
    }

    // Show selection screen by default
    generateMosaicBackground()
    showSelectionScreen()

    // Keep screen on
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // Register back pressed callback to prevent back button from closing the app
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        // Do nothing - back button / gesture is disabled
      }
    })

    // Set up circle detection callback to return to selection screen
    circleDetectionView.setCircleDetectionCallback {
      runOnUiThread {
        showSelectionScreen()
      }
    }

    // Set up tap callback to take a photo (blocked when time's up)
    circleDetectionView.setTapCallback {
      if (!isTimeUp) {
        takePhoto()
      }
    }

    // Check and request camera permission
    if (ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
      ) == PackageManager.PERMISSION_GRANTED
    ) {
      startCamera()
    } else {
      ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.CAMERA),
        cameraPermissionRequestCode
      )
    }
  }

  private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

    cameraProviderFuture.addListener({
      cameraProvider = cameraProviderFuture.get()

      // Calculate standard aspect ratio based on device screen size
      val metrics = resources.displayMetrics
      val screenAspectRatio = if (metrics.widthPixels > metrics.heightPixels) {
        metrics.widthPixels.toDouble() / metrics.heightPixels
      } else {
        metrics.heightPixels.toDouble() / metrics.widthPixels
      }

      val ratio = if (java.lang.Math.abs(screenAspectRatio - 4.0 / 3.0) < java.lang.Math.abs(screenAspectRatio - 16.0 / 9.0)) {
        AspectRatio.RATIO_4_3
      } else {
        AspectRatio.RATIO_16_9
      }

      // Set up the preview with dynamic aspect ratio
      preview = Preview.Builder()
        .setTargetAspectRatio(ratio)
        .build().also {
          it.setSurfaceProvider(previewView.surfaceProvider)
        }

      // Set up image capture with dynamic aspect ratio
      imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setTargetAspectRatio(ratio)
        .build()

      bindCameraUseCases()
    }, ContextCompat.getMainExecutor(this))
  }

  private fun bindCameraUseCases() {
    val provider = cameraProvider ?: return
    val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

    try {
      provider.unbindAll()
      provider.bindToLifecycle(this, selector, preview, imageCapture)
    } catch (exc: Exception) {
      android.util.Log.e("GiggleCam", "Camera use case binding failed", exc)
      runOnUiThread {
        android.widget.Toast.makeText(this@MainActivity, "Failed to switch camera.", android.widget.Toast.LENGTH_LONG).show()
      }
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    if (requestCode == cameraPermissionRequestCode) {
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        startCamera()
      } else {
        finish() // Exit if camera permission denied
      }
    } else if (requestCode == storagePermissionRequestCode) {
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        savePhotosToGallery = true
        val prefs = getSharedPreferences("GiggleCamPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("save_photos_to_gallery", true).apply()
        android.widget.Toast.makeText(this, "Storage permission granted! Photos will be saved.", android.widget.Toast.LENGTH_SHORT).show()
      } else {
        savePhotosToGallery = false
        val prefs = getSharedPreferences("GiggleCamPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("save_photos_to_gallery", false).apply()
        android.widget.Toast.makeText(
          this,
          "Storage permission denied. Photos will only show as preview (Cache Mode).",
          android.widget.Toast.LENGTH_LONG
        ).show()
      }
    }
  }

  override fun onResume() {
    super.onResume()
    // Start lock task mode (screen pinning) to disable home/recents buttons and hide taskbar
    try {
      startLockTask()
    } catch (e: Exception) {
      // Ignore if lock task mode fails or is not supported
    }
    checkLockTaskMode()

    // Register game rotation sensor for tilt sensing
    gameRotation?.let {
      sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
    }
  }

  override fun onPause() {
    super.onPause()
    sensorManager.unregisterListener(this)
    // Cancel play time timer when app goes to background
    playTimeLimitRunnable?.let { mainHandler.removeCallbacks(it) }
    playTimeLimitRunnable = null
  }

  override fun onDestroy() {
    super.onDestroy()
    try {
      photoPreviewView.removeCallbacks(hidePreviewRunnable)
    } catch (e: Exception) {
      // Ignore
    }
    try {
      stopLockTask()
    } catch (e: Exception) {
      // Ignore
    }
    try {
      mediaActionSound.release()
    } catch (e: Exception) {
      // Ignore
    }
    cameraExecutor.shutdown()
  }

  // Disable back button to prevent kids from exiting
  override fun onBackPressed() {
    // Do nothing - back button is disabled
    // Only circle gesture will exit the app
  }

  // Disable system gesture navigation and home button
  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    return when (keyCode) {
      KeyEvent.KEYCODE_BACK -> true // Consume back button
      KeyEvent.KEYCODE_HOME -> true // Consume home button
      else -> super.onKeyDown(keyCode, event)
    }
  }

  private fun hideSystemUI() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      window.setDecorFitsSystemWindows(false)
      window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
      window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
      @Suppress("DEPRECATION")
      window.decorView.systemUiVisibility = (
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
      )
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      hideSystemUI() // Re-apply when window regains focus
    }
  }

  private fun playClickSound() {
    try {
      mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
    } catch (e: Exception) {
      // Ignore audio playback exceptions
    }
  }

  private fun takePhoto() {
    val imageCapture = this.imageCapture ?: return

    // Play click sound immediately for instant feedback
    playClickSound()

    // Create time-stamped name with extension
    val name = "GiggleCam_" + SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
      .format(System.currentTimeMillis()) + ".jpg"

    // Create output options object based on preference and SDK version
    val outputOptions = if (!savePhotosToGallery) {
      val file = java.io.File(cacheDir, name)
      ImageCapture.OutputFileOptions.Builder(file).build()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DCIM + "/Camera")
      }
      ImageCapture.OutputFileOptions
        .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        .build()
    } else {
      val dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
      val file = java.io.File(dir, name)
      ImageCapture.OutputFileOptions.Builder(file).build()
    }

    // Set up image capture listener, which is triggered after photo has been taken
    imageCapture.takePicture(
      outputOptions,
      ContextCompat.getMainExecutor(this),
      object : ImageCapture.OnImageSavedCallback {
        override fun onError(exc: ImageCaptureException) {
          android.util.Log.e("GiggleCam", "Photo capture failed: ${exc.message}", exc)
          android.widget.Toast.makeText(this@MainActivity, "Failed to save: ${exc.message}", android.widget.Toast.LENGTH_LONG).show()
        }

        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
          val savedUri = output.savedUri
          android.util.Log.d("GiggleCam", "Photo capture succeeded: $savedUri")
          
          val uriToLoad = savedUri ?: Uri.fromFile(
            if (!savePhotosToGallery) {
              java.io.File(cacheDir, name)
            } else {
              java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), name)
            }
          )

          uriToLoad?.let { uri ->
            // Add decorations before showing preview and notifying gallery
            addDecorationsToSavedPhoto(uri)

            if (savePhotosToGallery) {
              try {
                contentResolver.notifyChange(uri, null)
              } catch (e: Exception) {
                // Ignore
              }
            }

            runOnUiThread {
              photoPreviewView.setImageURI(uri)
              photoPreviewView.visibility = View.VISIBLE
              circleDetectionView.isEnabled = false

              // Remove any pending callbacks and schedule hiding in 3 seconds
              photoPreviewView.removeCallbacks(hidePreviewRunnable)
              photoPreviewView.postDelayed(hidePreviewRunnable, 3000)
            }
          }

          if (savePhotosToGallery) {
            android.widget.Toast.makeText(this@MainActivity, "Photo saved to Gallery!", android.widget.Toast.LENGTH_SHORT).show()
          } else {
            android.widget.Toast.makeText(this@MainActivity, "Cute picture captured!", android.widget.Toast.LENGTH_SHORT).show()
          }

          // If API < Q and saving to gallery, notify the media scanner so the photo appears in gallery
          if (savePhotosToGallery && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            val file = java.io.File(dir, name)
            android.media.MediaScannerConnection.scanFile(
              this@MainActivity,
              arrayOf(file.absolutePath),
              null,
              null
            )
          }
        }
      }
    )
  }

  private fun setupMenuClicks() {
    findViewById<View>(R.id.btn_mode_seasons).setOnClickListener {
      selectMode(Mode.SEASONS)
    }
    findViewById<View>(R.id.btn_mode_animals).setOnClickListener {
      selectMode(Mode.ANIMALS)
    }
    btnBackToModes.setOnClickListener {
      goBackToModes()
    }
    btnChangeMode.setOnClickListener {
      showSelectionScreen()
    }

    val exitClickListener = View.OnClickListener {
      try {
        stopLockTask()
      } catch (e: Exception) {
        // Ignore
      }
      finish()
    }
    btnExitAppMain.setOnClickListener(exitClickListener)
    btnExitAppSub.setOnClickListener(exitClickListener)

    btnInstructions.setOnClickListener {
      runParentGate {
        showInstructionsDialog()
      }
    }
    btnSettings.setOnClickListener {
      runParentGate {
        showSettingsDialog()
      }
    }

    btnOption1.setOnClickListener { handleOptionClick(1) }
    btnOption2.setOnClickListener { handleOptionClick(2) }
    btnOption3.setOnClickListener { handleOptionClick(3) }
    btnOption4.setOnClickListener { handleOptionClick(4) }
    btnOption5.setOnClickListener { handleOptionClick(5) }
  }

  private fun selectMode(mode: Mode) {
    currentMode = mode
    containerModeSelection.visibility = View.GONE
    containerSubSelection.visibility = View.VISIBLE

    if (mode == Mode.SEASONS) {
      tvSubSelectionTitle.text = "Choose a Season:"
      btnOption1.text = "🌸 Spring"
      btnOption2.text = "☀️ Summer"
      btnOption3.text = "🍂 Autumn"
      btnOption4.text = "❄️ Winter"
      layoutOptionRow3.visibility = View.GONE
    } else if (mode == Mode.ANIMALS) {
      tvSubSelectionTitle.text = "Choose an Animal:"
      btnOption1.text = "🐱 Cat"
      btnOption2.text = "🐶 Dog"
      btnOption3.text = "🐦 Bird"
      btnOption4.text = "🐮 Cow"
      btnOption5.text = "🦆 Duck"
      layoutOptionRow3.visibility = View.VISIBLE
    }
  }

  private fun goBackToModes() {
    currentMode = Mode.NONE
    containerSubSelection.visibility = View.GONE
    containerModeSelection.visibility = View.VISIBLE
  }

  private fun handleOptionClick(index: Int) {
    if (currentMode == Mode.SEASONS) {
      currentSeason = when (index) {
        1 -> Season.SPRING
        2 -> Season.SUMMER
        3 -> Season.AUTUMN
        4 -> Season.WINTER
        else -> Season.NONE
      }
      if (::themeOverlayView.isInitialized) {
        themeOverlayView.reinitializeParticles(themeOverlayView.width.toFloat(), themeOverlayView.height.toFloat())
      }
    } else if (currentMode == Mode.ANIMALS) {
      currentAnimal = when (index) {
        1 -> Animal.CAT
        2 -> Animal.DOG
        3 -> Animal.BIRD
        4 -> Animal.COW
        5 -> Animal.DUCK
        else -> Animal.NONE
      }
    }

    startPlaying()
  }

  private fun startPlaying() {
    layoutSelectionScreen.visibility = View.GONE
    tvCameraBanner.text = getBannerText()
    tvCameraBanner.visibility = View.VISIBLE
    btnChangeMode.visibility = View.VISIBLE
    btnSwitchCamera.visibility = View.VISIBLE
    
    // Enable circle exit drawing
    circleDetectionView.isEnabled = true
    
    // Initialize drifting stickers with physics
    initializeStickers()
    
    // Reset tilt-sensitive ball position
    themeOverlayView.resetBall()
    
    // Schedule play time limit
    isTimeUp = false
    layoutTimeUp.visibility = View.GONE
    playTimeLimitRunnable?.let { mainHandler.removeCallbacks(it) }
    val limitMs = playTimeLimitMinutes * 60 * 1000L
    playTimeLimitRunnable = Runnable {
      showTimeUpScreen()
    }
    mainHandler.postDelayed(playTimeLimitRunnable!!, limitMs)
    
    // Force overlay to redraw
    themeOverlayView.invalidate()
  }

  private fun showTimeUpScreen() {
    isTimeUp = true
    layoutTimeUp.visibility = View.VISIBLE
    layoutTimeUp.bringToFront()
    // Allow circle gesture to pass through and exit camera
    layoutTimeUp.isClickable = false
    layoutTimeUp.isFocusable = false
    circleDetectionView.isEnabled = true
    // Stop sticker physics
    currentMode = Mode.NONE
    currentSeason = Season.NONE
    currentAnimal = Animal.NONE
    themeOverlayView.invalidate()
  }

  private fun generateMosaicBackground() {
    try {
      val display = windowManager.defaultDisplay
      val size = android.graphics.Point()
      display.getSize(size)
      val cx = size.x / 2f
      val cy = size.y / 2f
      val radius = maxOf(size.x, size.y) * 0.7f
      val bmp = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bmp)
      val shader = android.graphics.RadialGradient(
        cx, cy, radius,
        0xFFFBD9F8.toInt(), 0xFFFFFFFF.toInt(),
        android.graphics.Shader.TileMode.CLAMP
      )
      val paint = Paint().apply { this.shader = shader }
      canvas.drawRect(0f, 0f, size.x.toFloat(), size.y.toFloat(), paint)
      layoutSelectionScreen.background = BitmapDrawable(resources, bmp)
    } catch (_: Exception) {
      // fallback: just keep the default background
    }
  }

  private fun showSelectionScreen() {
    // Cancel play time timer when exiting camera
    playTimeLimitRunnable?.let { mainHandler.removeCallbacks(it) }
    playTimeLimitRunnable = null
    isTimeUp = false
    layoutTimeUp.visibility = View.GONE

    // Reset selections
    currentMode = Mode.NONE
    currentSeason = Season.NONE
    currentAnimal = Animal.NONE

    layoutSelectionScreen.visibility = View.VISIBLE
    containerSubSelection.visibility = View.GONE
    containerModeSelection.visibility = View.VISIBLE
    tvCameraBanner.visibility = View.GONE
    btnChangeMode.visibility = View.GONE
    btnSwitchCamera.visibility = View.GONE

    // Disable exit drawing when selection menu is visible
    circleDetectionView.isEnabled = false

    // Clear and invalidate decorations overlay
    themeOverlayView.invalidate()
  }

  private fun getBannerText(): String {
    return when (currentMode) {
      Mode.SEASONS -> {
        when (currentSeason) {
          Season.SPRING -> "🌸 SPRING 🌸"
          Season.SUMMER -> "☀️ SUMMER ☀️"
          Season.AUTUMN -> "🍂 AUTUMN 🍂"
          Season.WINTER -> "❄️ WINTER ❄️"
          else -> ""
        }
      }
      Mode.ANIMALS -> {
        when (currentAnimal) {
          Animal.CAT -> "🐱 CAT 🐱"
          Animal.DOG -> "🐶 DOG 🐶"
          Animal.BIRD -> "🐦 BIRD 🐦"
          Animal.COW -> "🐮 COW 🐮"
          Animal.DUCK -> "🦆 DUCK 🦆"
          else -> ""
        }
      }
      else -> ""
    }
  }

  private fun drawDecorations(canvas: Canvas, w: Float, h: Float, isForSavedPhoto: Boolean) {
    val paint = Paint().apply {
      isAntiAlias = true
      textAlign = Paint.Align.CENTER
    }

    // 1. Draw full screen filters and detailed weather effects based on season
    if (currentMode == Mode.SEASONS) {
      when (currentSeason) {
        Season.SPRING -> {
          // Soft pink spring tint
          val springFilter = Paint().apply {
            color = 0x12FFC0CB.toInt() // Pink tint
            style = Paint.Style.FILL
          }
          canvas.drawRect(0f, 0f, w, h, springFilter)

          // Falling flower petals in unicorn colors
          val petalPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
          }
          val unicornPetals = intArrayOf(
            0xCCFFB5D5.toInt(), 0xCCD5B5FF.toInt(), 0xCCB5D5FF.toInt(),
            0xCCB5FFD5.toInt(), 0xCCFFF5B5.toInt(), 0xCCB5E0FF.toInt(), 0xCCFFC5E0.toInt()
          )
          if (isForSavedPhoto) {
            val numPetals = 10
            for (i in 0 until numPetals) {
              val rand = java.util.Random((i * 9999).toLong())
              val px = rand.nextFloat() * w
              val py = rand.nextFloat() * h
              val rx = w * 0.015f + rand.nextFloat() * (w * 0.02f)
              val ry = rx * (0.3f + rand.nextFloat() * 0.5f)
              petalPaint.color = unicornPetals[rand.nextInt(unicornPetals.size)]
              canvas.save()
              canvas.translate(px, py)
              canvas.rotate(rand.nextFloat() * 360f)
              val rect = RectF(-rx, -ry, rx, ry)
              canvas.drawOval(rect, petalPaint)
              canvas.restore()
            }
          } else {
            val cycleMs = 600L
            val baseTime = System.currentTimeMillis()
            for (i in themeOverlayView.petalX.indices) {
              val px = themeOverlayView.petalX[i]
              val py = themeOverlayView.petalY[i]
              val rx = themeOverlayView.petalSize[i]
              val ry = rx * (0.3f + (i % 3) * 0.15f)
              val elapsed = (baseTime + i * 200L) % (cycleMs * unicornPetals.size)
              val ci = (elapsed / cycleMs).toInt()
              val color1 = unicornPetals.getOrElse(ci) { unicornPetals[0] }
              val color2 = unicornPetals.getOrElse((ci + 1) % unicornPetals.size) { unicornPetals[0] }
              val t = (elapsed % cycleMs) / cycleMs.toFloat()
              petalPaint.color = themeOverlayView.blendColor(color1, color2, t)
              canvas.save()
              canvas.translate(px, py)
              canvas.rotate(themeOverlayView.petalRot[i])
              val rect = RectF(-rx, -ry, rx, ry)
              canvas.drawOval(rect, petalPaint)
              canvas.restore()
            }
          }
        }
        Season.SUMMER -> {
          // Warm sunny golden/yellow tint
          val sunnyFilter = Paint().apply {
            color = 0x18FFD700.toInt() // Subtle transparent gold
            style = Paint.Style.FILL
          }
          canvas.drawRect(0f, 0f, w, h, sunnyFilter)

          // Draw a soft glowing sun in the top right corner
          val sunPaint = Paint().apply {
            color = 0x33FF9800.toInt() // Glowing orange
            style = Paint.Style.FILL
            isAntiAlias = true
          }
          canvas.drawCircle(w * 0.9f, h * 0.1f, w * 0.25f, sunPaint)
          sunPaint.color = 0x44FFEB3B.toInt() // Glowing yellow core
          canvas.drawCircle(w * 0.9f, h * 0.1f, w * 0.15f, sunPaint)
        }
        Season.AUTUMN -> {
          // Cool rainy blue/gray tint
          val rainyFilter = Paint().apply {
            color = 0x180000FF.toInt() // Cool blue tint
            style = Paint.Style.FILL
          }
          canvas.drawRect(0f, 0f, w, h, rainyFilter)

          // Falling raindrops (teardrop shapes)
          val rainPaint = Paint().apply {
            color = 0x88FFFFFF.toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
          }
          if (isForSavedPhoto) {
            val numRaindrops = 12
            for (i in 0 until numRaindrops) {
              val rand = java.util.Random((i * 1234).toLong())
              val rx = rand.nextFloat() * w
              val ry = rand.nextFloat() * h
              val size = w * 0.015f + rand.nextFloat() * (w * 0.015f)
              canvas.save()
              canvas.translate(rx, ry)
              canvas.scale(size, size)
              canvas.drawPath(themeOverlayView.raindropPath, rainPaint)
              canvas.restore()
            }
          } else {
            for (i in themeOverlayView.rainX.indices) {
              canvas.save()
              canvas.translate(themeOverlayView.rainX[i], themeOverlayView.rainY[i])
              val size = w * 0.018f
              canvas.scale(size, size)
              canvas.drawPath(themeOverlayView.raindropPath, rainPaint)
              canvas.restore()
            }
          }
        }
        Season.WINTER -> {
          // Cold snowy white tint
          val winterFilter = Paint().apply {
            color = 0x12FFFFFF.toInt() // Subtle white/snowy tint
            style = Paint.Style.FILL
          }
          canvas.drawRect(0f, 0f, w, h, winterFilter)

          // Gentle falling snowflakes (drawn as 6-branch crystal shapes)
          val snowPaint = Paint().apply {
            color = 0xAAFFFFFF.toInt() // Soft white
            style = Paint.Style.STROKE
            strokeWidth = w * 0.004f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
          }
          if (isForSavedPhoto) {
            val numSnowflakes = 10
            for (i in 0 until numSnowflakes) {
              val rand = java.util.Random((i * 5678).toLong())
              val sx = rand.nextFloat() * w
              val sy = rand.nextFloat() * h
              val size = w * 0.008f + rand.nextFloat() * (w * 0.01f)
              canvas.save()
              canvas.translate(sx, sy)
              canvas.rotate(rand.nextFloat() * 360f)
              for (j in 0 until 3) {
                canvas.drawLine(0f, -size, 0f, size, snowPaint)
                canvas.rotate(60f)
              }
              canvas.restore()
            }
          } else {
            for (i in themeOverlayView.snowX.indices) {
              canvas.save()
              canvas.translate(themeOverlayView.snowX[i], themeOverlayView.snowY[i])
              canvas.rotate(themeOverlayView.snowRot[i])
              val size = themeOverlayView.snowSize[i]
              for (j in 0 until 3) {
                canvas.drawLine(0f, -size, 0f, size, snowPaint)
                canvas.rotate(60f)
              }
              canvas.restore()
            }
          }
        }
        else -> {}
      }
    }

    // 2. Draw static themed stickers
    if (currentStickers.isNotEmpty()) {
      val stickerPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
      }
      currentStickers.forEach { sticker ->
        val px = sticker.x * w
        val py = sticker.y * h
        stickerPaint.textSize = w * sticker.sizePercent
        val centerY = py - (stickerPaint.descent() + stickerPaint.ascent()) / 2
        canvas.drawText(sticker.emoji, px, centerY, stickerPaint)
      }
    }

    // 3. Draw static animal decorations (large corner mascot + accent)
    if (currentMode == Mode.ANIMALS) {
      val (animalEmoji, accentEmoji) = when (currentAnimal) {
        Animal.CAT -> Pair("🐱", "🐾")
        Animal.DOG -> Pair("🐶", "🐾")
        Animal.BIRD -> Pair("🐦", "🌿")
        Animal.COW -> Pair("🐮", "🌾")
        Animal.DUCK -> Pair("🦆", "🌊")
        else -> Pair("", "")
      }

      if (animalEmoji.isNotEmpty()) {
        val stickerSize = w * 0.28f
        paint.textSize = stickerSize
        val centerY = h * 0.88f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(animalEmoji, w * 0.78f, centerY, paint)

        if (accentEmoji.isNotEmpty()) {
          val accentSize = w * 0.15f
          paint.textSize = accentSize
          val accentY = h * 0.88f - (paint.descent() + paint.ascent()) / 2
          canvas.drawText(accentEmoji, w * 0.22f, accentY, paint)
        }
      }
    }

    // If saving the photo, we also draw the header banner text
    if (isForSavedPhoto) {
      val bannerText = getBannerText()
      if (bannerText.isNotEmpty()) {
        val bannerHeight = h * 0.08f
        val bannerWidth = w * 0.6f
        val left = (w - bannerWidth) / 2
        val top = h * 0.03f
        val right = left + bannerWidth
        val bottom = top + bannerHeight

        val rectPaint = Paint().apply {
          color = 0x99000000.toInt()
          style = Paint.Style.FILL
        }
        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, bannerHeight / 2, bannerHeight / 2, rectPaint)

        // Draw text
        val textPaint = Paint().apply {
          color = Color.WHITE
          textSize = bannerHeight * 0.5f
          isAntiAlias = true
          textAlign = Paint.Align.CENTER
          typeface = Typeface.DEFAULT_BOLD
        }
        val textY = top + bannerHeight / 2 - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(bannerText, w / 2, textY, textPaint)
      }
    }
  }

  private fun addDecorationsToSavedPhoto(uri: Uri) {
    try {
      // 1. Load the original bitmap
      val inputStream = contentResolver.openInputStream(uri) ?: return
      val originalBitmap = BitmapFactory.decodeStream(inputStream)
      inputStream.close()

      if (originalBitmap == null) return

      // 2. Rotate bitmap if necessary using EXIF data
      val rotatedBitmap = rotateBitmapFromUri(this, uri, originalBitmap)

      // 3. Create a mutable copy of the bitmap
      val mutableBitmap = rotatedBitmap.copy(Bitmap.Config.ARGB_8888, true)
      
      // 4. Draw decorations on the canvas
      val canvas = Canvas(mutableBitmap)
      drawDecorations(canvas, mutableBitmap.width.toFloat(), mutableBitmap.height.toFloat(), isForSavedPhoto = true)

      // 5. Save the updated bitmap back to the same Uri
      val outputStream = contentResolver.openOutputStream(uri) ?: return
      mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
      outputStream.close()

      // Recycle bitmaps to free memory
      if (rotatedBitmap != originalBitmap) {
        originalBitmap.recycle()
      }
      rotatedBitmap.recycle()
      mutableBitmap.recycle()
    } catch (e: Exception) {
      android.util.Log.e("GiggleCam", "Error decorating photo: ${e.message}", e)
    }
  }

  private fun rotateBitmapFromUri(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
    try {
      val inputStream = context.contentResolver.openInputStream(uri)
      val orientation = inputStream?.use { stream ->
        ExifInterface(stream).getAttributeInt(
          ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL
        )
      } ?: ExifInterface.ORIENTATION_NORMAL

      val rotationDegrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
      }

      if (rotationDegrees == 0f) return bitmap

      val matrix = Matrix().apply { postRotate(rotationDegrees) }
      val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
      if (rotated != bitmap) {
        bitmap.recycle()
      }
      return rotated
    } catch (e: Exception) {
      return bitmap
    }
  }

  private fun showInstructionsDialog() {
    val dialog = android.app.Dialog(this)
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
    dialog.setContentView(R.layout.dialog_instructions)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    
    // Set custom layout params to make it look spacious and nice
    dialog.window?.setLayout(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.WRAP_CONTENT
    )

    // Keep immersive inside dialog
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      dialog.window?.insetsController?.hide(WindowInsets.Type.systemBars())
    } else {
      @Suppress("DEPRECATION")
      dialog.window?.decorView?.systemUiVisibility = (
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_FULLSCREEN
      )
    }

    dialog.findViewById<View>(R.id.btn_close_instructions).setOnClickListener {
      dialog.dismiss()
      hideSystemUI()
    }

    dialog.setOnDismissListener {
      hideSystemUI()
    }

    dialog.show()
  }

  private fun showSettingsDialog() {
    val dialog = android.app.Dialog(this)
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
    dialog.setContentView(R.layout.dialog_settings)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    
    // Set custom layout params to make it look spacious and nice
    dialog.window?.setLayout(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.WRAP_CONTENT
    )

    // Keep immersive inside dialog
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      dialog.window?.insetsController?.hide(WindowInsets.Type.systemBars())
    } else {
      @Suppress("DEPRECATION")
      dialog.window?.decorView?.systemUiVisibility = (
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_FULLSCREEN
      )
    }

    val switchSave = dialog.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_save_photos)
    switchSave.isChecked = savePhotosToGallery

    val switchFront = dialog.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_front_camera)
    switchFront.isChecked = useFrontCamera

    val switchStickers = dialog.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_show_stickers)
    switchStickers.isChecked = showStickers

    // Time limit picker buttons
    val timeOptions = listOf(
      dialog.findViewById<TextView>(R.id.btn_time_15),
      dialog.findViewById<TextView>(R.id.btn_time_30),
      dialog.findViewById<TextView>(R.id.btn_time_45),
      dialog.findViewById<TextView>(R.id.btn_time_60)
    )
    val timeValues = listOf(5, 15, 30, 45)
    var selectedTime = playTimeLimitMinutes

    fun updateTimeButtonStyles() {
      timeOptions.forEachIndexed { index, btn ->
        val isSelected = timeValues[index] == selectedTime
        if (isSelected) {
          btn.setBackgroundColor(0xFF3F51B5.toInt())
          btn.setTextColor(android.graphics.Color.WHITE)
        } else {
          btn.setBackgroundResource(R.drawable.card_sub_option)
          btn.setTextColor(0xFF333333.toInt())
        }
      }
    }
    selectedTime = playTimeLimitMinutes
    updateTimeButtonStyles()

    timeOptions.forEachIndexed { index, btn ->
      btn.setOnClickListener {
        selectedTime = timeValues[index]
        updateTimeButtonStyles()
      }
    }

    dialog.findViewById<View>(R.id.btn_cancel_settings).setOnClickListener {
      dialog.dismiss()
      hideSystemUI()
    }

    dialog.findViewById<View>(R.id.btn_save_settings).setOnClickListener {
      val isSaveChecked = switchSave.isChecked
      if (isSaveChecked && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this@MainActivity, permission) != PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(permission),
            storagePermissionRequestCode
          )
          dialog.dismiss()
          hideSystemUI()
          return@setOnClickListener
        }
      }

      savePhotosToGallery = isSaveChecked
      playTimeLimitMinutes = selectedTime
      useFrontCamera = switchFront.isChecked
      showStickers = switchStickers.isChecked
      
      val prefs = getSharedPreferences("GiggleCamPrefs", Context.MODE_PRIVATE)
      prefs.edit().putBoolean("save_photos_to_gallery", savePhotosToGallery).apply()
      prefs.edit().putInt("play_time_limit_minutes", playTimeLimitMinutes).apply()
      prefs.edit().putBoolean("use_front_camera", useFrontCamera).apply()
      prefs.edit().putBoolean("show_stickers", showStickers).apply()

      // Rebind camera with new setting immediately
      bindCameraUseCases()
      
      val status = if (savePhotosToGallery) "enabled" else "disabled"
      android.widget.Toast.makeText(this, "Photo saving $status!", android.widget.Toast.LENGTH_SHORT).show()
      
      dialog.dismiss()
      hideSystemUI()
    }

    dialog.setOnDismissListener {
      hideSystemUI()
    }

    dialog.show()
  }

  private var pinningGuideDialog: android.app.Dialog? = null

  private fun checkLockTaskMode() {
    val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val isInLockTask = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    } else {
      @Suppress("DEPRECATION")
      activityManager.isInLockTaskMode
    }

    if (!isInLockTask) {
      showPinningGuideDialog()
    } else {
      pinningGuideDialog?.dismiss()
      pinningGuideDialog = null
    }
  }

  private fun showPinningGuideDialog() {
    if (pinningGuideDialog?.isShowing == true) return

    val dialog = android.app.Dialog(this)
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
    dialog.setContentView(R.layout.dialog_pinning_guide)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    
    dialog.window?.setLayout(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.WRAP_CONTENT
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      dialog.window?.insetsController?.hide(WindowInsets.Type.systemBars())
    } else {
      @Suppress("DESUPPRESS", "DEPRECATION")
      dialog.window?.decorView?.systemUiVisibility = (
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_FULLSCREEN
      )
    }

    dialog.findViewById<View>(R.id.btn_open_settings).setOnClickListener {
      try {
        startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
      } catch (e: Exception) {
        android.widget.Toast.makeText(this, "Could not open settings.", android.widget.Toast.LENGTH_LONG).show()
      }
    }

    dialog.findViewById<View>(R.id.btn_close_guide).setOnClickListener {
      dialog.dismiss()
      hideSystemUI()
    }

    dialog.setOnDismissListener {
      hideSystemUI()
      pinningGuideDialog = null
    }

    pinningGuideDialog = dialog
    dialog.show()
  }

  private fun runParentGate(onSuccess: () -> Unit) {
    val num1 = (3..9).random()
    val num2 = (2..8).random()
    val answer = num1 + num2

    val dialog = android.app.Dialog(this)
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
    dialog.setContentView(R.layout.dialog_parent_gate)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    
    dialog.window?.setLayout(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.WRAP_CONTENT
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      dialog.window?.insetsController?.hide(WindowInsets.Type.systemBars())
    } else {
      @Suppress("DEPRECATION")
      dialog.window?.decorView?.systemUiVisibility = (
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_FULLSCREEN
      )
    }

    val tvQuestion = dialog.findViewById<TextView>(R.id.tv_gate_question)
    val etAnswer = dialog.findViewById<android.widget.EditText>(R.id.et_gate_answer)
    val btnVerify = dialog.findViewById<TextView>(R.id.btn_verify_gate)
    val btnCancel = dialog.findViewById<TextView>(R.id.btn_cancel_gate)

    tvQuestion.text = "Solve the math problem to enter:\n$num1 + $num2 = ?"

    btnCancel.setOnClickListener {
      dialog.dismiss()
      hideSystemUI()
    }

    btnVerify.setOnClickListener {
      val input = etAnswer.text.toString().trim()
      if (input == answer.toString()) {
        dialog.dismiss()
        hideSystemUI()
        onSuccess()
      } else {
        android.widget.Toast.makeText(this, "Incorrect answer. Try again!", android.widget.Toast.LENGTH_SHORT).show()
        etAnswer.text.clear()
      }
    }

    dialog.setOnDismissListener {
      hideSystemUI()
    }

    dialog.show()
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
      val rotationMatrix = FloatArray(9)
      SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
      val orientation = FloatArray(3)
      SensorManager.getOrientation(rotationMatrix, orientation)
      tiltRoll = orientation[2]  // roll (+ = right side down)
      tiltPitch = orientation[1] // pitch (+ = top tilting forward/away)
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

  private fun initializeStickers() {
    currentStickers.clear()
    if (!showStickers) return
    val emojis = when (currentMode) {
      Mode.SEASONS -> {
        when (currentSeason) {
          Season.SPRING -> listOf("🌸", "🌷", "🦋", "🐝", "⚽", "🧸", "🚗", "🎈", "🌈", "🐰")
          Season.SUMMER -> listOf("☀️", "🌻", "🍦", "⭐", "🏀", "🧸", "🚒", "🎈", "🌈", "🐸")
          Season.AUTUMN -> listOf("🍂", "🍁", "🍄", "🏈", "🚜", "🧸", "🎃", "🪁", "🚂", "🐿️")
          Season.WINTER -> listOf("❄️", "⛄", "🧤", "🎁", "🎾", "🚂", "🎄", "🧸", "🌟", "🐧")
          else -> emptyList()
        }
      }
      Mode.ANIMALS -> {
        when (currentAnimal) {
          Animal.CAT -> listOf("🐱", "🧶", "🐭", "🐟", "⚽", "🧸", "🐾", "🎈", "🚗", "🐰")
          Animal.DOG -> listOf("🐶", "🦴", "🎾", "⚽", "🏀", "⭐", "🧸", "🚗", "🐾", "🌈")
          Animal.BIRD -> listOf("🐦", "🪶", "🌸", "🦋", "⚽", "🎈", "🧸", "🌈", "🪀", "🐸")
          Animal.COW -> listOf("🐮", "🥛", "🌾", "🍀", "⚽", "🎈", "🧸", "🚜", "🌟", "🐰")
          Animal.DUCK -> listOf("🦆", "🫧", "🌊", "🪷", "⚽", "🎈", "🧸", "🚤", "🌈", "🐸")
          else -> emptyList()
        }
      }
      else -> emptyList()
    }

    val rand = java.util.Random()
    val largeEmojis = setOf("🐱", "🐶", "🐮", "🦆", "⚽", "🏀", "🏈", "🎾", "🧶", "🧸", "🚗", "🚒", "🚜", "🚂", "🚤", "🎃", "🌈", "🐰", "🐸", "🐧", "🐿️", "🦕", "🦄", "✈️", "📚", "🚁", "🚀", "🐘", "🦁", "🐯", "🐧", "🦊", "🐼", "🐨", "🐵", "🦉")

    // Add 3 random educational toy stickers to every mode
    val toyEmojis = listOf("🦕", "🦄", "🚗", "✈️", "📚", "🚁", "🚀", "🐘", "🦁", "🐯", "🦊", "🐼", "🐨", "🐵", "🦉", "🎨", "🧩", "🎵", "🔬", "🌍", "⭐", "🌈", "🍎", "🚲", "⛵", "🏰", "🎪", "🎠", "🪐", "🤖")
    val shuffledToys = toyEmojis.shuffled(rand).take(3)

    val allEmojis = emojis + shuffledToys
    allEmojis.forEachIndexed { index, emoji ->
      val x = 0.15f + (index % 3) * 0.3f + (rand.nextFloat() - 0.5f) * 0.1f
      val y = 0.2f + (index / 3) * 0.3f + (rand.nextFloat() - 0.5f) * 0.1f

      val isLarge = emoji in largeEmojis
      val size = if (isLarge) 0.14f else 0.08f

      currentStickers.add(Sticker(emoji, x.coerceIn(0.1f, 0.9f), y.coerceIn(0.1f, 0.9f), size))
    }
  }

  inner class ThemeOverlayView(context: Context) : View(context) {
    // Tilt-sensitive star with letter "D" and unicorn rainbow colors
    private var ballX = 0.5f
    private var ballY = 0.5f
    private var ballVx = 0f
    private var ballVy = 0f
    private val starPath = Path()
    val raindropPath = Path().apply {
      // Teardrop/raindrop: pointed top, rounded bottom, size = 1.0
      moveTo(0f, -1f)
      cubicTo(0.45f, -0.35f, 0.5f, 0.3f, 0f, 1f)
      cubicTo(-0.5f, 0.3f, -0.45f, -0.35f, 0f, -1f)
      close()
    }
    private val ballPaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.FILL
    }
    private val ballStrokePaint = Paint().apply {
      isAntiAlias = true
      style = Paint.Style.STROKE
      strokeWidth = 3f
      strokeJoin = Paint.Join.ROUND
    }
    private val ballTextPaint = Paint().apply {
      isAntiAlias = true
      textAlign = Paint.Align.CENTER
      color = android.graphics.Color.WHITE
      typeface = Typeface.DEFAULT_BOLD
    }
    // Gentle falling particles for weather effects
    val rainX = FloatArray(12)
    val rainY = FloatArray(12)
    val rainSpeed = FloatArray(12)
    val snowX = FloatArray(10)
    val snowY = FloatArray(10)
    val snowVx = FloatArray(10)
    val snowVy = FloatArray(10)
    val snowSize = FloatArray(10)
    val snowRot = FloatArray(10)
    val petalX = FloatArray(10)
    val petalY = FloatArray(10)
    val petalVx = FloatArray(10)
    val petalVy = FloatArray(10)
    val petalRot = FloatArray(10)
    val petalSize = FloatArray(10)

    fun reinitializeParticles(w: Float, h: Float) {
      if (w <= 0f || h <= 0f) return
      val rand = java.util.Random()
      for (i in rainX.indices) {
        rainX[i] = rand.nextFloat() * w
        rainY[i] = rand.nextFloat() * h
        rainSpeed[i] = h * (0.005f + rand.nextFloat() * 0.003f)
      }
      for (i in snowX.indices) {
        snowX[i] = rand.nextFloat() * w
        snowY[i] = rand.nextFloat() * h
        snowVx[i] = (rand.nextFloat() - 0.5f) * 1.5f
        snowVy[i] = h * (0.004f + rand.nextFloat() * 0.003f)
        snowSize[i] = 6f + rand.nextFloat() * 6f
        snowRot[i] = rand.nextFloat() * 360f
      }
      for (i in petalX.indices) {
        petalX[i] = rand.nextFloat() * w
        petalY[i] = rand.nextFloat() * h
        petalVx[i] = (rand.nextFloat() - 0.5f) * 0.8f
        petalVy[i] = h * (0.004f + rand.nextFloat() * 0.003f)
        petalRot[i] = rand.nextFloat() * 360f
        petalSize[i] = 6f + rand.nextFloat() * 8f
      }
    }

    fun updateParticles(w: Float, h: Float) {
      if (w <= 0f || h <= 0f) return
      when (currentSeason) {
        Season.AUTUMN -> {
          for (i in rainX.indices) {
            rainY[i] += rainSpeed[i]
            rainX[i] -= rainSpeed[i] * 0.15f
            if (rainY[i] > h) {
              rainY[i] = -10f
              rainX[i] = java.util.Random().nextFloat() * w
            }
          }
        }
        Season.WINTER -> {
          for (i in snowX.indices) {
            snowX[i] += snowVx[i]
            snowY[i] += snowVy[i]
            snowRot[i] += 0.8f
            if (snowY[i] > h) {
              snowY[i] = -5f
              snowX[i] = java.util.Random().nextFloat() * w
            }
          }
        }
        Season.SPRING -> {
          for (i in petalX.indices) {
            petalX[i] += petalVx[i]
            petalY[i] += petalVy[i]
            petalRot[i] += 0.5f
            if (petalY[i] > h) {
              petalY[i] = -10f
              petalX[i] = java.util.Random().nextFloat() * w
            }
          }
        }
        else -> {}
      }
    }

    private val unicornColors = intArrayOf(
      0xFFFFB5D5.toInt(), // pastel pink
      0xFFD5B5FF.toInt(), // pastel purple
      0xFFB5D5FF.toInt(), // pastel blue
      0xFFB5FFD5.toInt(), // pastel mint
      0xFFFFF5B5.toInt(), // pastel yellow
      0xFFB5E0FF.toInt(), // pastel sky blue
      0xFFFFC5E0.toInt(), // light rose
    )

    private fun buildStarPath(r: Float) {
      starPath.reset()
      val points = 5
      for (i in 0 until points * 2) {
        val angle = Math.toRadians((-90.0 + i * 360.0 / (points * 2))).toFloat()
        val radius = if (i % 2 == 0) r else r * 0.4f
        val x = kotlin.math.cos(angle) * radius
        val y = kotlin.math.sin(angle) * radius
        if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
      }
      starPath.close()
    }

    fun resetBall() {
      ballX = 0.5f
      ballY = 0.5f
      ballVx = 0f
      ballVy = 0f
    }

    private val updateRunnable = object : Runnable {
      override fun run() {
        if (currentMode != Mode.NONE) {
          if (showStickers) {
            updateBallPhysics()
          }
          updateParticles(width.toFloat(), height.toFloat())
          invalidate()
        }
        postOnAnimation(this)
      }
    }

    private fun updateBallPhysics() {
      val w = width.toFloat()
      val h = height.toFloat()
      if (w <= 0f || h <= 0f) return

      // Use game rotation vector:
      //   tiltRoll (+) = right side down → ball rolls right (+X)
      //   tiltPitch (+) = top tilting forward/away → ball rolls up (-Y)
      // Dead zone ~5° (0.08 rad) so star doesn't drift at neutral
      val dz = 0.08f
      val roll = if (kotlin.math.abs(tiltRoll) > dz) tiltRoll else 0f
      val pitch = if (kotlin.math.abs(tiltPitch) > dz) tiltPitch else 0f

      ballVx += roll * 0.005f
      ballVy -= pitch * 0.005f

      // Friction so star settles when held steady
      ballVx *= 0.96f
      ballVy *= 0.96f

      // Cap speed
      val maxSpeed = 0.04f
      ballVx = ballVx.coerceIn(-maxSpeed, maxSpeed)
      ballVy = ballVy.coerceIn(-maxSpeed, maxSpeed)

      // Move (normalized 0..1)
      ballX += ballVx
      ballY += ballVy

      // Bounce off edges
      val radius = 0.06f
      if (ballX < radius) {
        ballX = radius
        ballVx = -ballVx * 0.5f
      } else if (ballX > 1f - radius) {
        ballX = 1f - radius
        ballVx = -ballVx * 0.5f
      }
      if (ballY < 0.12f + radius) {
        ballY = 0.12f + radius
        ballVy = -ballVy * 0.5f
      } else if (ballY > 1f - radius) {
        ballY = 1f - radius
        ballVy = -ballVy * 0.5f
      }
    }

    override fun onAttachedToWindow() {
      super.onAttachedToWindow()
      postOnAnimation(updateRunnable)
    }

    override fun onDetachedFromWindow() {
      super.onDetachedFromWindow()
      removeCallbacks(updateRunnable)
    }

    override fun onDraw(canvas: Canvas) {
      super.onDraw(canvas)
      drawDecorations(canvas, width.toFloat(), height.toFloat(), isForSavedPhoto = false)

      if (showStickers) {
        drawBall(canvas)
      }
    }

    private fun drawBall(canvas: Canvas) {
      val w = width.toFloat()
      val h = height.toFloat()
      val cx = ballX * w
      val cy = ballY * h
      val r = w * 0.06f

      // Blinking unicorn colors based on time
      val cycleMs = 600L
      val elapsed = System.currentTimeMillis() % (cycleMs * unicornColors.size)
      val colorIndex = (elapsed / cycleMs).toInt()
      val color = unicornColors.getOrElse(colorIndex) { unicornColors[0] }
      val nextColor = unicornColors.getOrElse((colorIndex + 1) % unicornColors.size) { unicornColors[0] }
      val t = (elapsed % cycleMs) / cycleMs.toFloat()
      val blendedColor = blendColor(color, nextColor, t)

      buildStarPath(r)

      // Triple nested stars: outer, middle, inner
      val scales = floatArrayOf(1f, 0.6f, 0.3f)
      for (scale in scales) {
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(scale, scale)
        ballPaint.color = blendedColor
        canvas.drawPath(starPath, ballPaint)
        canvas.restore()

        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(scale, scale)
        ballStrokePaint.color = 0xAAFFFFFF.toInt()
        canvas.drawPath(starPath, ballStrokePaint)
        canvas.restore()
      }
    }

    fun blendColor(c1: Int, c2: Int, t: Float): Int {
      val a = (Color.alpha(c1) + ((Color.alpha(c2) - Color.alpha(c1)) * t).toInt()).coerceIn(0, 255)
      val r = (Color.red(c1) + ((Color.red(c2) - Color.red(c1)) * t).toInt()).coerceIn(0, 255)
      val g = (Color.green(c1) + ((Color.green(c2) - Color.green(c1)) * t).toInt()).coerceIn(0, 255)
      val b = (Color.blue(c1) + ((Color.blue(c2) - Color.blue(c1)) * t).toInt()).coerceIn(0, 255)
      return Color.argb(a, r, g, b)
    }
  }
}
