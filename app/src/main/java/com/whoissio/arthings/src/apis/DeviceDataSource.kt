package com.whoissio.arthings.src.apis

import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.whoissio.arthings.src.models.CloudBleDevice
import com.whoissio.arthings.src.models.CloudBleDeviceData
import com.whoissio.arthings.src.models.DeviceStatus
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.core.Completable
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
class DeviceDataSource @Inject constructor() {
  private val devicesDb = Firebase.database.getReference("devices")

  fun registerNewCandidateDevice(address: String, type: String, data: List<CloudBleDeviceData>, status: DeviceStatus): Completable {
    return Completable.create { out ->
      devicesDb.child(address)
        .setValue(CloudBleDevice(address, type, data, status))
        .addOnSuccessListener { out.onComplete() }
        .addOnFailureListener { out.onError(it) }
    }
  }
}