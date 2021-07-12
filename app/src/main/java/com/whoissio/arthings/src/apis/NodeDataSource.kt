package com.whoissio.arthings.src.apis

import com.whoissio.arthings.src.models.ARCoord
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
class NodeDataSource @Inject constructor() {

  fun registerNode(hostId: String, cameraPos: ARCoord, nodePos: ARCoord) {

  }

  fun moveNode(hostId: String, cameraPos: ARCoord, newPos: ARCoord) {

  }

  fun deleteNode(hostId: String) {

  }

  fun requestHarvCap(hostId: String) {

  }
}