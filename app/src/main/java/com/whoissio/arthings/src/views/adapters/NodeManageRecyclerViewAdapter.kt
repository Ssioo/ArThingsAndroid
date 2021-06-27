package com.whoissio.arthings.src.views.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import com.orhanobut.logger.Logger
import com.whoissio.arthings.databinding.ItemRowNodeBinding
import com.whoissio.arthings.src.models.CloudBleDevice
import com.whoissio.arthings.src.views.NodeEditActivity
import com.whoissio.arthings.src.views.NodeManageActivity
import com.whoissio.arthings.src.views.adapters.item.BaseItem
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class NodeManageRecyclerViewAdapter @Inject constructor(@ActivityContext private val context: Context) :
  BaseRecyclerViewAdapter<CloudBleDevice, ItemRowNodeBinding>() {
  override val bindingProvider: (LayoutInflater, ViewGroup?, Boolean) -> ItemRowNodeBinding =
    ItemRowNodeBinding::inflate

  override val controller: (CloudBleDevice) -> BaseItem<CloudBleDevice> = { NodeManageItem(it) }

  inner class NodeManageItem(override val data: CloudBleDevice) : BaseItem<CloudBleDevice> {
    fun onClickItem() {
      Logger.d(data)
      (context as? NodeManageActivity)?.let {
        it.startActivityForResult(Intent(it, NodeEditActivity::class.java).apply { putExtra("anchor", this@NodeManageItem.data) }, 100)
      }
    }
  }
}