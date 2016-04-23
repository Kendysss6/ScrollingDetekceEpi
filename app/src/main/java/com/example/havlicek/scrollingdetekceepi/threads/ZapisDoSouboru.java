package com.example.havlicek.scrollingdetekceepi.threads;

import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.example.havlicek.scrollingdetekceepi.datatypes.SensorValue;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

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

    public ZapisDoSouboru(ArrayList<SensorValue> values, String idMereni, String typMereni, String sourceDir, Handler serviceHandler){
        this.idMereni = idMereni;
        this.typMereni = typMereni;
        this.sourceDir = sourceDir;
        this.serviceHandler = serviceHandler;
        this.values = values;
        this.setName("ZapisDoSouboru");
    }

    public static File getAlbumStorageDir(String dirInDownloads, String fileName) {
        // Get the directory for the user's public pictures directory.
        return new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                dirInDownloads + fileName); // m tam je proto, proto mfile musi zacinat pismenem
    }

    @Override
    public void run(){
        ArrayList<SensorValue> sensorValues = this.values;
        this.values = null; // kvuli GC
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

    public void setIndex(int index){
        this.cisloMereni = index;
    }
}
