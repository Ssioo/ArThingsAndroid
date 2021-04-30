package com.whoissio.arthings.src.models

import java.util.*

typealias RssiTimeStamp = Map<Long, Int>

enum class ChartMode {
  RAW,
  SMOOTH,
}

data class CachedData<C>(
  val data: C,
  val cachedAt: Date = Date()
)