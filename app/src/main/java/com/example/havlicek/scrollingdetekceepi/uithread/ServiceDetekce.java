package com.example.havlicek.scrollingdetekceepi.uithread;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import com.example.havlicek.scrollingdetekceepi.asynchmereni.ThreadAsynchMereni;

public class ServiceDetekce extends Service {
    private Handler handlerUI;
    /**
     * Handler ktery predava Messages a runables do Messagequeue daneho vlakna.
     * Messagequeue se vytvoří když je k vlaknu přiřazen Looper, ktery defaultně není přiřazen k vlaknu.
     */
    private Handler handlerAsynchMereni;
    /**
     * HandlerThread, ktery vytvoři vlakno, kde se zpracuji sensor eventy
     */
    private ThreadAsynchMereni threadAsynchMereni;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    public static final int MY_SAMPLING_PERIOD_MS = 10000000;
    /**
     * Odhadovany počet prvku se kterymi budeme počítat fourierovu transformaci a klasifikaci
     */
    public static final int ODHADOVANY_POCET_PRVKU = 500;
    public final int MY_PERIOD = MY_SAMPLING_PERIOD_MS * ODHADOVANY_POCET_PRVKU; // perioda


    public ServiceDetekce() {
        super();
    }


    @Override
    public void onCreate(){
        // sensor
        long t = System.currentTimeMillis();

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Log.d("timeOnCreate", Long.toString(System.currentTimeMillis() - t));
        // Handler user interface
        handlerUI = new HandlerUI();
        // create new thread for asynch sampling with its handler
        threadAsynchMereni = new ThreadAsynchMereni("Asynchronní měření", handlerUI);
        // its handler
        handlerAsynchMereni = threadAsynchMereni.getHandlerThread();
        Log.d("timeOnCreate", Long.toString(System.currentTimeMillis() - t));
        // register to service to obtain values to this handler
        new Thread(new Runnable() { // je to moc pomale, aspon na tom starem telefonu, možna pak smazu
            @Override
            public void run() {
                mSensorManager.registerListener(threadAsynchMereni, mAccelerometer, MY_SAMPLING_PERIOD_MS, handlerAsynchMereni);
            }
        }).start();
        Log.d("timeOnCreate", Long.toString(System.currentTimeMillis() - t));
    }

    @Override
    public  void onDestroy(){
        mSensorManager.unregisterListener(threadAsynchMereni);
        threadAsynchMereni.quit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Log.d("Bound", "onstartcomand");
        return super.onStartCommand(intent,flags,startId);
    }



    @Override
    public IBinder onBind(Intent intent) {
        //Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        Log.d("Bound","OnBound/onBind");
        return mMessenger.getBinder();
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Handler of incoming messages from main activity.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d("what",Integer.toString(msg.what));
            switch (msg.what) {
                case 1:
                    Toast.makeText(getApplicationContext(), "hello!", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
