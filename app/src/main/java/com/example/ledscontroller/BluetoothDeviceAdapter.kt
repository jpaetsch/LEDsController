package com.example.ledscontroller

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BluetoothDeviceAdapter(private val devices: ArrayList<BluetoothDevice>) :
    RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceHolder>() {

    /**
     * Reference to the type of views used by this adapter (custom ViewHolder)
     **/
    class DeviceHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.tv_device_name)
        val deviceAddress: TextView = view.findViewById(R.id.tv_device_address)
    }

    /**
     * Override to create new views (invoked by the layout manager)
     **/
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.select_bluetooth_device_item, parent, false)
        return DeviceHolder(view)
    }

    /**
     * Override to replace the contents of a view (invoked by the layout manager)
     **/
    override fun onBindViewHolder(holder: DeviceHolder, position: Int) {
        holder.deviceName.text = devices[position].name
        holder.deviceAddress.text = devices[position].address
    }

    /**
     * Override to return the size of the dataset (invoked by the layout manager)
     **/
    override fun getItemCount(): Int {
        return devices.size
    }
}