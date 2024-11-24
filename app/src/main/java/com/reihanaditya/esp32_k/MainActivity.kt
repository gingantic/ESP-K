package com.reihanaditya.esp32_k

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import java.time.Instant
import java.util.concurrent.CountDownLatch

class MainActivity : AppCompatActivity() {

    lateinit var mAuth: FirebaseAuth
    lateinit var mDB: FirebaseDatabase
    lateinit var mRef: DatabaseReference

    lateinit var btn_logout: Button
    lateinit var txt_user: TextView
    lateinit var btn_add: Button
    lateinit var list_device: LinearLayout
    lateinit var rv_device: RecyclerView
    lateinit var refreshLayout: SwipeRefreshLayout

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
        NotificationHandler().createNotificationChannel(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()
        val currentUser = mAuth.currentUser

        list_device = findViewById(R.id.list_devices)
        rv_device = findViewById(R.id.rv_devices)
        btn_logout = findViewById(R.id.btn_logout)
        btn_add = findViewById(R.id.btn_add)
        txt_user = findViewById(R.id.txt_currentuser)
        refreshLayout = findViewById(R.id.swipe_refresh)

        txt_user.text = currentUser?.email

        mDB = FirebaseDatabase.getInstance()

        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (!it.isSuccessful) {
                println("Fetching FCM registration token failed")
                return@addOnCompleteListener
            }

            val token = it.result
            saveTokenFCM(token!!)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    println("Permission granted")
                } else {
                    Toast.makeText(this, "Permission untuk notifikasi ditolak", Toast.LENGTH_SHORT).show()
                }
            }.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        getListDevice()

        btn_logout.setOnClickListener {
            FirebaseMessaging.getInstance().token.addOnCompleteListener {
                if (!it.isSuccessful) {
                    println("Fetching FCM registration token failed")
                    mAuth.signOut()
                    return@addOnCompleteListener
                }

                val token = it.result
                removeTokenFCM(token!!)
                mAuth.signOut()
            }

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        btn_add.setOnClickListener {
            intent = Intent(this, BluetoothActivity::class.java)
            startActivity(intent)
        }

        refreshLayout.setOnRefreshListener {
            getListDevice()
            refreshLayout.isRefreshing = false
        }

//        val serviceIntent = Intent(this, ForegroundServices::class.java)
//        startForegroundService(serviceIntent)
    }

    private fun getListDevice() {
        rv_device.layoutManager = LinearLayoutManager(this)
        val deviceRefPath = mDB.getReference("uid/${mAuth.currentUser?.uid}/devices")
        val listDevice = mutableListOf<Model.DeviceList>()
        deviceRefPath.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                return@addOnSuccessListener
            }

            val latch = CountDownLatch(snapshot.childrenCount.toInt())

            snapshot.children.forEach { data ->
                println(data.key.toString())
                var status = ""
                val devicePath = mDB.getReference("uid/${mAuth.currentUser?.uid}/devices/${data.key.toString()}")

                devicePath.get().addOnSuccessListener { deviceData ->
                    if (deviceData.exists()) {
                        val startTime = Instant.now().epochSecond
                        val lastUpdateTime = deviceData.child("last_update").value.toString().toLongOrNull()
                        status = if (startTime - lastUpdateTime!! > 30) {
                            "Offline"
                        } else {
                            "Online"
                        }
                    }

                    listDevice.add(Model.DeviceList(data.key.toString(), status))
                    latch.countDown() // Decrease the latch count
                }.addOnFailureListener {
                    latch.countDown() // Decrease the latch count even on failure
                }
            }

            Thread {
                latch.await() // Wait for all Firebase operations to complete
                runOnUiThread {
                    val adapter = ListDevicesAdaptor(listDevice)
                    rv_device.adapter = adapter
                }
            }.start()
        }
    }

    private fun saveTokenFCM(token: String) {
        if (mAuth.currentUser == null) {
            return
        }
        val mRef = mDB.getReference("uid/${mAuth.currentUser?.uid}/fcmtokens/${token}")
        mRef.child("timestamp").setValue(Instant.now().epochSecond)
    }

    private fun removeTokenFCM(token: String) {
        val mRef = mDB.getReference("uid/${mAuth.currentUser?.uid}/fcmtokens/${token}")
        mRef.removeValue()
    }
}