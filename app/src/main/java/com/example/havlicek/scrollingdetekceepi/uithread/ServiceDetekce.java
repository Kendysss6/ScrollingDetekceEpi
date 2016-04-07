package com.example.havlicek.scrollingdetekceepi.uithread;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.example.havlicek.scrollingdetekceepi.R;
import com.example.havlicek.scrollingdetekceepi.asynchmereni.ThreadAsynchMereni;
import com.example.havlicek.scrollingdetekceepi.asynchtasks.LinInterpolace;
import com.example.havlicek.scrollingdetekceepi.asynchtasks.ZapisDoSouboru;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
     *  Je to <b>VLÁKNO</b>! NENÍ TO HANDLER! Je to třída HandlerThread, ktera vytvoři vlakno, kde se zpracuji sensor eventy.
     */
    private ThreadAsynchMereni threadAsynchMereni;
    /**
     * Timer, ktery každou periodu {@link #TIMER_PERIOD_MILISEC} zavolá měření (pošle {@link #timerTask}) pro vyhodnocení naměřených dat.
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
     * Perioda, za kterou časovač {@link ServiceDetekce#timer} spouští úkol {@link ServiceDetekce#timerTask}.
     */
    //public final int TIMER_PERIOD_MILISEC = MY_SAMPLING_PERIOD_MICROSEC * ODHADOVANY_POCET_PRVKU; // perioda
    public static final int TIMER_PERIOD_MILISEC = 10000; // perioda v milisekundach, kazdych 10 sekund

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
        // Handler for user interface
        handlerUI = new HandlerUI();
        // create new thread for asynch sampling with its handler
        threadAsynchMereni = new ThreadAsynchMereni("Asynchronní měření", handlerUI,
                p.getFloat("offsetX", 0f), p.getFloat("offsetY", 0f), p.getFloat("offsetZ", 0f));
        // its handler
        handlerAsynchMereni = threadAsynchMereni.getHandlerThread();

        // register to service to obtain values to this handler
        //mSensorManager.registerListener(threadAsynchMereni, mAccelerometer, MY_SAMPLING_PERIOD_MICROSEC, handlerAsynchMereni);
        mSensorManager.registerListener(threadAsynchMereni, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST, handlerAsynchMereni);
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, TIMER_PERIOD_MILISEC, TIMER_PERIOD_MILISEC);
        //Log.d("perioda",""+ TIMER_PERIOD_MILISEC);

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
        timer.cancel();
        mSensorManager.unregisterListener(threadAsynchMereni);
        threadAsynchMereni.quit();
        wakeLock.release();
        Log.d("wakelock", "released");
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
        Log.d("ServiceDetekce", "onstartcomand" + intent);
        if (intent != null){ // null pokud se to restartuje
            kalibrace = intent.getBooleanExtra("Kalibrovani", false);
            Log.d("kalibraceOnstart",""+kalibrace);
            this.idMereni = intent.getStringExtra("idMereni");
            this.sourceDir = intent.getStringExtra("sourceDir");
            // vytvořeni složky kam se bude ukladat data
            File f = ZapisDoSouboru.getAlbumStorageDir(sourceDir,"");
            f.mkdirs();
        } else {
            Log.d("Service","restart");
        }
        Log.d("Service","slozky vytvoreny");

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        new Intent(this, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Měření hodnot")
                .setContentText("Kontent text")
                .setSmallIcon(R.drawable.ic_notification)
               // .setContentIntent(resultPendingIntent)
                //.setLargeIcon(Icon.createWithResource(this, R.drawable.ic_notification2))
                .build();

        startForeground(notificationID, notification);
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
     * Handler, který dostává naměřené hodnoty a stará se o další vypočty a dále nastavuje a vytváří nové Thready pro zápis a výpočet.
     * POUZE ZDE SE SPOUSTI ZAPIS HODNOT DO SOUBORU.
     */
    public class HandlerUI extends Handler{
        public static final int MEASURING_FINISHED = 0;
        public static final int UPDATE_UI = 1;
        public static final int LIN_INTER_FINISHED = 2;
        public static final int NORM_FINISHED = 3;
        public static final int FFT_FINISHED = 4;
        public static final int KLASIFICATION_FINISHED = 5;

        public HandlerUI(){
            super(Looper.getMainLooper());
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
                    zapis.execute(l); // zapis nezpracovaných hodnot z akcelerometru do souboru
                    if (kalibrace){ // jediny rozdil, pokud dělam kalibraci, je ,že pouze měřim hodnoty a pošlu je pres Intent zpet
                        Intent i = new Intent("Kalibrace");
                        i.putExtra("Hodnoty", l);
                        LocalBroadcastManager.getInstance(ServiceDetekce.this).sendBroadcast(i);
                    } else {
                        LocalBroadcastManager.getInstance(ServiceDetekce.this).sendBroadcast(new Intent("DetekceZachvatu"));
                        LinInterpolace interpolace = new LinInterpolace(this);
                        interpolace.execute(l);
                    }
                    break;
                case LIN_INTER_FINISHED:
                    l = (ArrayList) msg.obj;
                    zapis = new ZapisDoSouboru(idMereni,"lin",sourceDir);
                    zapis.execute(l);
                    Message msgPom = this.obtainMessage(NORM_FINISHED, 1,1);
                    this.sendMessage(msgPom);
                    break;
                case NORM_FINISHED:
                    // tady ziskam z message true/false jestli signal ma dostatecnou energii a zda mam pokracovat dal
                    if (msg.arg1 == 1){
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
