package com.whoissio.arthings.src

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class BaseViewModel : ViewModel() {
  protected val disposable = CompositeDisposable()

  override fun onCleared() {
    if (!disposable.isDisposed) disposable.clear()
    super.onCleared()
  }

  protected fun onException(e: Throwable) {
    e.printStackTrace()
  }
}