package com.example.ledscontroller

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar

class SelectBluetoothDeviceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_bluetooth_device)

        val ledsService: LEDsBluetoothService = LEDsBluetoothService()
        startService(Intent(this, ledsService::class.java))

        // Display paired device using recyclerView using adapter and item animation
        val recyclerView = findViewById<RecyclerView>(R.id.rv_device)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val deviceList = ledsService.listenForDevices()
        val bluetoothDeviceAdapter = BluetoothDeviceAdapter(ledsService.listenForDevices())
        recyclerView.adapter = bluetoothDeviceAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()

        if(deviceList.isEmpty()) {
            val snackbar = Snackbar.make(
                recyclerView, "Make sure Bluetooth is on or " +
                        "pair the phone and Arduino HC-05 module", Snackbar.LENGTH_INDEFINITE
            )
            snackbar.setAction("OK") { }
            snackbar.show()
        }
    }
}