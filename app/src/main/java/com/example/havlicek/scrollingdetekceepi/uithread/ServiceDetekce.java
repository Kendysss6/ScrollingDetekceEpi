package com.example.havlicek.scrollingdetekceepi.uithread;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.havlicek.scrollingdetekceepi.SensorValue;
import com.example.havlicek.scrollingdetekceepi.asynchmereni.ThreadAsynchMereni;
import com.example.havlicek.scrollingdetekceepi.asynchtasks.ZapisDoSouboru;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ServiceDetekce extends Service {
    private Handler handlerUI;
    /**
     * Handler ktery predava Messages a runables do Messagequeue zaznamového vlakna.
     * Messagequeue se vytvoří když je k vlaknu přiřazen Looper, ktery defaultně není přiřazen k vlaknu.
     */
    private Handler handlerAsynchMereni;
    /**
     * HandlerThread, ktery vytvoři vlakno, kde se zpracuji sensor eventy
     */
    private ThreadAsynchMereni threadAsynchMereni;
    /**
     * Timer, ktery kazdou periodu zavola mereni pro vyhodnoceni namerenych dat
     */
    private Timer timer = null;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    /**
     * Id měření - řetězec, který jednoznačně určuje start detekce (stisknutí tlačítka Start detekce v
     * GUI). Skládá se z z času.
     */
    private String idMereni = null;

    public static final int MY_SAMPLING_PERIOD_MICROSEC = 500000; // 2 vzorky za sekundu
    /**
     * Odhadovany počet prvku se kterymi budeme počítat fourierovu transformaci a klasifikaci
     */
    public static final int ODHADOVANY_POCET_PRVKU = 10;
    //public final int TIMER_PERIOD_MILISEC = MY_SAMPLING_PERIOD_MICROSEC * ODHADOVANY_POCET_PRVKU; // perioda
    public static final int TIMER_PERIOD_MILISEC = 10000; // perioda v milisekundach, kazdych 10 sekund

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
        //mSensorManager.registerListener(threadAsynchMereni, mAccelerometer, MY_SAMPLING_PERIOD_MICROSEC, handlerAsynchMereni);
        mSensorManager.registerListener(threadAsynchMereni, mAccelerometer, SensorManager.SENSOR_DELAY_GAME, handlerAsynchMereni);
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, TIMER_PERIOD_MILISEC, TIMER_PERIOD_MILISEC);
        Log.d("perioda",""+ TIMER_PERIOD_MILISEC);

        new Thread(new Runnable() { // je to moc pomale, aspon na tom starem telefonu, možna pak smazu
            @Override
            public void run() {
               // mSensorManager.registerListener(threadAsynchMereni, mAccelerometer, MY_SAMPLING_PERIOD_MICROSEC, handlerAsynchMereni);
                //timer = new Timer();
                //timer.scheduleAtFixedRate(timerTask, 0, TIMER_PERIOD_MILISEC);
            }
        }).start();

        Log.d("timeOnCreate", Long.toString(System.currentTimeMillis() - t));
    }

    @Override
    public  void onDestroy(){
        timer.cancel();
        mSensorManager.unregisterListener(threadAsynchMereni);
        threadAsynchMereni.quit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Bound", "onstartcomand");
        String idMereni = intent.getStringExtra("idMereni");
        this.idMereni = idMereni;
        return super.onStartCommand(intent,flags,startId);
    }



    @Override
    public IBinder onBind(Intent intent) {
        //Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        Log.d("Bound","OnBound/onBind");
        // return mMessenger.getBinder();
        return null;
    }

    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            Log.d("timer","start");
            handlerAsynchMereni.sendEmptyMessage(ThreadAsynchMereni.GET_VALUES);
        }
    };

    public class HandlerUI extends Handler{
        public static final int UPDATE_UI = 1;

        public HandlerUI(){
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case UPDATE_UI:
                    ArrayList l = (ArrayList) msg.obj;
                    ZapisDoSouboru zapis = new ZapisDoSouboru(idMereni);
                    zapis.execute(l);
                    LocalBroadcastManager.getInstance(ServiceDetekce.this).sendBroadcast(new Intent("DetekceZachvatu"));
                    break;
                default:
                    break;
            }
        }
    }
}
