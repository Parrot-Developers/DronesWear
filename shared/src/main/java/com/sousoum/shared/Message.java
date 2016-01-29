package com.sousoum.shared;

import android.net.Uri;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by d.bertrand on 06/01/16.
 */
public class Message
{
    private static final String ACC_PATH = "/acc";
    private static final String ACTION_TYPE_PATH = "/at";
    private static final String ACTION_PATH = "/a";
    private static final String VALUE_STR = "value";

    private static Uri acceleroMessageUri;
    private static Uri actionMessageUri;

    public enum MESSAGE_TYPE {
        UNKNOWN,
        ACC,
        ACTION_TYPE,
        ACTION
    }

    public static MESSAGE_TYPE getMessageType(DataItem dataItem){
        MESSAGE_TYPE messageType = MESSAGE_TYPE.UNKNOWN;
        if (dataItem != null)
        {
            String path = dataItem.getUri().getPath();
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

    public static AccelerometerData decodeAcceleroMessage(DataItem dataItem) {
        AccelerometerData accelerometerData = null;
        if (MESSAGE_TYPE.ACC.equals(getMessageType(dataItem))) {
            DataMap dataMap = DataMap.fromByteArray(dataItem.getData());
            float sensordataArray[] = dataMap.getFloatArray(VALUE_STR);
            if (sensordataArray != null)
            {
                accelerometerData = new AccelerometerData(sensordataArray[0], sensordataArray[1], sensordataArray[2]);
            }
        }

        return accelerometerData;
    }

    public static PendingResult<DataApi.DataItemResult> sendAcceleroMessage(AccelerometerData accelerometerData, GoogleApiClient googleApiClient) {
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create(ACC_PATH);
        acceleroMessageUri = dataMapRequest.getUri();
        DataMap dataMap = dataMapRequest.getDataMap();
        //Data set
        dataMap.putFloatArray(VALUE_STR, new float[]{accelerometerData.getAccX(), accelerometerData.getAccY(), accelerometerData.getAccZ()});

        // Data Push
        PutDataRequest request = dataMapRequest.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, request);

        return pendingResult;
    }

    public static PendingResult<DataApi.DeleteDataItemsResult> sendEmptyAcceleroMessage(GoogleApiClient googleApiClient) {
        PendingResult<DataApi.DeleteDataItemsResult> pendingResult = Wearable.DataApi.deleteDataItems(googleApiClient, acceleroMessageUri);


        return pendingResult;
    }

    public static int decodeActionTypeMessage(DataItem dataItem) {
        int actionType = -1;
        if (MESSAGE_TYPE.ACTION_TYPE.equals(getMessageType(dataItem))) {
            DataMap dataMap = DataMap.fromByteArray(dataItem.getData());
            actionType =  dataMap.getInt(VALUE_STR);
        }

        return actionType;
    }

    public static PendingResult<DataApi.DataItemResult> sendActionTypeMessage(int actionType, GoogleApiClient googleApiClient) {
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create(ACTION_TYPE_PATH);
        DataMap dataMap = dataMapRequest.getDataMap();
        //Data set
        dataMap.putInt(VALUE_STR, actionType);

        // Data Push
        PutDataRequest request = dataMapRequest.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, request);

        return pendingResult;
    }

    public static PendingResult<DataApi.DataItemResult> sendActionMessage(GoogleApiClient googleApiClient) {
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create(ACTION_PATH);
        actionMessageUri = dataMapRequest.getUri();

        // Data Push
        PutDataRequest request = dataMapRequest.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, request);

        return pendingResult;
    }

    public static PendingResult<DataApi.DeleteDataItemsResult> emptyActionMessage(GoogleApiClient googleApiClient) {
        PendingResult<DataApi.DeleteDataItemsResult> pendingResult = Wearable.DataApi.deleteDataItems(googleApiClient, actionMessageUri);


        return pendingResult;
    }
}
