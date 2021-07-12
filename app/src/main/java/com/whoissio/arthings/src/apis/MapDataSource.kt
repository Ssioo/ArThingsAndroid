package com.whoissio.arthings.src.apis

import android.media.Image
import com.whoissio.arthings.src.models.ARCoord
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
class MapDataSource @Inject constructor() {

  fun sendNewCameraFrame(image: Image) {
  }

  fun requestHarvCap(cameraPos: ARCoord, targetPos: ARCoord) {

  }
}