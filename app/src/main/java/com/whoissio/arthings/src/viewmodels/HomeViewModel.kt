package com.whoissio.arthings.src.viewmodels

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.whoissio.arthings.ApplicationClass
import com.whoissio.arthings.ApplicationClass.Companion.sharedPref
import com.whoissio.arthings.src.BaseViewModel
import com.whoissio.arthings.src.models.BaseEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(): BaseViewModel() {
  val userId: MutableLiveData<String> = MutableLiveData("")
  val userPwd: MutableLiveData<String> = MutableLiveData("")

  fun tryLogin(onComplete: (Boolean) -> Unit) {
    val id = userId.value ?: ""
    val pwd = userPwd.value ?: ""
    if (id.isEmpty() || pwd.isEmpty()) {
      toastEvent.value = BaseEvent(data = "빈값")
      onComplete(false)
      return
    }
    Firebase.auth.signInWithEmailAndPassword(id, pwd)
      .addOnCompleteListener {
        if (it.isSuccessful) {
          sharedPref.edit()
            .putString("user_id", id)
            .putString("user_pwd", pwd)
            .apply()
          onComplete(true)
        }
        else {
          it.exception?.printStackTrace()
          onComplete(false)
        }
      }
  }
}