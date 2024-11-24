package com.reihanaditya.esp32_k

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText

class Utils {
    val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$".toRegex()

    fun isEmailValid(email: String): Boolean {
        return emailRegex.matches(email)
    }

    fun askQuestionWithInput(context: Context, title: String, message: String, callback: (String) -> Unit) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)

        val input = EditText(context)
        builder.setView(input)

        builder.setPositiveButton("Yes") { dialog, _ ->
            val userInput = input.text.toString()
            callback(userInput)
            dialog.dismiss()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }
}