package com.reihanaditya.esp32_k

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DeviceActivity : AppCompatActivity() {

    lateinit var txt_nama: TextView
    lateinit var txt_temp: TextView
    lateinit var txt_humd: TextView
    lateinit var txt_smoke: TextView
    lateinit var txt_flame: TextView
    lateinit var txt_status: TextView
    lateinit var btn_sprinkler: CardView
    lateinit var btn_img_sprinkler: ImageView
    lateinit var txt_update_time: TextView


    lateinit var id_device: String
    var sprinkler: Boolean = false

    lateinit var mAuth: FirebaseAuth
    lateinit var mDB: FirebaseDatabase
    lateinit var mRef: DatabaseReference

    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth.currentUser
        if (currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }else{
            mAuth.updateCurrentUser(mAuth.currentUser!!).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("sukses")
                } else {
                    try {
                        throw task.exception!!
                    } catch (e: FirebaseNetworkException) {
                        Toast.makeText(this, "Tidak bisa terhubung ke Server", Toast.LENGTH_SHORT)
                            .show()
                    } catch (e: FirebaseAuthInvalidUserException) {
                        Toast.makeText(this, "User tidak ditemukan, Silahkan Login kembali", Toast.LENGTH_SHORT).show()
                        mAuth.signOut()
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    } catch (e: Exception) {
                        println("Error: ${e}")
                        Toast.makeText(this, "Terjadi kesalahan, Silahkan coba lagi", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device)

        mAuth = FirebaseAuth.getInstance()
        mDB = FirebaseDatabase.getInstance()
        mRef = mDB.reference

        txt_nama = findViewById(R.id.txt_nama)
        txt_temp = findViewById(R.id.txt_temp)
        txt_humd = findViewById(R.id.txt_humd)
        txt_smoke = findViewById(R.id.txt_smoke)
        txt_flame = findViewById(R.id.txt_flame)
        txt_status = findViewById(R.id.txt_status)
        btn_sprinkler = findViewById(R.id.btn_sprinkle)
        btn_img_sprinkler = findViewById(R.id.img_sprinkle)

        txt_update_time = findViewById(R.id.txt_update_time)

        val intent_data = intent.getStringExtra("device_id")

        id_device = intent_data.toString()
        txt_nama.text = intent_data.toString()

        mRef = mDB.getReference("uid/${mAuth.currentUser?.uid}/devices/$id_device")

        mRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = snapshot.child("temperature").value.toString()
                val humd = snapshot.child("humidity").value.toString()
                val time = snapshot.child("last_update").value.toString()
                val smoke = snapshot.child("smoke").value.toString()
                val flame = snapshot.child("flame").value.toString()
                sprinkler = snapshot.child("servo").value.toString().toBoolean()
                val startTime = Instant.now().epochSecond

                if (sprinkler) {
                    btn_img_sprinkler.setColorFilter(resources.getColor(R.color.red))
                } else {
                    btn_img_sprinkler.setColorFilter(resources.getColor(R.color.green))
                }

                if (startTime - time.toLong() > 30) {
                    txt_status.text = "Offline"
                } else {
                    txt_status.text = "Online"
                }

                txt_temp.text = "$temp°C"
                txt_humd.text = "$humd %"
                txt_smoke.text = "$smoke ppm"
                txt_flame.text = "$flame %"
                txt_update_time.text = epochToLocalDateTime(time.toLong(), ZoneId.systemDefault()).format(formatter)


            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DeviceActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        btn_sprinkler.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirmation")
            if (sprinkler) {
                builder.setMessage("apakah ingin mematikan air?")
            } else {
                builder.setMessage("apakah ingin menghidupkan air?")
            }
            builder.setPositiveButton("Yes") { _, _ ->
                mRef.child("servo").setValue(!sprinkler)
            }
            builder.show()
        }
    }

    fun epochToLocalDateTime(epoch: Long, zoneId: ZoneId): LocalDateTime {
        return Instant.ofEpochSecond(epoch).atZone(zoneId).toLocalDateTime()
    }

    override fun onDestroy() {
        super.onDestroy()
        finish()
    }
}