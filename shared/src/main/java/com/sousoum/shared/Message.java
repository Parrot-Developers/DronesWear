package com.sousoum.shared;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by d.bertrand on 06/01/16.
 */
public class Message
{
    private static final String ACC_PATH = "/acc";
    private static final String ACTION_TYPE_PATH = "/at";
    private static final String ACTION_PATH = "/a";

    public enum MESSAGE_TYPE {
        ACC,
        ACTION_TYPE,
        ACTION
    }

    public static final int ACTION_TYPE_NONE = 0;
    public static final int ACTION_TYPE_JUMP = 1;
    public static final int ACTION_TYPE_TAKE_OFF = 2;
    public static final int ACTION_TYPE_LAND = 3;


    public static MESSAGE_TYPE getMessageType(MessageEvent messageEvent){
        MESSAGE_TYPE messageType = MESSAGE_TYPE.ACC;
        if (messageEvent != null)
        {
            String path = messageEvent.getPath();
            if (ACC_PATH.equalsIgnoreCase(path)) {
                messageType = MESSAGE_TYPE.ACC;
            }
            else if (ACTION_TYPE_PATH.equalsIgnoreCase(path)) {
                messageType = MESSAGE_TYPE.ACTION_TYPE;
            }
            else if (ACTION_PATH.equalsIgnoreCase(path)) {
                messageType = MESSAGE_TYPE.ACTION;
            }

        }
        return messageType;
    }

    public static AccelerometerData decodeAcceleroMessage(MessageEvent messageEvent) {
        AccelerometerData accelerometerData = null;
        if (MESSAGE_TYPE.ACC.equals(getMessageType(messageEvent))) {
            byte[] buffer = messageEvent.getData();
            FloatBuffer floatBuffer = ByteBuffer.wrap(buffer).asFloatBuffer();

            accelerometerData = new AccelerometerData(
                    floatBuffer.get(0), floatBuffer.get(1), floatBuffer.get(2));
        }

        return accelerometerData;
    }

    public static ArrayList<PendingResult<MessageApi.SendMessageResult>> sendAcceleroMessage(AccelerometerData accelerometerData, List<Node> nodes, GoogleApiClient googleApiClient) {
        ArrayList<PendingResult<MessageApi.SendMessageResult>> results = new ArrayList<>();

        ByteBuffer bufferObj = ByteBuffer.allocate(4 * 3);
        bufferObj.putFloat(accelerometerData.getAccX());
        bufferObj.putFloat(accelerometerData.getAccY());
        bufferObj.putFloat(accelerometerData.getAccZ());
        byte[] buffer = bufferObj.array();


        for(Node node : nodes) {
            PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                    googleApiClient, node.getId(), ACC_PATH, buffer);

            results.add(result);
        }

        return results;
    }

    public static int decodeActionTypeMessage(MessageEvent messageEvent) {
        int productType = -1;
        if (MESSAGE_TYPE.ACTION_TYPE.equals(getMessageType(messageEvent))) {
            byte[] buffer = messageEvent.getData();
            Log.e("TAG", "buffer = " + buffer[0]);
            productType = buffer[0];
        }

        return productType;
    }

    public static ArrayList<PendingResult<MessageApi.SendMessageResult>> sendActionTypeMessage(int productType, List<Node> nodes, GoogleApiClient googleApiClient) {
        ArrayList<PendingResult<MessageApi.SendMessageResult>> results = new ArrayList<>();
        byte[] buffer = new byte[1];
        buffer[0] = (byte)productType;


        for(Node node : nodes) {
            PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                    googleApiClient, node.getId(), ACTION_TYPE_PATH, buffer);

            results.add(result);
        }

        return results;
    }

    public static ArrayList<PendingResult<MessageApi.SendMessageResult>> sendActionMessage(List<Node> nodes, GoogleApiClient googleApiClient) {
        ArrayList<PendingResult<MessageApi.SendMessageResult>> results = new ArrayList<>();

        for(Node node : nodes) {
            PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                    googleApiClient, node.getId(), ACTION_PATH, null);

            results.add(result);
        }

        return results;
    }
}
