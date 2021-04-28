package com.whoissio.arthings.src.infra

import android.graphics.Color

object Constants {
    const val PERMISSION_REQUEST_CODE = 101

    val PERMISSION_ARRAY = arrayOf(
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.CAMERA,
    )

    const val SAMPLE_NODE_MAC_ADDRESS = "00:A0:50:17:2B:14"

    val COLOR_SET = listOf(Color.BLACK, Color.RED, Color.BLUE, Color.CYAN, Color.GRAY, Color.GREEN, Color.MAGENTA, Color.YELLOW, Color.DKGRAY, Color.LTGRAY)

    const val GLTF_SOLAR_PATH = "file:///android_asset/models/solar_battery_2.gltf"
    const val GLTF_RF_PATH = "file:///android_asset/models/rf_module.gltf"
    const val GLTF_SOLAR_SCALE = 0.00008f
    const val GLTF_RF_SCALE = 0.003f
}