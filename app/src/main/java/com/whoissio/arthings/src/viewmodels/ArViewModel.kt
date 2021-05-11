package com.whoissio.arthings.src.viewmodels

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.ar.core.Anchor
import com.orhanobut.logger.Logger
import com.whoissio.arthings.ApplicationClass.Companion.scanner
import com.whoissio.arthings.src.BaseViewModel
import com.whoissio.arthings.src.infra.Constants
import com.whoissio.arthings.src.infra.Helper.combine
import com.whoissio.arthings.src.infra.utils.BleSignalScanner
import com.whoissio.arthings.src.models.CloudAnchor
import com.whoissio.arthings.src.models.Device
import com.whoissio.arthings.src.models.RssiTimeStamp
import com.whoissio.arthings.src.repositories.CloudedAnchorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import javax.inject.Inject

@HiltViewModel
class ArViewModel @Inject constructor(
  private val cloudAnchorRepo: CloudedAnchorRepository
) : BaseViewModel() {

  @Inject lateinit var bleSignalScanner: BleSignalScanner
  var isScanning = false
  val scannedDevices: MutableLiveData<Map<Device, RssiTimeStamp>> = MutableLiveData(mapOf())

  val isDepthApiEnabled = MutableLiveData(false)
  val humidity: MutableLiveData<Double> = MutableLiveData()
  val temperature: MutableLiveData<Double> = MutableLiveData()
  val cloudedAnchors: MutableLiveData<List<CloudAnchor>> = MutableLiveData(emptyList())

  val closestScannedDevice = Transformations.map(scannedDevices) {
    it.map {
      it.key to it.value.toList().last().second
    }.sortedBy { it.second }
  }
    .combine(cloudedAnchors) { a, b ->
      a?.filter { b?.map { it.address }?.contains(it.first.address) == true }?.firstOrNull()
    }

  fun resumeScanBle() {
    if (isScanning) return
    bleSignalScanner.register({
      if (it.rssi < -80) return@register // 너무 낮은 데이터 필터링
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
          humidity.value = 125 * (it.getOrNull(27) ?: 0) * 256.0 / 65536 - 6
          temperature.value = 175.72 * (it.getOrNull(28) ?: 0) * 256 / 65536 - 46.85
          Logger.d("Humidity: $humidity% Temp: $temperature℃")
        }
      }
    }, null)
    scanner?.startScan(bleSignalScanner)
    isScanning = true
  }

  fun createNewHostAnchor(anchor: Anchor, address: String) {
    cloudAnchorRepo.createNewAnchorOnAddress(anchor, address)
      .subscribe({
        Logger.d("Created ${anchor.cloudAnchorId} to ${address}")
      }, {
        it.printStackTrace()
      })
      .addTo(disposable)
  }

  fun pauseScanBle() {
    if (!isScanning) return
    bleSignalScanner.register({}, null)
    scanner?.stopScan(bleSignalScanner)
    isScanning = false
  }

  override fun onCleared() {
    bleSignalScanner.onCleared()
    super.onCleared()
  }


  init {
    cloudAnchorRepo.loadData().subscribe({
      cloudedAnchors.value = it
    }, {
      it.printStackTrace()
    })
      .addTo(disposable)
  }
}