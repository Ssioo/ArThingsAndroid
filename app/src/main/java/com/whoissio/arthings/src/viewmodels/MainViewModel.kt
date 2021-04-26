package com.whoissio.arthings.src.viewmodels

import android.app.Application
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import androidx.lifecycle.MutableLiveData
import com.google.ar.core.Anchor
import com.orhanobut.logger.Logger
import com.whoissio.arthings.ApplicationClass.Companion.scanner
import com.whoissio.arthings.src.BaseViewModel
import com.whoissio.arthings.src.infra.Constants
import com.whoissio.arthings.src.infra.utils.BleSignalScanner
import com.whoissio.arthings.src.models.ColoredAnchor
import com.whoissio.arthings.src.models.Device
import com.whoissio.arthings.src.models.RssiTimeStamp
import java.util.concurrent.atomic.AtomicBoolean

class MainViewModel(application: Application) : BaseViewModel(application) {

  /* Variables for Bluetooth */
  val bleSignalScanner: BleSignalScanner = BleSignalScanner()
  var isScanning = false
  val scannedDevices: MutableLiveData<Map<Device, RssiTimeStamp>> = MutableLiveData(mapOf())
  val isDepthApiEnabled = MutableLiveData(false)

  fun resumeScanBle() {
    if (isScanning) return
    bleSignalScanner.register({
      val curList = (scannedDevices.value ?: mapOf()).toMutableMap()
      val txPower = when (it.scanRecord?.txPowerLevel) {
        Int.MIN_VALUE -> {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) it.txPower
          else 127
        }
        else -> it.scanRecord?.txPowerLevel ?: 127
      }
      val newDevice = Device(it.device.address, txPower)
      curList.putIfAbsent(newDevice, mapOf())
      curList[newDevice] = (curList[newDevice]?.toMutableMap() ?: mutableMapOf()).apply {
        set(it.timestampNanos, it.rssi)
      }
      scannedDevices.value = curList

      // For Debug
      if (it.device.address == Constants.SAMPLE_NODE_MAC_ADDRESS) {
        it.scanRecord?.bytes?.let {
          val humidity = 125 * (it.getOrNull(27) ?: 0) * 256.0 / 65536 - 6
          val temperature = 175.72 * (it.getOrNull(28) ?: 0) * 256 / 65536 - 46.85
          Logger.d("Humidity: $humidity% Temp: $temperature℃")
        }
      }
    }, null)
    scanner?.startScan(bleSignalScanner)
    isScanning = true
  }

  fun pauseScanBle() {
    if (!isScanning) return
    bleSignalScanner.register({}, null)
    scanner?.stopScan(bleSignalScanner)
    isScanning = false
  }

  /* Variables for Camera */
  var keysThatCanCauseCaptureDelaysWhenModified = mutableListOf<CaptureRequest.Key<*>>()
  val anchors: ArrayList<Anchor> = arrayListOf()
  val shouldUpdateSurfaceTexture = AtomicBoolean(false)
  var cameraDevice: CameraDevice? = null

  val cameraCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
    override fun onCaptureCompleted(
      session: CameraCaptureSession,
      request: CaptureRequest,
      result: TotalCaptureResult
    ) {
      shouldUpdateSurfaceTexture.set(true)
    }
  }

  fun releaseCameraDevice() {
    cameraDevice?.close()
    cameraDevice = null
  }

  override fun onCleared() {
    bleSignalScanner.onCleared()
    super.onCleared()
  }
}