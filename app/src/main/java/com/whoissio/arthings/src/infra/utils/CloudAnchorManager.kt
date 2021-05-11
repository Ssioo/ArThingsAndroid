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
  private val pendingAnchors: MutableMap<Anchor, (Anchor) -> Unit> = HashMap()

  fun hostCloudAnchor(session: Session?, anchor: Anchor, listener: (Anchor) -> Unit) {
    session ?: return
    val newAnchor = session.hostCloudAnchorWithTtl(anchor, 1)
    pendingAnchors[newAnchor] = listener
  }

   fun resolveCloudAnchor(session: Session?, anchorId: String, listener: (Anchor) -> Unit) {
     session?: return
     val newAnchor = session.resolveCloudAnchor(anchorId)
     pendingAnchors[newAnchor] = listener
  }
  
  fun onUpdate() {
    pendingAnchors.forEach { (key, value) ->
      if (!listOf(Anchor.CloudAnchorState.TASK_IN_PROGRESS, Anchor.CloudAnchorState.NONE).contains(key.cloudAnchorState)) {
        value(key)
        pendingAnchors.remove(key)
      }
    }
  }

  fun clear() {
    pendingAnchors.clear()
  }
}