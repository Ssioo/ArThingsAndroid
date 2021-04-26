package com.whoissio.arthings.src.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import com.whoissio.arthings.databinding.ActivityErrorBinding
import com.whoissio.arthings.src.BaseActivity

class ErrorActivity : BaseActivity.VBActivity<ActivityErrorBinding>() {
  override val bindingProvider: (LayoutInflater) -> ActivityErrorBinding =
    ActivityErrorBinding::inflate

  private val lastActivityIntent by lazy { intent.getParcelableExtra<Intent>("Intent") }
  private val lastError by lazy { intent.getSerializableExtra("Error") as? Throwable }

  override fun initView(savedInstanceState: Bundle?) {
    binding.tvStack.text = lastError?.stackTraceToString()
    binding.btnRefresh.setOnClickListener {
      startActivity(lastActivityIntent)
      finish()
    }
  }
}