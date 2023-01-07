package com.example.ledscontroller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.*

class LEDsBluetoothService : Service() {

    // Integer representing service state
    private var state: Int = Constants.STATE_UNINITIALIZED

    private lateinit var handler: Handler

    // Bluetooth connection threads
    private var threadCreateConnection: CreateConnectionThread? = null
    private var threadConnected: ConnectedThread? = null

    // Bluetooth components
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null

    /**
     * This function overrides the creation of the service, creating the notification channel and
     * starting the foreground service
     **/
    override fun onCreate() {
        super.onCreate()
        Log.i(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Service created")
        val channelId = createNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID, Constants.NOTIFICATION_CHANNEL_NAME)
        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle(getText(R.string.bluetooth_notification_title))
            .setContentText(getText(R.string.bluetooth_notification_message))
            //.addAction(action)
            //.setSmallIcon(R.drawable.bluetooth_notification_icon)
            //.setTicker(getText(R.string.bluetooth_notification_ticker_text))
            .build()
        startForeground(Constants.NOTIFICATION_ID, notification)
    }

    /**
     * This function overrides the service start to set it up to run in the foreground
     * With a required notification issued to the user to indicate the service is running
     * and using system resources
     * @param intent
     * @param flags
     * @param startId
     * @return value indicating this service will continue running until stopped explicitly
    **/
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        setState(Constants.STATE_INITIALIZED)
        return START_STICKY;
    }

    /**
     * This function creates the notification channel that is required and used by this service
     * @param channelId the unique id of this channel
     * @param channelName the user visible name of the channel
     * @return channelId
    **/
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }

    /**
     * This function returns the current status of the LEDs bluetooth controller service in a
     * string format to be easily displayed by UI elements
     * @param deviceName the bluetooth device name if the service is currently connecting
     * @return statusString
     **/
    public fun getStateString(deviceName: String = ""): String {
        val statusString = when (state) {
            -2 -> "Please enable bluetooth on your device"
            -1 -> "Bluetooth isn't supported on your device"
            0 -> "LEDs bluetooth controller is starting up"
            1 -> "LEDs bluetooth controller is standing by"
            2 -> "LEDs bluetooth controller is connecting to $deviceName"
            3 -> "LEDs bluetooth controller is connected to $deviceName"
            else -> {
                Log.e(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "getStateString tried to get unimplemented state: $state")
                throw NotImplementedError()
            }
        }
        return statusString
    }

    /**
     * Expose the state value of the service for custom and responsive UI options, etc.
     **/
    public fun getState(): Int {
        return state
    }

    /**
     * This boolean function should be used to set the state variable of the service - it attempts
     * to set the desired state and performs various checks for state transitions
     * @param codeToSet the desired state the service is attempting to transition to
     * @return true if the desired state was set and false if a different state was set
     **/
    private fun setState(codeToSet: Int): Boolean {
        when (codeToSet) {
            Constants.STATE_INITIALIZED -> {
                threadCreateConnection?.cancel()
                threadConnected?.cancel()
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                return when {
                    bluetoothAdapter == null -> {  // device doesn't support bluetooth
                        Log.e(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Bluetooth adapter is null")
                        state = Constants.STATE_NULL_BLUETOOTH_ADAPTER
                        false
                    }
                    bluetoothAdapter?.isEnabled == false -> {
                        Log.w(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Bluetooth adapter is disabled")
                        state = Constants.STATE_BLUETOOTH_DISABLED
                        false
                    }
                    else -> {
                        Log.i(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Bluetooth adapter successfully set up")
                        state = Constants.STATE_INITIALIZED
                        true
                    }
                }
            }
            Constants.STATE_CONNECTING -> {
                state = Constants.STATE_CONNECTING
                return true
            }
            else -> {
                Log.e(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "setState tried to set unimplemented state: $codeToSet")
                throw NotImplementedError()
            }
        }
    }

    /**
     * This function listens for devices based on pairing options available to the
     * bluetooth adapter
     * @return a list of bluetooth devices (may be empty if no devices are paired)
     **/
    public fun listenForDevices() : ArrayList<BluetoothDevice> {
        val deviceList: ArrayList<BluetoothDevice> = ArrayList<BluetoothDevice>()

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            deviceList.add(device)
        }
        return deviceList
    }

    /**
     * This function is used to connect to a bluetooth device
     * @param bluetoothDevice the device to connect to (uses this device's mac address string)
     **/
    public fun connectToDevice(bluetoothDevice: BluetoothDevice) {
        setState(Constants.STATE_CONNECTING)
        if(setState(Constants.STATE_INITIALIZED)) {
            threadCreateConnection = CreateConnectionThread(bluetoothDevice)
            threadCreateConnection?.start()
            val deviceName = bluetoothDevice.name
            Log.i(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Connecting to device: $deviceName")
        }
    }

    /**
     * This function is the binding function for this service
     * @param intent
     * @return null since no binding is provided as this is a foreground service
     **/
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * This function is called when this foreground service is destroyed
     **/
    override fun onDestroy() {
        threadCreateConnection?.cancel()
        threadConnected?.cancel()
        super.onDestroy()
    }

    /**
     * Inner class for the thread that creates the bluetooth connection
     **/
    private inner class CreateConnectionThread(device: BluetoothDevice) : Thread() {
        var uuid: UUID = device.uuids[0].uuid
        var tmpSocket =
            try {
                device.createInsecureRfcommSocketToServiceRecord(uuid)
            } catch(e: IOException) {
                Log.e(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Create connection's socket creation failed", e)
            } as BluetoothSocket

        init {
            bluetoothSocket = tmpSocket
        }

        /**
         * This function runs the connection thread, interacting with the handler to control the
         * bluetooth socket connection creation
         **/
        public override fun run() {
            // Cancel discovery to avoid slowing down connection
            bluetoothAdapter?.cancelDiscovery()

            // Connect to remote device through the bluetooth socket, blocking until
            // success or exception
            try {
                bluetoothSocket?.connect()
            } catch(e: IOException) {
                Log.e(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Failed to connect to device", e)
                try {
                    bluetoothSocket?.close()
                    // TODO implement handler message for other thread here
                } catch(e: IOException) {
                    Log.e(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Failed to close the create connection thread socket", e)
                }
                return
            }
            // Connection attempt succeeded; new thread for work associated with the connection
            // TODO implement handler message for other thread here
            Log.i(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Socket connected, starting connected thread for associated bluetooth work")
            threadConnected = ConnectedThread()
            threadConnected?.run()
        }

        /**
         * This function cancels the client socket and causes the thread to finish
         **/
        public fun cancel() {
            try {
                bluetoothSocket?.close()
            } catch(e: IOException) {
                Log.e(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Connecting thread failed to close the client socket", e)
            }
        }
    }


    /**
     * Inner class for the thread that is connected via bluetooth for the data transfer
     **/
    private inner class ConnectedThread() : Thread() {
        var bluetoothInputStream: InputStream? = null
        var bluetoothOutputStream: OutputStream? = null

        var tmpInputStream =
            try {
                bluetoothSocket?.inputStream
            } catch(e: IOException) {
                Log.e(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Failed to set up bluetooth input stream", e)
            } as InputStream
        var tmpOutputStream =
            try {
                bluetoothSocket?.outputStream
            } catch(e: IOException) {
                Log.e(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Failed to set up bluetooth output stream", e)
            } as OutputStream

        init {
            bluetoothInputStream = tmpInputStream
            bluetoothOutputStream = tmpOutputStream
        }

        /**
         * This function reads data from the input stream of the bluetooth connected thread
         **/
        public override fun run() {
            while(true) {
                try {
                    val inputAsString = bluetoothInputStream?.bufferedReader(Charsets.UTF_8).use {
                        it?.readText()
                    }
                    logExternalMessage(inputAsString)
                    //TODO add handler here
                } catch(e: IOException) {
                    Log.e(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Input stream message reading exception", e)
                    break
                }
            }
        }

        /**
         * This function writes data to the output stream of the bluetooth connected thread
         * @param data a string that will be encoded to bytes and written out
         **/
        public fun write(data: String) {
            // Convert entered string into bytes using special encoding and write it out
            val inputAsBytes = data.toByteArray(Charsets.ISO_8859_1)
            try {
                bluetoothOutputStream?.write(inputAsBytes)
            } catch (e: IOException) {
                Log.e(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Output stream message sending exception", e)
            }
        }

        /**
         * This function cancels the client socket and causes the thread to finish
         **/
        public fun cancel() {
            try {
                bluetoothSocket?.close()
            } catch(e: IOException) {
                Log.e(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Connected thread failed to close the client socket", e)
            }
        }

        /**
         * Log the external messages received from the microcontroller board.  The expected format
         * is either
         * Status: <message>
         *     or
         * Error: <message>
         * @param received the string that was received which will be logged
         **/
        private fun logExternalMessage(received: String?) {
            if(received != null) {
                when(received.first()) {
                    'S' -> {
                        Log.i(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Status message received: $received")
                    }
                    'E' -> {
                        Log.w(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Error message received: $received")
                    }
                    else -> {
                        Log.e(Constants.TAG_LEDS_BLUETOOTH_SERVICE, "Unexpected message received: $received")
                    }
                }
            }
        }
    }
}