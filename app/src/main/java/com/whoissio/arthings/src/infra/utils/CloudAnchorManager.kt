package com.whoissio.arthings.src.infra.utils

import com.google.ar.core.Anchor
import com.google.ar.core.Session
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
class CloudAnchorManager @Inject constructor() {

  fun interface OnCompleteListener {
    fun onComplete(anchor: Anchor)
  }

  private val pendingAnchors: MutableMap<Anchor, OnCompleteListener> = HashMap()

  fun hostCloudAnchor(session: Session?, anchor: Anchor, listener: OnCompleteListener) {
    session ?: return
    val newAnchor = session.hostCloudAnchorWithTtl(anchor, 1)
    pendingAnchors[newAnchor] = listener
  }

   fun resolveCloudAnchor(session: Session?, anchorId: String, listener: OnCompleteListener) {
     session ?: return
     val newAnchor = session.resolveCloudAnchor(anchorId)
     pendingAnchors[newAnchor] = listener
  }
  
  fun onUpdate() {
    val toRemove = mutableListOf<Anchor>()
    pendingAnchors.forEach { (key, value) ->
      if (!listOf(Anchor.CloudAnchorState.TASK_IN_PROGRESS, Anchor.CloudAnchorState.NONE).contains(key.cloudAnchorState)) {
        value.onComplete(key)
        toRemove.add(key)
      }
    }
    toRemove.forEach { pendingAnchors.remove(it) }
  }

  fun clear() = pendingAnchors.clear()
}