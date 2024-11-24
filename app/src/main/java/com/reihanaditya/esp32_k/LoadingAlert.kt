package com.reihanaditya.esp32_k

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog

class LoadingAlert {
    private var loadingDialog: AlertDialog? = null

    fun showLoadingDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.loading_alert, null)
        builder.setView(view)
        builder.setCancelable(false)
        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    fun changeLoadingDialogMessage(message: String) {
        loadingDialog?.findViewById<android.widget.TextView>(R.id.loadingText)?.text = message
    }

    fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
    }
}