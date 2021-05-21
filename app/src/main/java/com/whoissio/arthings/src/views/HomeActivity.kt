package com.whoissio.arthings.src.views

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.whoissio.arthings.ApplicationClass.Companion.sharedPref
import com.whoissio.arthings.R
import com.whoissio.arthings.databinding.ActivityHomeBinding
import com.whoissio.arthings.src.BaseActivity
import com.whoissio.arthings.src.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeActivity: BaseActivity.DBActivity<ActivityHomeBinding, HomeViewModel>(R.layout.activity_home) {
  override val vm: HomeViewModel by viewModels()

  override fun initView(savedInstanceState: Bundle?) {
    binding.btnSignIn.setOnClickListener { onClickBtnSignIn() }
    binding.btnGuest.setOnClickListener { onClickBtnGuestSignIn() }

    vm.userId.value = sharedPref.getString("user_id" ,"")
    vm.userPwd.value = sharedPref.getString("user_pwd" ,"")
  }

  private fun onClickBtnSignIn() {
    showProgress()
    vm.tryLogin {
      hideProgress()
      if (it) {
        showToast("환영합니다. Admin님")
        startActivity(Intent(this, NodeManageActivity::class.java))
        finish()
      } else {
        showToast("로그인 실패")
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