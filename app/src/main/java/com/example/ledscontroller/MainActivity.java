package com.example.ledscontroller;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static android.content.ContentValues.TAG;


// Communication protocol:    <------------------------>


public class MainActivity extends AppCompatActivity {

    private static final String KEY_DEVICE_NAME = "deviceName";
    private static final String KEY_DEVICE_ADDY = "deviceAddress";

    private String deviceName = null;
    private String deviceAddress;
    public static Handler handler;
    public static BluetoothSocket bluetoothSocket;
    public static ConnectedThread threadConnected;
    public static CreateConnectThread threadCreateConnect;

    /* Bluetooth handler uses these to identify message status and update */
    private static final int CONNECTING_STATUS = 1;
    private final static int MESSAGE_READ = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Initialize all our UI elements */
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final Button btnConnect = findViewById(R.id.btn_connect);
        final ProgressBar progressbar = findViewById(R.id.progress_bar);
        progressbar.setVisibility(View.GONE);
        final TextView tvExternalMessage = findViewById(R.id.tv_external_message);
        final Button btnOn = findViewById(R.id.btn_on);
        btnOn.setText("Turn On");
        btnOn.setEnabled(false);

        /* If a device has been selected from BluetoothDeviceSelectActivity */
        deviceName = getIntent().getStringExtra(KEY_DEVICE_NAME);
        if(deviceName != null) {
            /* Get address to make the bluetooth connection */
            deviceAddress = getIntent().getStringExtra(KEY_DEVICE_ADDY);

            /* Utilize progress bar to show connection status */
            btnConnect.setEnabled(false);
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressbar.setVisibility(View.VISIBLE);

            /* Create new thread for bluetooth connection to the selected device */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            threadCreateConnect = new CreateConnectThread(bluetoothAdapter, deviceAddress);
            threadCreateConnect.start();
        }

        /* Handler for the Bluetooth thread */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case CONNECTING_STATUS:
                        switch(msg.arg1) {
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressbar.setVisibility(View.GONE);
                                btnConnect.setEnabled(true);
                                btnOn.setEnabled(true);
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressbar.setVisibility(View.GONE);
                                btnConnect.setEnabled(true);
                                break;
                        }
                        break;
                    case MESSAGE_READ:
                        String externalMessage = msg.obj.toString();
                        switch(externalMessage.charAt(0)) {
                            case 'E':
                                tvExternalMessage.setTextColor(Color.RED);
                                break;
                            case 'S':
                                tvExternalMessage.setTextColor(Color.BLACK);
                                break;
                        }
                        tvExternalMessage.setText("Client Message >>>  " + externalMessage);
                    break;
                }
            }
        };

        /* Connect button listener to select Bluetooth device via activity */
        btnConnect.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this,
                    BluetoothDeviceSelectActivity.class);
            startActivity(intent);
        });

        /* On/Off button listener to turn on and of the led lights */
        btnOn.setOnClickListener(view -> {
            String instruction = null;
            String buttonLabel = btnOn.getText().toString();

            switch(buttonLabel) {
                case "Turn On":
                    btnOn.setText("Turn Off");
                    instruction = "<1----------------------->";
                    break;
                case "Turn Off":
                    btnOn.setText("Turn On");
                    instruction = "<0----------------------->";
                    break;
            }

            threadConnected.write(instruction);
        });
    }


    /* Class for the thread that creates a Bluetooth connection */
    public static class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /* Temporary object used as socket is final */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /* Get a socket to connect with device, may vary on different Androids */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch(IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            bluetoothSocket = tmp;
        }

        public void run() {
            /* Cancel discovery to avoid slowing down connection */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();

            try {
                /* Connect to remote device through socket, block until succeeding or
                * throwing an exception */
                bluetoothSocket.connect();
                Log.e("Status", "Device connected");
                handler.obtainMessage(CONNECTING_STATUS, 1 , -1).sendToTarget();
            } catch(IOException connectException) {
                /* Unable to connect so close the socket and return */
                try {
                    bluetoothSocket.close();
                    Log.e("Status", "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch(IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            /* Connection attempt succeeded; new thread for work associated with the connection */
            threadConnected = new ConnectedThread(bluetoothSocket);
            threadConnected.run();
        }

        /* Close the client socket; cause the thread to finish */
        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch(IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }


    /* Class for the thread that transfers data */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream bluetoothInStream;
        private final OutputStream bluetoothOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            /* Get input/output streams, using temporary objects as member streams are final */
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch(IOException e) {

            }

            bluetoothInStream = tmpIn;
            bluetoothOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];    // store for the stream
            int bytes = 0;    // number of bytes returned from read()

            /* Listen to the input stream until an exception occurs */
            while(true) {
                try {
                    /* Read from the input stream until termination character is reached - once
                    * that occurs send the whole string message to the handler */
                    buffer[bytes] = (byte) bluetoothInStream.read();
                    String externalMessage;
                    if(buffer[bytes] == '\n') {
                        externalMessage = new String(buffer, 0, bytes);
                        Log.e("External Message", externalMessage);
                        handler.obtainMessage(MESSAGE_READ, externalMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call from main activity to send date to the remote device */
        public void write(String input) {
            /* Convert entered string into bytes */
            byte[] bytes = input.getBytes();
            try {
                bluetoothOutStream.write(bytes);
            } catch(IOException e) {
                Log.e("Send Error", "Unable to send message", e);
            }
        }

        /* Call from main activity to shutdown the connection */
        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch(IOException e) {

            }
        }
    }


    /* Terminate connection when back is pressed */
    @Override
    public void onBackPressed() {
        /* Terminate Bluetooth connection and close app */
        if(threadCreateConnect != null) {
            threadCreateConnect.cancel();
        }
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }
}