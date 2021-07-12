package com.whoissio.arthings.src.models

import java.util.*

typealias RssiTimeStamp = Map<Long, Int>
typealias ARCoord = Triple<Float, Float, Float>

enum class ChartMode {
  RAW,
  SMOOTH,
}

data class CachedData<C>(
  val data: C,
  val cachedAt: Date = Date()
)

data class BaseEvent<T> (val data: T, var isHandled: Boolean = false) {
  fun get(): T? {
    if (isHandled) return null
    isHandled = true
    return data
  }

  fun getRaw(): T {
    return data
  }
}