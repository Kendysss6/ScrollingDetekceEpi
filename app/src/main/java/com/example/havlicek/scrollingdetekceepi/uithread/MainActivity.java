package com.example.havlicek.scrollingdetekceepi.uithread;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.havlicek.scrollingdetekceepi.R;
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


    /**
     * Pamatuju si posledni mereni
     */
    private ArrayList<SensorValue> raw = null;
    /**
     * Pamatuju si posledni interpolaci do te doby nez prijde nove mereni
     */
    private ArrayList<SensorValue> lin = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "onCreate()");
        setContentView(R.layout.scroll_activity);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "onDestroy()");
        stopService(new Intent(this, ServiceDetekce.class));
    }

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
    }

    @Override
    protected void onStop(){
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    private void setGUIKalibrovano(boolean kalibrovano){
        findViewById(R.id.but_detekce).setEnabled(kalibrovano);
        //findViewById(R.id.sdilet_but).setEnabled(false);
        findViewById(R.id.vykresli_but).setEnabled(kalibrovano);
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
            i.putExtra("sourceDir",sourceDir);
            startService(i);

        } else {
            detectionOff = true;
            b.setText(R.string.start);
            // stop service, must explicitly stop because http://developer.android.com/guide/components/bound-services.html#Lifecycle
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
        Intent i = new Intent(this, Grafy.class);
        /*
        i.putExtra("lin", vh.linData);
        i.putExtra("fft", vh.fft);
        i.putExtra("raw", vh.rawData);
        i.putExtra("modus", vh.signalModus);
        */
        if(true) {
            Intent in = new Intent(this, Grafy.class);
            in.putExtra("sourceDir", sourceDir);
            startActivity(in);
        } else {
            Toast toast = Toast.makeText(this, "Data zpracovávají, stiskněte později", Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    public void shit(View v){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean("kalibrovano", false);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //String datum = sdf.format(new Date());
        editor.putString("datum_kalibrace","1970-01-01");
        editor.apply();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("MainActivity","Broadcastreciever " + action);
            if(intent.hasExtra("ArraylistMeasuring")){
                raw = intent.getParcelableArrayListExtra("ArraylistMeasuring");
                // nove mereni
                lin = null;
            }
            if(intent.hasExtra("ArraylistInterpolace")){
                ArrayList<SensorValue> lin = intent.getParcelableArrayListExtra("ArraylistMeasuring");
            }
            if(action.equals("DetekceZachvatu")){

            } else if(action.equals("Destroying Service")){
                if(android.os.Build.VERSION.SDK_INT >= 21) {
                    finishAndRemoveTask();
                } else {
                    finish();
                }
                System.exit(0);
            }
        }
    };

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
