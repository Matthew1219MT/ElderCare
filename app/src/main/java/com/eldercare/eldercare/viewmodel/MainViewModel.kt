package com.eldercare.eldercare.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _welcomeText = MutableLiveData("Welcome to ElderCare")
    val welcomeText: LiveData<String> = _welcomeText
}