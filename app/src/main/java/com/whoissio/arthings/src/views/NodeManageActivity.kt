package com.whoissio.arthings.src.views

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.ViewModelProvider
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityNodeManageBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.viewmodels.NodeManageViewModel

class NodeManageActivity: BaseActivity.DBActivity<ActivityNodeManageBinding, NodeManageViewModel>(R.layout.activity_node_manage) {
  override val bindingProvider: (LayoutInflater) -> ActivityNodeManageBinding = ActivityNodeManageBinding::inflate

  override val vmProvider: () -> NodeManageViewModel = { ViewModelProvider(this).get(NodeManageViewModel::class.java) }

  override fun initView(savedInstanceState: Bundle?) {

  }
}