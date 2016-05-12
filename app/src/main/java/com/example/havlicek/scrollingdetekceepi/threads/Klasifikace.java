package com.example.havlicek.scrollingdetekceepi.threads;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.havlicek.scrollingdetekceepi.datatypes.FFTType;
import com.example.havlicek.scrollingdetekceepi.datatypes.SensorValue;
import com.example.havlicek.scrollingdetekceepi.uithread.ServiceDetekce;

import org.apache.commons.math3.complex.Complex;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Created by Havlicek on 23.4.2016.
 * Třída pro klasifikaci dat.
 */
public class Klasifikace extends Thread{
    private Handler serviceHandler;
    private FFTType pomvalues;

    public Klasifikace(FFTType values, Handler serviceHandler){
        this.serviceHandler = serviceHandler;
        this.pomvalues = values;
        this.setName("LinInterpolace");
    }

    @Override
    public void run(){
        FFTType sensorValues = this.pomvalues;
        // klasifikace, najdu dominantní frekvenci v 0-10 Hz
        Complex [] val = sensorValues.fft;
        sensorValues = null;

        // hledame dominantní frekvenci

        int maxIndex = 0;
        double maxVal = val[maxIndex].abs();
        for (int i = 0; i < val.length; i++){
            if(val[i].abs() > maxVal){
                maxIndex = i;
                maxVal = val[i].abs();
            }
        }
        double frekvenceVzorkovani = 100;
        Log.d("Klasifikace","index "+maxIndex+" frekvence "+(maxIndex * frekvenceVzorkovani / 1024)+" maxval "+maxVal);

        int klasifikace = 0;
        // indentifikace dominantní frekvence
        if (maxIndex * frekvenceVzorkovani / 1024 > 2){
            klasifikace = 1;
        }


        Message msg = serviceHandler.obtainMessage(ServiceDetekce.HandlerService.KLASIFICATION_FINISHED, klasifikace, maxIndex);
        serviceHandler.sendMessage(msg);
    }
}
