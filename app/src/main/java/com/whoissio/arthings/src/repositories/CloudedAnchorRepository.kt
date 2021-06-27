package com.whoissio.arthings.src.repositories

import com.google.ar.core.Anchor
import com.whoissio.arthings.src.apis.CloudAnchorDataSource
import com.whoissio.arthings.src.infra.Constants.DATE_FORMAT
import com.whoissio.arthings.src.infra.Helper.isAvailable
import com.whoissio.arthings.src.models.CachedData
import com.whoissio.arthings.src.models.CloudBleDevice
import com.whoissio.arthings.src.models.CloudBleDeviceData
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@Module
@InstallIn(SingletonComponent::class)
class CloudedAnchorRepository @Inject constructor(
  private val dataSource: CloudAnchorDataSource
) {
  var cachedAnchors: CachedData<ArrayList<CloudBleDevice>>? = null

  fun loadData(): Single<List<CloudBleDevice>> {
    if (cachedAnchors?.isAvailable() == true)
      return Single.just(cachedAnchors?.data ?: emptyList())
    return refreshData()
  }

  fun refreshData(): Single<List<CloudBleDevice>> {
    return dataSource.fetchCloudedAnchors()
      .map {
        cachedAnchors = CachedData(data = ArrayList(it))
        it
      }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
  }

  fun createNewAnchorOnAddress(anchor: Anchor, address: String, room: Int, type: String): Completable {
    return dataSource.createNewCloudAnchor(anchor.cloudAnchorId, address, room, type)
      .andThen {
        cachedAnchors?.data?.add(CloudBleDevice(anchor.cloudAnchorId, address, DATE_FORMAT.format(Date())))
        Completable.complete()
      }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
  }

  fun registerAddress(address: String, data: List<CloudBleDeviceData>): Completable {
    return dataSource.createNewAddress(address, data)
      .andThen(refreshData())
      .flatMapCompletable { Completable.complete() }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
  }
}