package com.whoissio.arthings.src

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.google.android.material.snackbar.Snackbar
import com.whoissio.arthings.BR

abstract class BaseActivity<B, VM>(@LayoutRes val layoutId: Int) : AppCompatActivity(),
  IBaseActivity<VM>
  where B : ViewDataBinding, VM : BaseViewModel {

  protected lateinit var binding: B
  protected lateinit var vm: VM

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = DataBindingUtil.setContentView(this, layoutId)
    vm = getViewModel()
    binding.setVariable(BR.vm, vm)
    binding.lifecycleOwner = this

    initView(savedInstanceState)
  }

  fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

  fun showMessage(src: String) = Snackbar.make(binding.root, src, Snackbar.LENGTH_SHORT)
    .setAction("닫기") {

    }
    .show()
}

interface IBaseActivity<VM : BaseViewModel> {
  fun initView(savedInstanceState: Bundle?)

  fun getViewModel(): VM
}