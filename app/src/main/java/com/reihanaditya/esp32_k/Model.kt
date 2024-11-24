package com.reihanaditya.esp32_k

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

class Model {
    data class DeviceList(val device_id: String, val status: String)
    data class WifiList(val ssid: String, val rssi: Int, val password: Boolean)
}