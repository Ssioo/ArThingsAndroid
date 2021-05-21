package com.whoissio.arthings.src.views

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
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

  override fun initView(savedInstanceState: Bundle?) {
    binding.btnDelete.setOnClickListener { onClickDelete() }
    binding.btnSubmit.setOnClickListener { onClickSubmit() }
    binding.btnAdd.setOnClickListener { onClickAddData() }

    vm.selectedCloudAnchor.observe(this) {
      if (it != null) {
        binding.etNewAddress.setText(it.address)
        binding.tvAnchorId.text = it.id
        binding.tvAnchoredAt.text = it.createdAt
      }
      binding.btnDelete.isEnabled = it != null
      binding.btnSubmit.isEnabled = it == null
    }

    /* Set Data */
    vm.setSelectedCloudAnchor(intent.getParcelableExtra("anchor"))
  }

  private fun onClickAddData() {
    val view = LayoutInflater.from(this).inflate(R.layout.item_node_data_converter, binding.nodeConvertersContainer, false)
    binding.nodeConvertersContainer.addView(view)
  }

  private fun onClickSubmit() {
    val address = binding.etNewAddress.text.toString()
    if (address.isEmpty()) return
    val dataList = arrayListOf<CloudAnchorNodeData>()
    for(i in 0 until binding.nodeConvertersContainer.childCount) {
      val childBinding = ItemNodeDataConverterBinding.bind(binding.nodeConvertersContainer.getChildAt(i))
      val byteIdx = childBinding.nodeByte.text.toString().toIntOrNull() ?: 0
      val name = childBinding.nodeName.text.toString()
      val function = childBinding.nodeFunction.text.toString()
      dataList.add(CloudAnchorNodeData(name, byteIdx, function))
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
    }, 500)
  }
}