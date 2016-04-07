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
 *
 * Here i will be managing offset (substracting)
 */
public class ThreadAsynchMereni extends HandlerThread implements SensorEventListener {
    public static final int  GET_VALUES = 1;


    private Handler uiHandler;
    private Handler mWorkerHandler;

    private List<SensorValue> values;

    private final int pocetPrvkuNavic = 50;
    private float offsetX;
    private float offsetY;
    private float offsetZ;

    private DecimalFormat decimalFormat = new DecimalFormat("0.0000000000", new DecimalFormatSymbols(Locale.ENGLISH));


    public ThreadAsynchMereni(String name, Handler uiHandler, float offsetX, float offsetY, float offsetZ) {
        super(name, Process.THREAD_PRIORITY_URGENT_AUDIO);
        start();
        this.uiHandler = uiHandler;
        this.mWorkerHandler = new HandlerAsynchMereni(getLooper());
        this.values = new ArrayList<SensorValue>(ServiceDetekce.ODHADOVANY_POCET_PRVKU + pocetPrvkuNavic);
        if(offsetX == 0 || offsetY == 0 || offsetZ == 0){
            Log.e("mereni","spatny offsety");
            throw new IllegalArgumentException("spatny offsety");
        }
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Log.d("JmenoVlakna", Thread.currentThread().getName());
       // if (values.size() <= ServiceDetekce.ODHADOVANY_POCET_PRVKU ){
            float [] val = event.values;
            float x = val[0] - offsetX;
            float y = val[1] - offsetY;
            float z = val[2] - offsetZ;
            values.add(new SensorValue(event.timestamp, x, y, z, decimalFormat));
        //}
        //Log.d("velikost",Integer.toString(values.size()));
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
            switch (msg.what){
                case GET_VALUES:
                    Message m = Message.obtain();
                    m.what = ServiceDetekce.HandlerUI.MEASURING_FINISHED;
                    m.obj = ThreadAsynchMereni.this.values;
                    values = new ArrayList<SensorValue>(ServiceDetekce.ODHADOVANY_POCET_PRVKU + pocetPrvkuNavic);
                    uiHandler.sendMessage(m);
                    break;
                default:
                    break;
            }
        }
    }
}
