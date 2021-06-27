package com.whoissio.arthings.src.viewmodels

import androidx.lifecycle.MutableLiveData
import com.whoissio.arthings.src.BaseViewModel
import com.whoissio.arthings.src.models.CloudBleDevice
import com.whoissio.arthings.src.models.CloudBleDeviceData
import com.whoissio.arthings.src.repositories.CloudedAnchorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import javax.inject.Inject

@HiltViewModel
class NodeEditViewModel @Inject constructor(
  private val cloudAnchorRepo: CloudedAnchorRepository
): BaseViewModel() {

  val selectedCloudBleDevice: MutableLiveData<CloudBleDevice?> = MutableLiveData(null)

  fun setSelectedCloudAnchor(bleDevice: CloudBleDevice?) {
    selectedCloudBleDevice.value = bleDevice
  }

  fun clearSelectedCloudAnchor() {
    selectedCloudBleDevice.value = null
  }

  fun submit(address: String, data: List<CloudBleDeviceData>, onSuccess: () -> Unit) {
    if (selectedCloudBleDevice.value == null) {
      cloudAnchorRepo.registerAddress(address, data)
        .subscribe({
          onSuccess()
        }, {
          onException(it)
        }).addTo(disposable)
    } else {

    }
  }
}