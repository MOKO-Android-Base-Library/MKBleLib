package com.moko.ble.lib.callback;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

public interface MokoResponseCallback {

    void onCharacteristicChanged(BluetoothGattCharacteristic characteristic, byte[] value);

    void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, byte[] value);

    void onCharacteristicRead(BluetoothGattCharacteristic characteristic, byte[] value);

    void onServicesDiscovered(BluetoothGatt gatt);

    void onDeviceDisconnected(BluetoothDevice device, int reason);
}
