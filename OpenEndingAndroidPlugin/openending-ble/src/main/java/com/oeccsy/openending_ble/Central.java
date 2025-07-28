package com.oeccsy.openending_ble;

import static android.content.Context.BLUETOOTH_SERVICE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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
import android.util.Base64;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import java.util.ArrayList;
import java.util.Arrays;
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
    private HashMap<String, BluetoothGatt> connectedGATT = new HashMap();

    public void initBluetoothSystem() {
        bluetoothManager = (BluetoothManager) _context.getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        centralThreadHandler = new Handler();

        for(BluetoothGatt gatt : connectedGATT.values()) {
            gatt.disconnect();
            gatt.close();
        }
        connectedGATT.clear();

        Log.i("OpenEnding", "Init BluetoothLE System");
    }

    public void startScanning(String deviceName) {
        if (!bluetoothAdapter.isEnabled()) return;
        bluetoothAdapter.setName(deviceName);

        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(GameProfile.GAME_SERVICE))
                .build();

        ArrayList<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
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
        AndroidUtils.toast("Scan 시작");
    }

    public void stopScanning() {
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        scanner.stopScan(scanCallback);

        Log.i("OpenEnding", "Central Stop Scan");
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
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);

            Log.i("OpenEnding", "Scan Fail");
        }
    };

    private BluetoothGattCallback bluetoothGattCallBack = new BluetoothGattCallback() {

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);

            switch(status) {
                case BluetoothGatt.GATT_SUCCESS:
                    Log.i("OpenEnding", "mtu 변경 성공 : " + mtu);
                    break;
                default:
                    Log.i("OpenEnding", "mtu 변경 실패 : " + mtu);
                    break;
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_FAILURE) {
                gatt.close();
                connectedGATT.remove(gatt.getDevice().getName());
                UnityPlayer.UnitySendMessage("AndroidConnection", "OnDeviceDisconnected", gatt.getDevice().getName());

                Log.i("OpenEnding", "GATT Failure, status : " + status);
                AndroidUtils.toast("GATT FAILURE");
                return;
            }

            switch(newState) {
                case BluetoothProfile.STATE_CONNECTED :
                    gatt.discoverServices();
                    connectedGATT.put(gatt.getDevice().getName(), gatt);

                    Log.i("OpenEnding", "Connect : " + gatt.getDevice().getName());
                    break;
                case BluetoothProfile.STATE_DISCONNECTED :
                    gatt.close();
                    connectedGATT.remove(gatt.getDevice().getName());
                    UnityPlayer.UnitySendMessage("AndroidConnection", "OnDeviceDisconnected", gatt.getDevice().getName());

                    Log.i("OpenEnding", "Disconnect : " + gatt.getDevice().getName());
                    break;
            }
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            switch(status) {
                case BluetoothGatt.GATT_SUCCESS:
                    Log.i("OpenEnding", "service 발견 성공 : " + gatt.getDevice().getName());

                    BluetoothGattService service = gatt.getService(GameProfile.GAME_SERVICE);
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(GameProfile.CHARACTERISTIC_GAME_DATA);
                    gatt.setCharacteristicNotification(characteristic, true);

                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(GameProfile.CLIENT_CHARACTERISTIC_CONFIG);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    gatt.writeDescriptor(descriptor);

                    Log.i("OpenEnding", "descriptor 설정 : " + gatt.getDevice().getName());
                    break;
                default:
                    Log.i("OpenEnding", "service 발견 실패 : " + gatt.getDevice().getName());
                    break;
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            switch(status) {
                case BluetoothGatt.GATT_SUCCESS:
                    UnityPlayer.UnitySendMessage("AndroidConnection", "OnDeviceConnected", gatt.getDevice().getName());
                    Log.i("OpenEnding", "descriptor 설정 완료 : " + gatt.getDevice().getName());
                    break;
                default:
                    Log.i("OpenEnding", "descriptor 설정 실패 : " + gatt.getDevice().getName());
                    break;
            }
        }

        //P->C 수신
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i("OpenEnding", "onCharacteristicChanged");

            byte[] data = characteristic.getValue();

            String encodedData = Base64.encodeToString(Arrays.copyOfRange(data, 0, data.length), 0);
            UnityPlayer.UnitySendMessage("AndroidConnection", "OnDataReceive", encodedData);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            Log.i("OpenEnding", "onCharacteristicWrite  : " + status);
            switch(status) {
                case BluetoothGatt.GATT_SUCCESS :
                    Log.i("OpenEnding", "Write Success : " + gatt.getDevice().getAddress());
                    break;
                case BluetoothGatt.GATT_FAILURE :
                    Log.i("OpenEnding", "Write Fail : " + gatt.getDevice().getAddress());
                    break;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.i("OpenEnding", "Read Success : " + characteristic.getValue());

            switch(status) {
                case BluetoothGatt.GATT_SUCCESS :
                    Log.i("OpenEnding", "Read Success : " + gatt.getDevice().getAddress());
                    break;
                case BluetoothGatt.GATT_FAILURE :
                    Log.i("OpenEnding", "Read Fail : " + gatt.getDevice().getAddress());
                    break;
            }
        }
    };

    public void requestMtu(int mtu) {
        if(mtu > 512) mtu = 512;

        for(BluetoothGatt gatt : connectedGATT.values()) {
            gatt.requestMtu(mtu);
        }

        Log.i("OpenEnding", "Try Set MTU : " + mtu);
    }

    //C->P 송신
    public void write(String deviceName, byte[] data) {
        BluetoothGatt gatt = connectedGATT.get(deviceName);
        BluetoothGattService service = gatt.getService(GameProfile.GAME_SERVICE);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(GameProfile.CHARACTERISTIC_GAME_DATA);

        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        gatt.writeCharacteristic(characteristic);
    }
}
