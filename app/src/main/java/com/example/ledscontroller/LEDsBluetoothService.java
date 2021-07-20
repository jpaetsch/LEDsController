package com.example.ledscontroller;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.UUID;

/**
 * This class represents the single bluetooth service to keep the Bluetooth
 * connection running in the background.  HC-05 module currently used by the
 * Arduino sadly does not support Bluetooth low energy so implementing classic
 **/
public class LEDsBluetoothService extends Service {
    private static final String TAG = "LEDsBluetoothService";
    private static final String STANDARD_BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private BluetoothAdapter bluetoothAdapter;
    private static Handler handler;

    private CreateConnectionThread threadCreateConnection;
    private ConnectedThread threadConnected;

    private static final int STATE_NONE = 0;
    private static final int STATE_LISTEN = 1;
    private static final int STATE_CONNECTING = 2;
    private static final int STATE_CONNECTED = 3;

    private static int currentState = STATE_NONE;

    private final IBinder ledBinder = new LEDBinder();

    /**
     * Required to create a bound service; define interface with how a client
     * can communicate with the service; the way to return the instance with use
     * of an inner class
     * @param intent
     **/
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return ledBinder;
    }

    /**
     * This is called with arguments supplied by the client - service will continue running
     * until stopped explicitly as START_STICKY is used
     * @param intent
     * @param flags
     * @param startID
     **/
    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        String deviceAddy = intent.getStringExtra("BLUETOOTH_DEVICE");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {  // should never occur unless hardware doesn't support bt
            Log.e(TAG, "Bluetooth adapter retrieved is null");
        }

        connectToDevice(deviceAddy);
        return START_STICKY;
    }

    /**
     * This function is used to connect to a device - it controls the state of the Bluetooth
     * service and resets the connection / connected threads if necessary
     * @param macAddress the address of the device which will be connected to
     **/
    private synchronized void connectToDevice(String macAddress) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        if(currentState == STATE_CONNECTING) {
            if(threadCreateConnection != null) {
                threadCreateConnection.cancel();
                threadCreateConnection = null;
            }
        }
        if(threadConnected != null) {
            threadConnected.cancel();
            threadConnected = null;
        }
        threadCreateConnection = new CreateConnectionThread(device);
        threadCreateConnection.start();
        currentState = STATE_CONNECTING;
    }

    /**
     * Custom function to stop the LED Bluetooth service - it cancels both connecting and connected
     * threads, cancels discovery, and finally stops itself
     **/
    public synchronized void stop() {
        currentState = STATE_NONE;
        // Cancel connection thread if necessary
        if(threadCreateConnection != null) {
            threadCreateConnection.cancel();
            threadCreateConnection = null;
        }
        // Cancel connected thread if necessary
        if(threadConnected != null) {
            threadConnected.cancel();
            threadConnected = null;
        }
        // Cancel bluetooth adapter discovery if necessary
        if(bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }
        // Equivalent to calling context stop service for this service
        stopSelf();
    }

    /**
     * Stop the service with a passed intent; cancel all threads, cancel discovery,
     * and call the superclass's stopService function to end the service
     * @param name the intent used to stop the service
     **/
    @Override
    public boolean stopService(Intent name) {
        currentState = STATE_NONE;
        // Cancel connection thread if necessary
        if(threadCreateConnection != null) {
            threadCreateConnection.cancel();
            threadCreateConnection = null;
        }
        // Cancel connected thread if necessary
        if(threadConnected != null) {
            threadConnected.cancel();
            threadConnected = null;
        }
        // Cancel the bluetooth adapter discovery
        bluetoothAdapter.cancelDiscovery();
        return super.stopService(name);
    }


    /**
     * Inner class to create bound service; return this instance of LocalBinder
     * so clients can call public methods
     **/
    public class LEDBinder extends Binder {
        LEDsBluetoothService getService() {
            return LEDsBluetoothService.this;
        }
    }


    /**
     * Inner class for the thread that creates the bluetooth connection
     **/
    private class CreateConnectionThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public CreateConnectionThread(BluetoothDevice device) {
            bluetoothDevice = device;
            BluetoothSocket socket = null;
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(STANDARD_BLUETOOTH_UUID));
            } catch(IOException e1) {
                e1.printStackTrace();
            }
            bluetoothSocket = socket;
        }

        @Override
        public void run() {
            bluetoothAdapter.cancelDiscovery();

            try {
                bluetoothSocket.connect();
                Log.i(TAG, "create connection thread run(): connected");
            } catch(IOException e1) {
                try {
                    bluetoothSocket.close();
                    Log.i(TAG, "create connection thread run(): closed");
                } catch(IOException e2) {
                    e2.printStackTrace();
                }
                e1.printStackTrace();
            }
            threadConnected = new ConnectedThread(bluetoothSocket);
            threadConnected.start();
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
                Log.i(TAG, "create connection thread cancel(): cancelled");
            } catch(IOException e1) {
                e1.printStackTrace();
            }
        }
    }


    // REDO A FEW VARIABLE NAMES, FINISH FOLLOWING TUTORIAL, LOOK UP YOUTUBE VIDEOS TO FLESH IT OUT
    // AND RE-WORK CONNECTION FUNCTIONALITY TO USE THIS NEW METHODOLOGY

    /**
     * Inner class for the thread that is connected to the bluetooth for data transfer
     **/
    private class ConnectedThread extends Thread {
        private final BluetoothSocket connectedSocket
    }
}
