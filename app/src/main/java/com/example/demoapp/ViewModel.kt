package com.example.demoapp

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel

class ViewModel(application: Application) :
    AndroidViewModel(application) {
    private val repository: Repository = Repository(application)

    fun uploadData(
        imeiNo: String,
        internetConnectionStatus: String,
        batteryPercentage: String,
        chargingStatus: String,
        location: String
    ) {
        repository.uploadData(imeiNo, internetConnectionStatus, batteryPercentage, chargingStatus, location)
    }

    fun uploadImage(imgUri: Uri) {
        repository.uploadImage(imgUri)
    }
}