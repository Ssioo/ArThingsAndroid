package com.whoissio.arthings.src

import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelLazy
import androidx.viewbinding.ViewBinding
import com.whoissio.arthings.BR
import dagger.hilt.android.AndroidEntryPoint

abstract class BaseActivity<B: ViewBinding> : AppCompatActivity() {

  abstract val binding: B

  protected val progressDialog by lazy {
    ProgressDialog(this).apply {
      isIndeterminate = true
      this.setMessage("Loading...")
    }
  }

  abstract class DBActivity<B : ViewDataBinding, VM : BaseViewModel>(@LayoutRes val layoutId: Int) :
    BaseActivity<B>() {

    override val binding: B by lazy { DataBindingUtil.setContentView<B>(this, layoutId) }

    abstract val vm: VM

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      binding.setVariable(BR.vm, vm)
      binding.lifecycleOwner = this

      vm.toastEvent.observe(this) { it.get()?.let { showToast(it) } }
      vm.alertEvent.observe(this) { it.get()?.let { showAlert(it) } }
      initView(savedInstanceState)
    }
  }

  abstract class VBActivity<B : ViewBinding> : BaseActivity<B>() {

    protected abstract val bindingProvider: (LayoutInflater) -> B

    override val binding: B by lazy { bindingProvider(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(binding.root)
      initView(savedInstanceState)
    }
  }

  abstract fun initView(savedInstanceState: Bundle?)

  fun showToast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

  fun showAlert(message: String) {
    AlertDialog.Builder(this)
      .setTitle("알림")
      .setMessage(message)
      .setPositiveButton("확인") { dialog, _ ->
        dialog.dismiss()
      }
      .show()
  }

  fun showProgress() {
    if (!isFinishing && !progressDialog.isShowing) progressDialog.show()
  }

  fun hideProgress() {
    if (!isFinishing && progressDialog.isShowing) progressDialog.hide()
  }

  override fun onStop() {
    super.onStop()
    progressDialog.dismiss()
  }
}