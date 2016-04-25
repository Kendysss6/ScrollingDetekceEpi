package com.example.havlicek.scrollingdetekceepi.uithread;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.havlicek.scrollingdetekceepi.CustomScrollView;
import com.example.havlicek.scrollingdetekceepi.R;
import com.example.havlicek.scrollingdetekceepi.datatypes.FFTType;
import com.example.havlicek.scrollingdetekceepi.datatypes.SensorValue;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.util.ArrayList;
import java.util.Vector;

public class Grafy extends Activity {
    DataPoint [] X = null;
    DataPoint [] Y = null;
    DataPoint [] Z = null;
    private ArrayList<SensorValue> values;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grafy);
        Intent i = getIntent();
        ArrayList<SensorValue> raw = i.getParcelableArrayListExtra("List");
        if(raw == null){
            Log.d("Grafy","null");
            return;
        }
        convertArray(raw);
        LineGraphSeries<DataPoint> seriesX = new LineGraphSeries<DataPoint>(X);
        LineGraphSeries<DataPoint> seriesY = new LineGraphSeries<DataPoint>(Y);
        LineGraphSeries<DataPoint> seriesZ = new LineGraphSeries<DataPoint>(Z);

        GraphView graphX = (GraphView)findViewById(R.id.graph_X);
        GraphView graphY = (GraphView)findViewById(R.id.graph_Y);
        GraphView graphZ = (GraphView)findViewById(R.id.graph_Z);


        graphX.removeAllSeries();
        graphY.removeAllSeries();
        graphZ.removeAllSeries();

        graphX.addSeries(seriesX);
        graphX.getViewport().setXAxisBoundsManual(true);
        //graphX.getViewport().setScalable(true);
        //graphX.getViewport().setScrollable(true);
        graphX.getViewport().setMinX(0);
        graphX.getViewport().setMaxX(10);
        graphY.addSeries(seriesY);
        graphY.getViewport().setXAxisBoundsManual(true);
        //graphY.getViewport().setScalable(true);
        //graphY.getViewport().setScrollable(true);
        graphZ.addSeries(seriesZ);
        graphZ.getViewport().setXAxisBoundsManual(true);
        //graphZ.getViewport().setScalable(true);
        //graphZ.getViewport().setScrollable(true);
        zarovnatGrafy(null);
    }

    @Override
    protected  void onStart(){
        super.onStart();
    }

    @Override
    protected  void onResume() {
        super.onResume();
    }

    /**
     * @param values trololo
     * @return null
     */
    private void convertArray(ArrayList<SensorValue> values){
        X = new DataPoint[values.size()];
        Y = new DataPoint[values.size()];
        Z = new DataPoint[values.size()];
        long iTime = values.get(0).getTimeStamp();
        SensorValue v;
        for (int i = 0; i < values.size(); i++) {
            v = values.get(i);
            if((v.getTimeStamp() - iTime)/1e9 >= 10)break;
            X[i] = new DataPoint((v.getTimeStamp() - iTime)/1e9, v.getfX());
            Y[i] = new DataPoint((v.getTimeStamp() - iTime)/1e9, v.getfY());
            Z[i] = new DataPoint((v.getTimeStamp() - iTime)/1e9, v.getfZ());
        }
    }

    public void stopScroling(View v){
        CustomScrollView view = (CustomScrollView) findViewById(R.id.myScroll);
        view.setEnableScrolling(false);
    }
    public void zapnoutScroling(View v){
        CustomScrollView view = (CustomScrollView) findViewById(R.id.myScroll);
        view.setEnableScrolling(true);
    }
    public void zarovnatGrafy(View v){
        double i = X[X.length-1].getX();
        ((GraphView)findViewById(R.id.graph_X)).getViewport().setMaxX(i);
        ((GraphView)findViewById(R.id.graph_X)).getViewport().setMinX(0);
        ((GraphView)findViewById(R.id.graph_Y)).getViewport().setMaxX(i);
        ((GraphView)findViewById(R.id.graph_Y)).getViewport().setMinX(0);
        ((GraphView)findViewById(R.id.graph_Z)).getViewport().setMaxX(i);
        ((GraphView)findViewById(R.id.graph_Z)).getViewport().setMinX(0);
         findViewById(R.id.myScroll).refreshDrawableState();
    }
}
