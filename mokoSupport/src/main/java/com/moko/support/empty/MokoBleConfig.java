package com.moko.support.empty;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.MokoBleManager;
import com.moko.ble.lib.callback.MokoResponseCallback;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.support.empty.entity.OrderCHAR;
import com.moko.support.empty.entity.OrderServices;

import androidx.annotation.NonNull;

final class MokoBleConfig extends MokoBleManager {

    private MokoResponseCallback mMokoResponseCallback;
    private BluetoothGattCharacteristic paramsCharacteristic;
    private BluetoothGatt gatt;

    public MokoBleConfig(@NonNull Context context, MokoResponseCallback callback) {
        super(context);
        mMokoResponseCallback = callback;
    }

    @Override
    public boolean checkServiceCharacteristicSupported(BluetoothGatt gatt) {
        final BluetoothGattService service = gatt.getService(OrderServices.SERVICE_PARAMS.getUuid());
        if (service != null) {
            paramsCharacteristic = service.getCharacteristic(OrderCHAR.CHAR_PARAMS.getUuid());
            this.gatt = gatt;
            return paramsCharacteristic != null;
        }
        return false;
    }

    @Override
    public void write(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        mMokoResponseCallback.onCharacteristicWrite(gatt.getDevice(), characteristic, value);
    }

    @Override
    public void read(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        mMokoResponseCallback.onCharacteristicRead(gatt.getDevice(), characteristic, value);
    }

    @Override
    public void init() {
        requestMtu(247).with((device, mtu) -> {
            XLog.i("=========New mtu:" + mtu + "=========");
        }).done(device -> {
            enableWriteConfigNotify();
        }).enqueue();
    }

    @Override
    public void onDeviceConnecting(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceConnected(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
        mMokoResponseCallback.onDeviceDisconnected(device, reason);
    }

    @Override
    public void onDeviceReady(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {

    }

    @Override
    public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
        mMokoResponseCallback.onDeviceDisconnected(device, reason);
    }

    public void enableWriteConfigNotify() {
        setNotificationCallback(paramsCharacteristic).with((device, data) -> {
            final byte[] value = data.getValue();
            XLog.e("onDataReceived");
            XLog.e("device to app : " + MokoUtils.bytesToHexString(value));
            mMokoResponseCallback.onCharacteristicChanged(device, paramsCharacteristic, value);
        });
        enableNotifications(paramsCharacteristic).done(device -> {
            mMokoResponseCallback.onServicesDiscovered(gatt);
        }).enqueue();
    }

    public void disableWriteConfigNotify() {
        disableNotifications(paramsCharacteristic).enqueue();
    }
}