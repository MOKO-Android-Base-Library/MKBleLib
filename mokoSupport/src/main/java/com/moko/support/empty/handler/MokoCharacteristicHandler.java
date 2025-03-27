package com.moko.support.empty.handler;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import com.moko.support.empty.entity.OrderCHAR;
import com.moko.support.empty.entity.OrderServices;

import java.util.HashMap;

public class MokoCharacteristicHandler {

    private HashMap<OrderCHAR, BluetoothGattCharacteristic> mCharacteristicMap;

    public MokoCharacteristicHandler() {
        //no instance
        mCharacteristicMap = new HashMap<>();
    }

    public HashMap<OrderCHAR, BluetoothGattCharacteristic> getCharacteristics(final BluetoothGatt gatt) {
        if (mCharacteristicMap != null && !mCharacteristicMap.isEmpty()) {
            mCharacteristicMap.clear();
        }
        BluetoothGattService service = gatt.getService(OrderServices.SERVICE_PARAMS.getUuid());
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(OrderCHAR.CHAR_PARAMS.getUuid());
            if (characteristic != null) {
                mCharacteristicMap.put(OrderCHAR.CHAR_PARAMS, characteristic);
            }
        }
        return mCharacteristicMap;
    }
}
