package com.whoissio.arthings.src.viewmodels

import com.whoissio.arthings.src.BaseViewModel
import com.whoissio.arthings.src.repositories.CloudedAnchorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import javax.inject.Inject

@HiltViewModel
class NodeEditViewModel @Inject constructor(
  private val cloudAnchorRepo: CloudedAnchorRepository
): BaseViewModel() {

  fun submit(address: String, onSuccess: () -> Unit) {
    cloudAnchorRepo.registerAddress(address)
      .subscribe({
        onSuccess()
      }, {
        onException(it)
      }).addTo(disposable)
  }
}