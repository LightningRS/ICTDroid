package org.square16.ictdroid.testbridge.activities;

import android.app.Activity;
import android.os.Handler;

public abstract class BasicActivity extends Activity {
    protected Handler mHandler;

    public Handler getHandler() {
        return mHandler;
    }
}
