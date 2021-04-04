package com.whoissio.arthings.src.infra.utils

open class KalmanFilteredList(
  data: List<Number>,
  indices: List<Any> = listOf(),
  var tag: Any? = null,
  observer: ((List<Number>) -> Unit)? = null,
) : IKalmanFilter {
  val kalmanConstant = 0.6
  val smoothedData: ArrayList<Number> = arrayListOf()

  private val rawData: ArrayList<Number> = arrayListOf()
  private val subscribers: MutableList<(List<Number>) -> Unit> = mutableListOf()

  override val hasObserver get() = subscribers.isNotEmpty()

  override val size get() = rawData.size

  override fun append(element: Number, customIndex: Any?) {
    this.customIndices.add(customIndex)
    this.rawData.add(element)
    smoothNewItemThenAdd(element)
    notifyDataSetChanged(false)
  }

  override fun append(elements: List<Number>, customIndices: List<Any>) {
    this.customIndices.addAll(customIndices)
    this.rawData.addAll(elements)
    this.smoothedData.addAll(smoothAll())
    notifyDataSetChanged(false)
  }

  override fun clear() {
    this.customIndices.clear()
    this.rawData.clear()
    this.smoothedData.clear()
  }

  override fun removeAt(idx: Int): Boolean {
    if (idx !in 0 until size) return false
    rawData.removeAt(idx)
    refreshAllSmoothedData()
    notifyDataSetChanged(false)
    return true
  }

  override fun notifyDataSetChanged(isUserCalled: Boolean) {
    if (isUserCalled) refreshAllSmoothedData()
    subscribers.forEach { it(smoothedData) }
  }

  override fun smoothAll(): List<Number> {
    val result = arrayListOf<Number>()
    for (i in rawData.indices) {
      if (i == 0) {
        result.add(rawData[i])
        continue
      }
      result.add(rawData[i - 1].toDouble() * (1 - kalmanConstant) + rawData[i].toDouble() * kalmanConstant)
    }
    return result
  }

  override fun observe(observer: (List<Number>) -> Unit) {
    subscribers.add(observer)
    notifyDataSetChanged(false)
  }

  override fun unObserve(observer: (List<Number>) -> Unit) {
    subscribers.removeAll { it == observer }
  }

  override fun unObserve() = subscribers.clear()

  override fun dispose() {
    unObserve()
    clear()
  }

  private fun smoothNewItemThenAdd(element: Number) {
    smoothedData.add(
      when (rawData.size) {
        1 -> element
        else -> rawData.last().toDouble() * (1 - kalmanConstant) + element.toDouble() * kalmanConstant
      }
    )
  }

  private fun refreshAllSmoothedData() {
    smoothedData.clear()
    smoothedData.addAll(smoothAll())
  }

  init {
    this.append(data, indices)
    observer?.let { this.observe(it) }
  }
}

interface IKalmanFilter {
  val size: Int get() = 0
  val customIndices: ArrayList<Any?> get() = arrayListOf()

  val hasObserver: Boolean get() = false

  fun append(element: Number, customIndex: Any? = null)

  fun append(elements: List<Number>, customIndices: List<Any> = emptyList())

  fun removeAt(idx: Int): Boolean

  fun smoothAll(): List<Number>

  fun observe(observer: (List<Number>) -> Unit)

  fun unObserve(observer: (List<Number>) -> Unit)

  fun unObserve()

  fun clear()

  fun dispose()

  fun notifyDataSetChanged(isUserCalled: Boolean = true)
}