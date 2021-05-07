package com.whoissio.arthings.src.infra.utils

import com.google.ar.core.Anchor
import com.google.ar.core.Session

class CloudAnchorManager (private val session: Session) {

  private val pendingAnchors: MutableMap<Anchor, (Anchor) -> Unit> = HashMap()

  fun hostCloudAnchor(anchor: Anchor, listener: (Anchor) -> Unit) {
    val newAnchor = session.hostCloudAnchorWithTtl(anchor, 1)
    pendingAnchors[newAnchor] = listener
  }

   fun resolveCloudAnchor(anchorId: String, listener: (Anchor) -> Unit) {
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