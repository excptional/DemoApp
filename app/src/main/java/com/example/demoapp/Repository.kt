package com.example.demoapp

import android.app.Application
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

@Suppress("NAME_SHADOWING")
class Repository(private val application: Application) {
    private val firebaseStorage = FirebaseStorage.getInstance()
    private val firebaseDB = FirebaseFirestore.getInstance()

    fun uploadData(
        imeiNo: String,
        internetConnectionStatus: String,
        batteryPercentage: String,
        chargingStatus: String,
        location: String
    ) {
        val dateAndTime =
            SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa", Locale.getDefault()).format(Date())
        val date = SimpleDateFormat("yyyy_MM_dd_hh_mm_ss", Locale.getDefault()).format(Date())

        val postData = hashMapOf(
            "IMEI No" to imeiNo,
            "Internet Connection Status" to internetConnectionStatus,
            "Battery Percentage" to batteryPercentage,
            "Charging Status" to chargingStatus,
            "Location" to location,
            "time" to dateAndTime
        )
        firebaseDB.collection("App Data").document(date).set(postData)
    }

    fun uploadImage(imgUri: Uri) {
        val date = SimpleDateFormat("yyyy_MM_dd_hh:mm:ss", Locale.getDefault()).format(Date())
        val ref = firebaseStorage.reference.child("capture image/$date")
        ref.putFile(imgUri)
    }

}