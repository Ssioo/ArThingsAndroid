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
    binding.rvNodes.adapter = nodeManageRecyclerViewAdapter
    binding.srlNodes.setOnRefreshListener {
      binding.srlNodes.isRefreshing = true
      vm.refresh {
        binding.srlNodes.isRefreshing = false
      }
    }
    binding.btnAr.setOnClickListener { startActivity(Intent(this, ArActivity::class.java)) }
    binding.btnAdd.setOnClickListener {
      startActivityForResult(Intent(this, NodeEditActivity::class.java), 100)
    }
    vm.cloudedAnchors.observe(this, nodeManageRecyclerViewAdapter::setItems)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == 100 && resultCode == RESULT_OK) {
      showProgress()
      vm.refresh {
        hideProgress()
      }
    }
  }
}