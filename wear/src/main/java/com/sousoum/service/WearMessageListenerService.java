package com.sousoum.service;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.sousoum.droneswear.MainActivity;
import com.sousoum.shared.Message;

/**
 * Created by d.bertrand on 12/02/16.
 */
public class WearMessageListenerService extends WearableListenerService{

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        if(messageEvent.getPath().equals(Message.OPEN_ACTIVITY_MESSAGE)){
            Intent intent = new Intent(this , MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
