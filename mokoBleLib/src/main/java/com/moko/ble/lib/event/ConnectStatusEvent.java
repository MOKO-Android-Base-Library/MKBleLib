package com.moko.ble.lib.event;

import android.bluetooth.BluetoothDevice;

public class ConnectStatusEvent {
    private String action;
    private BluetoothDevice bluetoothDevice;

    public BluetoothDevice getBluetoothDevice(){
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice){
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

}
