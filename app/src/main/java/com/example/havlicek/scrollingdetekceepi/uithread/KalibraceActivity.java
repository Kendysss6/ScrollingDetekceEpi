package com.example.havlicek.scrollingdetekceepi.uithread;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.example.havlicek.scrollingdetekceepi.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class KalibraceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kalibrace_layout);
    }

    public void konec(View v){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean("kalibrovano", true);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String datum = sdf.format(new Date());
        editor.putString("datum_kalibrace",datum);
        editor.commit();
        finish();
    }
}
