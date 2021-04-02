package com.whoissio.arthings.src.views

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.data.LineData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityBleResultBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.models.Device
import com.whoissio.arthings.src.models.DeviceInfo
import com.whoissio.arthings.src.models.RssiTimeStamp
import com.whoissio.arthings.src.viewmodels.BleResultViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.*

class BleResultActivity :
  BaseActivity<ActivityBleResultBinding, BleResultViewModel>(R.layout.activity_ble_result) {
  override fun initView(savedInstanceState: Bundle?) {
    binding.btnExport.setOnClickListener { onClickExport() }
    binding.btnRefresh.setOnClickListener { refreshChartData(vm.chartDataSet.value) }

    /* Init ChartView */
    binding.bleResultChart.apply {

    }

    /* Data Observing */
    vm.chartDataSet.observe(this) { refreshChartData(it) }
  }

  private fun refreshChartData(it: LineData?) {
    binding.bleResultChart.apply {
      data = it
      invalidate()
    }
  }

  private fun onClickExport() {
    try {
      val bitmap = binding.bleResultChart.chartBitmap
      val dir = File(filesDir, "image")
      dir.mkdirs()
      val file = File("$filesDir/image", "${Date().time}.png")
      file.createNewFile()
      val stream = FileOutputStream(file)
      bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
      stream.close()
      val uri = FileProvider.getUriForFile(this, "com.whoissio.arthings.fileprovider", file)
      startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        type = "image/png"
      }, null))
    } catch (e: Exception) {
      e.printStackTrace()
    }
    /*val exportTxt = vm.scannedDevices.value?.map {
      "${it.address}, Pow: ${it.pow}, Rssi: ${it.data}"
    }?.joinToString("\n") ?: "Error while parsing scannedDevice Data"
    startActivity(
      Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TITLE, "${vm.scannedDevices.value?.size}개의 스캔된 디바이스 데이터 전송")
        putExtra(Intent.EXTRA_TEXT, exportTxt)
        type = "text/plain"
      }, null)
    )*/
  }

  override fun getViewModel(): BleResultViewModel = ViewModelProvider(
    this,
    BleResultViewModelFactory(application, intent.getStringExtra("Data"))
  ).get(BleResultViewModel::class.java)

  inner class BleResultViewModelFactory(
    private val application: Application,
    private val data: String?
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
      BleResultViewModel(
        application,
        Gson().fromJson(data ?: "{}", object : TypeToken<List<DeviceInfo>>() {}.type)
      ) as T
  }
}