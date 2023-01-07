package com.example.ledscontroller;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * This class is an activity for selecting a bluetooth device to pair to
 **/
public class BluetoothDeviceSelectActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;
    Set<BluetoothDevice> pairedDevices;
    List<Object> deviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_device_select);

        // Set up the default bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {

        }

        // Get a list of paired bluetooth devices
        pairedDevices = bluetoothAdapter.getBondedDevices();
        deviceList = new ArrayList<>();

        if(pairedDevices.size() > 0) { // There are paired devices so add them to the list
            for(BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName(); // device name
                String deviceHardwareAddress = device.getAddress(); // device MAC address
                BluetoothDeviceInformation deviceInfo = new BluetoothDeviceInformation(
                        deviceName, deviceHardwareAddress);

                // Add the paired device info
                deviceList.add(deviceInfo);
            }

            // Display paired device using recyclerView using adapter and item animation
            RecyclerView recyclerView = findViewById(R.id.recycler_view_device);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            BluetoothDeviceListAdapter bluetoothDeviceListAdapter = new BluetoothDeviceListAdapter(
                    this, deviceList);
            recyclerView.setAdapter(bluetoothDeviceListAdapter);
            recyclerView.setItemAnimator(new DefaultItemAnimator());

        } else { // no paired devices so inform the user that bluetooth is off or some other issue
            View view = findViewById(R.id.recycler_view_device);
            Snackbar snackbar = Snackbar.make(view,"Make sure Bluetooth is on or " +
                    "pair the phone and Arduino HC-05 module", Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
            snackbar.show();
        }
    }
}