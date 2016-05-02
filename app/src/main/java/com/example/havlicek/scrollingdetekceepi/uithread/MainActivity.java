package com.example.havlicek.scrollingdetekceepi.uithread;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.havlicek.scrollingdetekceepi.R;
import com.example.havlicek.scrollingdetekceepi.datatypes.FFTType;
import com.example.havlicek.scrollingdetekceepi.datatypes.ModusSignaluType;
import com.example.havlicek.scrollingdetekceepi.datatypes.SensorValue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends Activity {
    private boolean detectionOff = true;
    private String idMereni = null;
    private String sourceDir = null;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");

    private final int VYKRESLI_RAW = 1;
    private final int VYKRESLI_LIN = 2;
    private final int VYKRESLI_FFT = 3;

    /**
     * Pamatuju si posledni mereni
     */
    private ArrayList<SensorValue> raw = null;
    /**
     * Pamatuju si posledni interpolaci do te doby nez prijde nove mereni
     */
    private ArrayList<SensorValue> lin = null;

    /**
     * Opět si pamatuju posledni
     */
    private FFTType fft = null;
    private ModusSignaluType modus = null;
    private ModusSignaluType fModus = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate()");
        setContentView(R.layout.scroll_activity);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "onDestroy() activity and Service");
        stopService(new Intent(this, ServiceDetekce.class));
    }
/* Moznost jak zabranit orientacim
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }*/

    @Override
    protected  void onStart(){
        super.onStart();
        Log.d("MainActivity","onStart");
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this); // (context == aktivita)
        boolean kalibrovano = p.getBoolean("kalibrovano", false);
        String strDate = p.getString("datum_kalibrace", "1970-01-01");
        try {
            Date datumKalibrace = new SimpleDateFormat("yyyy-MM-dd").parse(strDate);
            long days = (new Date().getTime() - datumKalibrace.getTime()) / (24 * 60 * 60 * 1000);
            if (days > 10){
                kalibrovano = false;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        setGUIKalibrovano(kalibrovano);
        if(kalibrovano){
            TextView b = (TextView) findViewById(R.id.offsetX);
            b.setText(Float.toString(p.getFloat("offsetX", 0f)));
            b = (TextView) findViewById(R.id.offsetY);
            b.setText(Float.toString(p.getFloat("offsetY", 0f)));
            b = (TextView) findViewById(R.id.offsetZ);
            b.setText(Float.toString(p.getFloat("offsetZ", 0f)));
            b = (TextView) findViewById(R.id.sampling_period);
            b.setText(Float.toString(p.getFloat("meanTimeNanosec", 0f)));
            //b.setText(""+p.getFloat("meanTimeNanosec", 0f));
        }


        // register localBroadCastReciever to respond changes in UI from service
        TextView textView = (TextView) findViewById(R.id.pomText);
        Log.d("Path", getFilesDir().getAbsolutePath());
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("DetekceZachvatu"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Destroying Service"));

        Button b = (Button) findViewById(R.id.but_detekce);
        if (isMyServiceRunning(ServiceDetekce.class)){
            detectionOff = false;
            b.setText(R.string.stop);
        } else {
            detectionOff = true;
            b.setText(R.string.start);
        }

        // smazat
        /*
        SensorManager mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        TextView text = (TextView) findViewById(R.id.pomText);
        String textDisplay;
        textDisplay =               "LOL"+mAccelerometer.getMaximumRange();
        textDisplay = textDisplay+"\nLOL"+mAccelerometer.getMinDelay();
        textDisplay = textDisplay+"\nLOL"+mAccelerometer.getName();
        textDisplay = textDisplay+"\nLOL"+mAccelerometer.getPower();
        textDisplay = textDisplay+"\nLOL"+mAccelerometer.getResolution();
        textDisplay = textDisplay+"\nLOL"+mAccelerometer.getType();
        textDisplay = textDisplay+"\nLOL"+mAccelerometer.getVendor();
        textDisplay = textDisplay+"\nLOL"+mAccelerometer.getVersion();
        textDisplay = textDisplay+"\nLOL"+mAccelerometer.toString();
        if(Build.VERSION.SDK_INT >= 21){
            textDisplay = textDisplay+"\nLOL"+mAccelerometer.getMaxDelay();
            textDisplay = textDisplay+"\nLOL"+mAccelerometer.getStringType();
            textDisplay = textDisplay+"\nLOL"+mAccelerometer.isWakeUpSensor(); // neco s power saving
        /}
        Log.d("SDK",""+Build.VERSION.SDK_INT);
        Log.d("resolution",""+mAccelerometer.getResolution());
        text.setText(textDisplay);

        */
    }

    @Override
    protected void onStop(){
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    private void setGUIKalibrovano(boolean kalibrovano){
        findViewById(R.id.but_detekce).setEnabled(kalibrovano);
        //findViewById(R.id.sdilet_but).setEnabled(false);
        //findViewById(R.id.vykresli_but).setEnabled(kalibrovano);
    }

    public void onButStartDetekce(View v){
        Button b = (Button) findViewById(R.id.but_detekce);
        if (detectionOff) {
            detectionOff = false;
            b.setText(R.string.stop);
            // Bind to the service
            Intent i = new Intent(this, ServiceDetekce.class);
            this.idMereni = sdf.format(new Date());
            this.sourceDir = getResources().getString(R.string.app_name)+"/"+idMereni+"/";
            TextView t = (TextView) findViewById(R.id.id_mereni);
            t.setText(idMereni);
            i.putExtra("idMereni", idMereni);
            i.putExtra("kalibrace", false);
            i.putExtra("sourceDir", sourceDir);
            startService(i);
            //bindService(i, mConnection,Context.BIND_AUTO_CREATE);


        } else {
            detectionOff = true;
            b.setText(R.string.start);
            // stop service, must explicitly stop because http://developer.android.com/guide/components/bound-services.html#Lifecycle
            Log.d("MainActivity", "Stopping Service");
            stopService(new Intent(this, ServiceDetekce.class));
        }
    }

    /**
     * Spusti aktivitu, která se stará o kalibraci a zjištění konstant senzorů.
     * @param v Button ktery to spustil
     */
    public void kalibraceSenzotu(View v){
        Intent i = new Intent(this,KalibraceActivity.class);
        i.putExtra("sourceDir", sourceDir);
        startActivity(i);
    }

    /**
     * Pokud budu mít všechny data tj. raw data, lin data, modus data a fft data pak spusti aktivitu Grafy.
     * Jinak hodi upozorneni "Data zpracovávají, stiskněte později". pmoci Toast
     * @param v
     */
    public void vykresli(View v){
        //FFTType vh = new FFTType();
      //  Intent i = new Intent(this, Grafy.class);
       // i.putParcelableArrayListExtra("RawList",this.raw);
        /*
        i.putExtra("fft", vh.fft);

        i.putExtra("modus", vh.signalModus);
        */

        RadioButton b = (RadioButton) findViewById(R.id.namerenaDat_radio);
        if(b.isChecked()) {
            Intent in = new Intent(this, Grafy.class);
            in.putExtra("sourceDir", sourceDir);
            in.putParcelableArrayListExtra("List", this.raw);
            startActivity(in);
        }
        b = (RadioButton) findViewById(R.id.inter_radio);
        if(b.isChecked()) {
            Intent in = new Intent(this, Grafy.class);
            in.putExtra("sourceDir", sourceDir);
            in.putParcelableArrayListExtra("List", this.lin);
            startActivity(in);
        }
        b = (RadioButton) findViewById(R.id.fft_radio);
        if(b.isChecked()) {
            Intent in = new Intent(this, Grafy.class);
            in.putExtra("sourceDir", sourceDir);
            in.putExtra("FFT", this.fft);
            in.putExtra("Modus", this.modus);
            in.putExtra("FModus",this.fModus);
            startActivity(in);
        }
        /*
        b = (RadioButton) findViewById(R.id.fft_radio);
        if(b.isChecked()) {
            Intent in = new Intent(this, Grafy.class);
            in.putExtra("sourceDir", sourceDir);
            in.putParcelableArrayListExtra("List", this.fft);
            startActivity(in);
        }
        */
    }

    public void shit(View v){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean("kalibrovano", false);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //String datum = sdf.format(new Date());
        editor.putString("datum_kalibrace", "1970-01-01");
        editor.apply();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("MainActivity", "Broadcastreciever " + action);
            CheckBox zachovat = (CheckBox)findViewById(R.id.zachovat_data);
            if(intent.hasExtra("ArraylistMeasuring")){
                raw = intent.getParcelableArrayListExtra("ArraylistMeasuring");
                //Log.d("list",""+raw);
                // nove mereni
                if(!zachovat.isChecked()){
                    lin = null;
                    fft = null;
                    modus = null;
                    fModus = null;
                    // set state
                    Toast.makeText(MainActivity.this,"Nová data",Toast.LENGTH_SHORT).show();
                    setStateVykresli(VYKRESLI_RAW);
                }
            }
            if(intent.hasExtra("ArraylistInterpolace")){
                if(!zachovat.isChecked()){
                    lin = intent.getParcelableArrayListExtra("ArraylistInterpolace");
                    setStateVykresli(VYKRESLI_LIN);
                }
            }
            if(intent.hasExtra("Modus")){
                if(!zachovat.isChecked()){
                    modus = intent.getParcelableExtra("Modus");
                }
            }
            if (intent.hasExtra("TimeAnalysis")){
                if (!intent.getBooleanExtra("TimeAnalysis",false)){
                    TextView t = (TextView) findViewById(R.id.vysledek_mereni);
                    t.setText("False, non-movement");
                }
            }
            if(intent.hasExtra("FModus")){
                if(!zachovat.isChecked()){
                    fModus = intent.getParcelableExtra("FModus");
                }
            }
            if(intent.hasExtra("FFT")){
                if(!zachovat.isChecked()){
                    fft = intent.getParcelableExtra("FFT");
                    setStateVykresli(VYKRESLI_FFT);
                }
            }

            if(action.equals("DetekceZachvatu")){

            }
            if (intent.hasExtra("Klasifikace")){
                boolean klas = intent.getBooleanExtra("Klasifikace", false);
                int frek = intent.getIntExtra("DomFrek",-1);

                TextView t = (TextView)findViewById(R.id.vysledek_mereni);
                t.setText("Klasif. "+klas+" dom.frek. "+((frek * 100.0) / 1024));
            }
            /* Na testovani to zakometuju
            else if(action.equals("Destroying Service")){
                if(android.os.Build.VERSION.SDK_INT >= 21) {
                    finishAndRemoveTask();
                } else {
                    finish();
                }
                System.exit(0);
            }*/
        }
    };

    /**
     * Metoda ktery zpristupni button a checkboxy pro vykresleni, zpravidla 1-3 range
     * ale je možne i uplne vypnout když pošlem 0
     * @param numberOfField pocet hodnot které máme naměřených, zatim 0-3
     */
    private void setStateVykresli(int numberOfField){
        View v = findViewById(R.id.namerenaDat_radio);
        if(numberOfField >= VYKRESLI_RAW){
            v.setEnabled(true);
            ((RadioButton)v).setChecked(true);
            findViewById(R.id.vykresli_but).setEnabled(true);
        } else {
            v.setEnabled(false);
            findViewById(R.id.vykresli_but).setEnabled(false);
        }
        v = findViewById(R.id.inter_radio);
        if(numberOfField >= VYKRESLI_LIN){
            v.setEnabled(true);
        } else {
            v.setEnabled(false);
        }
        v = findViewById(R.id.fft_radio);
        if(numberOfField >= VYKRESLI_FFT){
            v.setEnabled(true);
        } else {
            v.setEnabled(false);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
