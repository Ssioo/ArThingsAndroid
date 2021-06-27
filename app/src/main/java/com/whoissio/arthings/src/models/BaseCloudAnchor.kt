package com.whoissio.arthings.src.models

data class BaseCloudAnchor<T>(
  val id: String,
  val createdAt: String,
  val lifetime: Int,
  val room: Int,
  val data: T,
)