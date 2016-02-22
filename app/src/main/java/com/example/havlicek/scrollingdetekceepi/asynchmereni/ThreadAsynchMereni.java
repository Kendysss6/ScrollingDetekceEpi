package com.example.havlicek.scrollingdetekceepi.asynchmereni;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.*;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.havlicek.scrollingdetekceepi.SensorValue;
import com.example.havlicek.scrollingdetekceepi.uithread.ServiceDetekce;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

/**
 * Created by Ondřej on 16. 2. 2016.
 */
public class ThreadAsynchMereni extends HandlerThread implements SensorEventListener {
    private Handler uiHandler;
    private Handler mWorkerHandler;

    private List<SensorValue> values;

    private final int pocetPrvkuNavic = 50;

    private DecimalFormat decimalFormat = new DecimalFormat("0.0000000000", new DecimalFormatSymbols(Locale.ENGLISH));

    public ThreadAsynchMereni(String name, Handler uiHandler) {
        super(name, Process.THREAD_PRIORITY_BACKGROUND);
        start();
        this.uiHandler = uiHandler;
        this.mWorkerHandler = new HandlerAsynchMereni(getLooper());
        this.values = new ArrayList<SensorValue>(ServiceDetekce.ODHADOVANY_POCET_PRVKU + pocetPrvkuNavic);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d("JmenoVlakna", Thread.currentThread().getName());
        float [] val = event.values;
        float x = val[0];
        float y = val[1];
        float z = val[2];
        values.add(new SensorValue(event.timestamp, val, decimalFormat));
        Log.d("velikost",Integer.toString(values.size()));

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public Handler getHandlerThread(){
        return mWorkerHandler;
    }



    public class HandlerAsynchMereni extends Handler{

        public HandlerAsynchMereni(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg){

        }
    }
}
