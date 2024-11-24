package com.reihanaditya.esp32_k

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.*

class LoginActivity: AppCompatActivity() {
    lateinit var edt_email: EditText
    lateinit var edt_password: EditText
    lateinit var btn_login: Button
    lateinit var txt_daftar: TextView
    lateinit var progress_bar: ProgressBar
    lateinit var mAuth: FirebaseAuth


    override fun onStart() {
        super.onStart()
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            Intent(this, MainActivity::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        edt_email = findViewById(R.id.edt_email)
        edt_password = findViewById(R.id.edt_password)
        btn_login = findViewById(R.id.btn_login)
        progress_bar = findViewById(R.id.progressBar)
        txt_daftar = findViewById(R.id.txt_daftar)

        mAuth = FirebaseAuth.getInstance()

        txt_daftar.setOnClickListener {
            Intent(this, DaftarActivity::class.java).also {
                startActivity(it)
            }
        }

        btn_login.setOnClickListener {
            val email = edt_email.text.toString()
            val password = edt_password.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email atau password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                edt_email.requestFocus()
                return@setOnClickListener
            }

            if (!Utils().isEmailValid(email)) {
                Toast.makeText(this, "Email tidak valid", Toast.LENGTH_SHORT).show()
                edt_email.requestFocus()
                return@setOnClickListener
            }

            btn_login.visibility = Button.GONE
            progress_bar.visibility = ProgressBar.VISIBLE

            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                        Intent(this, MainActivity::class.java).also {
                            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(it)
                        }
                    } else {
                        try {
                            throw task.exception!!
                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(this, "Email atau password salah", Toast.LENGTH_SHORT).show()
                        } catch (e: FirebaseAuthInvalidUserException) {
                            Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
                            println(e)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    btn_login.visibility = Button.VISIBLE
                    progress_bar.visibility = ProgressBar.GONE
                }
        }
    }
}