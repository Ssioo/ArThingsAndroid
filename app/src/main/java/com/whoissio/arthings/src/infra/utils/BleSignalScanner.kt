package com.whoissio.arthings.src.infra.utils

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject

class BleSignalScanner: ScanCallback(), IBleSignalScanner {

  private val disposable = CompositeDisposable()
  private val prov: PublishSubject<ScanResult> = PublishSubject.create()

  override fun onScanResult(callbackType: Int, result: ScanResult?) {
    result?.let { prov.onNext(it) }
  }

  override fun onBatchScanResults(results: MutableList<ScanResult>?) {
    results?.forEach { prov.onNext(it) }
  }

  override fun onScanFailed(errorCode: Int) {
    prov.onError(Error(when(errorCode) {
      SCAN_FAILED_ALREADY_STARTED -> "Already Started"
      SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App Registration Failed"
      SCAN_FAILED_FEATURE_UNSUPPORTED -> "Unsupported"
      SCAN_FAILED_INTERNAL_ERROR -> "Internal Error"
      else -> ""
    }))
  }

  override fun register(onReceive: (ScanResult) -> Unit, onError: ((Throwable) -> Unit)?) {
    if (disposable.size() > 0)
      disposable.clear()
    prov.subscribe(onReceive, { onError?.invoke(it) })
      .also { disposable.add(it) }
  }

  override fun onCleared() {
    if (!disposable.isDisposed) disposable.clear()
  }

  init {
    prov.subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
  }
}

interface IBleSignalScanner {
  fun onCleared()

  fun register(onReceive: (ScanResult) -> Unit, onError: ((Throwable) -> Unit)?)
}