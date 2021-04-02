package com.whoissio.arthings.src.models

import com.google.ar.core.Anchor

data class ColoredAnchor(
  val anchor: Anchor,
  val color: FloatArray,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ColoredAnchor

    if (anchor != other.anchor) return false
    if (!color.contentEquals(other.color)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = anchor.hashCode()
    result = 31 * result + color.contentHashCode()
    return result
  }
}
