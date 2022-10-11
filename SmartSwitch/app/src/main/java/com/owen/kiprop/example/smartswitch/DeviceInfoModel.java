package com.owen.kiprop.example.smartswitch;

// defined data type to store details about paired devices with an android phone
public class DeviceInfoModel {

    private String deviceName, deviceHardwareAddress;

    public DeviceInfoModel(){}

    public DeviceInfoModel(String deviceName, String deviceHardwareAddress){
        this.deviceName = deviceName;
        this.deviceHardwareAddress = deviceHardwareAddress;
    }

    public String getDeviceName(){return deviceName;}

    public String getDeviceHardwareAddress(){return deviceHardwareAddress;}

}