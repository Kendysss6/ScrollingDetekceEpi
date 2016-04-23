package com.example.havlicek.scrollingdetekceepi.asynchtasks;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.example.havlicek.scrollingdetekceepi.datatypes.FFTType;
import com.example.havlicek.scrollingdetekceepi.datatypes.ModusSignaluType;
import com.example.havlicek.scrollingdetekceepi.uithread.ServiceDetekce;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * Created by Ondřej on 24. 2. 2016.
 *
 * Algoritmus
 * 1. Urči sampling frequenci
 * 2. Urči ekvidistatnt spaces
 */
public class FourierTransform extends AsyncTask<ModusSignaluType, Integer, FFTType> {
    private final int pocetHodnotFFT = 1024;
    private Handler uiHandler;

    public FourierTransform(Handler uiHandler){
        this.uiHandler = uiHandler;
    }
    /**
     * Moznost 1. oneNote / projektovy plan / vypočet algoritmus / lin interpolace
     * @param params naměřené hodnoty
     * @return interpolované naměřené hodnoty
     */
    @Override
    protected FFTType doInBackground(ModusSignaluType... params) {
        Thread.currentThread().setName("FFT");
        ModusSignaluType vh = params[0];
        double [] sensorValues = vh.val;

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
        Complex [] fft = fastFourierTransformer.transform(paddedField, TransformType.FORWARD);

        FFTType fftVal = new FFTType(fft);
        return fftVal;
    }

    @Override
    protected void onPostExecute(FFTType values){
        Message msg = uiHandler.obtainMessage(ServiceDetekce.HandlerService.FFT_FINISHED, values);
        uiHandler.sendMessage(msg);
    }
}