package com.example.havlicek.scrollingdetekceepi.asynchtasks;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.havlicek.scrollingdetekceepi.SensorValue;
import com.example.havlicek.scrollingdetekceepi.uithread.ServiceDetekce;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.distribution.LogisticDistribution;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Created by Ondřej on 24. 2. 2016.
 *
 * Algoritmus
 * 1. Urči sampling frequenci
 * 2. Urči ekvidistatnt spaces
 */
public class LinInterpolace extends AsyncTask<ArrayList<SensorValue>, Integer, ArrayList<SensorValue>> {
    private Handler uiHandler;

    public LinInterpolace(Handler uiHandler){
        this.uiHandler = uiHandler;
    }
    /**
     * Moznost 1. oneNote / projektovy plan / vypočet algoritmus / lin interpolace
     * @param params naměřené hodnoty
     * @return interpolované naměřené hodnoty
     */
    @Override
    protected ArrayList<SensorValue> doInBackground(ArrayList<SensorValue>... params) {
        Thread.currentThread().setName("Linearni interpolace");
        ArrayList<SensorValue> sensorValues = params[0];
        ListIterator<SensorValue> iterator = sensorValues.listIterator();

        int pocetNamerenychHodnot = sensorValues.size();
        long dobaMereni = sensorValues.get(pocetNamerenychHodnot - 1).getTimeStamp() - sensorValues.get(0).getTimeStamp();
        /**
         * ta -1 dole je tam proto, že mezi N hodnotami je N-1 period :), N = pocetNamerenychHodnot
         */
        long periodaVzorkovani = dobaMereni / (pocetNamerenychHodnot - 1); // zaokrouhlime, je to v nanosekundach

        Log.d("Interpolace","perioda vzorkovani "+periodaVzorkovani);
        Log.d("Interpolace","pocet hodnot "+pocetNamerenychHodnot);

        ArrayList<SensorValue> interpolatedValues = new ArrayList<SensorValue>(pocetNamerenychHodnot);

        long time = 0;
        int i = 0;
        long x0,x1;
        float y0X, y1X,y0Y, y1Y,y0Z, y1Z;
        float interpolatedX, interpolatedY, interpolatedZ;
        SensorValue value1;
        SensorValue value2 = iterator.next(); //  prvni hodnota
        long initialTime = value2.getTimeStamp();
        do {
            value1 = value2;
            value2 = iterator.next();

            x0 = value1.getTimeStamp() - initialTime;
            x1 = value2.getTimeStamp() - initialTime;
            y0X = value1.getfX();
            y1X = value2.getfX();
            y0Y = value1.getfY(); y1Y = value2.getfY();
            y0Z = value1.getfZ(); y1Z = value2.getfZ();
            while (x0 <= time && time <= x1){
                interpolatedX = y0X + (y1X - y0X)*(time - x0)/(x1 - x0);
                interpolatedY = y0Y + (y1Y - y0Y)*(time - x0)/(x1 - x0);
                interpolatedZ = y0Z + (y1Z - y0Z)*(time - x0)/(x1 - x0);
                interpolatedValues.add(new SensorValue(time, interpolatedX, interpolatedY, interpolatedZ));
                time += periodaVzorkovani;
                i++;
            }
        } while (iterator.hasNext());
        return interpolatedValues;
    }

    @Override
    protected void onPostExecute(ArrayList<SensorValue> values){
        Message msg = uiHandler.obtainMessage(ServiceDetekce.HandlerUI.LIN_INTER_FINISHED, values);
        uiHandler.sendMessage(msg);
    }
}