package com.whoissio.arthings.src.viewmodels

import androidx.lifecycle.MutableLiveData
import com.whoissio.arthings.src.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(): BaseViewModel() {
  val userId: MutableLiveData<String> = MutableLiveData("")
  val userPwd: MutableLiveData<String> = MutableLiveData("")
}