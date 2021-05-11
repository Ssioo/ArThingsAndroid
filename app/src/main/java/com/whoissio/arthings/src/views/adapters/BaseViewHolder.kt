package com.whoissio.arthings.src.views.adapters

import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.whoissio.arthings.BR
import com.whoissio.arthings.src.views.adapters.item.BaseItem

abstract class BaseViewHolder<B: ViewDataBinding, I>(val binding: B): RecyclerView.ViewHolder(binding.root) {

  fun bindTo(baseItem: BaseItem<I>) {
    binding.setVariable(BR.item, baseItem)
  }
}