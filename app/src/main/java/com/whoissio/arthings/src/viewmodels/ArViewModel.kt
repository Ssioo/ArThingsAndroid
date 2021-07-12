package com.whoissio.arthings.src.viewmodels

import android.annotation.SuppressLint
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.google.ar.core.Anchor
import com.orhanobut.logger.Logger
import com.whoissio.arthings.ApplicationClass.Companion.scanner
import com.whoissio.arthings.src.BaseViewModel
import com.whoissio.arthings.src.infra.Constants
import com.whoissio.arthings.src.infra.Constants.SAMPLE_NODE_ARRAY
import com.whoissio.arthings.src.infra.Converters
import com.whoissio.arthings.src.infra.Helper.combine
import com.whoissio.arthings.src.infra.Helper.randomBleRecordGenerator
import com.whoissio.arthings.src.infra.core.MockFunction
import com.whoissio.arthings.src.infra.utils.BleSignalScanner
import com.whoissio.arthings.src.models.*
import com.whoissio.arthings.src.repositories.CloudedAnchorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class ArViewModel @Inject constructor(
  private val cloudAnchorRepo: CloudedAnchorRepository
) : BaseViewModel() {

  val isAddableOpen = MutableLiveData(false)

  @Inject lateinit var bleSignalScanner: BleSignalScanner
  val isScanning = AtomicBoolean(false)
  val scannedDevices: MutableLiveData<Map<Device, RssiTimeStamp>> = MutableLiveData(mapOf())

  val isDepthApiEnabled = MutableLiveData(false)
  val cloudedAnchors: MutableLiveData<List<CloudAnchor>> = MutableLiveData(emptyList())

  val closestScannedDevice: LiveData<Pair<Device, Int>?> = Transformations.map(scannedDevices) {
    it.map {
      it.key to it.value.toList().last().second
    }.sortedBy { it.second }
  }
    .combine(cloudedAnchors) { a, b ->
      a?.filter { b?.map { it.address }?.contains(it.first.address) == true }?.firstOrNull()
    }

  val scannedDevicesData: MutableLiveData<Map<Device, ByteArray>> = MutableLiveData(mapOf())

  val scannedDevicesDataSortedByDistance = Transformations.map(scannedDevicesData) {
    it.toList().sortedByDescending { it.second.last().toInt() }
  }

  val notUploadedBleData = scannedDevicesDataSortedByDistance.combine(cloudedAnchors) { src, indices ->
    src?.filter { indices?.filter { it.id.isNotEmpty() }?.map { it.address }?.contains(it.first.address) == false } ?: listOf()
  }

  val selectedDeviceNode: MutableLiveData<DeviceNode?> = MutableLiveData(null)

  @SuppressLint("NewApi")
  fun resumeScanBle() {
    if (isScanning.get()) return
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
      Logger.d(listOf(txPower, it.scanRecord?.txPowerLevel, it.txPower))
      if (!SAMPLE_NODE_ARRAY.contains(it.device.address)) return@register // 목록에 없는 디바이스 필터링
      val newDevice = Device(it.device.address, txPower)
      curList.putIfAbsent(newDevice, mapOf())
      curList[newDevice] = (curList[newDevice]?.toMutableMap() ?: mutableMapOf()).apply {
        set(it.timestampNanos, it.rssi)
      }
      val curDataList = scannedDevicesData.value?.toMutableMap() ?: mutableMapOf()
      curDataList[Device(it.device.address, txPower)] = it.scanRecord?.bytes ?: return@register
      scannedDevicesData.value = curDataList
      scannedDevices.value = curList
    }, null)
    scanner?.startScan(bleSignalScanner)
    isScanning.set(true)
  }

  fun createNewHostAnchor(anchor: Anchor, address: String, room: Int, type: String) {
    cloudAnchorRepo.createNewAnchorOnAddress(anchor, address, room, type)
      .subscribe({
        Logger.d("Created ${anchor.cloudAnchorId} to $address in Room$room")
        loadCloudAnchors()
      }, {
        onException(it)
        alertEvent.value = BaseEvent(data = "등록에 실패했습니다. BLE 리스트에 먼저 등록해주세요.")
      })
      .addTo(disposable)
  }

  fun pauseScanBle() {
    if (!isScanning.get()) return
    bleSignalScanner.register({}, null)
    scanner?.stopScan(bleSignalScanner)
    isScanning.set(false)
  }

  @MockFunction
  fun refreshData() {
    val currentDeviceList = scannedDevices.value?.toMutableMap() ?: mutableMapOf()
    val newTestData = SAMPLE_NODE_ARRAY.map { System.currentTimeMillis() to -(Math.random() * 10).toInt() / 10 - 56 }
    val prev = currentDeviceList.filter { SAMPLE_NODE_ARRAY.contains(it.key.address) }.toMutableMap()
    val currentDeviceDataList = scannedDevicesData.value?.toMutableMap() ?: mutableMapOf()

    SAMPLE_NODE_ARRAY.forEachIndexed { idx, addr ->
      prev.filter { it.key.address == addr }.toList().firstOrNull()?.let {
        currentDeviceList[it.first] = it.second.toMutableMap().plus(newTestData[idx])
        currentDeviceDataList[it.first] = randomBleRecordGenerator()
      } ?: kotlin.run {
        Device(address = addr, txPower = 3).let {
          currentDeviceList[it] = mapOf(newTestData[idx])
          currentDeviceDataList[it] = randomBleRecordGenerator()
        }
      }
    }
    scannedDevices.value = currentDeviceList
    scannedDevicesData.value = currentDeviceDataList
  }

  fun refreshDataReal() {

  }

  override fun onCleared() {
    bleSignalScanner.onCleared()
    super.onCleared()
  }

  fun loadCloudAnchors() {
    cloudAnchorRepo.loadData().subscribe({
      cloudedAnchors.value = it
    }, {
      onException(it)
    })
      .addTo(disposable)
  }

  init {
    loadCloudAnchors()
  }
}