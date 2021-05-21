package com.whoissio.arthings.src.views

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.getIntents
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.activityScenarioRule
import com.whoissio.arthings.R
import com.whoissio.arthings.src.views.ToastMatcher.Companion.isToast
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class HomeActivityTest {

  @get:Rule(order = 0)
  var hiltRule = HiltAndroidRule(this)

  @get:Rule(order = 1)
  var homeActivityTestRule = activityScenarioRule<HomeActivity>()

  @Before
  fun setUp() {
    hiltRule.inject()
  }

  @After
  fun tearDown() {
  }

  @Test
  fun loginWithEmpty() {
    onView(withId(R.id.user_id)).perform(typeText(""), closeSoftKeyboard())
    onView(withId(R.id.user_pwd)).perform(typeText(""), closeSoftKeyboard())
    onView(withId(R.id.btn_sign_in)).perform(click())
    onView(withText("빈값"))
      .inRoot(isToast())
      .check(matches(isDisplayed()))
  }

  @Test
  fun loginWithAdmin() {
    onView(withId(R.id.user_id)).perform(typeText("admin@test.com"), closeSoftKeyboard())
    onView(withId(R.id.user_pwd)).perform(typeText("admin1111"), closeSoftKeyboard())
    onView(withId(R.id.btn_sign_in)).perform(click())
    getIntents().firstOrNull()?.let {
      it.component?.className == NodeManageActivity::class.java.name
    }
  }

  @Test
  fun loginAnonymous() {
    onView(withId(R.id.btn_guest)).perform(click())
    onView(withId(R.id.ar_view)).check(matches(isDisplayed()))
  }
}