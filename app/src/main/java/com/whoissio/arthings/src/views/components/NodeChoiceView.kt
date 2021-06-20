package com.whoissio.arthings.src.views.components

import android.view.MotionEvent
import android.view.View
import com.google.ar.core.Anchor
import com.whoissio.arthings.databinding.ViewNodeAttacherBinding

class NodeChoiceView(val view: View, val anchor: Anchor) {

  private val binding: ViewNodeAttacherBinding = ViewNodeAttacherBinding.bind(view)

  enum class ButtonChoice {
    BUTTON_SOLAR, BUTTON_RF, BUTTON_CLOSE
  }

  fun interface OnClickListener {
    fun onClick(anchor: Anchor, which: ButtonChoice)
  }

  private fun onTouchArButton (v: View?, event: MotionEvent?): Boolean {
    when (event?.action) {
      MotionEvent.ACTION_DOWN -> v?.alpha = 0.38f
      MotionEvent.ACTION_UP -> v?.alpha = 1f
    }
    return false
  }

  fun setOnClickListener(onClickListener: OnClickListener) {
    binding.btnSolar.apply {
      setOnTouchListener(this@NodeChoiceView::onTouchArButton)
      setOnClickListener { onClickListener.onClick(anchor, ButtonChoice.BUTTON_SOLAR) }
    }
    binding.btnRf.apply {
      setOnTouchListener(this@NodeChoiceView::onTouchArButton)
      setOnClickListener { onClickListener.onClick(anchor, ButtonChoice.BUTTON_RF) }
    }
    binding.btnClose.apply {
      setOnTouchListener(this@NodeChoiceView::onTouchArButton)
      setOnClickListener { onClickListener.onClick(anchor, ButtonChoice.BUTTON_CLOSE) }
    }
  }

}