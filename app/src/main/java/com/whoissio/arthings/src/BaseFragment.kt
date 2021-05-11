package com.whoissio.arthings.src

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.whoissio.arthings.BR

abstract class BaseFragment<B: ViewBinding>: Fragment() {

  abstract class DBFragment<B: ViewDataBinding, VM: BaseViewModel>: BaseFragment<B>() {

    protected lateinit var binding: B
    protected abstract val vm: VM

    abstract val bindingProvider: (LayoutInflater, ViewGroup?, Boolean) -> B

    override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
    ): View? {
      binding = bindingProvider(inflater, container, false)
      binding.setVariable(BR.vm, vm)
      binding.lifecycleOwner = viewLifecycleOwner
      initView()
      return binding.root
    }

    abstract fun initView()
  }

  fun showToast(message: String) = Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()

}