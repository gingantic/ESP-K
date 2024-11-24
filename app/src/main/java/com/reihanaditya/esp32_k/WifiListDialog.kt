package com.reihanaditya.esp32_k

import android.R
import android.content.Context
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog

class WifiListDialog {

    fun showWifiDialog(context: Context, wifiList: List<Model.WifiList>, callback: (Model.WifiList) -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Pilih Wifi")

        val arrayAdapter = ArrayAdapter<String>(context, R.layout.simple_list_item_1)
        wifiList.forEach {
            arrayAdapter.add("${it.ssid} (${it.rssi}), ${if (it.password) "password" else "open"}")
        }

        builder.setAdapter(arrayAdapter) { _, which ->
            callback(wifiList[which])
        }

        builder.show()
    }

}