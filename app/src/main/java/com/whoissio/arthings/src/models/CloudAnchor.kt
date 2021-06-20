package com.whoissio.arthings.src.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class CloudAnchor(
  @SerializedName("id") val id: String = "",
  @SerializedName("address") val address: String = "",
  @SerializedName("createdAt") val createdAt: String = "",
  @SerializedName("room") val room: Int = 0,
  @SerializedName("type") val type: String = "SOLAR",
  @SerializedName("data") val data: List<CloudAnchorNodeData> = listOf(),
): Parcelable

@Parcelize
data class CloudAnchorNodeData(
  val name: String = "",
  val byteIdx: Int = 0,
  val function: String = "",
): Parcelable
