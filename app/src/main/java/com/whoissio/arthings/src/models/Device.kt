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