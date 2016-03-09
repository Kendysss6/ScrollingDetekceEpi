package com.example.havlicek.scrollingdetekceepi;

import java.text.DecimalFormat;

/**
 * Created by Ondra on 1. 5. 2015.
 */
public class SensorValue {
    private long timeStamp;
    private float[] values;
    private float fX;
    private float fY;
    private float fZ;
    private String x;
    private String y;
    private String z;

    /**
     * Kontruktor
     *
     * @param timeStamp kdy byl zaznam porizen
     * @param values    hodnoty X,Y,Z akcelerometru
     */
    public SensorValue(long timeStamp, float[] values, DecimalFormat decimalFormat) {
        this.timeStamp = timeStamp;
        fX = values[0];
        fY = values[1];
        fZ = values[2];
        x = decimalFormat.format(values[0]);
        y = decimalFormat.format(values[1]);
        z = decimalFormat.format(values[2]);
    }

    public SensorValue(long timeStamp, float[] values) {
        this.timeStamp = timeStamp;
        fX = values[0];
        fY = values[1];
        fZ = values[2];
        x = null;
        y = null;
        z = null;
    }
    public long getTimeStamp() {
        return timeStamp;
    }
    public String getX(){
        return x;
    }
    public String getY(){
        return y;
    }
    public String getZ(){
        return z;
    }

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
        return "Hodnota: "+x+" "+ y+" "+z+" "+Long.toString(timeStamp);
    }
}

