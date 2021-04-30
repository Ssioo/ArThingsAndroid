package com.whoissio.arthings.src.views

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityNodeManageBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.viewmodels.NodeManageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NodeManageActivity: BaseActivity.DBActivity<ActivityNodeManageBinding, NodeManageViewModel>(R.layout.activity_node_manage) {
  override val vm: NodeManageViewModel by viewModels()

  override fun initView(savedInstanceState: Bundle?) {

  }
}