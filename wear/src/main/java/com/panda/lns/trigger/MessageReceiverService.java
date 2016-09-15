package com.panda.lns.trigger;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.panda.lns.trigger.shared.ClientPaths;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class MessageReceiverService extends WearableListenerService {
    private static final String TAG = "SensorDashboard/MessageReceiverService";
    private SensorService sensorService;
    private DeviceClient deviceClient;

    @Override
    public void onCreate() {
        super.onCreate();
        deviceClient = DeviceClient.getInstance(this);
        sensorService = new SensorService();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataItem dataItem = dataEvent.getDataItem();
                Uri uri = dataItem.getUri();
                String path = uri.getPath();

                if (path.startsWith("/filter")) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                    // int filterById = dataMap.getInt(DataMapKeys.FILTER);
                    // deviceClient.setSensorFilter(filterById);
                }
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "Received message: " + messageEvent.getPath());

        if (messageEvent.getPath().equals(ClientPaths.START_MEASUREMENT)) {
            SensorService.storeData=true;
            startService(new Intent(this, SensorService.class));
            deviceClient.sendString("Storing data");
        }

        if (messageEvent.getPath().equals(ClientPaths.STOP_MEASUREMENT)) {
            SensorService.storeData=false;
            deviceClient.sendString("Sensors stopped");

            stopService(new Intent(this, SensorService.class));
            deviceClient.sendString("File store finished");
        }
        if (messageEvent.getPath().equals(ClientPaths.CALIBRATE)) {
            deviceClient.sendString("Calibrating....");
            stopService(new Intent(this, SensorService.class));
            startService(new Intent(this, SensorService.class));
            sensorService.calibrate();
            stopService(new Intent(this, SensorService.class));
        }
    }
}
