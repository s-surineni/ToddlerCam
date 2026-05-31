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

/**
 * Main Screen - Camera app with circle gesture to exit
 */
class MainActivity : AppCompatActivity() {

  private lateinit var previewView: PreviewView
  private lateinit var circleDetectionView: CircleDetectionView
  private lateinit var photoPreviewView: ImageView
  private lateinit var cameraExecutor: ExecutorService
  private val cameraPermissionRequestCode = 100
  private var imageCapture: ImageCapture? = null
  private val mediaActionSound = MediaActionSound()

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
    cameraExecutor = Executors.newSingleThreadExecutor()

    // Keep screen on
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    // Register back pressed callback to prevent back button from closing the app
    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        // Do nothing - back button / gesture is disabled
      }
    })

    // Set up circle detection callback to exit app
    circleDetectionView.setCircleDetectionCallback {
      try {
        stopLockTask()
      } catch (e: Exception) {
        // Ignore
      }
      finish() // Close the app
    }

    // Set up tap callback to take a photo
    circleDetectionView.setTapCallback {
      takePhoto()
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
      val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

      // Set up the preview
      val preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
      }

      // Set up image capture
      val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()
      this.imageCapture = imageCapture

      // Select back camera
      val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

      try {
        // Unbind any previous camera
        cameraProvider.unbindAll()

        // Bind preview and imageCapture to lifecycle
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
      } catch (exc: Exception) {
        // Handle camera binding error
      }
    }, ContextCompat.getMainExecutor(this))
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
      // Android 11+ - Modern approach
      window.insetsController?.hide(WindowInsets.Type.systemBars())
      window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
      // Older Android versions - Deprecated but still works
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
    val name = "ToddlerCam_" + SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
      .format(System.currentTimeMillis()) + ".jpg"

    // Create output options object based on SDK version
    val outputOptions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
          android.util.Log.e("ToddlerCam", "Photo capture failed: ${exc.message}", exc)
          android.widget.Toast.makeText(this@MainActivity, "Failed to save: ${exc.message}", android.widget.Toast.LENGTH_LONG).show()
        }

        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
          val savedUri = output.savedUri
          android.util.Log.d("ToddlerCam", "Photo capture succeeded: $savedUri")
          
          val uriToLoad = savedUri ?: Uri.fromFile(
            java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES), name)
          )

          uriToLoad?.let { uri ->
            try {
              contentResolver.notifyChange(uri, null)
            } catch (e: Exception) {
              // Ignore
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

          android.widget.Toast.makeText(this@MainActivity, "Photo saved to Gallery!", android.widget.Toast.LENGTH_SHORT).show()

          // If API < Q, notify the media scanner so the photo appears in gallery
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
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
}
