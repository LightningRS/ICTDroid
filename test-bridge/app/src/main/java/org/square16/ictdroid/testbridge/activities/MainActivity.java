package org.square16.ictdroid.testbridge.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import org.square16.ictdroid.testbridge.Constants;
import org.square16.ictdroid.testbridge.R;
import org.square16.ictdroid.testbridge.controller.TestController;

import java.util.List;

public class MainActivity extends BasicActivity {
    private final static String TAG = "MainActivity";
    private TestController mController;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView loadingTv = findViewById(R.id.loadingTextView);

        this.mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case Constants.MSG_INIT_OK:
                        loadingTv.setText(R.string.loading_service);
                        mController = new TestController(MainActivity.this);
                        mController.init();
                        break;
                    case Constants.MSG_START_ACTIVITY:
                        Intent i = (Intent) msg.obj;
                        i.setClassName(MainActivity.this, i.getComponent().getClassName());
                        MainActivity.this.startActivity(i);
                        break;
                    default:
                        loadingTv.setText(R.string.loading_unknown);
                }
            }
        };
        findViewById(R.id.button_test).setOnClickListener(v -> test());
        requestPermissions();
    }

    private void requestPermissions() {
        XXPermissions.with(this)
                .permission(Permission.MANAGE_EXTERNAL_STORAGE)
                .permission(Permission.SYSTEM_ALERT_WINDOW)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (all) {
                            Message msg = Message.obtain();
                            msg.what = Constants.MSG_INIT_OK;
                            mHandler.sendMessage(msg);
                        }
                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        String errMsg = "Failed to grant permission(s):\n" + TextUtils.join("\n", permissions);
                        Log.e(TAG, errMsg);
                        Message msg = Message.obtain();
                        msg.what = Constants.ERR_PERMISSION_DENIED;
                        msg.obj = errMsg;
                        mHandler.sendMessage(msg);
                    }
                });
    }

    private void test() {

    }
}