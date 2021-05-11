package com.whoissio.arthings.src.views.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.whoissio.arthings.src.views.adapters.item.BaseItem

abstract class BaseRecyclerViewAdapter<I, B: ViewDataBinding>: RecyclerView.Adapter<BaseViewHolder<B, I>>() {
  protected abstract val bindingProvider: (LayoutInflater, ViewGroup?, Boolean) -> B

  protected val items: ArrayList<I> = arrayListOf()
  protected abstract val controller: (I) -> BaseItem<I>

  fun addItems(items: List<I>) {
    this.items.addAll(items)
    notifyDataSetChanged()
  }

  open fun setItems(items: List<I>) {
    this.items.clear()
    this.items.addAll(items)
    notifyDataSetChanged()
  }

  fun clear() {
    this.items.clear()
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<B, I> {
    return object : BaseViewHolder<B, I>(
      bindingProvider(LayoutInflater.from(parent.context), parent, false)
    ) {
    }
  }


  override fun onBindViewHolder(holder: BaseViewHolder<B, I>, position: Int) {
    items.getOrNull(position)?.let { holder.bindTo(controller(it)) }
  }

  override fun getItemCount(): Int = items.size
}