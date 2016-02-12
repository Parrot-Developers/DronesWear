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
    public static final String OPEN_ACTIVITY_MESSAGE = "/open_app";
    private static final String ACC_PATH = "/acc";
    private static final String JOYSTICK_PATH = "/joy";
    private static final String INTERACTION_TYPE_PATH = "/it";
    private static final String ACTION_TYPE_PATH = "/at";
    private static final String ACTION_PATH = "/a";
    private static final String VALUE_STR = "value";

    private static Uri acceleroMessageUri;
    private static Uri joystickMessageUri;
    private static Uri actionMessageUri;

    public enum MESSAGE_TYPE {
        UNKNOWN,
        ACC,
        JOYSTICK,
        INTERACTION_TYPE,
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
            else if (JOYSTICK_PATH.equalsIgnoreCase(path)) {
                messageType = MESSAGE_TYPE.JOYSTICK;
            }
            else if (INTERACTION_TYPE_PATH.equalsIgnoreCase(path)) {
                messageType = MESSAGE_TYPE.INTERACTION_TYPE;
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
        PendingResult<DataApi.DeleteDataItemsResult> pendingResult = null;
        if (acceleroMessageUri != null) {
            pendingResult = Wearable.DataApi.deleteDataItems(googleApiClient, acceleroMessageUri);
        }

        return pendingResult;
    }

    public static JoystickData decodeJoystickMessage(DataItem dataItem) {
        JoystickData joystickData = null;
        if (MESSAGE_TYPE.JOYSTICK.equals(getMessageType(dataItem))) {
            DataMap dataMap = DataMap.fromByteArray(dataItem.getData());
            float joystickArray[] = dataMap.getFloatArray(VALUE_STR);
            if (joystickArray != null)
            {
                joystickData = new JoystickData(joystickArray[0], joystickArray[1]);
            }
        }

        return joystickData;
    }

    public static PendingResult<DataApi.DataItemResult> sendJoystickMessage(JoystickData joystickData, GoogleApiClient googleApiClient) {
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create(JOYSTICK_PATH);
        joystickMessageUri = dataMapRequest.getUri();
        DataMap dataMap = dataMapRequest.getDataMap();
        //Data set
        dataMap.putFloatArray(VALUE_STR, new float[]{joystickData.getPercentX(), joystickData.getPercentY()});

        // Data Push
        PutDataRequest request = dataMapRequest.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, request);

        return pendingResult;
    }

    public static PendingResult<DataApi.DeleteDataItemsResult> sendEmptyJoystickMessage(GoogleApiClient googleApiClient) {
        PendingResult<DataApi.DeleteDataItemsResult> pendingResult = null;

        if (joystickMessageUri != null) {
            pendingResult = Wearable.DataApi.deleteDataItems(googleApiClient, joystickMessageUri);
        }

        return pendingResult;
    }

    public static int decodeInteractionTypeMessage(DataItem dataItem) {
        int interactionBitfield = InteractionType.NONE;
        if (MESSAGE_TYPE.INTERACTION_TYPE.equals(getMessageType(dataItem))) {
            DataMap dataMap = DataMap.fromByteArray(dataItem.getData());
            interactionBitfield =  dataMap.getInt(VALUE_STR);
        }

        return interactionBitfield;
    }

    public static PendingResult<DataApi.DataItemResult> sendInteractionTypeMessage(int interactionBitfield, GoogleApiClient googleApiClient) {
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create(INTERACTION_TYPE_PATH);
        DataMap dataMap = dataMapRequest.getDataMap();
        //Data set
        dataMap.putInt(VALUE_STR, interactionBitfield);

        // Data Push
        PutDataRequest request = dataMapRequest.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, request);

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
        PendingResult<DataApi.DeleteDataItemsResult> pendingResult = null;
        if (actionMessageUri != null) {
            pendingResult = Wearable.DataApi.deleteDataItems(googleApiClient, actionMessageUri);
        }

        return pendingResult;
    }
}
