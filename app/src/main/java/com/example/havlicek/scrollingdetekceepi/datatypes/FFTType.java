package com.example.havlicek.scrollingdetekceepi.datatypes;

import android.os.Parcel;
import android.os.Parcelable;

import org.apache.commons.math3.complex.Complex;

/**
 * Created by Ondřej on 7. 4. 2016.
 * Třída pro přenos fft a případně výsledky klasifikaci. Ale to spíš jen pro FFT.
 */
public class FFTType implements Parcelable{
    public Complex [] fft = null;
    /**
     * Do které třídy jsme dané hodnoty zaklasifikovali
     */
    public int klasifikace = 0;

    public FFTType(Complex[] fft){
        this.fft = fft;
    }


    public FFTType(double [] real, double [] imag){
        if (real.length != imag.length){
            throw new IllegalArgumentException("Spatne velikosti");
        }
        this.fft = new Complex[real.length];
        for(int i = 0; i < real.length; i++){
            fft[i] = new Complex(real[i], imag[i]);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        double [] real = new double [fft.length];
        double [] imag = new double [fft.length];
        for (int i = 0; i < fft.length; i++){
            real [i] = fft[i].getReal();
            imag[i] = fft[i].getImaginary();
        }
        dest.writeDoubleArray(real);
        dest.writeDoubleArray(imag);
    }

    public static final FFTType.Creator<FFTType> CREATOR
            = new FFTType.Creator<FFTType>() {
        public FFTType createFromParcel(Parcel in) {
            return new FFTType(in.createDoubleArray(),in.createDoubleArray());
        }

        public FFTType[] newArray(int size) {
            return new FFTType[size];
        }
    };
}
