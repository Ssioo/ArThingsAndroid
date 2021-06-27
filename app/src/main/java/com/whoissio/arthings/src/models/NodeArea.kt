package com.whoissio.arthings.src.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.lang.Exception
import java.util.*
import java.util.stream.Stream

data class NodeArea(
  val id: String,
  val pxX: Float,
  val pxY: Float,
  val radius: Float,
  val type: Int,
)

@Parcelize
data class CloudNodeArea(
  val radius: Float,
  val type: Int,
): Parcelable

@Parcelize
data class AreaNodeRelation(
  val nodeId: String,
  val areaId: String,
  val percentage: Double,
): Parcelable