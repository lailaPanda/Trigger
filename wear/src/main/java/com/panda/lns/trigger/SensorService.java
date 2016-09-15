package com.panda.lns.trigger;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.util.SparseLongArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;

public class SensorService extends Service implements SensorEventListener {
    public static boolean storeData=false;
    public static boolean run=false;
    public static boolean checkTrigger=false;


    private static final String TAG = "SensorService";
    private final static int SENS_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    private final static int SENS_LINEAR_ACCELERATION = Sensor.TYPE_LINEAR_ACCELERATION;



    //recording stuff
    public static final String DATA_FILE_NAME = "data.txt";
    private static final int RECORDING_RATE = 8000; // 8 kHz
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL_IN, FORMAT)*16; //2 seconds

    private State mState = State.IDLE;

    private AsyncTask<Void, Void, Void> mRecordingAsyncTask;

    enum State {
        IDLE, RECORDING
    }



    SensorManager mSensorManager;
    private static DeviceClient client;
    private ScheduledExecutorService mScheduler;

    private AsyncTask<Void, Void, Void> sensorFileWriteTask;
    private AsyncTask<Void, Void, Void> triggerTask;

    Context context;
    private SparseLongArray lastSensorData;

    private static float calibX=0.0f;
    private static float calibY=0.0f;
    private static float calibZ=0.0f;
    private  static float lastX=0.0f;
    private static float lastY=0.0f;
    private static float lastZ=0.0f;


    private static boolean record=false;

    @Override
    public void onCreate() {
        super.onCreate();
        client = DeviceClient.getInstance(this);
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("Sensor Dashboard");
        builder.setContentText("Collecting sensor data..");
        dataList.clear();
        startForeground(1, builder.build());
        clearVariables();
        checkTrigger=true;
        checkTrigger();
        storeDataList.clear();
        storeDataList.clear();
        context=this;
        lastSensorData = new SparseLongArray();
    }

    ArrayList<String> l = new ArrayList<String>();

    @Override
    public void onDestroy() {
        super.onDestroy();
        checkTrigger=false;
        storeDataList.clear();
        dataList.clear();
        stopMeasurement();
        Log.w(TAG, "Stopping measurement");

    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void checkTrigger(){
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        Sensor linearAccelerationSensor = mSensorManager.getDefaultSensor(SENS_LINEAR_ACCELERATION);
        Sensor gyroscopeSensor = mSensorManager.getDefaultSensor(SENS_GYROSCOPE);
        // Register the listener
        if (mSensorManager != null) {
            if (linearAccelerationSensor != null) {
                mSensorManager.registerListener(this, linearAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.d(TAG, "No Linear Acceleration Sensor found");
            }
            if (gyroscopeSensor != null) {
                mSensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.w(TAG, "No Gyroscope Sensor found");
            }
        }
        Log.w(TAG, "in check trigger!");
        // check for trigger motion in supination and pronation and sound of water
        triggerTask = new AsyncTask<Void, Void, Void>() {
            short[] buffer = new short[BUFFER_SIZE/2];
            AudioRecord mAudioRecord=null;
            @Override
            protected void onPreExecute() {
                mAudioRecord=new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDING_RATE, CHANNEL_IN, FORMAT, BUFFER_SIZE/2);
                mState = State.RECORDING;
                mAudioRecord.startRecording();
            }

            @Override
            protected Void doInBackground(Void... params) {
                //showMessage("Checking Trigger...");
                client.sendString("Checking for Trigger....");
                long lastts=0;
                while((checkTrigger)){
                    long ts = System.currentTimeMillis();
                    if((ts-lastts) < 30) continue;

                    String t="";
                    boolean isWater=false;
                    double distBefore=0;
                    double distAfter=0;
                    for(int i=0;i<gyrList.size()/2;i++){
                        distBefore+=gyrList.get(i).v;
                    }
                    for(int i=gyrList.size()/2;i<gyrList.size();i++){
                        distAfter+=gyrList.get(i).v;
                    }
                    if((Math.abs(distBefore) > 200)&&(Math.abs(distAfter) > 200) && (distAfter*distBefore < 0)){
                        showMessage("Roll");
                        client.sendString("Roll");
                        double value1 = getWaterScore();
                        double value2 = getWaterScore();
                        if(value2>value1) value1=value2;
                        if(value1 > 0.7){
                             showMessage("water");
                            client.sendString("Water sound Detected");

                        }else{
                            showMessage("no water");
                            client.sendString("No water sound Detected");
                        }
                    }else{
                        //showMessage("no roll");
                    }
                    lastts=ts;
                }

                return null;
            }
            double getWaterScore(){
                getSample();
                double[] array = getEnergies(getDoubleArray(buffer), 512);
                double min=array[0];
                for(int i=0;i<array.length-1;i++){
                    if(array[i] < min){
                        min = array[i];
                    }
                }
                int count=0;
                Log.w(TAG, "min = " + min);

                if(min > 14000){    //first criteria for selection
                    for(int i=0;i<array.length-1;i++){
                        if(array[i] > 20000) count++;
                    }
                }
                return ((double)count)/(array.length-1);
            }
            void getSample() {
                if (mState != State.IDLE) {
                    Log.w(TAG, "Requesting to start recording while state was not IDLE");
                }
                mState = State.RECORDING;
                Log.e(TAG, "State = " + mAudioRecord.getRecordingState()) ;
                int read = mAudioRecord.read(buffer, 0, buffer.length,AudioRecord.READ_BLOCKING);
                Log.w("buffer length  = ", Integer.toString(buffer.length));
                Log.w("sound size = ", Integer.toString(read));
                mState = State.IDLE;
            }

            double[] getEnergies(double[] data, int sampleLength){
                double[] tmp = new double[sampleLength];
                double[] energies = new double[(int)(data.length/sampleLength)];
                int count=0;
                for(int i=0;i<data.length;i++){
                    if((i%sampleLength == 0)&&(i>0)){
                        energies[count] = getSpectralCover(tmp);
                        count++;
                    }
                    tmp[i%sampleLength] = data[i];
                }
                return energies;
            }

            double getSpectralCover(double[] array){
                double num=0;
                double den =0;
                Complex[] compAr = new Complex[array.length];
                for(int i=0;i<array.length;i++){
                    compAr[i] = new Complex(array[i], 0.0);
                }
                Complex[] fftAr = new Complex[array.length];
                fftAr = FFT.fft(compAr);
                for(int i=0;i<fftAr.length/2-5;i++){
                    num+=Math.pow((fftAr[i].abs() * i), 2.0);
                    den+=fftAr[i].abs();
                }
                den = Math.pow(den,1.5);
                return num/den;


            }

            double[] getDoubleArray(short[] array){
                double[] dArray = new double[array.length];
                for(int i=0;i<array.length;i++){
                    dArray[i] = array[i];
                }
                return dArray;
            }

            void showMessage(String s){
                Message msg = Message.obtain();
                msg.obj = new String(s);
                wearActivity.txtLogHandler.sendMessage(msg) ;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                triggerTask = null;
                mState = State.IDLE;
                mAudioRecord.stop();
                mRecordingAsyncTask = null;
            }

            @Override
            protected void onCancelled() {
                triggerTask = null;
            }


        };

        triggerTask.execute();
    }


    public static void calibrate(){
        calibX= lastX;
        calibY= lastY;
        calibZ= lastZ;
        client.sendString("Calibrated : " + calibX +" " + calibY + " " + calibZ );

    }

    private void stopMeasurement() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        checkTrigger=false;
        run=false;
    }

    static boolean isFirstTime=true;
    long ts;
    long firstTs=0;
    long lastTs=0;
    static int accCount=0;
    float xSum=0;
    float ySum=0;
    float zSum=0;
    float xv=0;
    float yv=0;
    float zv=0;
    float xDist=0;
    float yDist=0;
    float zDist=0;
    float xwDist=0;
    float ywDist=0;
    float zwDist=0;
    int n=0;
    float avgAccXY=0;
    float lastV=0;
    float gyrSum=0;
    float lastGyrSum=0;
    float dist=0;
    int triggerGyrCount=0;
    int strokes=0;
    long[] last2ts = new long[2];
    boolean measureAcc=false;
    int gyrCount=0;

    ArrayList<Long> zCrossingTs = new ArrayList<Long>();
    LimitedArrayList vList = new LimitedArrayList(300);
    LimitedArrayList gyrList = new LimitedArrayList(300);//contains last 300 Xgyr data points
    ArrayList<StrokeData> dataList = new ArrayList<StrokeData>();
    ArrayList<String> storeDataList = new ArrayList<String>();

    void showMessage(String s){
        Message msg = Message.obtain();
        msg.obj = new String(s);
        wearActivity.txtLogHandler.sendMessage(msg) ;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        long lastTimestamp = lastSensorData.get(event.sensor.getType());
        long timeAgo = event.timestamp - lastTimestamp; // in nano seconds

        if (lastTimestamp != 0) {
            if (timeAgo < 100) { //1 ms
                return;
            }
            if (!((event.sensor.getType()==10) || (event.sensor.getType() == 4))) {
                return;
            }
        }
        lastSensorData.put(event.sensor.getType(), event.timestamp);

        float x=0;
        float y=0;
        float z=0;
        float xGyr,yGyr,zGyr;

        if((event.sensor.getType()==10)){

        }else {  // if its gyro
        if (checkTrigger) {
                triggerGyrCount++;
                gyrList.add(new DataPoint(event.timestamp, event.values[0]));//contains last 300 data points
            }
        }
    }


    public void clearVariables(){
        isFirstTime=true;
        ts=0;
        firstTs=0;
        lastTs=0;
        accCount=0;
        xSum=0;
        ySum=0;
        zSum=0;
        xv=0;
        yv=0;
        zv=0;
        xDist=0;
        yDist=0;
        zDist=0;
        xwDist=0;
        ywDist=0;
        zwDist=0;
        n=0;
        avgAccXY=0;
        lastV=0;
        gyrSum=0;
        lastGyrSum=0;
        dist=0;
        gyrCount=0;
        triggerGyrCount=0;
        strokes=0;
        last2ts = new long[2];
        zCrossingTs.clear();
        vList.clear();
        dataList.clear();
        gyrList.clear();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

class StrokeData{
    double xy,xz,xwx, xwy, xwz, yz,ywx, ywy, ywz, zwx, zwy, zwz, wxwy, wxwz, wywz,x,y,z,wx,wy,wz,wdist;
    long firstTs,lastTs;
    String data="";
    StrokeData(double x, double y, double z, double wx, double wy, double wz, long firstTs,long lastTs,double wdist){
        this.xy = x/y;
        this.xz = x/z;
        this.xwx = x/wx;
        this.xwy = x/wy;
        this.xwz = x/wz;
        this.yz = y/z;
        this.ywx = y/wx;
        this.ywy = y/wy;
        this.ywz = y/wz;
        this.zwx = z/wx;
        this.zwy = z/wy;
        this.zwz = z/wz;
        this.wxwy = wx/wy;
        this.wxwz = wx/wz;
        this.wywz = wy/wz;
        this.x=x;
        this.y=y;
        this.z=z;
        this.wx=wx;
        this.wy=wy;
        this.wz=wz;
        data = Double.toString(xy) + " " + Double.toString(xz) + " " + Double.toString(xwx) + " " + Double.toString(xwy) + " " + Double.toString(xwz) + " " + Double.toString(yz) + " " + Double.toString(ywx) + " " + Double.toString(ywy) + " " + Double.toString(ywz) + " " +Double.toString(zwx) + " " +Double.toString(zwy) + " " +Double.toString(zwz) + " " +Double.toString(wxwy) + " " +Double.toString(wxwz) + " " +Double.toString(wywz) + " " +Double.toString(x) + " " +Double.toString(y) +" " +Double.toString(z)  + " " +Double.toString(wx) + " " +Double.toString(wy) + " " +Double.toString(wz);

        this.firstTs=firstTs;
        this.lastTs=lastTs;
        this.wdist=wdist;
    }
}

class DataPoint{
    long ts;
    float v;

    DataPoint(long ts, float v){
        this.ts=ts;
        this.v=v;
    }

}


class LimitedArrayList extends ArrayList<DataPoint>{
    private int limit=0;
    LimitedArrayList(int limit){
        this.limit=limit;
    }
    public boolean add(DataPoint sp){
        if(this.size() < limit){
            return super.add(sp);
        }else{
            super.remove(0);
            return super.add(sp);
        }
    }
}








