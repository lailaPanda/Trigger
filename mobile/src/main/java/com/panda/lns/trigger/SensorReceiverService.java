package com.panda.lns.trigger;

import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.util.Log;

import com.panda.lns.trigger.shared.DataMapKeys;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SensorReceiverService extends WearableListenerService {
    static ArrayList<String> dataList = new ArrayList<String>();
    private static final String TAG = "SRService";

    //File stuff
    private GoogleApiClient googleApiClient;
    final static String directory = Environment.getExternalStorageDirectory().getAbsolutePath().toString() + "/SensorDashboard/";
    static File file=null;
    static String fileName="poo.txt";


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Created££££££££££££££££££££££££££££££££££££££££££££££££££££££££££££££££");
        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
        googleApiClient.connect();
    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);

        Log.i(TAG, "Connected: " + peer.getDisplayName() + " (" + peer.getId() + ")");
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        super.onPeerDisconnected(peer);

        Log.i(TAG, "Disconnected: " + peer.getDisplayName() + " (" + peer.getId() + ")");
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataItem dataItem = dataEvent.getDataItem();
                Uri uri = dataItem.getUri();
                String path = uri.getPath();
                if (path.startsWith("/message")) {
                    Log.d(TAG, "data Changed!");
                     DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
                    Message msg = Message.obtain();
                    msg.what=1;
                    msg.obj = dataMap.getString(DataMapKeys.MESSAGE);
                    //MainActivity.txtMessagesHandler.removeMessages(1);
                    MainActivity.txtMessagesHandler.sendMessage(msg) ;
                }
            }
        }

    }

    @Override
    public void onChannelOpened(Channel channel) {
        Log.d(TAG, "channel opened!");

        if (channel.getPath().equals("/mypath")) {
            try {
                file = new File(directory + System.currentTimeMillis()+".txt");
                if(!file.exists()){
                    file.createNewFile();
                }
            } catch (IOException e) {
                Log.d("Main","Could not create file :(");
            }
            Uri uri = Uri.fromFile(file);
            channel.receiveFile(googleApiClient, uri, false);
        }
    }

    //when file is ready
    @Override
    public void onInputClosed(Channel channel, int i, int i1) {
        Log.d(TAG, "Got File !!!!!!!!!!!!!!!!!!!!!!!!!!!! size = " + file.length());
        Message msg = Message.obtain();
        msg.obj = Long.toString(file.length());
        MainActivity.txtLogHandler.sendMessage(msg);
    }

    public static void clearList(String className){
        dataList.clear();
        if(className.length()!=0){
            dataList.add(className);
        }
    }
}