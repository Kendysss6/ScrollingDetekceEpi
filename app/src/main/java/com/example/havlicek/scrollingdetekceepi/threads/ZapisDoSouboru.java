package com.example.havlicek.scrollingdetekceepi.threads;

import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.example.havlicek.scrollingdetekceepi.datatypes.FFTType;
import com.example.havlicek.scrollingdetekceepi.datatypes.ModusSignaluType;
import com.example.havlicek.scrollingdetekceepi.datatypes.SensorValue;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.complex.ComplexFormat;
import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Havlicek on 23.4.2016.
 * Třída pro zápis do souboru
 */
public class ZapisDoSouboru extends Thread{
    Handler serviceHandler;
    private String idMereni;
    private String typMereni;
    private String sourceDir;
    private int cisloMereni = -1;
    ArrayList<SensorValue> values;
    FFTType fft;
    ModusSignaluType mod;
    private int typMereniInt;

    private final int RAW = 1;
    private final int LIN = 2;
    private final int MOD = 3;
    private final int FFT = 4;

    public ZapisDoSouboru(ArrayList<SensorValue> values, String idMereni, String typMereni, String sourceDir, Handler serviceHandler){
        this.idMereni = idMereni;
        this.typMereni = typMereni;
        this.sourceDir = sourceDir;
        this.serviceHandler = serviceHandler;
        this.values = values;
        this.setName("ZapisDoSouboru");
        if (typMereni.equals("raw")){
            typMereniInt = RAW;
        } else {
            typMereniInt = LIN;
        }
    }

    public ZapisDoSouboru(FFTType type, String idMereni, String typMereni, String sourceDir, Handler serviceHandler){
        this.idMereni = idMereni;
        this.typMereni = typMereni;
        this.sourceDir = sourceDir;
        this.serviceHandler = serviceHandler;
        this.fft = type;
        this.setName("ZapisDoSouboru");
        typMereniInt = FFT;
    }

    public ZapisDoSouboru(ModusSignaluType type, String idMereni, String typMereni, String sourceDir, Handler serviceHandler){
        this.idMereni = idMereni;
        this.typMereni = typMereni;
        this.sourceDir = sourceDir;
        this.serviceHandler = serviceHandler;
        this.mod = type;
        this.setName("ZapisDoSouboru");
        typMereniInt = MOD;
    }

    public static File getAlbumStorageDir(String dirInDownloads, String fileName) {
        // Get the directory for the user's public pictures directory.
        return new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                dirInDownloads + fileName); // m tam je proto, proto mfile musi zacinat pismenem
    }

    @Override
    public void run(){
        switch (typMereniInt){
            case RAW:
                zapisCasData(values);
                values = null;
                break;
            case LIN:
                zapisCasData(values);
                values = null;
                break;
            case MOD:
                zapisModus(mod);
                break;
            case FFT:
                zapisFFT(fft);
                fft = null;
                break;
        }
    }

    private void zapisCasData(ArrayList<SensorValue> sensorValues){
        File file = getAlbumStorageDir(sourceDir, "m"+ idMereni +"_"+ Build.PRODUCT + "_" +typMereni+ ".txt");
        try {
            FileOutputStream stream = new FileOutputStream(file, true);
            OutputStreamWriter writer = new OutputStreamWriter(stream, "UTF-8");
            JSONArray list = new JSONArray();
            for (int i = 0; i < sensorValues.size(); i++) {
                JSONArray record = new JSONArray();
                record.put(sensorValues.get(i).getfX());
                record.put(sensorValues.get(i).getfY());
                record.put(sensorValues.get(i).getfZ());
                record.put(sensorValues.get(i).getTimeStamp());
                list.put(record);
                if(isInterrupted()){
                    break;
                }
            }
            /* if(index != -1){
                 writer.write("data"+cisloMereni+" = ");
            } else {
                    writer.write("data = ");
                }*/
            if(!isInterrupted()){
                writer.write("data = ");
                //String jsonZapis = list.toString(4).replaceAll("],", "];");
                //Log.d("json", jsonZapis);
                //Log.d("jsonFloat",listFloats.toString(4).replaceAll("],", "];"));
                writer.write(list.toString().replaceAll("],", "];"));
                writer.write(";\n");
            }
            writer.close();
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(isInterrupted()){
            Log.d("Zapis", "Interupted " + idMereni + typMereni);
        } else {
            Log.d("Zapis", "Done " + idMereni + typMereni);
        }
    }

    private void zapisFFT(FFTType type){
        File file = getAlbumStorageDir(sourceDir, "m"+ idMereni +"_"+ Build.PRODUCT + "_" +typMereni+ ".txt");
        Complex [] fft = type.fft;
        try {
            FileOutputStream stream = new FileOutputStream(file, true);
            OutputStreamWriter writer = new OutputStreamWriter(stream, "UTF-8");
            JSONArray list = new JSONArray();
            NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
            ComplexFormat format = new ComplexFormat(nf);
            for (int i = 0; i < fft.length; i++) {
                JSONArray record = new JSONArray();
                record.put(format.format(fft[i]));
                list.put(format.format(fft[i]));
                if(isInterrupted()){
                    break;
                }
            }
            /* if(index != -1){
                 writer.write("data"+cisloMereni+" = ");
            } else {
                    writer.write("data = ");
                }*/
            if(!isInterrupted()){
                writer.write("data = ");
                //String jsonZapis = list.toString(4).replaceAll("],", "];");
                //Log.d("json", jsonZapis);
                //Log.d("jsonFloat",listFloats.toString(4).replaceAll("],", "];"));
                StringBuilder builder = new StringBuilder();
                writer.write(list.toString().replaceAll("],", "];").replaceAll("\"", "").replace(',', ';'));

                writer.write(";\n");
            }
            writer.close();
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(isInterrupted()){
            Log.d("Zapis", "Interupted " + idMereni + typMereni);
        } else {
            Log.d("Zapis", "Done " + idMereni + typMereni);
        }
    }

    private void zapisModus(ModusSignaluType m){
        File file = getAlbumStorageDir(sourceDir, "m"+ idMereni +"_"+ Build.PRODUCT + "_" +typMereni+ ".txt");
        double [] x = m.val;
        long [] time = m.time;
        try {
            FileOutputStream stream = new FileOutputStream(file, true);
            OutputStreamWriter writer = new OutputStreamWriter(stream, "UTF-8");
            JSONArray list = new JSONArray();
            for (int i = 0; i < x.length; i++) {
                JSONArray record = new JSONArray();
                record.put(time[i]);
                record.put(x[i]);
                list.put(record);
                if(isInterrupted()){
                    break;
                }
            }
            /* if(index != -1){
                 writer.write("data"+cisloMereni+" = ");
            } else {
                    writer.write("data = ");
                }*/
            if(!isInterrupted()){
                writer.write("data = ");
                //String jsonZapis = list.toString(4).replaceAll("],", "];");
                //Log.d("json", jsonZapis);
                //Log.d("jsonFloat",listFloats.toString(4).replaceAll("],", "];"));
                StringBuilder builder = new StringBuilder();
                writer.write(list.toString().replaceAll("],", "];"));

                writer.write(";\n");
            }
            writer.close();
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(isInterrupted()){
            Log.d("Zapis", "Interupted " + idMereni + typMereni);
        } else {
            Log.d("Zapis", "Done " + idMereni + typMereni);
        }
    }

    public void setIndex(int index){
        this.cisloMereni = index;
    }
}
