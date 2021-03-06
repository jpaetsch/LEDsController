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
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
    TextView tvFeedback;

    EditText etHueField;
    EditText etSaturationField;
    EditText etValueField;

    Button btnOff;
    Button btnHSV;
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

        btnOff.setEnabled(false);
        btnHSV.setEnabled(false);
        btnRed.setEnabled(false);
        btnOrange.setEnabled(false);
        btnYellow.setEnabled(false);
        btnGreen.setEnabled(false);
        btnBlue.setEnabled(false);
        btnPurple.setEnabled(false);

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
                btnOff.setEnabled(true);
                btnHSV.setEnabled(true);
                btnRed.setEnabled(true);
                btnOrange.setEnabled(true);
                btnYellow.setEnabled(true);
                btnGreen.setEnabled(true);
                btnBlue.setEnabled(true);
                btnPurple.setEnabled(true);
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
                                tvFeedback.setTextColor(Color.RED);
                                break;
                            case 'S':
                                tvFeedback.setTextColor(Color.BLACK);
                                break;
                        }
                        // Either way the message is displayed
                        tvFeedback.setText(">>>  " + externalMessage);
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

        // Off button listener to turn the lights off
        btnOff.setOnClickListener(view -> {
            sendInstructionOffPattern();
            etHueField.setText("0");
            etSaturationField.setText("0");
            etValueField.setText("0");
        });

        // Red button listener to turn the lights solid red
        btnRed.setOnClickListener(view -> {
            sendInstructionSolidPattern(0, 255, 150);
            etHueField.setText("0");
            etSaturationField.setText("255");
            etValueField.setText("150");
        });

        // Orange button listener to turn the lights solid orange
        btnOrange.setOnClickListener(view -> {
            sendInstructionSolidPattern(15, 255, 150);
            etHueField.setText("15");
            etSaturationField.setText("255");
            etValueField.setText("150");
        });

        // Yellow button listener to turn the lights solid yellow
        btnYellow.setOnClickListener(view -> {
            sendInstructionSolidPattern(40, 230, 150);
            etHueField.setText("40");
            etSaturationField.setText("230");
            etValueField.setText("150");
        });

        // Green button listener to turn the lights solid green
        btnGreen.setOnClickListener(view -> {
            sendInstructionSolidPattern(97, 255, 150);
            etHueField.setText("97");
            etSaturationField.setText("255");
            etValueField.setText("150");
        });

        // Blue button listener to turn the lights solid blue
        btnBlue.setOnClickListener(view -> {
            sendInstructionSolidPattern(160, 255, 150);
            etHueField.setText("160");
            etSaturationField.setText("255");
            etValueField.setText("150");
        });

        // Purple button listener to turn the lights solid purple
        btnPurple.setOnClickListener(view -> {
            sendInstructionSolidPattern(194, 255, 150);
            etHueField.setText("194");
            etSaturationField.setText("255");
            etValueField.setText("150");
        });

        // Button listener for the manual setting of solid colour, make sure to validate the input
        // data to ensure it is within bounds and purely numerical, otherwise return an error msg
        btnHSV.setOnClickListener(view -> {
            int h;
            int s;
            int v;

            try {
                h = Integer.parseInt(etHueField.getText().toString());
                s = Integer.parseInt(etSaturationField.getText().toString());
                v = Integer.parseInt(etValueField.getText().toString());
            } catch(NumberFormatException e) {
                Log.e(TAG, "Error parsing the CSV solid colour value user input", e);
                h = 0;
                s = 0;
                v = 0;
            }

            if(h > 255 || s > 255 || v > 255 || h < 0 || s < 0 || v < 0)    {
                Log.e(TAG, "Invalid user input for CSV solid colour value");
                tvFeedback.setText("Invalid input, values must be in the range 0 to 255");
            } else {
                sendInstructionSolidPattern(h, s, v);
            }
        });
    }

    /**
     * Initialize all the UI elements with their view and starting values
    **/
    private void initializeViews() {
        btnConnect = findViewById(R.id.btn_connect);
        toolBar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progress_bar);
        tvFeedback = findViewById(R.id.tv_user_feedback);

        etHueField = findViewById(R.id.et_hue);
        etSaturationField = findViewById(R.id.et_saturation);
        etValueField = findViewById(R.id.et_value);

        btnOff = findViewById(R.id.btn_off);
        btnHSV = findViewById(R.id.btn_hsv);
        btnRed = findViewById(R.id.btn_red);
        btnOrange = findViewById(R.id.btn_orange);
        btnYellow = findViewById(R.id.btn_yellow);
        btnGreen = findViewById(R.id.btn_green);
        btnBlue = findViewById(R.id.btn_blue);
        btnPurple = findViewById(R.id.btn_purple);
    }

    /**
     * Builds and sends the solid pattern instruction string that will be expected by the receiver
     * @param hue the hue of the HSV colour (0 to 255)
     * @param saturation the saturation of the HSV colour (0 to 255)
     * @param value the value field of the HSV colour (0 to 255)
     **/
    private void sendInstructionSolidPattern(int hue, int saturation, int value) {
        // ERROR CHECKING

        char[] modifyInstruction = getResources().getString(R.string.instruction).toCharArray();
        modifyInstruction[1] = (char) getResources().getInteger(R.integer.id_solid_pattern);
        modifyInstruction[2] = (char) hue;
        modifyInstruction[3] = (char) saturation;
        modifyInstruction[4] = (char) value;

        Log.w("SENDING", String.valueOf(modifyInstruction));

//        char[] modifyInstruction = getResources().getString(R.string.instruction).toCharArray();
//        modifyInstruction[1] = (char) ((byte) (getResources().getInteger(R.integer.id_solid_pattern)));  // NOTE: normal cast was adding
//        modifyInstruction[2] = h;
//        modifyInstruction[3] = s;
//        modifyInstruction[4] = v;

        threadConnected.write(String.valueOf(modifyInstruction));
    }


    /**
     * Builds and sends the off pattern instruction string that will be expected by the receiver
     **/
    private void sendInstructionOffPattern() {
        // ERROR CHECKING

        char[] modifyInstruction = getResources().getString(R.string.instruction).toCharArray();
        modifyInstruction[1] = (char) getResources().getInteger(R.integer.id_off_pattern);

        threadConnected.write(String.valueOf(modifyInstruction));
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
            // Convert entered string into bytes using special encoding and write it out
            byte[] bytes = input.getBytes(StandardCharsets.ISO_8859_1);
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