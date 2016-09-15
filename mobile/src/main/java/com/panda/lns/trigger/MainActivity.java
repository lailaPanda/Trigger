package com.panda.lns.trigger;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;


public class MainActivity extends AppCompatActivity{
    private RemoteSensorManager remoteSensorManager;

    Toolbar mToolbar;
    public static TextView txtLog = null;
    public static TextView txtMessages = null;
    public static TextView txtTime = null;
    public static ImageView image=null;

    public static EditText classNameTxt=null;
    static boolean initOk=false;
    private static Handler mHandler = new Handler();
    static Handler txtLogHandler;
    static Handler txtMessagesHandler;
    static Handler txtTimeHandler;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);

        txtLog=(TextView)findViewById(R.id.txtLog);
        txtMessages=(TextView)findViewById(R.id.txtMessages);
        txtTime=(TextView)findViewById(R.id.txtTime);
        image = (ImageView)findViewById(R.id.imageView);

        classNameTxt = (EditText)findViewById(R.id.classNameTxt);
        remoteSensorManager = RemoteSensorManager.getInstance(this);

        final ToggleButton trackBtn = (ToggleButton)findViewById(R.id.toggleButton);
        trackBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SensorReceiverService.clearList(classNameTxt.getText().toString());
                    remoteSensorManager.startMeasurement();
                } else {
                    remoteSensorManager.stopMeasurement();
                }
            }
        });

        Button btnCalib = (Button) findViewById(R.id.btnCalib);
        btnCalib.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        if(trackBtn.isChecked()){
                            trackBtn.setChecked(false);
                            remoteSensorManager.stopMeasurement();
                        }
                        remoteSensorManager.calibrate();
                    }

                }
        );

        txtLogHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                txtLog.setText((String)msg.obj);
            }
        };
        txtMessagesHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String s = (String)msg.obj;
                txtMessages.setText(s);
                if(s.compareTo("Water sound Detected")==0){
                    image.setImageResource(R.drawable.water);
                }
                if(s.compareTo("No water sound Detected")==0){
                    image.setImageResource(R.drawable.nowater);
                }
            }
        };
        txtTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                txtTime.setText((String)msg.obj);
            }
        };
    }

    public static void displayData(String s){
        final String str = s;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // This gets executed on the UI thread so it can safely modify Views
                txtLog.setText(str);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    protected void onDestroy() {
        remoteSensorManager.stopMeasurement();
        initOk=false;
        super.onDestroy();
    }
}
