package com.example.havlicek.scrollingdetekceepi.datatypes;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.DecimalFormat;

/**
 * Created by Ondra on 1. 5. 2015.
 * SensorValue, reprezentuje hodnotu naměřenou ze senzoru.
 */
public class SensorValue implements Parcelable {
    private long timeStamp;
    private float fX;
    private float fY;
    private float fZ;


    public SensorValue(long timeStamp, float fX, float fY, float fZ, DecimalFormat decimalFormat) {
        this.timeStamp = timeStamp;
        this.fX = fX;
        this.fY = fY;
        this.fZ = fZ;
    }

    public SensorValue(long timeStamp, float fX, float fY, float fZ) {
        this.timeStamp = timeStamp;
        this.fX = fX;
        this.fY = fY;
        this.fZ = fZ;
    }

    /**
     * Timestamp in nanosecond
     * @return timestamp in nanosecond
     */
    public long getTimeStamp() {
        return timeStamp;
    }
    /*
    public String getX(){
        return x;
    }
    public String getY(){
        return y;
    }
    public String getZ(){
        return z;
    }
    */

    public float getfX() {
        return fX;
    }
    public float getfY() {
        return fY;
    }
    public float getfZ() {
        return fZ;
    }

    @Override
    public String toString() {
        return "Hodnota: "+fX+" "+ fY+" "+fZ+" "+Long.toString(timeStamp);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(timeStamp);
        dest.writeFloat(fX);
        dest.writeFloat(fY);
        dest.writeFloat(fZ);
    }

    public static final Parcelable.Creator<SensorValue> CREATOR
            = new Parcelable.Creator<SensorValue>() {
        public SensorValue createFromParcel(Parcel in) {
            return new SensorValue(in.readLong(),in.readFloat(), in.readFloat(), in.readFloat(),null);
        }

        public SensorValue[] newArray(int size) {
            return new SensorValue[size];
        }
    };
}

