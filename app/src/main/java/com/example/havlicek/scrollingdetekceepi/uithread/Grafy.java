package com.example.havlicek.scrollingdetekceepi.uithread;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.havlicek.scrollingdetekceepi.R;
import com.example.havlicek.scrollingdetekceepi.datatypes.SensorValue;

import java.util.ArrayList;

public class Grafy extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grafy);

    }

    @Override
    protected  void onStart(){
        super.onStart();
        Intent i = getIntent();
        ArrayList<SensorValue> raw = i.getParcelableArrayListExtra("RawList");
    }

    @Override
    protected  void onResume() {
        super.onResume();
        Intent i = getIntent();
        if (i.hasExtra("RawList")){
            Toast toast = Toast.makeText(this, "Je to null", Toast.LENGTH_SHORT);
            toast.show();
        }
    }
}
