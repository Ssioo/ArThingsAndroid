package com.whoissio.arthings

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.content.SharedPreferences
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.LogStrategy
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ApplicationClass: Application() {

  companion object {
    var bleManager: BluetoothManager? = null
    var adapter: BluetoothAdapter? = null
    var scanner: BluetoothLeScanner? = null
    lateinit var sharedPref: SharedPreferences
  }

  private val loggerAdapter = object: AndroidLogAdapter(
    PrettyFormatStrategy.newBuilder()
      .showThreadInfo(false)
      .methodCount(0)
      .methodOffset(7)
      .build()) {
    override fun isLoggable(priority: Int, tag: String?): Boolean = BuildConfig.DEBUG
  }

  override fun onCreate() {
    super.onCreate()
    sharedPref = getSharedPreferences("APP_TAG", MODE_PRIVATE)
    bleManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
    adapter = bleManager?.adapter
    scanner = adapter?.bluetoothLeScanner

    Logger.addLogAdapter(loggerAdapter)
  }
}