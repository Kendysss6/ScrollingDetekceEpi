package com.example.havlicek.scrollingdetekceepi.datatypes;

import android.os.Parcel;
import android.os.Parcelable;

import org.apache.commons.math3.complex.Complex;

/**
 * Created by Havlicek on 23.4.2016.
 * Nosic pro modus signalu
 */
public class ModusSignaluType implements Parcelable {
    /**
     * poue pro time analysis
     */
    public double [] modus;
    /**
     * Skutecny hodnoty se kterymi pracuji
     */
    public double [] val;
    /**
     * Cas v nano sekundách
     */
    public long [] time;

    /**
     *
     * @param val
     * @param time cas v nansekundach
     */
    public ModusSignaluType(double [] val, long [] time, double [] modus){
        this.time = time;
        this.val = val;
        this.modus = modus;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDoubleArray(val);
        dest.writeLongArray(time);
        dest.writeDoubleArray(modus);
    }

    public static final ModusSignaluType.Creator<ModusSignaluType> CREATOR
            = new ModusSignaluType.Creator<ModusSignaluType>() {
        public ModusSignaluType createFromParcel(Parcel in) {
            return new ModusSignaluType(in.createDoubleArray(),in.createLongArray(), in.createDoubleArray());
        }

        public ModusSignaluType[] newArray(int size) {
            return new ModusSignaluType[size];
        }
    };
}
