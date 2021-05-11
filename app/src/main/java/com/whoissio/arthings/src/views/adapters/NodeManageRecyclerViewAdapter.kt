package com.whoissio.arthings.src.views.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import com.orhanobut.logger.Logger
import com.whoissio.arthings.databinding.ItemRowNodeBinding
import com.whoissio.arthings.src.models.CloudAnchor
import com.whoissio.arthings.src.views.adapters.item.BaseItem
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class NodeManageRecyclerViewAdapter @Inject constructor(): BaseRecyclerViewAdapter<CloudAnchor, ItemRowNodeBinding>() {
  override val bindingProvider: (LayoutInflater, ViewGroup?, Boolean) -> ItemRowNodeBinding = ItemRowNodeBinding::inflate

  override val controller: (CloudAnchor) -> BaseItem<CloudAnchor> = { NodeManageItem(it) }

  inner class NodeManageItem(override val data: CloudAnchor): BaseItem<CloudAnchor> {
    fun onClickNodeId() {
      Logger.d(data)
    }
  }
}