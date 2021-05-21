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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.whoissio.arthings.BuildConfig
import com.whoissio.arthings.src.BaseActivity
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
    return abs(cachedAt.time - now.time) <= if (BuildConfig.DEBUG) 30 * 1000 else 300 * 1000 // 300 sec = 5 min
  }

  fun <T, K, R> LiveData<T>.combine(other: LiveData<K>, block: (T?, K?) -> R): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
      result.value = block(this.value, other.value)
    }
    result.addSource(other) {
      result.value = block(this.value, other.value)
    }
    return result
  }

  fun randomBleRecordGenerator(): ByteArray {
    val prefix = listOf(0x02, 0x01, 0x04, 0x1A, 0xFF.toByte(),0x00, 0x4C, 0x02, 0x15)
    val uuid = listOf(
      0x00, 0x05, 0x00, 0x01,
      0x00, 0x00, 0x10, 0x00,
      0x80.toByte(), 0x00, 0x00, 0x80.toByte(),
      0x5F, 0x9B.toByte(), 0x01, 0x31) // length = 16
    val major = listOf<Byte>(0x00, 0x01)
    val mockData1 = listOf(((Math.random() * 50).toInt() + 100).toByte())
    val mockData2 = listOf(((Math.random() * 50).toInt() + 195).toByte())
    val mockRssi = listOf((-(Math.random() * 30).toInt() - 50).toByte()) // -50 dbm ~ -80 dbm
    return prefix.plus(uuid).plus(major).plus(mockData1).plus(mockData2).plus(mockRssi).toByteArray()
  }
}