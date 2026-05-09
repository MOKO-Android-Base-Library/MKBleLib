package com.moko.ble.lib.callback;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

public interface MokoResponseCallback {

    void onCharacteristicChanged(BluetoothDevice device, BluetoothGattCharacteristic characteristic, byte[] value);

    void onCharacteristicWrite(BluetoothDevice device, BluetoothGattCharacteristic characteristic, byte[] value);

    void onCharacteristicRead(BluetoothDevice device, BluetoothGattCharacteristic characteristic, byte[] value);

    void onServicesDiscovered(BluetoothGatt gatt);

    void onDeviceDisconnected(BluetoothDevice device, int reason);
}
