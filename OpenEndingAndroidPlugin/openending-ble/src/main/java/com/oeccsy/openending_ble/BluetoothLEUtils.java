package com.oeccsy.openending_ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;

public class BluetoothLEUtils extends Plugin {

    private static BluetoothLEUtils _instance;

    public static BluetoothLEUtils getInstance() {
        if (_instance == null) {
            _instance = new BluetoothLEUtils();
        }
        return _instance;
    }

    private String[] permissions = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
    };

    private String[] permissionsApi31 = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    };

    private static final int REQUEST_PERMISSION_CODE = 1030;
    private static final int REQUEST_PERMISSION_CODE_S = 1031;

    public boolean isBluetoothLEFeatureExist() {
        BluetoothAdapter tempBluetoothLEAdapter = BluetoothAdapter.getDefaultAdapter();

        if (tempBluetoothLEAdapter != null && _context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            AndroidUtils.toast("BLE Supported");
            return true;
        }
        else
        {
            AndroidUtils.toast("BLE not Supported");
            return false;
        }
    }

    public boolean hasBluetoothLEPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (String permission : permissionsApi31) {
                if (_context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        } else {
            for (String permission : permissionsApi31) {
                if (_context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean isBluetoothLEEnable() {
        if (BluetoothAdapter.getDefaultAdapter().isEnabled())
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            _activity.requestPermissions(permissionsApi31, REQUEST_PERMISSION_CODE_S);
        } else {
            _activity.requestPermissions(permissionsApi31, REQUEST_PERMISSION_CODE);
        }
        AndroidUtils.toast("Bluetooth 권한 요청 완료");
    }

//    public void requestEnableBluetoothLE() {
//        Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//        _context.startActivityForResult(enableBluetoothIntent, REQUEST_PERMISSION_CODE);
//        AndroidUtils.toast("permission request done!");
//    }
}