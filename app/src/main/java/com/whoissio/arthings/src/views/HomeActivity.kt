package com.whoissio.arthings.src.views

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityHomeBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity: BaseActivity.DBActivity<ActivityHomeBinding, HomeViewModel>(R.layout.activity_home) {
  override val vm: HomeViewModel by viewModels()

  override fun initView(savedInstanceState: Bundle?) {
    with(binding) {
      btnSignIn.setOnClickListener { onClickBtnSignIn() }
      btnGuest.setOnClickListener { onClickBtnGuestSignIn() }
    }
  }

  private fun onClickBtnSignIn() {
    if (vm.userId.value?.isEmpty() == true || vm.userPwd.value?.isEmpty() == true) {
      showToast("빈값")
      return
    }
    showProgress()
    Firebase.auth.signInWithEmailAndPassword(vm.userId.value!!, vm.userPwd.value!!)
      .addOnCompleteListener(this) {
        hideProgress()
        if (it.isSuccessful) {
          showToast("환영합니다. Admin님")
          startActivity(Intent(this, NodeManageActivity::class.java))
          finish()
        } else {
          showToast("로그인 실패 ${it.exception?.localizedMessage}")
          it.exception?.printStackTrace()
        }
      }
  }

  private fun onClickBtnGuestSignIn() {
    showProgress()
    Firebase.auth.signInAnonymously()
      .addOnCompleteListener(this) {
        hideProgress()
        if (it.isSuccessful) {
          startActivity(Intent(this, ArActivity::class.java))
          finish()
        } else {
          showToast("실패")
          it.exception?.printStackTrace()
        }
      }
  }
}