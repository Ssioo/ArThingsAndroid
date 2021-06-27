package com.whoissio.arthings.src.infra

import android.graphics.Color
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object Constants {
    const val PERMISSION_REQUEST_CODE = 101

    val PERMISSION_ARRAY = arrayOf(
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.CAMERA,
    )

    const val SAMPLE_NODE_MAC_ADDRESS1 = "00:A0:50:17:2B:14"
    const val SAMPLE_NODE_MAC_ADDRESS2 = "40:BC:E0:74:3F:01"
    const val SAMPLE_NODE_MAC_ADDRESS3 = "A2:70:11:EA:B2:07"
    const val SAMPLE_NODE_MAC_ADDRESS4 = "89:22:A2:AF:F0:36"

    val SAMPLE_NODE_ARRAY = listOf(SAMPLE_NODE_MAC_ADDRESS1, SAMPLE_NODE_MAC_ADDRESS2, SAMPLE_NODE_MAC_ADDRESS3, SAMPLE_NODE_MAC_ADDRESS4)

    val COLOR_SET = listOf(Color.BLACK, Color.RED, Color.BLUE, Color.CYAN, Color.GRAY, Color.GREEN, Color.MAGENTA, Color.YELLOW, Color.DKGRAY, Color.LTGRAY)

    const val GLTF_SOLAR_PATH = "file:///android_asset/models/solar_battery_2.gltf"
    const val GLTF_RF_PATH = "file:///android_asset/models/rf_module.gltf"
    const val GLTF_EXCLAMATION_PATH = "file:///android_asset/models/exclamation.gltf"
    const val GLTF_SOLAR_SCALE = 0.00008f
    const val GLTF_RF_SCALE = 0.003f
    const val GLTF_EXCLAMATION_SCALE = 1f

    const val CLOUD_ANCHOR_BASE_URL = "https://arcorecloudanchor.googleapis.com"

    val DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.ENGLISH)
}