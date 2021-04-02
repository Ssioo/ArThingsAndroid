package com.whoissio.arthings.src.infra

import android.view.Surface
import com.google.ar.core.Pose
import kotlin.math.pow
import kotlin.math.sqrt

object Converters {

  fun Int.toDegrees(): Int {
    return when (this) {
      Surface.ROTATION_0 -> 0
      Surface.ROTATION_90 -> 90
      Surface.ROTATION_180 -> 180
      Surface.ROTATION_270 -> 270
      else -> throw java.lang.RuntimeException("Unknown rotation $this")
    }
  }

  fun Pose.distanceBetween(other: Pose) =
    sqrt((tx() - other.tx()).pow(2f) + (ty() - other.ty()).pow(2f) + (tz() - other.tz()).pow(2f))
}