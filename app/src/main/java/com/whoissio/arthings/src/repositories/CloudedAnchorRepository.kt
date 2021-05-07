package com.whoissio.arthings.src.repositories

import com.whoissio.arthings.src.apis.CloudAnchorDataSource
import com.whoissio.arthings.src.infra.Helper.isAvailable
import com.whoissio.arthings.src.models.CachedData
import com.whoissio.arthings.src.models.CloudedAnchor
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
class CloudedAnchorRepository @Inject constructor(
  private val dataSource: CloudAnchorDataSource
) {
  var cachedAnchors: CachedData<List<CloudedAnchor>>? = null

  fun loadData(): Single<List<CloudedAnchor>> {
    if (cachedAnchors?.isAvailable() == true)
      return Single.just(cachedAnchors?.data ?: emptyList())
    return refreshData()
      .map {
        cachedAnchors = CachedData(data = it)
        it
      }
  }

  fun refreshData(): Single<List<CloudedAnchor>> {
    return dataSource.fetchCloudedAnchors()
  }
}