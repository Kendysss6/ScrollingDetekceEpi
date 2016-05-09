package com.example.havlicek.scrollingdetekceepi.uithread;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.example.havlicek.scrollingdetekceepi.R;
import com.example.havlicek.scrollingdetekceepi.datatypes.SensorValue;
import com.example.havlicek.scrollingdetekceepi.asynchmereni.ThreadAsynchMereni;
import com.example.havlicek.scrollingdetekceepi.threads.LinInterpolace;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


/**
 * Kalibrace activity, ktera slouží pro zjištění hodnot offsetX, offsetY, offsetZ a estimated sampling frequenci.
 * Simuluji (a skutečně spouštím) všechny služby potřebné pro výpočty a klasifikaci, jelikož pokud budu jen sbírat data a nic nepočítat,
 * tak se perioda vzorkování zrychlí (Mě se zrychlilo zatim asi o cca asi o 0.01 sekundy).
 * Offsets použiju ve třídě {@link ThreadAsynchMereni}, které upravi hodnoty a sampling frequenci použiju v {@link LinInterpolace}, které
 * odečtou posunutí nuly senzorů daného telefonu.
 */
public class KalibraceActivity extends AppCompatActivity {

    private double estMeanX = 0;
    private double estMeanY = 0;
    private double estMeanZ = 0;

    private double odchylkaX = 0;
    private double odchylkaY = 0;
    private double odchylkaZ = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kalibrace_layout);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Kalibrace"));
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        stopService(new Intent(KalibraceActivity.this, ServiceDetekce.class));
    }

    @Override
    protected void onStart(){
        super.onStart();
        TextView t = (TextView) findViewById(R.id.dobaKalibraceNapoveda);
        t.setText(String.format("%s %d %s", "Kalibrace trvá přibližně ", 2 * ServiceDetekce.TIMER_PERIOD_MILISEC_KALIBRACE / 1000, " sekund."));
    }

    /**
     * Spusti kalibraci senzorů. Intent je poslan do metody {@link ServiceDetekce#onStartCommand(Intent, int, int)}.
     * @param v View
     */
    public void kalibrace(View v){
        EditText t = (EditText) findViewById(R.id.pass);
        String pass = t.getText().toString();
        if (!pass.equals("113366")){
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        findViewById(R.id.progressBarLayout).setVisibility(View.VISIBLE);
        Intent i = new Intent(this, ServiceDetekce.class);
        i.putExtra("Kalibrovani", true);
        i.putExtra("idMereni","KalibraceJmeno");
        i.putExtra("sourceDir", getResources().getString(R.string.app_name)+"/");
        probihaliVypoctyPriMereni = false;
        startService(i);
    }

    /**
     * Spočte skutečnou periodu vzorkovani a ostatní kalibrační konstanty, dle naměřených dat.
     */
    private void spoctiKalibraci(List<SensorValue> values){
        Mean mX = new Mean();
        Mean mY = new Mean();
        Mean mZ = new Mean();
        Mean mTime = new Mean();
        long lastTimeValue = values.get(0).getTimeStamp();
        boolean i = true;
        for (SensorValue value: values){
            // deterministicky vyradim hodnoty vzdalene od střední hodnoty vice než 3*odchylka
            if(Math.abs(estMeanX - value.getfX()) > 3*odchylkaX || Math.abs(estMeanY - value.getfY()) > 3*odchylkaY
                    || Math.abs(estMeanZ - value.getfZ()) > 3*odchylkaZ){
                Log.d("Kalibrace","skipped value");
                continue;
            }
            mX.increment(value.getfX());
            mY.increment(value.getfY());
            mZ.increment(value.getfZ());
            if (i){i=false;continue;} // vynecham prvni hodnotu
            mTime.increment(value.getTimeStamp() - lastTimeValue);
            lastTimeValue = value.getTimeStamp();
        }

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean("kalibrovano", true);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String datum = sdf.format(new Date());
        editor.putString("datum_kalibrace", datum);
        editor.putFloat("offsetX", (float) mX.getResult());
        editor.putFloat("offsetY", (float) mY.getResult());
        editor.putFloat("offsetZ", (float) mZ.getResult());
        editor.putLong("meanTimeNanosec", (long) mTime.getResult()); // vzorkovani
        editor.commit(); // ano chci aby to bylo okamžitě, protože to hned budu načítat
        Log.d("kalibrace","hodnoty ulozeny do preferences");
    }



    private boolean probihaliVypoctyPriMereni = false;

    /**
     * BroadcastReciver pro přijmutí zprávy, že bylo měření dokončeno. Zde probíhají výpočty, tj průměrování
     * naměřených hodnot.
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("kalibrace","reciver kalibrace aktivity");
            if(!probihaliVypoctyPriMereni){ // prvni mereni probíhalo bez vypoctu pro měření
                probihaliVypoctyPriMereni = true;
                //spoctiEstMeanAndVar((List) intent.getSerializableExtra("Hodnoty"));
                spoctiEstMeanAndVar((List) intent.getParcelableArrayListExtra("ArrayListRaw"));
                return;
            }
            String action = intent.getAction();
            Log.d("broadcast", action);
            //List<SensorValue> list = (List) intent.getSerializableExtra("Hodnoty");
            List<SensorValue> list = (List) intent.getParcelableArrayListExtra("ArrayListRaw");
            // vypnutí služby
            findViewById(R.id.progressBarLayout).setVisibility(View.GONE);
            stopService(new Intent(KalibraceActivity.this, ServiceDetekce.class));
            // případné výpočty
            if(list == null){
                Log.d("mesageReciver","Kalibrace, Je to null");
                return;
            }
            spoctiKalibraci(list);
            // ukonci aktivitu
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            finish();
        }
    };

    private void spoctiEstMeanAndVar(List<SensorValue> values){
        double sumX = 0, sumY = 0, sumZ = 0, time = 0;
        // stredni hodnota
        for (int i = 0; i < values.size(); i++){
            sumX += values.get(i).getfX();
            sumY += values.get(i).getfY();
            sumZ += values.get(i).getfZ();
            if(i == 0)continue;
            time += values.get(i).getTimeStamp() - values.get(i-1).getTimeStamp();
        }
        this.estMeanX = sumX / values.size();
        this.estMeanY = sumY / values.size();
        this.estMeanZ = sumZ / values.size();
        time = time / (values.size() - 1);

        // variance
        sumX = 0; sumY = 0; sumZ = 0;
        for (SensorValue value: values){
            // rucne spoctu prumer
            sumX += (value.getfX() - estMeanX)*(value.getfX() - estMeanX);
            sumY += (value.getfY() - estMeanY)*(value.getfY() - estMeanY);
            sumZ += (value.getfZ() - estMeanZ)*(value.getfZ() - estMeanZ);
        }
        this.odchylkaX = Math.sqrt(sumX / (values.size() - 1));
        this.odchylkaY = Math.sqrt(sumY / (values.size() - 1));
        this.odchylkaZ = Math.sqrt(sumZ / (values.size() - 1));

        Log.d("Kalibrace", "X: " + estMeanX + " " + odchylkaX);
        Log.d("Kalibrace", "Y: " + estMeanY + " " + odchylkaY);
        Log.d("Kalibrace", "Z: " + estMeanZ + " " + odchylkaZ);
        Log.d("Kalibrace", "time: "+time);
    }


}
