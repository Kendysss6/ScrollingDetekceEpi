package com.example.havlicek.scrollingdetekceepi.threads;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.havlicek.scrollingdetekceepi.datatypes.ModusSignaluType;
import com.example.havlicek.scrollingdetekceepi.datatypes.SensorValue;
import com.example.havlicek.scrollingdetekceepi.uithread.ServiceDetekce;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Created by Ondřej on 28. 4. 2016.
 */
public class HighPassFilter extends Thread{
    private Handler serviceHandler;
    private ModusSignaluType pomvalues;
    private double [] beforeValues;
    private RealMatrix matrix;


    public HighPassFilter(ModusSignaluType values, Handler serviceHandler, RealMatrix matrix){
        this.serviceHandler = serviceHandler;
        this.pomvalues = values;
        this.matrix = matrix;
    }

    @Override
    public void run(){
        ModusSignaluType sensorValues = this.pomvalues;
        this.pomvalues = null; // kvuli GC



        /* OLD
        double [] val = sensorValues.val; // neprepisuj stary values, dale se pouzivaji
        double [] fVal = new double[val.length];

        // diferenciator

        fVal[0] = 0;
        for (int i = 1; i < val.length; i++){
            fVal[i] = val[i] - val[i-1];
        }
        // centrální diferenciator

        fVal[0] = 0;
        fVal[1] = 0;
        for (int i = 2; i < val.length; i++){
            fVal[i] = (val[i] - val[i-2])/2;
        }*/

        int N = 256; // velikost matice pro vyhlazeni
        double [] val = sensorValues.val.clone();
        double [] pomVal = new double[N];
        double [] result = new double[1024];

        int i = 0;
        for (int j = 0; j < val.length/N; j++){
            for(; i < N*(j+1); i++){
                pomVal[i - j*N] = val[i];
            }
            RealVector vector = new ArrayRealVector(pomVal);
            double [] rslt = matrix.operate(vector).toArray();
            // zapis vysledku
            for (int m = 0; m < rslt.length; m++){
                result[m + j*N] = rslt[m];
            }
        }


        for (int m = 0; m < N; m++){
            pomVal[m] = val[val.length + m - N];
        }
        RealVector vector = new ArrayRealVector(pomVal);
        double [] rslt = matrix.operate(vector).toArray();
        for (int m = 0; m < rslt.length; m++){
            result[result.length + m - rslt.length] = rslt[m];
        }

        for (int m = 0; m < result.length; m++){
            Log.d("vysledky",""+result[m]);
        }


        //Log.d("pad", "" + fVal);
        // algoritmus
        ModusSignaluType vysledek = new ModusSignaluType(result,sensorValues.time); // s casem nic nedělam ten mužu nechat stejny

        Message msg = serviceHandler.obtainMessage(ServiceDetekce.HandlerService.FILTER_FINISHED, vysledek);
        serviceHandler.sendMessage(msg);
    }
}
