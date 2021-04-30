package com.whoissio.arthings.src.models

import com.google.gson.annotations.SerializedName

data class CloudedAnchorListResponse(
  @SerializedName("anchors") val anchors: List<CloudedAnchor>,
  @SerializedName("nextPageToken") val nextPageToken: String,
)

data class CloudedAnchor(
  @SerializedName("name") val name: String,
  @SerializedName("createdTime") val createdTime: String,
  @SerializedName("expiredTime") val expiredTime: String,
  @SerializedName("lastLocalizeTime") val lastLocalizeTime: String,
  @SerializedName("maximumExpireTime") val maximumExpireTime: String,
)