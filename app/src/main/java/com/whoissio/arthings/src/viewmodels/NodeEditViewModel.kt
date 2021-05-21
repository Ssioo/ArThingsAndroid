package com.whoissio.arthings.src.viewmodels

import androidx.lifecycle.MutableLiveData
import com.whoissio.arthings.src.BaseViewModel
import com.whoissio.arthings.src.models.CloudAnchor
import com.whoissio.arthings.src.models.CloudAnchorNodeData
import com.whoissio.arthings.src.repositories.CloudedAnchorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import javax.inject.Inject

@HiltViewModel
class NodeEditViewModel @Inject constructor(
  private val cloudAnchorRepo: CloudedAnchorRepository
): BaseViewModel() {

  val selectedCloudAnchor: MutableLiveData<CloudAnchor?> = MutableLiveData(null)

  fun setSelectedCloudAnchor(anchor: CloudAnchor?) {
    selectedCloudAnchor.value = anchor
  }

  fun clearSelectedCloudAnchor() {
    selectedCloudAnchor.value = null
  }

  fun submit(address: String, data: List<CloudAnchorNodeData>, onSuccess: () -> Unit) {
    if (selectedCloudAnchor.value == null) {
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