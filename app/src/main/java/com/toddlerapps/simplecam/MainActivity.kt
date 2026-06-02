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
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Main Screen - Kiosk mode for kids
 * - Tap anywhere to take a photo
 * - Photo preview shows for 3 seconds then returns to camera
 * - Draw an "O" circle gesture to exit the app
 */
class MainActivity : AppCompatActivity() {

  private lateinit var previewView: PreviewView
  private lateinit var circleGestureView: CircleGestureView
  private lateinit var photoPreview: ImageView
  private lateinit var effectNameText: TextView
  private lateinit var confettiAnimation: LottieAnimationView
  private var imageCapture: ImageCapture? = null
  private var previewUri: Uri? = null
  private lateinit var shutterSound: MediaActionSound

  private val requestPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      if (isGranted) {
        startCamera()
      } else {
        Toast.makeText(this, "Camera permission is required to use this app", Toast.LENGTH_LONG).show()
        finish()
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    // Switch to AppTheme for displaying the activity
    setTheme(R.style.AppTheme)

    super.onCreate(savedInstanceState)

    // Keep screen on
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    setContentView(R.layout.activity_main)

    previewView = findViewById(R.id.previewView)
    circleGestureView = findViewById(R.id.circleGestureView)
    photoPreview = findViewById(R.id.photoPreview)
    effectNameText = findViewById(R.id.effectNameText)
    confettiAnimation = findViewById(R.id.confettiAnimation)

    // Tap anywhere to take a photo (only when preview is not showing)
    circleGestureView.onTapDetected = {
      if (photoPreview.visibility != View.VISIBLE) {
        takePhoto()
      }
    }

    // Circle gesture to exit (only when preview is not showing)
    circleGestureView.onCircleDetected = {
      if (photoPreview.visibility != View.VISIBLE) {
        showExitDialog()
      }
    }

    // Initialize shutter sound
    shutterSound = MediaActionSound()
    shutterSound.load(MediaActionSound.SHUTTER_CLICK)

    // Enter lock task (kiosk) mode
    startLockTask()

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) {
      startCamera()
    } else {
      requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }
  }

  override fun onResume() {
    super.onResume()
    enterImmersiveMode()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      enterImmersiveMode()
      try {
        startLockTask()
      } catch (_: Exception) { }
    }
  }

  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    // Do nothing
  }

  private fun showExitDialog() {
    AlertDialog.Builder(this)
      .setTitle("Exit App")
      .setMessage("Are you sure you want to exit?")
      .setPositiveButton("Exit") { _, _ ->
        stopLockTask()
        finishAffinity()
      }
      .setNegativeButton("Cancel", null)
      .show()
  }

  private fun showPhotoPreview(uri: Uri) {
    previewUri = uri
    try {
      val inputStream = contentResolver.openInputStream(uri)
      val originalBitmap = BitmapFactory.decodeStream(inputStream)
      inputStream?.close()

      // Apply a random fun effect (may use face detection)
      PhotoEffects.applyRandomEffect(this@MainActivity, originalBitmap) { effectedBitmap, effectName ->
        runOnUiThread {
          photoPreview.setImageBitmap(effectedBitmap)
          photoPreview.visibility = View.VISIBLE

          // Show effect name
          effectNameText.text = "✨ $effectName ✨"
          effectNameText.visibility = View.VISIBLE

          // Play Lottie confetti animation
          confettiAnimation.visibility = View.VISIBLE
          confettiAnimation.playAnimation()

          // Auto-dismiss after 3 seconds
          photoPreview.postDelayed({
            hidePhotoPreview()
          }, 3000)
        }
      }
    } catch (e: Exception) {
      // If preview fails, just continue
    }
  }

  private fun hidePhotoPreview() {
    photoPreview.visibility = View.GONE
    photoPreview.setImageDrawable(null)
    effectNameText.visibility = View.GONE
    confettiAnimation.visibility = View.GONE
    confettiAnimation.cancelAnimation()
    previewUri = null
  }

  private fun enterImmersiveMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      window.insetsController?.let { controller ->
        controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        controller.systemBarsBehavior =
          WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      }
    } else {
      @Suppress("DEPRECATION")
      window.decorView.systemUiVisibility = (
        View.SYSTEM_UI_FLAG_FULLSCREEN
          or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
          or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
          or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
          or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
          or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
      )
    }
  }

  private fun takePhoto() {
    val imageCapture = imageCapture ?: return

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val fileName = "TODDLERCAM_$timestamp"

    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
      put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ToddlerCam")
      }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
      contentResolver,
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
      contentValues
    ).build()

    // Play shutter sound
    shutterSound.play(MediaActionSound.SHUTTER_CLICK)

    imageCapture.takePicture(
      outputOptions,
      ContextCompat.getMainExecutor(this),
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
          output.savedUri?.let { uri ->
            showPhotoPreview(uri)
          }
        }

        override fun onError(exception: ImageCaptureException) {
          Toast.makeText(this@MainActivity, "Failed to save photo", Toast.LENGTH_SHORT).show()
        }
      }
    )
  }

  private fun startCamera() {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
    cameraProviderFuture.addListener({
      val cameraProvider = cameraProviderFuture.get()

      val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
      }

      imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

      val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

      try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
      } catch (e: Exception) {
        Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
      }
    }, ContextCompat.getMainExecutor(this))
  }
}