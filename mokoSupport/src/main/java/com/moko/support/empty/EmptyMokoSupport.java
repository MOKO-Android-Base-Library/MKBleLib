package com.moko.support.empty;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.MokoBleLib;
import com.moko.ble.lib.MokoBleManager;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.support.empty.entity.OrderCHAR;
import com.moko.support.empty.handler.MokoCharacteristicHandler;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class EmptyMokoSupport extends MokoBleLib {
    private Map<String, Map<OrderCHAR, BluetoothGattCharacteristic>> mCharacteristicMap = new LinkedHashMap<>();

    private static volatile EmptyMokoSupport INSTANCE;

    private Context mContext;

    private Map<String, MokoBleConfig> mBleConfigMap = new LinkedHashMap<>();


    private EmptyMokoSupport() {
        //no instance
    }

    public static EmptyMokoSupport getInstance() {
        if (INSTANCE == null) {
            synchronized (EmptyMokoSupport.class) {
                if (INSTANCE == null) {
                    INSTANCE = new EmptyMokoSupport();
                }
            }
        }
        return INSTANCE;
    }

    public void init(Context context) {
        mContext = context;
        super.init(context);
    }

    @Override
    public MokoBleManager getMokoBleManager(String address) {
        MokoBleConfig bleConfig = mBleConfigMap.get(address);
        if (bleConfig == null) {
            bleConfig = new MokoBleConfig(mContext, this);
            mBleConfigMap.put(address, bleConfig);
        }
        return bleConfig;
    }

    /// Connect

    @Override
    public void onDeviceConnected(BluetoothGatt gatt) {
        if (mCharacteristicMap.get(gatt.getDevice().getAddress()) == null) {
            mCharacteristicMap.put(gatt.getDevice().getAddress(), new MokoCharacteristicHandler().getCharacteristics(gatt));
        }
        ConnectStatusEvent connectStatusEvent = new ConnectStatusEvent();
        connectStatusEvent.setAction(MokoConstants.ACTION_DISCOVER_SUCCESS);
        connectStatusEvent.setBluetoothDevice(gatt.getDevice());
        EventBus.getDefault().post(connectStatusEvent);
    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice device) {
        mBleConfigMap.remove(device.getAddress());
        mCharacteristicMap.remove(device.getAddress());
        ConnectStatusEvent connectStatusEvent = new ConnectStatusEvent();
        connectStatusEvent.setAction(MokoConstants.ACTION_DISCONNECTED);
        connectStatusEvent.setBluetoothDevice(device);
        EventBus.getDefault().post(connectStatusEvent);
    }

    @Override
    public BluetoothGattCharacteristic getCharacteristic(String address, Enum orderCHAR) {
        return mCharacteristicMap.get(address).get(orderCHAR);
    }

    public ArrayList<String> getConnectedDeviceList() {
        return new ArrayList<>(mCharacteristicMap.keySet());
    }

    /// OrderTask

    @Override
    public boolean isCHARNull(String address) {
        if (mCharacteristicMap == null || mCharacteristicMap.isEmpty()) {
            disConnectBle(address);
            return true;
        }
        return false;
    }

    @Override
    public void orderFinish() {
        OrderTaskResponseEvent event = new OrderTaskResponseEvent();
        event.setAction(MokoConstants.ACTION_ORDER_FINISH);
        EventBus.getDefault().post(event);
    }

    @Override
    public void orderTimeout(OrderTaskResponse response) {
        OrderTaskResponseEvent event = new OrderTaskResponseEvent();
        event.setAction(MokoConstants.ACTION_ORDER_TIMEOUT);
        event.setResponse(response);
        EventBus.getDefault().post(event);
    }

    @Override
    public void orderResult(OrderTaskResponse response) {
        OrderTaskResponseEvent event = new OrderTaskResponseEvent();
        event.setAction(MokoConstants.ACTION_ORDER_RESULT);
        event.setResponse(response);
        EventBus.getDefault().post(event);
    }

    @Override
    public boolean orderResponseValid(BluetoothGattCharacteristic characteristic, OrderTask orderTask) {
        final UUID responseUUID = characteristic.getUuid();
        final OrderCHAR orderCHAR = (OrderCHAR) orderTask.orderCHAR;
        return responseUUID.equals(orderCHAR.getUuid());
    }

    @Override
    public boolean orderNotify(BluetoothDevice device, BluetoothGattCharacteristic characteristic, byte[] value) {
        final UUID responseUUID = characteristic.getUuid();
        OrderCHAR orderCHAR = null;
        if (responseUUID.equals(OrderCHAR.CHAR_PARAMS.getUuid())) {
            orderCHAR = OrderCHAR.CHAR_PARAMS;
        }
        if (orderCHAR == null)
            return false;
        XLog.i(orderCHAR.name());
        OrderTaskResponse response = new OrderTaskResponse();
        response.orderCHAR = OrderCHAR.CHAR_PARAMS;
        response.responseValue = value;
        response.address = device.getAddress();
        OrderTaskResponseEvent event = new OrderTaskResponseEvent();
        event.setAction(MokoConstants.ACTION_CURRENT_DATA);
        event.setResponse(response);
        EventBus.getDefault().post(event);
        return true;
    }
}
