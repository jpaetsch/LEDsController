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


// Communication protocol:    <------------------------>
// 24 bytes enclosed by delimiters

/**
 * This class is the main activity that is transitioned to after the bluetooth device selection
 **/
public class MainActivity extends AppCompatActivity {
    // Tag strings that will be used for error or status messages associated with this activity
    // on either the app or board side
    private static final String TAG = "MyActivity";

    // Key strings linked to the bluetooth device selection activity
    private static final String KEY_DEVICE_NAME = "deviceName";
    private static final String KEY_DEVICE_ADDY = "deviceAddress";

    // Int codes that the bluetooth handler uses to identify message status and updates
    private static final int CONNECTING_STATUS = 1;
    private final static int MESSAGE_READ = 2;

    private String deviceName = null;
    private String deviceAddress;

    // Variables bluetooth threads as well as a handler for them
    public static Handler handler;
    public static BluetoothSocket bluetoothSocket;
    public static ConnectedThread threadConnected;
    public static CreateConnectThread threadCreateConnect;

    Toolbar toolBar;
    Button btnConnect;
    ProgressBar progressBar;
    TextView tvExternalMessage;

    Button btnOff;
    Button btnRed;
    Button btnOrange;
    Button btnYellow;
    Button btnGreen;
    Button btnBlue;
    Button btnPurple;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements and set their initial values
        initializeViews();

        progressBar.setVisibility(View.GONE);

        // Retrieve the device name (which cannot be null to set up the bluetooth connection)
        deviceName = getIntent().getStringExtra(KEY_DEVICE_NAME);

        if(deviceName != null) {
            // Get address to make the bluetooth connection
            deviceAddress = getIntent().getStringExtra(KEY_DEVICE_ADDY);

            // Utilize progress bar to show connection status
            btnConnect.setEnabled(false);
            toolBar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);

            // Create new thread for bluetooth connection to the selected device
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(bluetoothAdapter == null) { // device doesn't support bluetooth
                toolBar.setSubtitle("Bluetooth isn't supported on your device");
                Log.e(TAG, "Bluetooth adapter retrieved is null");
            } else { // set up new create connection thread using the retrieved adapter
                threadCreateConnect = new CreateConnectThread(bluetoothAdapter, deviceAddress);
                threadCreateConnect.start();
                Log.i(TAG, "Started bluetooth create connection thread");
            }
        } else {
            Log.e(TAG, "Passed device name is null");
        }

        // Set up the handler for the bluetooth thread with its own name-space for message codes
        handler = new Handler(Looper.getMainLooper()) {

            @Override
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case CONNECTING_STATUS: // Handler received a connection status message
                        switch(msg.arg1) {
                            case 1: // Device successfully connected
                                toolBar.setSubtitle("Connected to " + deviceName);
                                btnOff.setEnabled(true);
                                break;
                            case -1: // Device failed to connect
                                toolBar.setSubtitle("Device failed to connect");
                                break;
                        }
                        // Either way the following UI updates occur
                        progressBar.setVisibility(View.GONE);
                        btnConnect.setEnabled(true);
                        break;
                    case MESSAGE_READ: // Handler received a read message
                        String externalMessage = msg.obj.toString();
                        switch(externalMessage.charAt(0)) {
                            case 'E':
                                tvExternalMessage.setTextColor(Color.RED);
                                break;
                            case 'S':
                                tvExternalMessage.setTextColor(Color.BLACK);
                                break;
                        }
                        // Either way the message is displayed
                        tvExternalMessage.setText(">>>  " + externalMessage);
                        break;
                }
            }
        };

        // Connect button listener to select Bluetooth device via activity
        btnConnect.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this,
                    BluetoothDeviceSelectActivity.class);
            startActivity(intent);
        });

        // On/Off button listener to turn on and of the led lights
        btnOff.setOnClickListener(view -> {
            String instruction = "";
            String buttonLabel = btnOff.getText().toString();

            switch(buttonLabel) {
                case "Turn On":
                    btnOff.setText("Turn Off");
                    instruction = "<1----------------------->";
                    break;
                case "Turn Off":
                    btnOff.setText("Turn On");
                    instruction = "<0----------------------->";
                    break;
            }

            // Write the instruction to the connected bluetooth thread
            threadConnected.write(instruction);
        });
    }

    /**
     * Initialize all the UI elements with their view and starting values
    **/
    private void initializeViews() {
        btnConnect = findViewById(R.id.btn_connect);
        toolBar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);
        tvExternalMessage = findViewById(R.id.tv_external_message);
        btnOff = findViewById(R.id.btn_off);
        btnRed = findViewById(R.id.btn_red);
        btnOrange = findViewById(R.id.btn_orange);
        btnYellow = findViewById(R.id.btn_yellow);
        btnGreen = findViewById(R.id.btn_green);
        btnBlue = findViewById(R.id.btn_blue);
        btnPurple = findViewById(R.id.btn_purple);
    }

    /**
     * Set the back button press behaviour; terminate the bluetooth connections and exit app
     **/
    @Override
    public void onBackPressed() {
        // Terminate Bluetooth connection and close the app
        if(threadCreateConnect != null) {
            threadCreateConnect.cancel();
        }
        if(threadConnected != null) {
            threadConnected.cancel();
        }
        finish();
    }


    /**
     * Inner class for the thread that creates a Bluetooth connection
     **/
    public static class CreateConnectThread extends Thread {

        /**
         * Constructor
         * @param bluetoothAdapter the bluetooth adapter from which we receive the remote device
         * @param address the address of the remote device to connect to
         **/
        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            // Temporary object used as socket is final
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                // Get a socket to connect with device, may vary on different Androids
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);
            } catch(IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            bluetoothSocket = tmp;
        }

        /**
         * Run the connection thread which interacts with the handler to control the bluetooth
         * connection socket
        **/
        public void run() {
            // Cancel discovery to avoid slowing down connection
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();

            // Connect to remote device through socket, block until succeeding throwing an exception
            try {
                bluetoothSocket.connect();
            } catch(IOException connectException) { // Unable to connect so close the socket/return
                try {
                    bluetoothSocket.close();
                    Log.e(TAG, "Cannot connect to device");
                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget();
                } catch(IOException closeException) {
                    Log.e(TAG, "Could not close the client connecting thread socket",
                            closeException);
                }
                return;
            }

            // Connection attempt succeeded; new thread for work associated with the connection
            handler.obtainMessage(CONNECTING_STATUS, 1 , -1).sendToTarget();
            threadConnected = new ConnectedThread(bluetoothSocket);
            Log.i(TAG, "Started connected thread for associated bluetooth work");
            threadConnected.run();
        }

        // Close the client socket; cause the thread to finish
        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch(IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }


    /**
     * Inner class for the connected thread that handles input and output stream communications
     * between the Arduino and app
     **/
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream bluetoothInStream;
        private final OutputStream bluetoothOutStream;

        // Tag for externally received messages logging purposes and limit for input bytes
        private final static String TAG_INPUT = ">>>";
        private static final int MAX_INPUT_BYTES = 1024;

        /**
         * Constructor
         * @param socket The bluetooth socket that the input and output streams are connected to
         **/
        public ConnectedThread(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get input/output streams, using temporary objects as member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch(IOException e) {
                Log.e(TAG, "Bluetooth connected thread input/output stream", e);
            }

            bluetoothInStream = tmpIn;
            bluetoothOutStream = tmpOut;
        }

        /**
         * This function handles the running of the bluetooth connected thread and works with
         * associated activity ie. listens for input stream data, logs it, communicates status with
         * the handler, etc.
         **/
        public void run() {
            byte[] buffer = new byte[MAX_INPUT_BYTES];    // store for the stream
            int bytes = 0;    // number of bytes returned from read()

            // Listen to the input stream until an issue of either exception occurs or max bytes
            // are read in (should hopefully never occur)
            while(true) {
                try {
                    // Read from the input stream until termination character is reached - once
                    // that occurs send the whole string message to the handler
                    buffer[bytes] = (byte) bluetoothInStream.read();
                    String externalMessage;
                    if(buffer[bytes] == '\n') {
                        externalMessage = new String(buffer, 0, bytes);
                        logExternalMessage(externalMessage);
                        handler.obtainMessage(MESSAGE_READ, externalMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        // Ensure bytes from the input do not exceed the max limit constant set
                        bytes++;
                        if(bytes == MAX_INPUT_BYTES) {
                            Log.e(TAG, "Input stream max bytes input error");
                            break;
                        }
                    }
                } catch(IOException e) {
                    Log.e(TAG, "Input stream listen exception", e);
                    break;
                }
            }
        }

        /**
         * This function writes data to the output stream of the bluetooth connected thread and
         * logs any exception error that occurs during that process
         * @param input the string that will be converted to bytes and written out
        **/
        public void write(String input) {
            // Convert entered string into bytes using default utf-8 encoding and write it out
            byte[] bytes = input.getBytes();
            try {
                bluetoothOutStream.write(bytes);
            } catch(IOException e) {
                Log.e(TAG, "Unable to send message", e);
            }
        }

        /**
         *  Call to shutdown the connection
         **/
        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch(IOException e) {
                Log.e(TAG,"Could not close the client connected thread socket", e);
            }
        }

        /**
         * Log the external messages received from the microcontroller board - should be either
         * Status: message
         * OR
         * Error: message
         * In string format according to the documentation folder - logging categorization and
         * severity will be based on the first character of that string
         * @param received the string that was received and that will be logged
         **/
        private void logExternalMessage(String received) {
            switch(received.charAt(0)) {
                case 'E':
                    Log.e(TAG, "Error message received:");
                    Log.e(TAG_INPUT, received);
                    break;
                case 'S':
                    Log.i(TAG, "Status message received:");
                    Log.i(TAG_INPUT, received);
                    break;
                default:
                    Log.e(TAG, "Unexpected format message received:");
                    Log.w(TAG_INPUT, received);
                    break;
            }
        }
    }
}