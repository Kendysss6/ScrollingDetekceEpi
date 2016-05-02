package com.example.havlicek.scrollingdetekceepi.threads;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.havlicek.scrollingdetekceepi.datatypes.ModusSignaluType;
import com.example.havlicek.scrollingdetekceepi.datatypes.SensorValue;
import com.example.havlicek.scrollingdetekceepi.uithread.ServiceDetekce;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Created by Havlicek on 23.4.2016.
 */
public class Modus extends Thread{
    private Handler serviceHandler;
    private ArrayList<SensorValue> pomvalues;

    public Modus(ArrayList<SensorValue> values, Handler serviceHandler){
        this.serviceHandler = serviceHandler;
        this.pomvalues = values;
    }

    @Override
    public void run(){
        ArrayList<SensorValue> sensorValues = pomvalues;
        ListIterator<SensorValue> iterator = sensorValues.listIterator();

        double [] modusSignalu = new double[sensorValues.size()];
        long [] time = new long[sensorValues.size()];
        double [] timeAnalysisModus = new double[sensorValues.size()];


        int in = 0;
        double partialEnergy = 0;
        while (iterator.hasNext()){
            SensorValue sv = iterator.next();
            float X = sv.getfX(),Y = sv.getfY(),Z = sv.getfZ();
            partialEnergy = X*X+Y*Y+Z*Z;
            timeAnalysisModus[in] = Math.sqrt(partialEnergy);
            modusSignalu[in] = Z;
            time[in] = sv.getTimeStamp();
            in++;
        }
        ModusSignaluType modus = new ModusSignaluType(modusSignalu, time, timeAnalysisModus);

        Message msg = serviceHandler.obtainMessage(ServiceDetekce.HandlerService.MODUS_FINISHED, modus);
        serviceHandler.sendMessage(msg);
    }
}
