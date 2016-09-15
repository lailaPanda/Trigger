package com.panda.lns.trigger;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.SparseLongArray;

import com.panda.lns.trigger.shared.DataMapKeys;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DeviceClient {
    private static final String TAG = "DeviceClient";
    private static final int CLIENT_CONNECTION_TIMEOUT = 15000;
    public static boolean isDataSending=false;

    public static DeviceClient instance;

    public static DeviceClient getInstance(Context context) {
        if (instance == null) {
            instance = new DeviceClient(context.getApplicationContext());
        }

        return instance;
    }

    private Context context;
    private GoogleApiClient googleApiClient;
    private GoogleApiClient channelGoogleApiClient;
    private Channel fileChannel=null;

    private ExecutorService executorService;
    private int filterId;

    private SparseLongArray lastSensorData;

    private DeviceClient(Context context) {
        this.context = context;

        Log.d(TAG, "in client!");

        googleApiClient = new GoogleApiClient.Builder(context).addApi(Wearable.API).build();
        // ChannelApi.OpenChannelResult result = Wearable.ChannelApi.openChannel(googleApiClient, nodeId, "/mypath").await();

        executorService = Executors.newCachedThreadPool();
        lastSensorData = new SparseLongArray();
    }


    public void sendFile(File file){
        Log.w("deviceClient", "in audio channel!");
        if (validateConnection()) {
            Log.w("deviceClient", "*************************************************");
            List<Node> nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await().getNodes();
            Log.w("deviceClient", "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            if(nodes.size()==1){
                ChannelApi.OpenChannelResult result = Wearable.ChannelApi.openChannel(googleApiClient, nodes.get(0).getId().toString(), "/mypath").await();
                fileChannel = result.getChannel();
            }
            if (fileChannel != null) {
                Log.w("deviceClient", "opened!!!!!!!&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
                fileChannel.sendFile(googleApiClient, Uri.fromFile(file));
                Log.w("deviceClient", "file sent!!!!! " + Long.toString(file.length()));
            }
        }else{
            Log.w("deviceClient", "cannot validate connection!");
        }
    }


    public void sendString(String s) {
        final String str =s;
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                sendStringInBackground(str);
            }
        });
    }
    private void sendStringInBackground(String s) {
        PutDataMapRequest dataMap = PutDataMapRequest.create("/message/");

        dataMap.getDataMap().putString(DataMapKeys.MESSAGE, s);
        PutDataRequest putDataRequest = dataMap.asPutDataRequest();
        send(putDataRequest);
    }


    private boolean validateConnection() {
        if (googleApiClient.isConnected()) {
            return true;
        }

        ConnectionResult result = googleApiClient.blockingConnect(CLIENT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);

        return result.isSuccess();
    }

    private void send(PutDataRequest putDataRequest) {
        if (validateConnection()) {
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    // Log.v(TAG, "Sending sensor data: " + dataItemResult.getStatus().isSuccess());
                }
            });
            //Log.e(TAG, "sent!");
        }

    }
}
