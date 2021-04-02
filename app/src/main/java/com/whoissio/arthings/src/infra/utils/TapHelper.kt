package com.whoissio.arthings.src.infra.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue


class TapHelper(context: Context): View.OnTouchListener {

  private val queuedSingleTaps: BlockingQueue<MotionEvent> = ArrayBlockingQueue(16)

  private val gestureDetector: GestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {
    override fun onSingleTapUp(e: MotionEvent?): Boolean {
      queuedSingleTaps.offer(e)
      return true
    }

    override fun onDown(e: MotionEvent?): Boolean = true
  })

  fun poll(): MotionEvent? = queuedSingleTaps.poll()

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouch(v: View?, event: MotionEvent?): Boolean {
    return gestureDetector.onTouchEvent(event)
  }
}