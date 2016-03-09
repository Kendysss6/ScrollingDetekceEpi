package com.example.havlicek.scrollingdetekceepi.uithread;

import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.example.havlicek.scrollingdetekceepi.R;
import com.example.havlicek.scrollingdetekceepi.SensorValue;
import com.example.havlicek.scrollingdetekceepi.asynchmereni.ThreadAsynchMereni;
import com.example.havlicek.scrollingdetekceepi.asynchtasks.LinInterpolace;
import com.example.havlicek.scrollingdetekceepi.asynchtasks.ZapisDoSouboru;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Kalibrace activity, ktera slouží pro zjištění hodnot offsetX, offsetY, offsetZ a estimated sampling frequenci.
 * Simuluji (a skutečně spouštím) všechny služby potřebné pro výpočty a klasifikaci, jelikož pokud budu jen sbírat data a nic nepočítat,
 * tak se perioda vzorkování zrychlí (Mě se zrychlilo zatim asi o cca asi o 0.01 sekundy).
 * Offsets použiju ve třídě {@link ThreadAsynchMereni}, které upravi hodnoty a sampling frequenci použiju v {@link LinInterpolace}, které
 * odečtou posunutí nuly senzorů daného telefonu.
 */
public class KalibraceActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private List<SensorValue> values;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kalibrace_layout);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        //mSensorManager.registerListener(this, mAccelerometer,SensorManager.SENSOR_DELAY_FASTEST);
        this.values = new LinkedList<SensorValue>();
    }


    public void kalibrace(View v){
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        double milisecToSec = 1e03;
        long dobaKalibraceMilisec = (long) (10 * milisecToSec);
        new Timer("Kalibrace").schedule(new TimerTask() {
            @Override
            public void run() {
                mSensorManager.unregisterListener(KalibraceActivity.this);
                spoctiKalibraci();
            }
        }, dobaKalibraceMilisec);




    }

    private void spoctiKalibraci(){
        // ListIterator<SensorValue> iterator = values.listIterator();
        ZapisDoSouboru zapis = new ZapisDoSouboru("kalibrace");
        zapis.execute(values);
        float offsetX = 0;
        Mean mX = new Mean();
        Mean mY = new Mean();
        Mean mZ = new Mean();
        Mean time = new Mean();
        long lastTimeValue = values.get(0).getTimeStamp();
        boolean i = true;
        double perioda_sum = 0;
        for (SensorValue value: values){
            mX.increment(value.getfX());
            mY.increment(value.getfY());
            mZ.increment(value.getfZ());
            if (i){i=false;continue;} // vynecham prvni hodnotu
            time.increment(value.getTimeStamp() - lastTimeValue);
            perioda_sum += value.getTimeStamp() - lastTimeValue;
            lastTimeValue = value.getTimeStamp();
        }
        double meanTsSec = time.getResult() * 1e-9;
        double meanTsSec2 = perioda_sum / (values.size() - 1);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean("kalibrovano", true);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String datum = sdf.format(new Date());
        editor.putString("datum_kalibrace", datum);
        editor.putFloat("offsetX", (float) mX.getResult());
        editor.putFloat("offsetY", (float) mY.getResult());
        editor.putFloat("offsetZ", (float) mZ.getResult());
        editor.putFloat("meanTimeNanosec", (float) meanTsSec2);

        editor.commit();
        finish();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float [] val = event.values;
        float x = val[0];
        float y = val[1];
        float z = val[2];
        values.add(new SensorValue(event.timestamp, val));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
