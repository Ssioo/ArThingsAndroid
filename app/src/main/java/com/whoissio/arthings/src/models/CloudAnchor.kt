package com.whoissio.arthings.src.models

import com.google.gson.annotations.SerializedName


data class CloudAnchor(
  @SerializedName("id") val id: String,
  @SerializedName("address") val address: String,
  @SerializedName("createdAt") val createdAt: String,
)