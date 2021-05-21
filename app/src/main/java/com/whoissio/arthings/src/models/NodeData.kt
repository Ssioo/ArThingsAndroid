package com.whoissio.arthings.src.models

import java.lang.Exception
import java.util.*

data class NodeData(
  val bytes: ByteArray,
  val targetData: Pair<Double, (Double) -> (Double)>
) {
  fun parseCalculation(): Double {
    return targetData.second(targetData.first)
  }
}

data class NodeDataConverter(
  var initialDataGenerator: (ByteArray) -> (Double) = { 0.0 },
  var resultGenerator: (Double) -> Double = { 0.0 }
) {
  fun calc(bytes: ByteArray): Double {
    return resultGenerator(initialDataGenerator(bytes))
  }

  fun parseInitialDataGenerator(src: String): (ByteArray) -> Double {
    return {
      var result = 0.0
      var srcCopy = src.replace(" ", "")
      while (srcCopy.contains("")) {
        val acc = srcCopy.substringBefore("").toDouble()
        val where = srcCopy.substringAfter("").substringBefore("").toInt()
        result += acc * it[where].toInt()
        srcCopy = srcCopy.substringAfter("").substringAfter("")
      }
      result
    }
  }

  // src: y = 125x - 45
  fun parseResultGenerator(src: String): (Double) -> Double {
    return {
      val padded = src.replace(" ", " ")
      val acc: Double = padded.substringBefore("x").toDouble() * it
      val left = padded.substringAfter("x")
      if (left.isEmpty()) {
        acc
      }
      else {
        when(left.first()) {
          '+' -> acc + left.substringAfter("+").toDouble()
          '-' -> acc - left.substringAfter("-").toDouble()
          else -> throw Exception("Unexpected Operators")
        }
      }
    }
  }
}