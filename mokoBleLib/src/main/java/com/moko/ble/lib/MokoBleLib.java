package com.moko.ble.lib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.callback.MokoResponseCallback;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import androidx.annotation.NonNull;

public abstract class MokoBleLib implements MokoResponseCallback {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BlockingQueue<OrderTask> mQueue;

    private Context mContext;

    private MokoBleManager mokoBleManager;

    private Handler mHandler;


    public MokoBleLib() {
        mQueue = new LinkedBlockingQueue<>();
    }

    public void init(Context context) {
        mContext = context;
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler(Looper.getMainLooper());
        mokoBleManager = getMokoBleManager();
    }

    public void connDevice(@NonNull String address) {
        if (!isBluetoothOpen()) {
            XLog.i("connDevice: bluetooth close");
            return;
        }
        if (isConnDevice(address)) {
            XLog.i("connDevice: device connected");
            disConnectBle();
            return;
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    XLog.i("start connect");
                    mokoBleManager.connect(device)
                            .retry(5, 200)
                            .timeout(50000)
                            .enqueue();
                }
            });
        } else {
            XLog.i("connDevice: the device is null");
        }
    }

    public boolean isBluetoothOpen() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public boolean isConnDevice(String address) {
        if (mContext == null || mBluetoothAdapter == null)
            return false;
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        int connState = bluetoothManager.getConnectionState(mBluetoothAdapter.getRemoteDevice(address), BluetoothProfile.GATT);
        return connState == BluetoothProfile.STATE_CONNECTED;
    }

    public void disConnectBle() {
        mokoBleManager.disconnect().enqueue();
    }

    public void enableBluetooth() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.enable();
        }
    }

    public void disableBluetooth() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.disable();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////


    public void sendOrder(OrderTask... orderTasks) {
        if (orderTasks.length == 0) {
            return;
        }
        if (!isSyncData()) {
            for (OrderTask ordertask : orderTasks) {
                if (ordertask == null) {
                    continue;
                }
                mQueue.offer(ordertask);
            }
            executeTask();
        } else {
            for (OrderTask ordertask : orderTasks) {
                if (ordertask == null) {
                    continue;
                }
                mQueue.offer(ordertask);
            }
        }
    }

    public void executeTask() {
        if (!isSyncData()) {
            orderFinish();
            return;
        }
        if (mQueue.isEmpty()) {
            return;
        }
        final OrderTask orderTask = mQueue.peek();
        if (mBluetoothGatt == null) {
            XLog.i("executeTask : BluetoothGatt is null");
            disConnectBle();
            return;
        }
        if (orderTask == null) {
            XLog.i("executeTask : orderTask is null");
            disConnectBle();
            return;
        }
        if (isCHARNull()) {
            XLog.i("executeTask : characteristicMap is null");
            disConnectBle();
            return;
        }
        final Enum orderCHAR = orderTask.orderCHAR;
        final BluetoothGattCharacteristic characteristic = getCharacteristic(orderCHAR);
        if (characteristic == null) {
            XLog.i("executeTask : mokoCharacteristic is null");
            disConnectBle();
            return;
        }
        sendOrder(orderTask, characteristic);
        timeoutHandler(orderTask);
    }

    private void sendOrder(OrderTask orderTask, final BluetoothGattCharacteristic characteristic) {
        if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_READ) {
            XLog.i("app to device read : " + orderTask.orderCHAR.name());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothGatt.readCharacteristic(characteristic);
                }
            }, 50);
        } else if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_WRITE) {
            XLog.i("app to device write : " + orderTask.orderCHAR.name());
            XLog.i(MokoUtils.bytesToHexString(orderTask.assemble()));
            characteristic.setValue(orderTask.assemble());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothGatt.writeCharacteristic(characteristic);
                }
            }, 50);
        } else if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE) {
            XLog.i("app to device write no response : " + orderTask.orderCHAR.name());
            XLog.i(MokoUtils.bytesToHexString(orderTask.assemble()));
            characteristic.setValue(orderTask.assemble());
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothGatt.writeCharacteristic(characteristic);
                }
            }, 50);
        }

    }

    public void sendDirectOrder(OrderTask orderTask) {
        final BluetoothGattCharacteristic characteristic = getCharacteristic(orderTask.orderCHAR);
        XLog.i("app to device write direct : " + orderTask.orderCHAR.name());
        XLog.i(MokoUtils.bytesToHexString(orderTask.assemble()));
        characteristic.setValue(orderTask.assemble());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothGatt.writeCharacteristic(characteristic);
            }
        }, 50);
    }

    public synchronized boolean isSyncData() {
        return mQueue != null && !mQueue.isEmpty();
    }

    public void pollTask() {
        if (mQueue != null && !mQueue.isEmpty()) {
            OrderTask orderTask = mQueue.peek();
            XLog.i("remove " + orderTask.orderCHAR.name());
            mQueue.poll();
        }
    }

    public void timeoutHandler(final OrderTask orderTask) {
        Runnable timeoutRunner = () -> {
            if (orderTask.orderStatus != OrderTask.ORDER_STATUS_SUCCESS) {
                if (orderTask.timeoutPreTask()) {
                    pollTask();
                    executeTask();
                    orderTimeout(orderTask.response);
                }
            }
        };
        mHandler.postDelayed(timeoutRunner, orderTask.delayTime);
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (!orderNotify(characteristic, value)) {
            if (isSyncData()) {
                OrderTask orderTask = mQueue.peek();
                if (value != null
                        && value.length > 0
                        && orderTask != null
                        && orderTask.response != null
                        && orderResponseValid(characteristic, orderTask)
                        && orderTask.parseValue(value)) {
                    formatCommonOrder(orderTask, value);
                }
            }
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (!isSyncData()) {
            return;
        }
        OrderTask orderTask = mQueue.peek();
        if (value != null
                && value.length > 0
                && orderTask != null
                && orderTask.response != null
                && orderTask.response.responseType == OrderTask.RESPONSE_TYPE_WRITE
                && orderResponseValid(characteristic, orderTask)
                && orderTask.parseValue(value)) {
            formatCommonOrder(orderTask, value);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (!isSyncData()) {
            return;
        }
        OrderTask orderTask = mQueue.peek();
        if (value != null
                && value.length > 0
                && orderTask != null
                && orderTask.response != null
                && orderTask.response.responseType == OrderTask.RESPONSE_TYPE_READ
                && orderResponseValid(characteristic, orderTask)
                && orderTask.parseValue(value)) {
            formatCommonOrder(orderTask, value);
        }
    }

    private void formatCommonOrder(OrderTask task, byte[] value) {
        task.orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        task.response.responseValue = value;
        mQueue.poll();
        executeTask();
        orderResult(task.response);
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt) {
        mBluetoothGatt = gatt;
        onDeviceConnected(gatt);
    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice device, int reason) {
        if (isSyncData()) {
            mQueue.clear();
        }
        XLog.i("disconnected reason:" + reason);
        onDeviceDisconnected(device);
    }


    public abstract MokoBleManager getMokoBleManager();

    public abstract void onDeviceConnected(BluetoothGatt gatt);

    public abstract void onDeviceDisconnected(BluetoothDevice device);

    public abstract BluetoothGattCharacteristic getCharacteristic(Enum orderCHAR);

    public abstract boolean isCHARNull();

    public abstract void orderFinish();

    public abstract void orderTimeout(OrderTaskResponse response);

    public abstract void orderResult(OrderTaskResponse response);

    public abstract boolean orderNotify(BluetoothGattCharacteristic characteristic, byte[] value);

    public abstract boolean orderResponseValid(BluetoothGattCharacteristic characteristic, OrderTask orderTask);
}
