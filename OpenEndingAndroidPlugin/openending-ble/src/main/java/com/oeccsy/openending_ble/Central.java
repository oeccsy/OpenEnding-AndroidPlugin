package com.oeccsy.openending_ble;

import static android.content.Context.BLUETOOTH_SERVICE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class Central extends Plugin {

    private static Central _instance;

    public static Central getInstance() {
        if (_instance == null) {
            _instance = new Central();
        }
        return _instance;
    }

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private Handler centralThreadHandler;

    private ArrayList<BluetoothDevice> scanResults = new ArrayList<BluetoothDevice>();
    private HashMap<String, BluetoothDevice> connectedDevices = new HashMap();
    private HashMap<String, BluetoothGatt> connectedGATT = new HashMap();

    public void initBluetoothSystem() {
        bluetoothManager = (BluetoothManager) _context.getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        centralThreadHandler = new Handler();

        Log.i("OpenEnding", "Init BluetoothLE System");
        AndroidUtils.toast("init done!");
    }

    public void startScanning() {
        if (!bluetoothAdapter.isEnabled()) return;

        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(GameProfile.GAME_SERVICE))
                .build();

        ArrayList<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
        scanFilters.clear();
        scanFilters.add(scanFilter);

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        scanResults.clear();

        centralThreadHandler.postDelayed(() -> {
            scanner.stopScan(scanCallback);
        }, 2 * 60 * 1000);

        scanner.startScan(scanFilters, scanSettings, scanCallback);

        Log.i("OpenEnding", "Central Start Scan");
        AndroidUtils.toast("start scanning");
    }

    public void stopScanning() {
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        scanner.stopScan(scanCallback);

        Log.i("OpenEnding", "Central Stop Scan");
        AndroidUtils.toast("stop scanning");
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            for (BluetoothDevice existDevice : scanResults) {
                if (result.getDevice().getAddress().equals(existDevice.getAddress())) return;
            }

            BluetoothDevice device = result.getDevice();
            scanResults.add(device);
            device.connectGatt(_context, false, bluetoothGattCallBack);

            Log.i("OpenEnding", "Scan Target Found : " + result.getDevice().getAddress());
            AndroidUtils.toast("scan target found");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            Log.i("OpenEnding", "Scan Fail");
            AndroidUtils.toast("scan fail");
        }
    };

    private BluetoothGattCallback bluetoothGattCallBack = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.i("OpenEnding", "GATT Failure, status : " + status);
                AndroidUtils.toast("GATT FAILURE");
                return;
            }

            switch(newState) {
                case BluetoothProfile.STATE_CONNECTED :
                    connectedGATT.put(gatt.getDevice().getName(), gatt);
                    AndroidUtils.toast("connect : " + gatt.getDevice().getName());
                    Log.i("OpenEnding", "Connect : " + gatt.getDevice().getName());
                    break;
                case BluetoothProfile.STATE_DISCONNECTED :
                    connectedGATT.remove(gatt.getDevice().getName());
                    AndroidUtils.toast("disconnect : " + gatt.getDevice().getName());
                    Log.i("OpenEnding", "Disconnect : " + gatt.getDevice().getName());
                    break;
            }
        }

        //P->C 수신
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            String msg = characteristic.getStringValue(0);
            AndroidUtils.toast(msg);
            Log.i("OpenEnding", "Disconnect : " + gatt.getDevice().getAddress());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            switch(status) {
                case BluetoothGatt.GATT_SUCCESS :
                    AndroidUtils.toast("write success");
                    Log.i("OpenEnding", "Write Success : " + gatt.getDevice().getAddress());
                    break;
                case BluetoothGatt.GATT_FAILURE :
                    AndroidUtils.toast("write fail");
                    Log.i("OpenEnding", "Write Fail : " + gatt.getDevice().getAddress());
                    break;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            switch(status) {
                case BluetoothGatt.GATT_SUCCESS :
                    AndroidUtils.toast("read success");
                    Log.i("OpenEnding", "Read Success : " + gatt.getDevice().getAddress());
                    break;
                case BluetoothGatt.GATT_FAILURE :
                    AndroidUtils.toast("read fail");
                    Log.i("OpenEnding", "Read Fail : " + gatt.getDevice().getAddress());
                    break;
            }
        }
    };

    //C->P 송신
    public void write(String deviceName, byte[] data) {
        BluetoothGatt gatt = connectedGATT.get(deviceName);
        BluetoothGattService service = GameProfile.getService();
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(GameProfile.TEST_A);

        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        gatt.writeCharacteristic(characteristic);
    }
}
