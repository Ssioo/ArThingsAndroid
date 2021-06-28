package com.whoissio.arthings.src.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Device(
  val address: String,
  val txPower: Int,
) : Parcelable

@Parcelize
data class DeviceInfo(
  val address: String,
  val pow: Int,
  val data: RssiTimeStamp
): Parcelable


@Parcelize
data class DeviceNode(
  val id: Int,
  val macAddress: String,
  val harvestingType: HarvestingType,
  val sensingType: SensingType,
  val status: DeviceStatus
): Parcelable

enum class HarvestingType {
  SOLAR, RF, ETC
}

enum class SensingType {
  TEMP, HUMID
}

enum class DeviceStatus {
  NOT_REGISTERED,
  NORMAL,
  ABNORMAL_LOW_DUTY_CYCLE,
  ABNORMAL_NOT_WAKE_UP_LONG,
  ABNORMAL_UNEXPECTED_DATA
}