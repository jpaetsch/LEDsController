package com.example.ledscontroller;


import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


/**
 * This class is an adapter used to show bluetooth devices in a recyclerview
 **/
public class BluetoothDeviceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<Object> deviceList;

    /**
     * Constructor
     * @param context the provided context for the adapter
     * @param deviceList the list of device object items that will be provided to the adapter
    **/
    public BluetoothDeviceListAdapter(Context context, List<Object> deviceList) {
        this.context = context;
        this.deviceList = deviceList;
    }

    /**
     * Create a new ViewHolder object when the RecyclerView requires a new one
     * @param parent the ViewGroup into which the new View will be added after bound
     *               to an adapter position
     * @param viewType the type of the new View
     * @return the new ViewHolder object
     **/
    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.device_info_layout, parent, false);
        return new ViewHolder(v);
    }

    /**
     * Bind the bluetooth device information data to the view - a specified position for display
     * @param holder The ViewHolder that will be updated to represent the contents of the specific
     *               list item
     * @param position The location of the item in the adapter's data
     **/
    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder,
                                 final int position) {
        ViewHolder itemHolder = (ViewHolder) holder;
        final BluetoothDeviceInformation deviceInfoModel = (BluetoothDeviceInformation)
                deviceList.get(position);

        itemHolder.textName.setText(deviceInfoModel.getDeviceName());
        itemHolder.textAddress.setText(deviceInfoModel.getDeviceHardwareAddress());

        // When a device is selected
        itemHolder.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, MainActivity.class);

                // Send device details to the MainActivity
                intent.putExtra("deviceName", deviceInfoModel.getDeviceName());
                intent.putExtra("deviceAddress",deviceInfoModel.getDeviceHardwareAddress());

                // Call MainActivity
                context.startActivity(intent);
            }
        });
    }

    /**
    * @return the number of items in the list of devices
    **/
    @Override
    public int getItemCount() {
        return deviceList.size();
    }


    /**
     * The internal ViewHolder pattern class used in this Recycler View adapter
    **/
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textAddress;
        LinearLayout linearLayout;

        /**
         * Constructor
         * @param v the view for the device item
         **/
        public ViewHolder(View v) {
            super(v);
            textName = v.findViewById(R.id.textViewDeviceName);
            textAddress = v.findViewById(R.id.textViewDeviceAddress);
            linearLayout = v.findViewById(R.id.linearLayoutDeviceInfo);
        }
    }
}
