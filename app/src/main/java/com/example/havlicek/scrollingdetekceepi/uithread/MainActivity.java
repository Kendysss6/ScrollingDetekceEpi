package com.example.havlicek.scrollingdetekceepi.uithread;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.havlicek.scrollingdetekceepi.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    /** Messenger for communicating with the service. */
    Messenger mService = null;

    /** Flag indicating whether we have called bind on the service. */
    boolean mBound = false;

    private long days = 0;
    private boolean detectionOff = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scroll_activity);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound){
            unbindService(mConnection);
            stopService(new Intent(this, ServiceDetekce.class));
        }
    }

    @Override
    protected  void onStart(){
        super.onStart();
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this); // (context == aktivita)
        boolean kalibrovano = p.getBoolean("kalibrovano", false);
        String strDate = p.getString("datum_kalibrace", "1970-01-01");
        try {
            Date datumKalibrace = new SimpleDateFormat("yyyy-MM-dd").parse(strDate);
            days = (new Date().getTime() - datumKalibrace.getTime()) / (24 * 60 * 60 * 1000);
            if (days > 10){
                kalibrovano = false;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        setGUIKalibrovano(kalibrovano);
    }

    private void setGUIKalibrovano(boolean kalibrovano){
        findViewById(R.id.but_detekce).setEnabled(kalibrovano);
        findViewById(R.id.sdilet_but).setEnabled(kalibrovano);
        findViewById(R.id.vykresli_but).setEnabled(kalibrovano);
    }

    public void onButStartDetekce(View v){
        Button b = (Button) findViewById(R.id.but_detekce);
        if (detectionOff) {
            detectionOff = false;
            b.setText(R.string.stop);
            // Bind to the service
            bindService(new Intent(this, ServiceDetekce.class), mConnection, Context.BIND_AUTO_CREATE);
        } else {
            detectionOff = true;
            b.setText(R.string.start);
            // Unbind and stop service, must explicitly stop because http://developer.android.com/guide/components/bound-services.html#Lifecycle
            unbindService(mConnection);
            stopService(new Intent(this, ServiceDetekce.class));
        }
    }

    public void kalibraceSenzotu(View v){
        Intent i = new Intent(this,KalibraceActivity.class);
        startActivity(i);
    }

    public void shit(View v){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean("kalibrovano", false);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //String datum = sdf.format(new Date());
        editor.putString("datum_kalibrace","1970-01-01");
        editor.apply();
    }





    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;
            Log.d("Boud","Bouded");

            /**
             * Notice that this example does not show how the service can respond to the client.
             * If you want the service to respond, then you need to also create a Messenger in the client.
             * Then when the client receives the onServiceConnected() callback,
             * it sends a Message to the service that includes the client's Messenger in the replyTo parameter of the send() method.
             * http://developer.android.com/guide/components/bound-services.html#Binding
             * http://developer.android.com/reference/android/os/Message.html#replyTo
             */
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };
}
