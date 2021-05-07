package com.whoissio.arthings.src.apis

import com.whoissio.arthings.src.infra.ApiModule.retrofit
import com.whoissio.arthings.src.models.CloudedAnchor
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
class CloudAnchorDataSource @Inject constructor() {
  private val cloudAnchorApi = retrofit().create(CloudAnchorApi::class.java)

  fun fetchCloudedAnchors(): Single<List<CloudedAnchor>> {
    return cloudAnchorApi
      .getAnchors()
      .observeOn(AndroidSchedulers.mainThread())
      .map {
        it.anchors
      }
  }

  fun updateCloudedAnchor(id: String, mask: String): Completable {
    return cloudAnchorApi
      .updateAnchor(id, mask)
      .observeOn(AndroidSchedulers.mainThread())
      .flatMapCompletable {
        Completable.complete()
      }
  }

  fun deleteCloudedAnchor(id: String): Completable {
    return cloudAnchorApi
      .deleteAnchor(id)
      .observeOn(AndroidSchedulers.mainThread())
      .flatMapCompletable {
        Completable.complete()
      }
  }
}