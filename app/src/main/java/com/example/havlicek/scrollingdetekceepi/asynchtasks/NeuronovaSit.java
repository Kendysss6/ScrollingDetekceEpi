package com.example.havlicek.scrollingdetekceepi.asynchtasks;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.havlicek.scrollingdetekceepi.datatypes.SensorValue;
import com.example.havlicek.scrollingdetekceepi.uithread.ServiceDetekce;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Created by Ondřej on 24. 2. 2016.
 *
 * Algoritmus
 * 1. Urči sampling frequenci
 * 2. Urči ekvidistatnt spaces
 */
public class NeuronovaSit extends AsyncTask<ArrayList<SensorValue>, Integer, ArrayList<SensorValue>> {
    private final int pocetHodnotFFT = 512;
    private Handler uiHandler;

    public NeuronovaSit(Handler uiHandler){
        this.uiHandler = uiHandler;
    }
    /**
     * Moznost 1. oneNote / projektovy plan / vypočet algoritmus / lin interpolace
     * @param params naměřené hodnoty
     * @return interpolované naměřené hodnoty
     */
    @Override
    protected ArrayList<SensorValue> doInBackground(ArrayList<SensorValue>... params) {
        Thread.currentThread().setName("Neuronova sit");
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
        double [] interpolatedX = new double[pocetHodnotFFT];
        double [] interpolatedY = new double[pocetHodnotFFT];
        double [] interpolatedZ = new double[pocetHodnotFFT];

        ArrayList<SensorValue> interpolatedValues = new ArrayList<SensorValue>(pocetHodnotFFT);

        long time = 0;
        int i = 0;
        long x0,x1;
        float y0X, y1X,y0Y, y1Y,y0Z, y1Z;
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
                interpolatedX[i] = y0X + (y1X - y0X)*(time - x0)/(x1 - x0);
                interpolatedY[i] = y0Y + (y1Y - y0Y)*(time - x0)/(x1 - x0);
                interpolatedZ[i] = y0Z + (y1Z - y0Z)*(time - x0)/(x1 - x0);
                interpolatedValues.add(new SensorValue(time, (float) interpolatedX[i], (float) interpolatedY[i], (float) interpolatedZ[i]));
                time += periodaVzorkovani;
                i++;
                if(i >= pocetHodnotFFT){break;}
            }
            if(i >= pocetHodnotFFT){break;}
        } while (iterator.hasNext());

        while (i < pocetHodnotFFT){
            interpolatedValues.add(new SensorValue(time, 0, 0, 0));
            time += periodaVzorkovani;
            i++;
        }

        //FastFourierTransformer fastFourierTransformer = new FastFourierTransformer(DftNormalization.STANDARD);
        // Complex [] fft = fastFourierTransformer.transform(interpolatedX, TransformType.FORWARD);


        return interpolatedValues;
    }

    @Override
    protected void onPostExecute(ArrayList<SensorValue> values){
        Message msg = uiHandler.obtainMessage(ServiceDetekce.HandlerService.KLASIFICATION_FINISHED, 1 | 9 ,1);
        uiHandler.sendMessage(msg);
    }
}