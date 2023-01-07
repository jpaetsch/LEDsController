package com.example.ledscontroller

object Constants {

    // --- LEDS SECTION ---

    // Blank instruction constant with delimiters
    const val INSTRUCTION = "<------------------------>"

    // LED Pattern Ids
    const val ID_OFF_PATTERN = 0
    const val ID_SOLID_PATTERN = 1

    // ---  ---

    // --- DEBUGGING TAGS SECTION ---

    // Tags for different activities and components for logging purposes
    const val TAG_LEDS_BLUETOOTH_SERVICE = "LEDsBluetoothService"
    const val TAG_MAIN_ACTIVITY = "MainActivity"

    // ---  ---

    // --- INTENT KEYS SECTION ---

    // Key-value pairs for service intent commands
    const val KEY_SERVICE_COMMAND = "command"
    const val VAL_SERVICE_COMMAND_START = "start"
    const val VAL_SERVICE_COMMAND_STOP = "stop"

    // ---  ---

    // --- BLUETOOTH SECTION ---

    // Status valid state integers
    const val STATE_UNINITIALIZED = 0
    const val STATE_INITIALIZED = 1
    const val STATE_CONNECTING = 2
    const val STATE_CONNECTED = 3

    // Status error state integers
    const val STATE_NULL_BLUETOOTH_ADAPTER = -1
    const val STATE_BLUETOOTH_DISABLED = -2

    // UUID for bluetooth connections
    const val STANDARD_BLUETOOTH_UUID = "00001101-0000-1000-8000-00805F9B34FB"

    // Notification Constants
    const val NOTIFICATION_ID = 420
    const val NOTIFICATION_CHANNEL_ID = "LEDsBluetoothServiceNotificationChannel"
    const val NOTIFICATION_CHANNEL_NAME = "LEDs Bluetooth"

    // Limit for input bytes received
    const val MAX_INPUT_BYTES = 1024

    // ---  ---
}