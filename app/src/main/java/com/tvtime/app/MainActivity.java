package com.tvtime.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;
import com.tvtime.app.widgets.WidgetPlugin;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerPlugin(WidgetPlugin.class);
    }
}
