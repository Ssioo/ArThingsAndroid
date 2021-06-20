package com.whoissio.arthings.src.views

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.children
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityNodeEditBinding
import com.whoissio.arthings.databinding.ItemNodeDataConverterBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.models.CloudAnchorNodeData
import com.whoissio.arthings.src.viewmodels.NodeEditViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NodeEditActivity: BaseActivity.DBActivity<ActivityNodeEditBinding, NodeEditViewModel>(R.layout.activity_node_edit) {
  override val vm: NodeEditViewModel by viewModels()

  private val nodeDataViews: ArrayList<ItemNodeDataConverterBinding> = arrayListOf()

  override fun initView(savedInstanceState: Bundle?) {
    binding.btnDelete.setOnClickListener { onClickDelete() }
    binding.btnSubmit.setOnClickListener { onClickSubmit() }
    binding.btnAdd.setOnClickListener { onClickAddData() }

    vm.selectedCloudAnchor.observe(this) {
      if (it != null) {
        binding.etNewAddress.setText(it.address)
        binding.tvAnchorId.text = it.id
        binding.tvAnchoredAt.text = it.createdAt
        it.data.forEach {
          onClickAddData(it.name, it.byteIdx, it.function)
        }
      }
      binding.btnDelete.isEnabled = it != null
      binding.btnSubmit.isEnabled = it == null
    }

    /* Set Data */
    vm.setSelectedCloudAnchor(intent.getParcelableExtra("anchor"))
  }

  private fun onClickAddData(name: String = "", byteIdx: Int = 0, function: String = "") {
    val binding = ItemNodeDataConverterBinding.inflate(layoutInflater, binding.nodeConvertersContainer, true).apply {
      nodeNo.text = "${(nodeDataViews.size + 1)}"
      nodeName.setText(name)
      nodeByte.setText("$byteIdx")
      nodeFunction.setText(function)
      btnClose.setOnClickListener {
        binding.nodeConvertersContainer.removeView(this.root)
        nodeDataViews.remove(this)
        nodeDataViews.forEachIndexed { idx, b ->
          b.nodeNo.text = "${(idx + 1)}"
        }
      }
    }
    nodeDataViews.add(binding)
  }

  private fun onClickSubmit() {
    val address = binding.etNewAddress.text.toString()
    if (address.isEmpty()) return
    val dataList = nodeDataViews.map {
      val byteIdx = it.nodeByte.text.toString().toIntOrNull() ?: 0
      val name = it.nodeName.text.toString()
      val function = it.nodeFunction.text.toString()
      CloudAnchorNodeData(name, byteIdx, function)
    }
    showProgress()
    vm.submit(address, dataList) {
      hideProgress()
      vm.clearSelectedCloudAnchor()
      setResult(RESULT_OK)
      finish()
    }
  }

  private fun onClickDelete() {
    showProgress()
    binding.root.postDelayed({
      hideProgress()
      finish()
    }, 500)
  }
}