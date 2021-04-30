package com.whoissio.arthings.src.viewmodels

import androidx.lifecycle.MutableLiveData
import com.orhanobut.logger.Logger
import com.whoissio.arthings.src.BaseViewModel
import com.whoissio.arthings.src.models.CloudedAnchor
import com.whoissio.arthings.src.repositories.CloudedAnchorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.kotlin.addTo
import javax.inject.Inject

@HiltViewModel
class NodeManageViewModel @Inject constructor(private val cloudAnchorRepo: CloudedAnchorRepository) : BaseViewModel() {

  val cloudedAnchors: MutableLiveData<List<CloudedAnchor>> = MutableLiveData(listOf())

  init {
    cloudAnchorRepo.loadData()
      .subscribe({
        Logger.d(it)
        this.cloudedAnchors.value = it
      }, {
        onException(it)
      })
      .addTo(disposable)
  }
}