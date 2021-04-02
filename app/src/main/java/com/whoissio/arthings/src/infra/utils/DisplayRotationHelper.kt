package com.whoissio.arthings.src.infra.utils

import android.content.Context
import android.content.Context.*
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.view.WindowManager
import com.google.ar.core.Session
import com.whoissio.arthings.src.infra.Converters.toDegrees


class DisplayRotationHelper(context: Context): DisplayManager.DisplayListener {
  private var viewportWidth = 0
  private var viewportHeight = 0
  private var isViewPortChanged = false

  private val displayManager = context.getSystemService(DISPLAY_SERVICE) as DisplayManager
  private val cameraManager = context.getSystemService(CAMERA_SERVICE) as CameraManager
  private val windowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
  private val display = windowManager.defaultDisplay

  fun onResume() {
    displayManager.registerDisplayListener(this, null)
  }

  fun onPause() {
    displayManager.unregisterDisplayListener(this)
  }

  fun onSurfaceChanged(width: Int, height: Int) {
    viewportWidth = width
    viewportHeight = height
    isViewPortChanged = true
  }

  fun updateSessionIfNeeded(session: Session?) {
    if (!isViewPortChanged) return
    session?.setDisplayGeometry(display.rotation, viewportWidth, viewportHeight)
    isViewPortChanged = false
  }

  fun getCameraSensorRelativeViewportAspectRatio(cameraId: String?): Float {
    val aspectRatio: Float
    val cameraSensorToDisplayRotation: Int = getCameraSensorToDisplayRotation(cameraId)
    aspectRatio = when (cameraSensorToDisplayRotation) {
      90, 270 -> viewportHeight.toFloat() / viewportWidth.toFloat()
      0, 180 -> viewportWidth.toFloat() / viewportHeight.toFloat()
      else -> throw RuntimeException("Unhandled rotation: $cameraSensorToDisplayRotation")
    }
    return aspectRatio
  }

  private fun getCameraSensorToDisplayRotation(cameraId: String?): Int {
    val characteristics = try {
      cameraManager.getCameraCharacteristics(cameraId!!)
    } catch (e: CameraAccessException) {
      throw java.lang.RuntimeException("Unable to determine display orientation", e)
    }

    val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
    val displayOrientation = display.rotation.toDegrees()

    return (sensorOrientation - displayOrientation + 360) % 360
  }

  override fun onDisplayAdded(displayId: Int) = Unit

  override fun onDisplayRemoved(displayId: Int) = Unit

  override fun onDisplayChanged(displayId: Int) {
    isViewPortChanged = true
  }
}