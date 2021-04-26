package com.whoissio.arthings.src

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.viewbinding.ViewBinding
import com.google.android.material.snackbar.Snackbar
import com.whoissio.arthings.BR

sealed class BaseActivity: AppCompatActivity() {
  abstract class DBActivity<B: ViewDataBinding, VM: BaseViewModel>(@LayoutRes val layoutId: Int): BaseActivity() {
    protected lateinit var binding: B
    protected lateinit var vm: VM

    protected abstract val bindingProvider: (LayoutInflater) -> B
    protected abstract val vmProvider: () -> VM

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      binding = DataBindingUtil.setContentView(this, layoutId)
      vm = vmProvider()
      binding.setVariable(BR.vm, vm)
      binding.lifecycleOwner = this
      initView(savedInstanceState)
    }
  }

  abstract class VBActivity<B: ViewBinding>: BaseActivity() {
    protected lateinit var binding: B

    protected abstract val bindingProvider: (LayoutInflater) -> B

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      binding = bindingProvider(layoutInflater)
      setContentView(binding.root)
      initView(savedInstanceState)
    }
  }

  abstract fun initView(savedInstanceState: Bundle?)

  fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}