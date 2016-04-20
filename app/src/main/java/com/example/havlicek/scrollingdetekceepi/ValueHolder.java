package com.example.havlicek.scrollingdetekceepi;

import android.os.Parcel;
import android.os.Parcelable;

import org.apache.commons.math3.complex.Complex;

import java.util.ArrayList;

/**
 * Created by Ondřej on 7. 4. 2016.
 */
public class ValueHolder {
    public ArrayList<SensorValue> rawData = null;
    public ArrayList<SensorValue> linData = null;
    public Complex [] fft = null;
    //public long [] time = null;
    public double [] signalModus = null;
    public double signalEnergy = 0;
    /**
     * Do které třídy jsme dané hodnoty zaklasifikovali
     */
    public int klasifikace = 0;

}
