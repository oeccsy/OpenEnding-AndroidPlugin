package com.oeccsy.openending_ble;

import android.view.Gravity;
import android.widget.Toast;

public class AndroidUtils extends Plugin {

    private static AndroidUtils _instance;

    public static AndroidUtils getInstance() {
        if(_instance == null) {
            _instance = new AndroidUtils();
        }
        return _instance;
    }

    public static void toast(String text) {
        _activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(_context, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}