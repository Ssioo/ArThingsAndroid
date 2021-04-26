package com.whoissio.arthings.src.infra.core

import android.app.Activity
import android.app.Application
import android.content.ComponentCallbacks
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Process
import com.whoissio.arthings.src.views.ErrorActivity
import kotlin.system.exitProcess

class MainErrorHandler(
  application: Application?,
  private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

  private var lastActivity: Activity? = null
  private var activityCnt: Int = 0

  private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
      if (isSkipActivity(activity)) return
      lastActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {
      if (isSkipActivity(activity)) return
      activityCnt++
      lastActivity = activity
    }

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) {
      if (isSkipActivity(activity)) return
      activityCnt--
      if (activityCnt < 0) lastActivity = null
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    private fun isSkipActivity(activity: Activity) = activity is ErrorActivity
  }

  init {
    application?.registerActivityLifecycleCallbacks(lifecycleCallbacks)
  }

  override fun uncaughtException(t: Thread, e: Throwable) {
    try {
      lastActivity?.run {
        startActivity(Intent(this, ErrorActivity::class.java).apply {
          putExtra("Error", e)
          putExtra("Intent", intent)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
      }
      defaultHandler?.uncaughtException(t, e)
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      Process.killProcess(Process.myPid())
      exitProcess(-1)
    }
  }
}

class MainErrorHandlerContentProvider : ContentProvider() {
  override fun onCreate(): Boolean {
    val handler = MainErrorHandler(
      context?.applicationContext as Application?,
      Thread.getDefaultUncaughtExceptionHandler()
    )
    Thread.setDefaultUncaughtExceptionHandler(handler)
    return true
  }

  override fun query(
    uri: Uri,
    projection: Array<out String>?,
    selection: String?,
    selectionArgs: Array<out String>?,
    sortOrder: String?
  ): Cursor? = null

  override fun getType(uri: Uri): String? = null

  override fun insert(uri: Uri, values: ContentValues?): Uri? = null

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

  override fun update(
    uri: Uri,
    values: ContentValues?,
    selection: String?,
    selectionArgs: Array<out String>?
  ): Int = 0
}