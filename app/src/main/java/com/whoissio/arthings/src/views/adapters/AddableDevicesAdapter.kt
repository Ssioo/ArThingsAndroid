package com.whoissio.arthings.src.views.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.whoissio.arthings.databinding.ItemAddableNodeBinding
import com.whoissio.arthings.src.models.DeviceNode
import com.whoissio.arthings.src.views.adapters.item.BaseItem
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class AddableDevicesAdapter @Inject constructor(@ActivityContext private val context: Context):
  BaseRecyclerViewAdapter<DeviceNode, ItemAddableNodeBinding>() {
  override val bindingProvider: (LayoutInflater, ViewGroup?, Boolean) -> ItemAddableNodeBinding = ItemAddableNodeBinding::inflate

  override val controller: (DeviceNode) -> BaseItem<DeviceNode> = { DeviceNodeController(it) }


  inner class DeviceNodeController(override val data: DeviceNode) : BaseItem<DeviceNode> {
    fun onClick() {

    }
  }
}