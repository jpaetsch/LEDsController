package com.example.ledscontroller;

public class BluetoothDeviceInformation {
    private String deviceName, deviceHardwareAddress;

    public BluetoothDeviceInformation() {

    }

    public BluetoothDeviceInformation(String deviceName, String deviceHardwareAddress) {
        this.deviceName = deviceName;
        this.deviceHardwareAddress = deviceHardwareAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceHardwareAddress() {
        return deviceHardwareAddress;
    }
}
