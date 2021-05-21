
package com.whoissio.arthings.src.views.components

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.DpToMetersViewSizer
import com.google.ar.sceneform.rendering.ViewRenderable
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ViewEmptyNodeViewBinding
import com.whoissio.arthings.src.infra.Constants.DATE_FORMAT
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class NodeInfoView(val context: Context, private val address: String = "") {

  private var anchor: AnchorNode? = null
  private val binding: ViewEmptyNodeViewBinding = ViewEmptyNodeViewBinding.inflate(LayoutInflater.from(context))
  val view get() = binding.root

  private val dataMap: ConcurrentHashMap<TextView, (ByteArray) -> Double> = ConcurrentHashMap()

  fun attach(anchor: AnchorNode, renderable: ViewRenderable) {
    Node().apply {
      localPosition = Vector3(0f, 0.06f, -0.02f)
      this.renderable = renderable
      setParent(anchor)
    }
    this.anchor = anchor
  }

  fun onUpdate(bytes: ByteArray) {
    binding.lastDataDate.text = DATE_FORMAT.format(Date())
    dataMap.forEach { (key, value) ->
      key.text = String.format("%.2f", value(bytes))
    }
  }

  fun addDataView(name: String, dataConverter: (ByteArray) -> Double = { 0.0 }) {
    val container = LinearLayout(context).apply {
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      ).apply {
        weight = 1f
        setPadding(0, 8, 0, 8)
        setMargins(0, 2, 0, 2)
      }
      setBackgroundColor(context.getColor(R.color.opaque_white))
      orientation = LinearLayout.HORIZONTAL
    }
    val dataNameView = TextView(context).apply {
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      )
      text = name
      setTextColor(context.getColor(R.color.white))
      textSize = 14f
    }
    val dataView = TextView(context).apply {
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
      )
      text = "--"
      setTextColor(context.getColor(R.color.white))
      textSize = 18f
      setTypeface(typeface, Typeface.BOLD)
    }
    dataMap[dataView] = dataConverter
    container.addView(dataNameView)
    container.addView(dataView)
    binding.dataContainer.addView(container)
  }

  fun build(): CompletableFuture<ViewRenderable> {
    return ViewRenderable.builder()
      .setView(context, view)
      .setSizer(DpToMetersViewSizer(1000))
      .build()
  }

  init {
    binding.nodeName.text = address
  }
}