package com.example.ledscontroller;


/**
 * This class represents information attributed to a Bluetooth device, used for connection and
 * visibility purposes
 **/
public class BluetoothDeviceInformation {
    private String deviceName, deviceHardwareAddress;

    /**
     * Empty constructor
     **/
    public BluetoothDeviceInformation() {

    }

    /**
     * Constructor
     * @param deviceName the name of the bluetooth device
     * @param deviceHardwareAddress the mac address of the bluetooth device
    **/
    public BluetoothDeviceInformation(String deviceName, String deviceHardwareAddress) {
        this.deviceName = deviceName;
        this.deviceHardwareAddress = deviceHardwareAddress;
    }

    /**
     * Gets the bluetooth device name
     * @return deviceName the name of the bluetooth device
     **/
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Gets the bluetooth device mac address
     * @return deviceHardwareAddress the name of the bluetooth device
     **/
    public String getDeviceHardwareAddress() {
        return deviceHardwareAddress;
    }
}
