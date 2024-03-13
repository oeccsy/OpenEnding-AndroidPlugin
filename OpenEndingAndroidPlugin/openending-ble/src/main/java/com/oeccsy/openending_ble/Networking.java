package com.oeccsy.openending_ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Networking extends Plugin {

    private static Networking _instance;

    public static Networking getInstance() {
        if (_instance == null) {
            _instance = new Networking();
        }
        return _instance;
    }

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothLeScanner bluetoothLeScanner;
    private Handler handler;

    private AdvertiseData advertiseData;
    private AdvertiseSettings advertiseSettings;
    private AdvertiseCallback advertiseCallback;

    private ArrayList<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
    private ScanSettings scanSettings;
    private ScanCallback scanCallback;
    private ArrayList<BluetoothDevice> scanResult = new ArrayList<BluetoothDevice>();

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCallback bluetoothGattCallBack;

    public void initBluetoothSystem() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        handler = new Handler();

        AndroidUtils.toast("init done!");
    }

    public void initAdvertiseOptions() {
        String SERVICE_STRING = "F45E7B11-9A82-4C6F-B9A8-3F2D8A6E97C7";

        advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_STRING)))
                .build();

        advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build();

        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                AndroidUtils.toast("Advertising started");
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                AndroidUtils.toast("Advertising failed" + errorCode);
            }
        };

        AndroidUtils.toast("init done!");
    }

    public void startAdvertising() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopAdvertising();
            }
        }, 10000);

        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
        AndroidUtils.toast("start advertising!");
    }

    public void stopAdvertising() {
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
        AndroidUtils.toast("stop advertising!");
    }

    public void initScanOptions() {
        String SERVICE_STRING = "F45E7B11-9A82-4C6F-B9A8-3F2D8A6E97C7";

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_STRING)))
                .build();

        scanFilters.clear();
        scanFilters.add(scanFilter);

        scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build();

        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                for (BluetoothDevice existDevice : scanResult) {
                    if (result.getDevice().getAddress() == existDevice.getAddress()) return;
                }

                AndroidUtils.toast("Remote device name : " + result.getDevice().getName());
                scanResult.add(result.getDevice());
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                for (ScanResult result : results) {

                    for (BluetoothDevice existDevice : scanResult) {
                        if (result.getDevice().getAddress().equals(existDevice.getAddress())) return;
                    }

                    AndroidUtils.toast("Remote device name : " + result.getDevice().getName());
                    scanResult.add(result.getDevice());
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                AndroidUtils.toast("scan fail");
            }
        };

        AndroidUtils.toast("init done!");
    }

    public void startScanning() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanning();
            }
        }, 10000);

        bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);
        AndroidUtils.toast("start scanning!");
    }

    public void stopScanning() {
        bluetoothLeScanner.stopScan(scanCallback);
        AndroidUtils.toast("stop Scanning!");
    }

    public void initConnectOption() {

        bluetoothGattCallBack = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (status == BluetoothGatt.GATT_FAILURE || status != BluetoothGatt.GATT_SUCCESS) {
                    return;
                }
                if (newState == BluetoothProfile.STATE_CONNECTED) {

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    AndroidUtils.toast("service discovery failed");
                    return;
                }

                AndroidUtils.toast("service discovery success");
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                readCharacteristic(characteristic);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {

                } else {
                    disconnectGattServer();
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    readCharacteristic(characteristic);
                } else {
                    // disconnectGattServer();
                }
            }

            private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
                String msg = characteristic.getStringValue(0);
                AndroidUtils.toast(msg);
            }
        };

        AndroidUtils.toast("init done!");
    }

    public void connectDevice(BluetoothDevice device) {
        device.connectGatt(_context, false, bluetoothGattCallBack);
        AndroidUtils.toast("try connect!");
    }

    public void connectDevice() {
        BluetoothDevice device = scanResult.get(0);
        device.connectGatt(_context, false, bluetoothGattCallBack);
        AndroidUtils.toast("try connect!");
    }

    public void disconnectGattServer() {
        bluetoothGatt.disconnect();
        bluetoothGatt.close();
    }

    public void write(byte[] bytes) {
        //bluetoothGatt.writeCharacteristic();
    }
}
