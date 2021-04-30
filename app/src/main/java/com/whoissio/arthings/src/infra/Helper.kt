package com.whoissio.arthings.src.infra

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.whoissio.arthings.src.infra.Constants.PERMISSION_ARRAY
import com.whoissio.arthings.src.models.CachedData
import java.util.*
import kotlin.math.abs

object Helper {
  fun Context.hasPermissions(): Boolean = PERMISSION_ARRAY.all {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
  }

  fun Activity.shouldShowAnyRequestPermissionRationales(): Boolean =
    PERMISSION_ARRAY.any { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }

  fun Activity.launchPermissionSettings() {
    startActivity(Intent().apply {
      action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
      data = Uri.fromParts("package", packageName, null)
    })
  }

  fun Activity.setFullScreen(hasFocus: Boolean) {
    if (!hasFocus) return
    this.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
      View.SYSTEM_UI_FLAG_FULLSCREEN or
      View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
      View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
      View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
  }

  fun CachedData<*>.isAvailable(): Boolean {
    val now = Date()
    return abs(cachedAt.time - now.time) <= 300 * 1000 // 300 sec = 5 min
  }
}