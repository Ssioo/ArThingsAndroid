package com.whoissio.arthings.src.infra

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.whoissio.arthings.R
import com.whoissio.arthings.src.models.DeviceNode
import com.whoissio.arthings.src.models.DeviceStatus
import com.whoissio.arthings.src.models.HarvestingType

object BindingAdapters {

  @JvmStatic
  @BindingAdapter("device_node")
  fun deviceToNodeItem(view: ImageView, item: DeviceNode) {
    view.setBackgroundResource(when (item.status) {
      DeviceStatus.NORMAL -> R.drawable.bg_ring_green
      DeviceStatus.NOT_REGISTERED -> R.drawable.bg_ring_grey
      else -> R.drawable.bg_ring_red
    })
    view.setImageResource(when (item.harvestingType) {
      HarvestingType.SOLAR -> R.drawable.img_solar_node
      HarvestingType.RF -> R.drawable.img_solar_node
      HarvestingType.ETC -> R.drawable.img_solar_node
    })
  }
}