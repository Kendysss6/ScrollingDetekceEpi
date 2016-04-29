package com.example.havlicek.scrollingdetekceepi.threads;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.havlicek.scrollingdetekceepi.datatypes.FFTType;
import com.example.havlicek.scrollingdetekceepi.datatypes.ModusSignaluType;
import com.example.havlicek.scrollingdetekceepi.datatypes.SensorValue;
import com.example.havlicek.scrollingdetekceepi.uithread.ServiceDetekce;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.ListIterator;

/**
 * Created by Havlicek on 23.4.2016.
 */
public class FastFT extends Thread{
    private Handler serviceHandler;
    private ModusSignaluType pomvalues;

    public FastFT(ModusSignaluType values, Handler serviceHandler){
        this.serviceHandler = serviceHandler;
        this.pomvalues = values;
        this.setName("LinInterpolace");
    }

    @Override
    public void run(){
        ModusSignaluType vh = this.pomvalues;
        this.pomvalues = null;
        double [] sensorValues = vh.val;
        int pocetHodnotFFT =1024;


        // zeropad na 1024 hodnot
        double [] paddedField;
        if (sensorValues.length < pocetHodnotFFT){
            // doplnim nuly
            paddedField = new double[pocetHodnotFFT];
            for(int i = 0;i < pocetHodnotFFT; i++){
                if(i < sensorValues.length){
                    paddedField[i] = sensorValues[i];
                } else {
                    paddedField[i] = 0;
                }
            }
        } else if (sensorValues.length > pocetHodnotFFT){
            // oriznu pole
            paddedField = new double[pocetHodnotFFT];
            for(int i = 0;i < pocetHodnotFFT; i++){
                paddedField[i] = sensorValues[i];
            }
        } else {
            paddedField = sensorValues;
        }


        // FFT
        FastFourierTransformer fastFourierTransformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] fft = fastFourierTransformer.transform(paddedField, TransformType.FORWARD);

        FFTType fftVal = new FFTType(fft);

        /*for(int j = 0; j < fft.length; j++){
            Log.d("FastFT",""+fft[j].toString());
        }*/

        Message msg = serviceHandler.obtainMessage(ServiceDetekce.HandlerService.FFT_FINISHED, fftVal);
        serviceHandler.sendMessage(msg);
    }
}
