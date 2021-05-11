package com.whoissio.arthings.src.views

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityNodeEditBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.viewmodels.NodeEditViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NodeEditActivity: BaseActivity.DBActivity<ActivityNodeEditBinding, NodeEditViewModel>(R.layout.activity_node_edit) {
  override val vm: NodeEditViewModel by viewModels()

  override fun initView(savedInstanceState: Bundle?) {
    with(binding) {
      btnDelete.setOnClickListener { onClickDelete() }
      btnSubmit.setOnClickListener { onClickSubmit() }
    }
  }

  private fun onClickSubmit() {
    val address = binding.etNewAddress.text.toString()
    if (address.isEmpty()) return
    vm.submit(address) {
      setResult(RESULT_OK)
      finish()
    }
  }

  private fun onClickDelete() {

  }
}