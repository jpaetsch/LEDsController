package com.example.ledscontroller;


import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * This class represents the single bluetooth service to keep the Bluetooth
 * connection running in the background.  HC-05 module currently used by the
 * Arduino sadly does not support Bluetooth low energy so implementing classic
 **/
public class LEDsBluetoothService extends Service {
    private static final String TAG = "LEDsBluetoothService";

    private BluetoothAdapter bluetoothAdapter;
    private static Handler handler;

    private CreateConnectionThread createConnectionThread;
    private ConnectedThread connectedThread;

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

    private synchronized void connectToDevice(String macAddress) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
        if(currentState == STATE_CONNECTING) {
            if(createConnectionThread != null) {
                createConnectionThread.cancel();
                createConnectionThread = null;
            }
        }
        if(connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
        createConnectionThread = new CreateConnectionThread(device);
        createConnectionThread.start();
        setState(STATE_CONNECTING);
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
}
