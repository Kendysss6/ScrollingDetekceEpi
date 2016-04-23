package com.example.havlicek.scrollingdetekceepi.uithread;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.*;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import com.example.havlicek.scrollingdetekceepi.R;
import com.example.havlicek.scrollingdetekceepi.asynchmereni.ThreadAsynchMereni;
import com.example.havlicek.scrollingdetekceepi.asynchtasks.LinInterpolace;
import com.example.havlicek.scrollingdetekceepi.asynchtasks.ZapisDoSouboru;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ServiceDetekce extends Service {
    //private Looper serviceLooper;
    private Handler handlerService;
    /**
     * Handler ktery predava Messages a runables do Messagequeue zaznamového vlakna.
     * Messagequeue se vytvoří když je k vlaknu přiřazen Looper, ktery defaultně není přiřazen k vlaknu.
     * <p>Použití: Zatím pouze pro registraci měření senzoru v metodě {@link #onCreate()} a odregistraci v {@link #onDestroy()}</p>
     */
    private Handler handlerAsynchMereni;
    /**
     *  Je to <b>VLÁKNO</b>! NENÍ TO HANDLER! Je to třída HandlerThread, ktera vytvoři vlakno, kde se zpracuji sensor eventy.
     */
    private ThreadAsynchMereni threadAsynchMereni;
    /**
     * Timer, ktery každou periodu {@link #TIMER_PERIOD_MILISEC} zavolá měření {@link ServiceDetekce#getTask(boolean)} pro vyhodnocení naměřených dat.
     */
    private Timer timer = null;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private WakeLock wakeLock;
    private boolean kalibrace = false;
    /**
     * Id měření - řetězec, který jednoznačně určuje start detekce (stisknutí tlačítka Start detekce v
     * GUI). Skládá se z z času.
     */
    private String idMereni = null;

    private String sourceDir = null;

    public static final int MY_SAMPLING_PERIOD_MICROSEC = 500000; // 2 vzorky za sekundu
    /**
     * Odhadovany počet prvku se kterymi budeme počítat fourierovu transformaci a klasifikaci
     */
    public static final int ODHADOVANY_POCET_PRVKU = 512;

    /**
     * Perioda, za kterou časovač {@link ServiceDetekce#timer} spouští úkol {@link ServiceDetekce#getTask(boolean)}
     */
    //public final int TIMER_PERIOD_MILISEC = MY_SAMPLING_PERIOD_MICROSEC * ODHADOVANY_POCET_PRVKU; // perioda
    public static final int TIMER_PERIOD_MILISEC = 10000; // perioda v milisekundach, kazdych 10 sekund
    public static final int TIMER_PERIOD_MILISEC_KALIBRACE = 2 * TIMER_PERIOD_MILISEC;

    /**
     * Id notifikace foreground service, nastavuje se v {@link ServiceDetekce#timer}
     */
    public static final int notificationID = 156;
    public ServiceDetekce() {
        super();
    }

    /**
     * Metoda onCreate je vždy stejná pro všechny typy měření. Tj. Kalibrace, pouze měření dat a Měření a výpočet.
     */
    @Override
    public void onCreate(){
        Log.d("ServiceDetekce","onCreate()");
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        // Load offsets
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);

        // Service Thread
        HandlerThread thread = new HandlerThread("Service Detekce", Process.THREAD_PRIORITY_FOREGROUND);
        thread.start();
        // Handler for Service
        handlerService = new HandlerService(thread.getLooper());
        // create new thread for asynch sampling with its handler
        threadAsynchMereni = new ThreadAsynchMereni("Asynchronní měření", handlerService,
                p.getFloat("offsetX", 0f), p.getFloat("offsetY", 0f), p.getFloat("offsetZ", 0f));
        // its handler
        handlerAsynchMereni = threadAsynchMereni.getHandlerThread();

        // register to service to obtain values to this handler
        //mSensorManager.registerListener(threadAsynchMereni, mAccelerometer, MY_SAMPLING_PERIOD_MICROSEC, handlerAsynchMereni);
        mSensorManager.registerListener(threadAsynchMereni, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST, handlerAsynchMereni);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        wakeLock.acquire();
        Log.d("wakelock", "aquired");
    }

    @Override
    public void onDestroy() {
        Log.d("ServiceDetekce", "onDestroy()");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String filePath = Environment.getExternalStorageDirectory() + "/logcat.txt";
                    filePath = ZapisDoSouboru.getAlbumStorageDir(sourceDir,"logcat"+idMereni+".txt").toString();
                    Log.d("logcat", filePath);
                    Runtime.getRuntime().exec(new String[]{"logcat", "-f", filePath, "-v", "threadtime", "*:V"});
                } catch (Exception e){
                    Log.d("logcat","fail to save logcat");
                }

            }
        }).start();
        LocalBroadcastManager.getInstance(ServiceDetekce.this).sendBroadcast(new Intent("Destroying Service"));
        if (timer != null) {timer.cancel();}
        mSensorManager.unregisterListener(threadAsynchMereni);
        threadAsynchMereni.quit();
        wakeLock.release();
        Log.d("wakelock", "released");
    }

    /**
     * Spuštění služby. Zavoláno z {@link MainActivity#onButStartDetekce(View)} nebo z {@link KalibraceActivity#kalibrace(View)}.
     * <p>Nastavuje se:</p>
     * <ul>
     *     <li>Jestli bude: Měření+klasifikace/Měření/Kalibrace</li>
     *     <li>Perioda měření (default {@link #TIMER_PERIOD_MILISEC})</li>
     *     <li>Type of timerTask to execute</li>
     * </ul>
     * <p>Udělá se</p>
     * <ul>
     *     <li>Vytvoření hierarchie složek dle idMereni, kam se data ukládájí</li>
     *     <li>Označení service jako foreground service společně s notifikatorem</li>
     *     <li>Pošle se zpráva do {@link ThreadAsynchMereni.HandlerAsynchMereni} pro nastaveni offsetu
     *     (kvuli kalibraci) a vynulovani hodnot. (Message what: {@link ThreadAsynchMereni#KALIBRACE_SETTINGS})</li>
     * </ul>
     * @see MainActivity#onButStartDetekce(View)
     * @see MainActivity#kalibraceSenzotu(View)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ServiceDetekce", "onstartcomand" + intent);
        if (intent != null){ // null pokud se to restartuje
            Message message;
            kalibrace = intent.getBooleanExtra("Kalibrovani", false);
            Log.d("kalibraceOnstart",""+kalibrace);
            if(kalibrace){
                timer = new Timer();
                timer.scheduleAtFixedRate(getTask(kalibrace), TIMER_PERIOD_MILISEC_KALIBRACE, TIMER_PERIOD_MILISEC_KALIBRACE);
                message = handlerAsynchMereni.obtainMessage(ThreadAsynchMereni.KALIBRACE_SETTINGS, ThreadAsynchMereni.HandlerAsynchMereni.KALIBRUJEME, ThreadAsynchMereni.HandlerAsynchMereni.KALIBRUJEME);
            } else {
                timer = new Timer();
                timer.scheduleAtFixedRate(getTask(kalibrace), TIMER_PERIOD_MILISEC, TIMER_PERIOD_MILISEC);
                message = handlerAsynchMereni.obtainMessage(ThreadAsynchMereni.KALIBRACE_SETTINGS, ThreadAsynchMereni.HandlerAsynchMereni.NEKALIBRUJEME, ThreadAsynchMereni.HandlerAsynchMereni.NEKALIBRUJEME);
                //Log.d("perioda",""+ TIMER_PERIOD_MILISEC);
            }
            // nastaveni offsetu a mereni
            handlerAsynchMereni.sendMessage(message);

            this.idMereni = intent.getStringExtra("idMereni");
            this.sourceDir = intent.getStringExtra("sourceDir");
            // vytvořeni složky kam se bude ukladat data
            File f = ZapisDoSouboru.getAlbumStorageDir(sourceDir,"");
            f.mkdirs();
            Log.d("Service", "slozky vytvoreny");
        } else {
            Log.d("Service", "restart"); // ještě se mi to nestalo
        }

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        new Intent(this, MainActivity.class),
                        0
                );
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Měření hodnot")
                .setContentText("Kontent text")
                .setSmallIcon(R.drawable.ic_notification)
                //.setContentIntent(resultPendingIntent)
                //.setLargeIcon(Icon.createWithResource(this, R.drawable.ic_notification2))
                .build();

        startForeground(notificationID, notification);
        return super.onStartCommand(intent,flags,startId);
    }

    /**
     * Úkol, který spouští časovač {@link ServiceDetekce#timer} každých {@link ServiceDetekce#TIMER_PERIOD_MILISEC} milisekund.
     * @param isKalibrace jestli je zrovna kalibrujeme senzory
     */
    private TimerTask getTask(boolean isKalibrace){
        TimerTask task = null;
        if (isKalibrace){
            task =  new TimerTask() {
                @Override
                public void run() {
                    Log.d("timer","start kalibrace");
                    handlerAsynchMereni.sendEmptyMessage(ThreadAsynchMereni.GET_VALUES);
                }
            };
        } else {
            task =  new TimerTask() {
                @Override
                public void run() {
                    Log.d("timer","start");
                    handlerAsynchMereni.sendEmptyMessage(ThreadAsynchMereni.GET_VALUES);
                }
            };

        }
        return task;
    }

    /**
     * Handler, který dostává naměřené hodnoty a stará se o další vypočty a dále nastavuje a vytváří nové Thready pro zápis a výpočet.
     * POUZE ZDE SE SPOUSTI ZAPIS HODNOT DO SOUBORU.
     */
    public class HandlerService extends Handler{

        public static final int MEASURING_FINISHED = 0;
        public static final int UPDATE_UI = 1;
        public static final int LIN_INTER_FINISHED = 2;
        public static final int MODUS_FINISHED = 3;
        public static final int FFT_FINISHED = 4;
        public static final int KLASIFICATION_FINISHED = 5;

        private int cisloMereni = 0;

        public HandlerService(Looper looper){
            super(looper);
        }

        /**
         * Broadcasting changes to GUI to {@link MainActivity#mMessageReceiver} or if kalibrace, then to
         * {@link KalibraceActivity#mMessageReceiver}.
         * Bude sloužit pro upozorneni že se něco stalo.
         *
         * @param msg message to handle
         */
        @Override
        public void handleMessage(Message msg){
            ArrayList l;
            ZapisDoSouboru zapis;
            Log.d("Service","incoming Message "+kalibrace);
            switch (msg.what){
                case UPDATE_UI:
                    // zde budu vysilat zmeny na UI, případně upozorneni že se něco děje
                    break;
                case MEASURING_FINISHED:
                    l = (ArrayList) msg.obj;
                    zapis = new ZapisDoSouboru(idMereni,"raw",sourceDir);
                    zapis.setIndex(++cisloMereni);
                    zapis.execute(l); // zapis nezpracovaných hodnot z akcelerometru do souboru
                    if (kalibrace){ // jediny rozdil, pokud dělam kalibraci, je ,že pouze měřim hodnoty a pošlu je pres Intent zpet
                        Intent i = new Intent("Kalibrace");
                        i.putExtra("Hodnoty", l);
                        i.putParcelableArrayListExtra("Array list", l);
                        LocalBroadcastManager.getInstance(ServiceDetekce.this).sendBroadcast(i);
                    } else {
                        Intent i = new Intent("DetekceZachvatu");
                        i.putParcelableArrayListExtra("ArraylistMeasuring", l);
                        LocalBroadcastManager.getInstance(ServiceDetekce.this).sendBroadcast(i);
                        LinInterpolace interpolace = new LinInterpolace(this);
                        interpolace.execute(l);
                    }
                    break;
                case LIN_INTER_FINISHED:
                    l = (ArrayList) msg.obj;
                    zapis = new ZapisDoSouboru(idMereni,"lin",sourceDir);
                    zapis.execute(l);
                    Message msgPom = this.obtainMessage(MODUS_FINISHED, 1,1);
                    this.sendMessage(msgPom);
                    break;
                case MODUS_FINISHED:
                    // tady ziskam z message true/false jestli signal ma dostatecnou energii a zda mam pokracovat dal
                    if (msg.arg1 == 1){
                        int signalEnergy = msg.arg2;
                        // energie signalu
                        // spocteni Fouerierovy transformace
                    }
                    break;
                case FFT_FINISHED:
                    // klasifikace
                    break;
                case KLASIFICATION_FINISHED:
                    break;
                default:
                    Log.e("message","wrong what message");
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
