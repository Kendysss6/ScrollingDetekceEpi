package com.example.havlicek.scrollingdetekceepi.uithread;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.havlicek.scrollingdetekceepi.R;
import com.example.havlicek.scrollingdetekceepi.datatypes.FFTType;
import com.example.havlicek.scrollingdetekceepi.datatypes.ModusSignaluType;
import com.example.havlicek.scrollingdetekceepi.datatypes.SensorValue;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import org.apache.commons.math3.complex.Complex;
import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Vector;

public class GrafActivity extends Activity {
    DataPoint [] X = null;
    DataPoint [] Y = null;
    DataPoint [] Z = null;
    private ArrayList<SensorValue> values;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grafy);
        Intent i = getIntent();
        if (i.hasExtra("FFT")){
            FFTType t = i.getParcelableExtra("FFT");
            if(t == null)return;
            convertFFT(t);
        } else {
            ArrayList<SensorValue> raw = i.getParcelableArrayListExtra("List");
            if(raw == null){
                Log.d("GrafActivity","null");
                return;
            }
            convertArray(raw);
        }

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
        //graphX.getViewport().setMinX(0);
        //graphX.getViewport().setMaxX(10);
        graphY.addSeries(seriesY);
        graphY.getViewport().setXAxisBoundsManual(true);
        //graphY.getViewport().setScalable(true);
        //graphY.getViewport().setScrollable(true);
        graphZ.addSeries(seriesZ);
        graphZ.getViewport().setXAxisBoundsManual(true);
        //graphZ.getViewport().setScalable(true);
        //graphZ.getViewport().setScrollable(true);
        zarovnatGrafy(null);
        setPopiskyGrafu(i.getAction());
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

    private void convertFFT(FFTType val){
        Complex [] values = val.fft;
        ModusSignaluType m = getIntent().getParcelableExtra("Modus");
        long [] time = m.time;
        double [] modVal = m.val;

        ModusSignaluType m2 = getIntent().getParcelableExtra("FModus");
        long [] time2 = m2.time;
        double [] modVal2 = m2.val;

        X = new DataPoint[values.length/2];
        Y = new DataPoint[time.length];
        Z = new DataPoint[time2.length];

        double fs = 100;
        int N = 1024; // == values.size();
        // vynuluju DC slozku
        //values[0] = new Complex(0,0);
        for(int i = 0; i < X.length; i++){
            X[i] = new DataPoint((i*fs)/N, values[i].abs()/values.length);
            //Log.d("GrafActivity",((double)(i*fs)/N)+" "+values[i].abs());
        }

        // modus
        long it = time[0];
        for(int i = 0; i < time.length; i++){
            Y[i] = new DataPoint((time[i]-it)*1e-9, modVal[i]);
            //Log.d("GrafActivity",((time[i]-it)*1e-9)+" "+modVal[i]);

        }

        // modus filtered
        long it2 = time2[0];
        for(int i = 0; i < time.length; i++){
            Z[i] = new DataPoint((time2[i]-it2)*1e-9, modVal2[i]);
            //Log.d("GrafActivity",((time[i]-it)*1e-9)+" "+modVal[i]);

        }

    }
    public void zarovnatGrafy(View v){
        double i = Math.ceil(X[X.length - 1].getX());
        ((GraphView)findViewById(R.id.graph_X)).getViewport().setMaxX(i);
        ((GraphView)findViewById(R.id.graph_X)).getViewport().setMinX(0);
        i = Math.ceil(Y[Y.length-1].getX());
        ((GraphView)findViewById(R.id.graph_Y)).getViewport().setMaxX(i);
        ((GraphView)findViewById(R.id.graph_Y)).getViewport().setMinX(0);
        i = Math.ceil(Z[Z.length-1].getX());
        ((GraphView)findViewById(R.id.graph_Z)).getViewport().setMaxX(i);
        ((GraphView)findViewById(R.id.graph_Z)).getViewport().setMinX(0);
         findViewById(R.id.myScroll).refreshDrawableState();
    }

    private void setPopiskyGrafu(String action){
        TextView t;
        if (action.equals("raw") || action.equals("lin")){
            t = (TextView) findViewById(R.id.Osa_X);
            t.setText("Osa X");
            t = (TextView) findViewById(R.id.Osa_Y);
            t.setText("Osa Y");
            t = (TextView) findViewById(R.id.Osa_Z);
            t.setText("Osa Z");
        }  else if (action.equals("fft")){
            t = (TextView) findViewById(R.id.Osa_X);
            t.setText("Jednostraná Fourierova transformace");
            t = (TextView) findViewById(R.id.Osa_Y);
            t.setText("Nefiltrovaný signál");
            t = (TextView) findViewById(R.id.Osa_Z);
            t.setText("Signál filtrovaný vysokou propustí");
        }
    }
}
