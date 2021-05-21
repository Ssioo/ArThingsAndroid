package com.whoissio.arthings.src.views

import android.os.IBinder
import android.view.WindowManager
import androidx.test.espresso.Root
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

class ToastMatcher: TypeSafeMatcher<Root>() {
  private var currentFailures: Int = 0

  override fun describeTo(description: Description?) {
    description?.appendText("is toast")
  }

  override fun matchesSafely(root: Root?): Boolean {
    val type = root?.windowLayoutParams?.get()?.type
    if (type == WindowManager.LayoutParams.TYPE_TOAST) {
      val windowToken: IBinder = root.decorView.windowToken
      val appToken: IBinder = root.decorView.applicationWindowToken
      if (windowToken == appToken) return true
    }
    return ++currentFailures >= MAX_RETRIALS
  }

  companion object {
    const val MAX_RETRIALS = 10
    fun isToast(): Matcher<Root> {
      return ToastMatcher()
    }
  }
}