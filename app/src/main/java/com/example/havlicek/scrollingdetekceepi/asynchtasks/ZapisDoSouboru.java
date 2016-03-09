package com.example.havlicek.scrollingdetekceepi.asynchtasks;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.example.havlicek.scrollingdetekceepi.SensorValue;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Ondřej on 19. 2. 2016.
 */
public class ZapisDoSouboru extends AsyncTask<List<SensorValue>, Integer, Void> {
    private String idMereni;

    public ZapisDoSouboru(String idMereni){
        this.idMereni = idMereni;
    }

    @Override
    protected synchronized Void doInBackground(List ... params) {
        save(params);
        Log.d("zapis","done");
        return null;
    }

    public static File getAlbumStorageDir(String fileName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName);
        return file;
    }

    private void save(List ... params){
        File file = getAlbumStorageDir(Build.PRODUCT + "_" + idMereni + ".txt");
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
                String jsonZapis = list.toString(4).replaceAll("],", "];");
                Log.d("json", jsonZapis);
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

}