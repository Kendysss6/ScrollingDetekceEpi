package com.example.havlicek.scrollingdetekceepi.asynchtasks;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.example.havlicek.scrollingdetekceepi.SensorValue;
import com.example.havlicek.scrollingdetekceepi.ValueHolder;
import com.example.havlicek.scrollingdetekceepi.uithread.ServiceDetekce;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Created by Ondřej on 24. 2. 2016.
 *
 */
public class ModusSignalu extends AsyncTask<ArrayList<SensorValue>, Integer, ValueHolder> {
    private Handler uiHandler;

    public ModusSignalu(Handler uiHandler){
        this.uiHandler = uiHandler;
    }

    @Override
    protected  ValueHolder doInBackground(ArrayList<SensorValue>... params) {
        Thread.currentThread().setName("Modus signalu");
        ArrayList<SensorValue> sensorValues = params[0];
        ListIterator<SensorValue> iterator = sensorValues.listIterator();

        ValueHolder vh = new ValueHolder();
        double [] modusSignalu = new double[sensorValues.size()];
        //long [] time = new long[sensorValues.size()];

        int in = 0;
        double partialEnergy = 0;
        double wholeEnergy = 0;
        while (iterator.hasNext()){
            SensorValue sv = iterator.next();
            //time[in] = sv.getTimeStamp();
            float X = sv.getfX(),Y = sv.getfY(),Z = sv.getfZ();
            partialEnergy = X*X+Y*Y+Z*Z;
            modusSignalu[in] = Math.sqrt(partialEnergy);
            wholeEnergy += partialEnergy;
        }

        vh.signalModus = modusSignalu;
        //vh.time = time;
        vh.signalEnergy = wholeEnergy;

        return vh;
    }

    @Override
    protected void onPostExecute(ValueHolder values){
        Message msg = uiHandler.obtainMessage(ServiceDetekce.HandlerService.MODUS_FINISHED, 1, 0, values);
        uiHandler.sendMessage(msg);
    }
}