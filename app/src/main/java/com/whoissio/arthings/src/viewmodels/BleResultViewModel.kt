package com.whoissio.arthings.src.viewmodels

import android.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.whoissio.arthings.src.BaseViewModel
import com.whoissio.arthings.src.infra.Constants.COLOR_SET
import com.whoissio.arthings.src.infra.Constants.SAMPLE_NODE_MAC_ADDRESS1
import com.whoissio.arthings.src.infra.utils.KalmanFilteredList
import com.whoissio.arthings.src.models.ChartMode
import com.whoissio.arthings.src.models.DeviceInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BleResultViewModel @Inject constructor()
  : BaseViewModel() {

  val scannedDevices: MutableLiveData<List<DeviceInfo>> = MutableLiveData()
  val devicesKalmaned = Transformations.map(this.scannedDevices) {
    it.map { it to KalmanFilteredList(it.data.map { it.value }, it.data.map { it.key }) }
  }
  val chartDataSet: MutableLiveData<LineData> = MutableLiveData()
  val kalManchartDataSet: MutableLiveData<LineData> = MutableLiveData()

  val chartMode: MutableLiveData<ChartMode> = MutableLiveData(ChartMode.RAW)

  fun toggleChartMode() {
    chartMode.value = when (chartMode.value) {
      ChartMode.RAW -> ChartMode.SMOOTH
      else -> ChartMode.RAW
    }
  }

  fun setScannedDevices(devices: List<DeviceInfo>) {
    this.scannedDevices.value = devices

    chartDataSet.value = LineData(
      devices.toList().mapIndexed { idx, it ->
        LineDataSet(
          it.data.map { Entry(it.key.toFloat(), it.value.toFloat()) },
          it.address
        ).apply {
          color = COLOR_SET[idx % COLOR_SET.size]
          setDrawCircles(false)
          mode = LineDataSet.Mode.HORIZONTAL_BEZIER
          if (it.address == SAMPLE_NODE_MAC_ADDRESS1) {
            lineWidth = 8f
            color = Color.BLACK
          }
        }
      })

    kalManchartDataSet.value = LineData(
      devices.toList()
        .map {
          KalmanFilteredList(it.data.map { it.value }, it.data.map { it.key }, it.address, null)
        }
        .mapIndexed { idx, it ->
          LineDataSet(
            it.smoothedData.mapIndexed { idx, smoothen ->
              Entry((it.customIndices[idx] as Number).toFloat(), smoothen.toFloat())
            },
            it.tag as? String ?: ""
          ).apply {
            color = COLOR_SET[idx % COLOR_SET.size]
            setDrawCircles(false)
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            if (it.tag == SAMPLE_NODE_MAC_ADDRESS1) {
              lineWidth = 8f
              color = Color.BLACK
            }
          }
        }
    )
  }

  init {

  }
}