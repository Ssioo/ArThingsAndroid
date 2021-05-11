package com.whoissio.arthings.src.views

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityNodeManageBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.viewmodels.NodeManageViewModel
import com.whoissio.arthings.src.views.adapters.NodeManageRecyclerViewAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NodeManageActivity: BaseActivity.DBActivity<ActivityNodeManageBinding, NodeManageViewModel>(R.layout.activity_node_manage) {
  override val vm: NodeManageViewModel by viewModels()

  @Inject lateinit var nodeManageRecyclerViewAdapter: NodeManageRecyclerViewAdapter

  override fun initView(savedInstanceState: Bundle?) {
    with(binding) {
      rvNodes.adapter = nodeManageRecyclerViewAdapter
      btnAdd.setOnClickListener {
        startActivityForResult(Intent(this@NodeManageActivity, NodeEditActivity::class.java).apply {

        }, 100)
      }
    }
    with(vm) {
      cloudedAnchors.observe(this@NodeManageActivity, nodeManageRecyclerViewAdapter::setItems)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == 100 && resultCode == RESULT_OK) {
      vm.refresh()
    }
  }
}