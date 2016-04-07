package com.example.havlicek.scrollingdetekceepi.asynchtasks;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.util.Log;

import com.example.havlicek.scrollingdetekceepi.SensorValue;
import com.example.havlicek.scrollingdetekceepi.uithread.ServiceDetekce;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ondřej on 19. 2. 2016.
 * Třída vytvořený pro zápis do složky downloads.
 * <p>Spouští se pouze z {@link ServiceDetekce.HandlerUI#handleMessage(Message)}</p>
 */
public class ZapisDoSouboru extends AsyncTask<List<SensorValue>, Integer, Void> {
    private String idMereni;
    private String typMereni;
    private String sourceDir;

    public ZapisDoSouboru(String idMereni, String typMereni, String sourceDir){
        this.idMereni = idMereni;
        this.typMereni = typMereni;
        this.sourceDir = sourceDir;
    }

    @Override
    protected synchronized Void doInBackground(List ... params) {
        Thread.currentThread().setName("Zapis do souboru");
        save(params);
        return null;
    }

    public static File getAlbumStorageDir(String dirInDownloads, String fileName) {
        // Get the directory for the user's public pictures directory.
        return new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                dirInDownloads + fileName);
    }

    private void save(List ... params){
        File file = getAlbumStorageDir(sourceDir, idMereni +"_"+ Build.PRODUCT + "_" +typMereni+ ".txt");
        try {
            FileOutputStream stream = new FileOutputStream(file, true);
            OutputStreamWriter writer = new OutputStreamWriter(stream, "UTF-8");
            for (List<SensorValue> sensorValues : params) {
                JSONArray list = new JSONArray();
                for (int i = 0; i < sensorValues.size(); i++) {
                    JSONArray record = new JSONArray();
                    record.put(sensorValues.get(i).getfX());
                    record.put(sensorValues.get(i).getfY());
                    record.put(sensorValues.get(i).getfZ());
                    record.put(sensorValues.get(i).getTimeStamp());
                    list.put(record);
                }

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
    }

    @Override
    protected void onPostExecute(Void values){
        Log.d("Zapis","Done "+idMereni+typMereni);
    }

}