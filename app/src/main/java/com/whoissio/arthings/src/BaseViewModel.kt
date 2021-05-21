package com.whoissio.arthings.src

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.whoissio.arthings.src.models.BaseEvent
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class BaseViewModel : ViewModel() {
  protected val disposable = CompositeDisposable()
  val toastEvent: MutableLiveData<BaseEvent<String>> = MutableLiveData()
  val alertEvent: MutableLiveData<BaseEvent<String>> = MutableLiveData()

  override fun onCleared() {
    if (!disposable.isDisposed) disposable.clear()
    super.onCleared()
  }

  protected fun onException(e: Throwable) {
    e.printStackTrace()
  }
}