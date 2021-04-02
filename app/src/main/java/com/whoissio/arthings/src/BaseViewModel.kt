package com.whoissio.arthings.src

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class BaseViewModel(application: Application) : AndroidViewModel(application) {
  protected val disposable = CompositeDisposable()

  override fun onCleared() {
    if (!disposable.isDisposed) disposable.clear()
    super.onCleared()
  }
}