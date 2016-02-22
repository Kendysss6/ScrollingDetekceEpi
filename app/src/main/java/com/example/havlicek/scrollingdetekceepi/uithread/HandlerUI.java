package com.example.havlicek.scrollingdetekceepi.uithread;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by Ondřej on 21. 2. 2016.
 */
public class HandlerUI extends Handler{

    public HandlerUI(){
        super(Looper.getMainLooper());
    }

    @Override
    public void handleMessage(Message msg){

    }
}
