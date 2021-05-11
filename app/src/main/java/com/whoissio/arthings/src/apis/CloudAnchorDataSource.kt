package com.whoissio.arthings.src.apis

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.reflect.TypeToken
import com.orhanobut.logger.Logger
import com.whoissio.arthings.src.models.CloudAnchor
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

  val onUpdateAnchorList = object : ChildEventListener {
    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

    }

    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

    }

    override fun onChildRemoved(snapshot: DataSnapshot) {

    }

    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

    }

    override fun onCancelled(error: DatabaseError) {

    }
  }

  fun fetchCloudedAnchors(): Single<List<CloudAnchor>> {
    return Single.create { out ->
      anchorDb.get()
        .addOnSuccessListener {
          Logger.d(it)
          if (it.value == null) {
            out.onSuccess(emptyList())
            return@addOnSuccessListener
          }
          it.children
          out.onSuccess((it.value as Map<String, CloudAnchor>).map { it.value })
        }
        .addOnFailureListener { out.onError(it) }
    }
  }

  fun createNewCloudAnchor(id: String, address: String): Completable {
    return Completable.create { out ->
      anchorDb.child(address).updateChildren(mapOf("id" to id))
        .addOnSuccessListener { out.onComplete() }
        .addOnFailureListener { out.onError(it) }
    }
  }

  fun createNewAddress(address: String): Completable {
    return Completable.create { out ->
      anchorDb.child(address).setValue(CloudAnchor("000", address, Date().toString()))
        .addOnSuccessListener { out.onComplete() }
        .addOnFailureListener { out.onError(it) }
    }
  }

  fun deleteCloudAnchor(id: String): Completable {
    return Completable.create { out ->
      anchorDb.child(id).setValue(null)
        .addOnSuccessListener { out.onComplete() }
        .addOnFailureListener { out.onError(it) }
    }
  }

  init {
    anchorDb.addChildEventListener(onUpdateAnchorList)
  }
}