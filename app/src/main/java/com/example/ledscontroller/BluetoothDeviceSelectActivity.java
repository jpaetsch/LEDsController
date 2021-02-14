package com.example.ledscontroller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v4.os.IResultReceiver;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothDeviceSelectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_device_select);

        /* Setup Bluetooth */
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        /* Get list of paired Bluetooth devices */
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<Object> deviceList = new ArrayList<>();

        /* If there are paired devices get the name and address of each */
        if(pairedDevices.size() > 0) {
            for(BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                BluetoothDeviceInformation deviceInfo = new BluetoothDeviceInformation(
                        deviceName, deviceHardwareAddress);
                deviceList.add(deviceInfo);
            }

            /* Display paired device using recyclerView */
            RecyclerView recyclerView = findViewById(R.id.recycler_view_device);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            BluetoothDeviceListAdapter bluetoothDeviceListAdapter = new BluetoothDeviceListAdapter(
                    this, deviceList);
            recyclerView.setAdapter(bluetoothDeviceListAdapter);
            recyclerView.setItemAnimator(new DefaultItemAnimator());
        } else {
            View view = findViewById(R.id.recycler_view_device);
            Snackbar snackbar = Snackbar.make(view,"Activate Bluetooth or " +
                    "pair a Bluetooth device", Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
            snackbar.show();
        }
    }
}