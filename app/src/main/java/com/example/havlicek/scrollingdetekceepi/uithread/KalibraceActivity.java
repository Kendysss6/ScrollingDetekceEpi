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
import android.widget.TextView;

import com.example.havlicek.scrollingdetekceepi.R;
import com.example.havlicek.scrollingdetekceepi.SensorValue;
import com.example.havlicek.scrollingdetekceepi.asynchmereni.ThreadAsynchMereni;
import com.example.havlicek.scrollingdetekceepi.asynchtasks.LinInterpolace;
import com.example.havlicek.scrollingdetekceepi.asynchtasks.ZapisDoSouboru;

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
        t.setText(String.format("%s %d %s","Kalibrace trvá přibližně ", 2 * ServiceDetekce.TIMER_PERIOD_MILISEC / 1000," sekund."));
    }

    /**
     * Spusti kalibraci senzorů. Intent je poslan do metody {@link ServiceDetekce#onStartCommand(Intent, int, int)}.
     * @param v View
     */
    public void kalibrace(View v){
        findViewById(R.id.progressBarLayout).setVisibility(View.VISIBLE);
        Intent i = new Intent(this, ServiceDetekce.class);
        i.putExtra("Kalibrovani", true);
        i.putExtra("idMereni","KalibraceJmeno");
        startService(i);
    }

    /**
     * Spočte skutečnou periodu vzorkovani a ostatní kalibrační konstanty, dle naměřených dat.
     */
    private void spoctiKalibraciAndZapis(List<SensorValue> values){
        // zapis namerenych hodnot pri kalibraci
        ZapisDoSouboru zapis = new ZapisDoSouboru("Kalibrace");
        zapis.execute(values);
        // vypocet konstant
        Mean mX = new Mean();
        Mean mY = new Mean();
        Mean mZ = new Mean();
        Mean time = new Mean();
        long lastTimeValue = values.get(0).getTimeStamp();
        boolean i = true;
        double perioda_sum = 0;
        for (SensorValue value: values){
            mX.increment(value.getfX());
            mY.increment(value.getfY());
            mZ.increment(value.getfZ());
            if (i){i=false;continue;} // vynecham prvni hodnotu
            time.increment(value.getTimeStamp() - lastTimeValue);
            perioda_sum += value.getTimeStamp() - lastTimeValue;
            lastTimeValue = value.getTimeStamp();
        }
        double meanTsSec = time.getResult() * 1e-9;
        double meanTsSec2 = perioda_sum / (values.size() - 1);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean("kalibrovano", true);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String datum = sdf.format(new Date());
        editor.putString("datum_kalibrace", datum);
        editor.putFloat("offsetX", (float) mX.getResult());
        editor.putFloat("offsetY", (float) mY.getResult());
        editor.putFloat("offsetZ", (float) mZ.getResult());
        editor.putFloat("meanTimeNanosec", (float) meanTsSec2);
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
                return;
            }
            String action = intent.getAction();
            Log.d("broadcast", action);
            List<SensorValue> list = (List) intent.getSerializableExtra("Hodnoty");
            // vypnutí služby
            findViewById(R.id.progressBarLayout).setVisibility(View.GONE);
            stopService(new Intent(KalibraceActivity.this, ServiceDetekce.class));
            // případné výpočty
            if(list == null){
                Log.d("mesageReciver","Je to null");
                return;
            }
            spoctiKalibraciAndZapis(list);
            // ukonci aktivitu
            finish();
        }
    };
}
