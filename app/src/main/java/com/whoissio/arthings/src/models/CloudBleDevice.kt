package com.whoissio.arthings.src.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CloudBleDevice(
  val address: String,
  val type: String,
  val data: List<CloudBleDeviceData>,
  val status: DeviceStatus,
): Parcelable

@Parcelize
data class CloudBleDeviceData(
  val name: String = "",
  val byteIdx: Int = 0,
  val function: String = "",
): Parcelable
