package com.moko.ble.lib;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.utils.MokoUtils;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.observer.ConnectionObserver;

public abstract class MokoBleManager extends BleManager implements ConnectionObserver {

    @Override
    public void log(int priority, @NonNull String message) {
        XLog.v(message);
    }

    public MokoBleManager(@NonNull Context context) {
        super(context);
        setConnectionObserver(this);
    }

    @NonNull
    @Override
    protected BleManagerGattCallback getGattCallback() {
        return new MokoBleManagerGattCallback();
    }

    public class MokoBleManagerGattCallback extends BleManagerGattCallback {

        @Override
        protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
            return checkServiceCharacteristicSupported(gatt);
        }

        @Override
        protected void initialize() {
            // set mtu、enable characteristic
            init();
        }

        @Override
        protected void onServicesInvalidated() {
            XLog.e("onDeviceDisconnected()");
        }

        @Override
        protected void onCharacteristicWrite(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
            XLog.e("onCharacteristicWrite");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(characteristic.getValue()));
            write(gatt, characteristic, characteristic.getValue());
        }

        @Override
        protected void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
            XLog.e("onCharacteristicRead");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(characteristic.getValue()));
            read(gatt, characteristic, characteristic.getValue());
        }
    }

    public abstract boolean checkServiceCharacteristicSupported(BluetoothGatt gatt);

    public abstract void write(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value);

    public abstract void read(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value);

    public abstract void init();
}
