package com.whoissio.arthings.src.apis

import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.whoissio.arthings.src.infra.Constants.DATE_FORMAT
import com.whoissio.arthings.src.models.BaseCloudAnchor
import com.whoissio.arthings.src.models.CloudBleDevice
import com.whoissio.arthings.src.models.CloudBleDeviceData
import com.whoissio.arthings.src.models.DeviceStatus
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import java.util.*
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
class CloudAnchorDataSource @Inject constructor() {

  private val anchorDb = Firebase.database.getReference("anchors")
  private val devicesDb = Firebase.database.getReference("devices")

  fun fetchCloudedAnchors(): Single<List<BaseCloudAnchor<CloudBleDevice>>> {
    return Single.create { out ->
      anchorDb.get()
        .addOnSuccessListener {
          out.onSuccess(if (it.value == null) emptyList() else it.children.mapNotNull { it.getValue<BaseCloudAnchor<CloudBleDevice>>() })
        }
        .addOnFailureListener { out.onError(it) }
    }
  }

  fun createNewCloudAnchor(id: String, address: String, room: Int, type: String): Completable {
    return Completable.create { out ->
      anchorDb.child(address).get()
        .addOnSuccessListener {
          if (!it.exists()) {
            out.onError(Throwable("No address exists"))
            return@addOnSuccessListener
          }
          if (it.hasChild(id) && (it.value as? Map<String, Any?>)?.get("id") != "") {
            out.onError(Throwable("Duplicate Anchor exists"))
            return@addOnSuccessListener
          }
          anchorDb.child(address).child("data")
            .updateChildren(mapOf("id" to id, "room" to room, "type" to type))
            .addOnSuccessListener { out.onComplete() }
            .addOnFailureListener { out.onError(it) }
        }
        .addOnFailureListener {
          out.onError(it)
        }
    }
  }

  fun createNewAddress(address: String, data: List<CloudBleDeviceData>): Completable {
    return Completable.create { out ->
      anchorDb.child(address)
        .setValue(CloudBleDevice(address = address, createdAt = DATE_FORMAT.format(Date()), data = data))
        .addOnSuccessListener { out.onComplete() }
        .addOnFailureListener { out.onError(it) }
    }
  }

  fun registerNewCloudedDevice(id: String, address: String, room: Int, type: String) {
    Completable.create { out ->
      val newData = BaseCloudAnchor(
        id = id,
        createdAt = DATE_FORMAT.format(Date()),
        lifetime = 1,
        room = room,
        data = CloudBleDevice(
          address = address,
          type = type,
          data = listOf(),
          status = DeviceStatus.GOOD,
        )
      )
      anchorDb.child(id)
        .setValue(newData)
        .addOnSuccessListener { out.onComplete() }
        .addOnFailureListener { out.onError(it) }
    }
  }



  fun deleteCloudAnchor(address: String): Completable {
    return Completable.create { out ->
      anchorDb.child(address)
        .setValue(null)
        .addOnSuccessListener { out.onComplete() }
        .addOnFailureListener { out.onError(it) }
    }
  }

  init {
  }
}