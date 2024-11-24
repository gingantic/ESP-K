package com.reihanaditya.esp32_k

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID

class BluetoothActivity<FirebaseNetworkException> : AppCompatActivity() {

    private lateinit var scanButton: Button
    private lateinit var deviceListView: ListView
    private lateinit var progressLayout: LinearLayout

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var devicesList: MutableList<String>
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private lateinit var bluetoothSocket: BluetoothSocket

    private lateinit var inputStreamReader: InputStreamReader
    private lateinit var bufferedReader: BufferedReader

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID for SPP (Serial Port Profile)

    private lateinit var loadingalert: LoadingAlert

    private lateinit var mAuth: FirebaseAuth

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                if (ActivityCompat.checkSelfPermission(
                        this@BluetoothActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@BluetoothActivity,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        PERMISSION_REQUEST_ACCESS_FINE_LOCATION
                    )
                    return
                }
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = device?.name ?: "Unknown Device"
                val deviceAddress = device?.address ?: "Unknown Address"
                val deviceInfo = "$deviceName\n$deviceAddress"
                progressLayout.visibility = View.GONE
                devicesList.add(deviceInfo)
                arrayAdapter.notifyDataSetChanged()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_scan)

        scanButton = findViewById(R.id.scanButton)
        deviceListView = findViewById(R.id.deviceListView)
        progressLayout = findViewById(R.id.progressLayout)

        mAuth = FirebaseAuth.getInstance()

        devicesList = mutableListOf()
        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devicesList)
        deviceListView.adapter = arrayAdapter
        deviceListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val deviceInfo = devicesList[position]
                val deviceAddress = deviceInfo.split("\n")[1]

                loadingalert = LoadingAlert()
                loadingalert.showLoadingDialog(this)
                GlobalScope.launch {
                    connectToDevice(deviceAddress)
                }

                Toast.makeText(this, "Connecting to $deviceAddress", Toast.LENGTH_SHORT).show()
            }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT
                ),
                PERMISSION_REQUEST_ACCESS_FINE_LOCATION
            )
        }

        if (!isGPSEnabled()) {
            // If GPS is not enabled, ask the user to enable it
            showEnableGPSDialog()
        }

        scanButton.setOnClickListener {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                startScanning()
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun connectToDevice(deviceAddress: String) {
        runOnUiThread {
            loadingalert.changeLoadingDialogMessage("Connecting to $deviceAddress")
        }

        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    PERMISSION_REQUEST_ACCESS_FINE_LOCATION
                )
                return
            }

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

            val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                device.createBond()
            }

            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket.connect()

            inputStreamReader = InputStreamReader(bluetoothSocket.inputStream)
            bufferedReader = BufferedReader(inputStreamReader)
            val outputStream = bluetoothSocket.outputStream
            var jsonResult: JSONObject?

            // ========= DEVICE ID =========
            var command = "{\"type\":\"DEVICEID\"}\n"
            outputStream.write(command.toByteArray())

            var response: String = withTimeout(15000) {  // Timeout set to 15 seconds
                bufferedReader.readLine()
            }

            jsonResult = JSONObject(response)
            val deviceId: String = jsonResult.getString("device_id")
            println(deviceId)

            // ========= UID =========

            command = "{\"type\":\"UID\",\"uid\":\"${mAuth.currentUser?.uid}\"}\n"
            outputStream.write(command.toByteArray())

            response = withTimeout(15000) {  // Timeout set to 15 seconds
                bufferedReader.readLine()
            }

            jsonResult = response?.let { JSONObject(it) }
            var status: String = jsonResult?.getString("status").toString()
            if (status == "FAIL") {
                runOnUiThread {
                    Toast.makeText(this, "Failed, This Device owned", Toast.LENGTH_SHORT).show()
                }
            }

            runOnUiThread {
                Toast.makeText(this, "Success UID Registered", Toast.LENGTH_SHORT).show()
            }

            // ========= Wifi SCAN =========

            command = "{\"type\":\"SCAN\"}\n"
            outputStream.write(command.toByteArray())

            runOnUiThread {
                loadingalert.changeLoadingDialogMessage("Waiting for wifi scan...")
            }

            // need to wait for the response with timeout 15 seconds
            response = withTimeout(15000) {  // Timeout set to 15 seconds
                bufferedReader.readLine()
            }
            println(response)
            val wifiList = mutableListOf<Model.WifiList>()
            jsonResult = response?.let { JSONObject(it) }
            val jsonList = jsonResult?.getJSONArray("networks")

            if (jsonList != null) {
                if (jsonList.length() == 0) {
                    runOnUiThread {
                        Toast.makeText(this, "No wifi networks found", Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }
                }

                for (i in 0 until jsonList.length()) {
                    val jsonObject = jsonList.getJSONObject(i)

                    val name = jsonObject.getString("name")
                    val strength = jsonObject.getInt("strength")
                    val passwordRequired = jsonObject.getBoolean("password")

                    println("Network Name: $name, Strength: $strength, Password Required: $passwordRequired")
                    wifiList.add(Model.WifiList(name, strength, passwordRequired))
                }
            }

            var selectWifi: Model.WifiList? = null

            runOnUiThread {
                WifiListDialog().showWifiDialog(this, wifiList) { selectedWifi ->
                    selectWifi = selectedWifi
                }
            }

            while (selectWifi == null) {
               Thread.sleep(1000)
            }

            // ========= input Wifi pass =========

            var passWifi: String? = null

            runOnUiThread {
                Utils().askQuestionWithInput(this, "Input Password", "Please input password for ${selectWifi?.ssid}") { password ->
                    passWifi = password
                }
            }

            while (passWifi == null) {
                Thread.sleep(1000)
            }

            runOnUiThread {
                loadingalert.changeLoadingDialogMessage("Connecting to ${selectWifi?.ssid}...")
            }

            // ========= Connect to Wifi =========

            command = "{\"type\":\"CONNECT\",\"ssid\":\"${selectWifi?.ssid}\",\"password\":\"$passWifi\"}\n"
            outputStream.write(command.toByteArray())

            try {
                response = withTimeout(15000) {  // Timeout set to 15 seconds
                    bufferedReader.readLine()
                }

                jsonResult = response?.let { JSONObject(it) }
                status = jsonResult?.getString("status").toString()
                if (status == "FAIL") {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Failed to connect to ${selectWifi?.ssid}",
                            Toast.LENGTH_SHORT
                        ).show()
                        throw Exception("Failed to connect to ${selectWifi?.ssid}")
                    }
                }
            } catch (_: Exception) {
            }

            runOnUiThread {
                Toast.makeText(this, "Success connect to ${selectWifi?.ssid}", Toast.LENGTH_SHORT).show()
                showDialog(this, "Proses selesai", "Proses selesai, silakan cek menu utama jika tidak reset perangkat dan ulangi proses ini lagi")
            }


        } catch (e: Exception) {
            // Handle other exceptions
            e.printStackTrace()
            runOnUiThread {
                showDialog(this, "Gagal Menyandingkan", "Gagal saat menyandingkan perangkat, coba lagi nanti")
            }
        } finally {
            // Close streams and socket if needed
            bluetoothSocket.close()
            bufferedReader.close()
            inputStreamReader.close()
            loadingalert.dismissLoadingDialog()
            println("Connection closed")
        }
    }

    fun showDialog(context: Context, title: String, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    override fun onDestroy() {
        // Close when the activity is destroyed
        super.onDestroy()
        try {
            bluetoothSocket.close()
            bufferedReader.close()
            inputStreamReader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun startScanning() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                PERMISSION_REQUEST_ACCESS_FINE_LOCATION
            )
            return
        }
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        devicesList.clear()
        arrayAdapter.notifyDataSetChanged()
        bluetoothAdapter.startDiscovery()
        progressLayout.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(bluetoothReceiver)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // All requested permissions have been granted
                    println("Permission granted")
                } else {
                    // Handle the case where permissions were not granted
                    Toast.makeText(this, "Location permission is required for Bluetooth scanning", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_REQUEST_BLUETOOTH -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // All requested permissions have been granted
                    println("Permission granted")
                } else {
                    // Handle the case where permissions were not granted
                    Toast.makeText(this, "Bluetooth permission is required", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Function to check if GPS is enabled
    private fun isGPSEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
    }

    // Function to show an alert dialog asking the user to enable GPS
    private fun showEnableGPSDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable GPS")
            .setMessage("GPS is required for this app. Do you want to enable it?")
            .setPositiveButton("Yes") { _, _ ->
                // If the user confirms, open location settings
                openLocationSettings()
            }
            .setNegativeButton("No") { _, _ ->
                // If the user cancels, you can handle it here, or do nothing
                Toast.makeText(
                    this,
                    "GPS is Needed to make this app fully functional. Please enable GPS to make this app fully interactive",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }

    // Function to open the location settings
    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    companion object {
        private const val PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val PERMISSION_REQUEST_BLUETOOTH = 2
        private const val REQUEST_ENABLE_BT = 3
    }
}