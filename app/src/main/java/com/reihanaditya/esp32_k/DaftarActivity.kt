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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class DaftarActivity: AppCompatActivity() {
    lateinit var edt_email: EditText
    lateinit var edt_password: EditText
    lateinit var edt_repeat_password: EditText
    lateinit var btn_daftar: Button
    lateinit var progress_bar: ProgressBar
    lateinit var txt_login: TextView

    lateinit var mAuth: FirebaseAuth
    lateinit var mDB: FirebaseDatabase
    lateinit var mRef: DatabaseReference

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
        setContentView(R.layout.activity_daftar)

        edt_email = findViewById(R.id.edt_email)
        edt_password = findViewById(R.id.edt_password)
        edt_repeat_password = findViewById(R.id.edt_repeat_password)
        btn_daftar = findViewById(R.id.btn_daftar)
        progress_bar = findViewById(R.id.progressBar)
        txt_login = findViewById(R.id.txt_login)

        mAuth = FirebaseAuth.getInstance()
        mDB = FirebaseDatabase.getInstance()

        txt_login.setOnClickListener {
            Intent(this, LoginActivity::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(it)
            }
        }

        btn_daftar.setOnClickListener {
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

            if (edt_password.text.toString() != edt_repeat_password.text.toString()) {
                Toast.makeText(this, "Password tidak sama", Toast.LENGTH_SHORT).show()
                edt_repeat_password.requestFocus()
                return@setOnClickListener
            }

            btn_daftar.visibility = Button.GONE
            progress_bar.visibility = ProgressBar.VISIBLE

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Berhasil mendaftar", Toast.LENGTH_SHORT).show()

                        mRef = mDB.getReference("uid/${mAuth.currentUser?.uid}")
                        mRef.child("email").setValue(email)
                        mRef.child("created_at").setValue(ServerValue.TIMESTAMP)
                        mRef.child("devices").setValue("")

                        Intent(this, LoginActivity::class.java).also {
                            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(it)
                        }
                    } else {
                        try {
                            throw task.exception!!
                        } catch (e: FirebaseAuthWeakPasswordException) {
                            Toast.makeText(this, "Password terlalu lemah", Toast.LENGTH_SHORT).show()
                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(this, "Email tidak valid", Toast.LENGTH_SHORT).show()
                        } catch (e: FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "Email sudah terdaftar", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    btn_daftar.visibility = Button.VISIBLE
                    progress_bar.visibility = ProgressBar.GONE
                }
        }
    }
}