package com.example.havlicek.scrollingdetekceepi.asynchtasks;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.example.havlicek.scrollingdetekceepi.datatypes.FFTType;
import com.example.havlicek.scrollingdetekceepi.datatypes.ModusSignaluType;
import com.example.havlicek.scrollingdetekceepi.datatypes.SensorValue;
import com.example.havlicek.scrollingdetekceepi.uithread.ServiceDetekce;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Created by Ondřej on 24. 2. 2016.
 *
 */
public class ModusSignalu extends AsyncTask<ArrayList<SensorValue>, Integer, ModusSignaluType> {
    private Handler uiHandler;

    public ModusSignalu(Handler uiHandler){
        this.uiHandler = uiHandler;
    }

    @Override
    protected ModusSignaluType doInBackground(ArrayList<SensorValue>... params) {
        Thread.currentThread().setName("Modus signalu");
        ArrayList<SensorValue> sensorValues = params[0];
        ListIterator<SensorValue> iterator = sensorValues.listIterator();

        double [] modusSignalu = new double[sensorValues.size()];
        long [] time = new long[sensorValues.size()];


        int in = 0;
        double partialEnergy = 0;
        while (iterator.hasNext()){
            SensorValue sv = iterator.next();
            float X = sv.getfX(),Y = sv.getfY(),Z = sv.getfZ();
            partialEnergy = X*X+Y*Y+Z*Z;
            modusSignalu[in] = Math.sqrt(partialEnergy);
            time[in] = sv.getTimeStamp();
        }
        ModusSignaluType modus = new ModusSignaluType(modusSignalu, time);


        return modus;
    }

    @Override
    protected void onPostExecute(ModusSignaluType values){
        Message msg = uiHandler.obtainMessage(ServiceDetekce.HandlerService.MODUS_FINISHED, 1, 0, values);
        uiHandler.sendMessage(msg);
    }
}