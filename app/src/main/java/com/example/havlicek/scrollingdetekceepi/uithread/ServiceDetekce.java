package com.example.havlicek.scrollingdetekceepi.uithread;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import com.example.havlicek.scrollingdetekceepi.SensorValue;
import com.example.havlicek.scrollingdetekceepi.asynchmereni.ThreadAsynchMereni;
import com.example.havlicek.scrollingdetekceepi.asynchtasks.LinInterpolace;
import com.example.havlicek.scrollingdetekceepi.asynchtasks.ZapisDoSouboru;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ServiceDetekce extends Service {
    private Handler handlerUI;
    /**
     * Handler ktery predava Messages a runables do Messagequeue zaznamového vlakna.
     * Messagequeue se vytvoří když je k vlaknu přiřazen Looper, ktery defaultně není přiřazen k vlaknu.
     * <p>Použití: Zatím pouze pro registraci měření senzoru v metodě {@link #onCreate()} a odregistraci v {@link #onDestroy()}</p>
     */
    private Handler handlerAsynchMereni;
    /**
     *  Je to <b>VLÁKNO</b>! NENÍ TO HANDLER! HandlerThread, ktery vytvoři vlakno, kde se zpracuji sensor eventy.
     */
    private ThreadAsynchMereni threadAsynchMereni;
    /**
     * Timer, ktery každou periodu {@link #TIMER_PERIOD_MILISEC} zavolá měření (pošle {@link #timerTask}) pro vyhodnocení naměřených dat.
     */
    private Timer timer = null;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private boolean kalibrace = false;
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

    /**
     * Perioda, za kterou časovač {@link ServiceDetekce#timer} spouští úkol {@link ServiceDetekce#timerTask}.
     */
    //public final int TIMER_PERIOD_MILISEC = MY_SAMPLING_PERIOD_MICROSEC * ODHADOVANY_POCET_PRVKU; // perioda
    public static final int TIMER_PERIOD_MILISEC = 10000; // perioda v milisekundach, kazdych 10 sekund

    public ServiceDetekce() {
        super();
    }

    /**
     * Metoda onCreate je vždy stejná pro všechny typy měření. Tj. Kalibrace, pouze měření dat a Měření a výpočet.
     */
    @Override
    public void onCreate(){
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        // Handler for user interface
        handlerUI = new HandlerUI();
        // create new thread for asynch sampling with its handler
        threadAsynchMereni = new ThreadAsynchMereni("Asynchronní měření", handlerUI);
        // its handler
        handlerAsynchMereni = threadAsynchMereni.getHandlerThread();

        // register to service to obtain values to this handler
        //mSensorManager.registerListener(threadAsynchMereni, mAccelerometer, MY_SAMPLING_PERIOD_MICROSEC, handlerAsynchMereni);
        mSensorManager.registerListener(threadAsynchMereni, mAccelerometer, SensorManager.SENSOR_DELAY_GAME, handlerAsynchMereni);
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, TIMER_PERIOD_MILISEC, TIMER_PERIOD_MILISEC);
        Log.d("perioda",""+ TIMER_PERIOD_MILISEC);
    }

    @Override
    public  void onDestroy(){
        timer.cancel();
        mSensorManager.unregisterListener(threadAsynchMereni);
        threadAsynchMereni.quit();
    }

    /**
     * <h1>Česky</h1>
     * Spuštění služby. Zavoláno z {@link MainActivity#onButStartDetekce(View)} nebo z {@link KalibraceActivity#kalibrace(View)}.
     * Zde se nastavuje, co se bude dělat dle typu služby. tj Kalibrace, pouze měření nebo měření výpočet.
     * @see MainActivity#onButStartDetekce(View)
     * @see MainActivity#kalibraceSenzotu(View)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Service", "onstartcomand"+intent);
        if (intent != null){ // null pokud se to restartuje
            kalibrace = intent.getBooleanExtra("Kalibrovani", false);
            Log.d("kalibraceOnstart",""+kalibrace);
            this.idMereni = intent.getStringExtra("idMereni");;
        } else {
            Log.d("Service","restart");
        }
        return super.onStartCommand(intent,flags,startId);
    }

    /**
     * Úkol, který spouští časovač {@link ServiceDetekce#timer} každých {@link ServiceDetekce#TIMER_PERIOD_MILISEC} milisekund.
     */
    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            Log.d("timer","start");
            handlerAsynchMereni.sendEmptyMessage(ThreadAsynchMereni.GET_VALUES);
        }
    };

    /**
     * Handler, který dostává naměřené hodnoty a dále nastavuje a vytváří nové Thready pro zápis a výpočet.
     */
    public class HandlerUI extends Handler{
        public static final int UPDATE_UI = 1;

        public HandlerUI(){
            super(Looper.getMainLooper());
        }

        /**
         * Broadcasting changes to GUI to {@link MainActivity#mMessageReceiver} or if kalibrace, then to
         * {@link KalibraceActivity#mMessageReceiver}.
         * @param msg message to handle
         */
        @Override
        public void handleMessage(Message msg){
            ArrayList l;
            ZapisDoSouboru zapis;
            Log.d("Service","incoming Message "+kalibrace);
            switch (msg.what){
                case UPDATE_UI:
                    l = (ArrayList) msg.obj;
                    zapis = new ZapisDoSouboru(idMereni);
                    zapis.execute(l);
                    if (kalibrace){ // jediny rozdil pokud dělam kalibraci nebo měřim hodnoty
                        Intent i = new Intent("Kalibrace");
                        i.putExtra("Hodnoty", l);
                        LocalBroadcastManager.getInstance(ServiceDetekce.this).sendBroadcast(i);
                    } else {
                        LocalBroadcastManager.getInstance(ServiceDetekce.this).sendBroadcast(new Intent("DetekceZachvatu"));
                        LinInterpolace interpolace = new LinInterpolace(idMereni);
                        interpolace.execute(l);
                    }
                    break;
                default:
                    break;
            }
        }
    }



    /**
     * Nechci aby byla služba propojená. Vrací null.
     */
    @Override
    public IBinder onBind(Intent intent) {
        // Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        // Log.d("Bound","OnBound/onBind");
        // return mMessenger.getBinder();
        return null;
    }
}
