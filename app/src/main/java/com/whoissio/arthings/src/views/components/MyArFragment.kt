package com.whoissio.arthings.src.views.components

import android.util.Log
import android.widget.Toast
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.ux.BaseArFragment
import dagger.hilt.android.AndroidEntryPoint

class MyArFragment: BaseArFragment() {

  private val TAG = "StandardArFragment"

  override fun isArRequired(): Boolean = true

  override fun getAdditionalPermissions(): Array<String> = emptyArray()

  override fun handleSessionException(sessionException: UnavailableException?) {
    val message = when (sessionException) {
      is UnavailableArcoreNotInstalledException -> "Please install ARCore"
      is UnavailableApkTooOldException -> "Please update ARCore"
      is UnavailableSdkTooOldException -> "Please update this app"
      is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
      else -> "Failed to create AR session"
    }
    Log.e(TAG, "Error: $message", sessionException)
    Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
  }

  override fun getSessionConfiguration(session: Session?): Config = Config(session).apply {
    focusMode = Config.FocusMode.AUTO
    cloudAnchorMode = Config.CloudAnchorMode.ENABLED
    if (session?.isDepthModeSupported(Config.DepthMode.AUTOMATIC) == true) {
      depthMode = Config.DepthMode.AUTOMATIC
    }
  }

  override fun getSessionFeatures(): MutableSet<Session.Feature> = mutableSetOf(Session.Feature.SHARED_CAMERA)
}