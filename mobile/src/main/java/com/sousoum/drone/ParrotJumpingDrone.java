package com.sousoum.drone;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.parrot.arsdk.arcommands.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_JUMPINGSUMO_NETWORK_WIFISCAN_BAND_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARFeatureJumpingSumo;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.sousoum.shared.AccelerometerData;
import com.sousoum.shared.ActionType;

/**
 * Created by d.bertrand on 15/01/16.
 */
public class ParrotJumpingDrone extends ParrotDrone {
    private static final String TAG = "ParrotJumpingDrone";

    public ParrotJumpingDrone(@NonNull ARDiscoveryDeviceService deviceService, Context ctx) {
        super(deviceService, ctx);
    }

    @Override
    public void pilotWithAcceleroData(AccelerometerData accelerometerData) {
        if (mDeviceController != null) {
            byte speedVal = (byte) -(Math.max(-100, Math.min(100, ((accelerometerData.getAccY() / 9.0) * 50))));
            byte turnVal = (byte) -(Math.max(-100, Math.min(100, ((accelerometerData.getAccX() / 9.0) * 50))));
            Log.i(TAG, "speed = " + speedVal + " | turn = " + turnVal);
            mDeviceController.getFeatureJumpingSumo().setPilotingPCMD((byte) 1, speedVal, turnVal);
        }
    }

    @Override
    public void stopPiloting() {
        if (mDeviceController != null) {
            mDeviceController.getFeatureJumpingSumo().setPilotingPCMD((byte) 0, (byte) 0, (byte) 0);
        }
    }

    @Override
    public void sendAction() {
        if (mDeviceController != null) {
            mDeviceController.getFeatureJumpingSumo().sendAnimationsJump(ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_ENUM.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_HIGH);
        }
    }

    @Override
    public void onStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error) {
        super.onStateChanged(deviceController, newState, error);

        if (newState == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setCurrentAction(ActionType.ACTION_TYPE_JUMP);
                }
            });
        }
    }

    @Override
    public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary) {
        super.onCommandReceived(deviceController, commandKey, elementDictionary);

        if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_JUMPINGSUMO_NETWORKSETTINGSSTATE_WIFISELECTIONCHANGED) && (elementDictionary != null)) {
            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (args != null) {
                final int band;
                boolean isDetermined = false;
                ARCOMMANDS_JUMPINGSUMO_NETWORK_WIFISCAN_BAND_ENUM bandEnum = ARCOMMANDS_JUMPINGSUMO_NETWORK_WIFISCAN_BAND_ENUM.getFromValue((Integer) args.get(ARFeatureJumpingSumo.ARCONTROLLER_DICTIONARY_KEY_JUMPINGSUMO_NETWORKSETTINGSSTATE_WIFISELECTIONCHANGED_BAND));
                switch (bandEnum) {
                    case ARCOMMANDS_JUMPINGSUMO_NETWORK_WIFISCAN_BAND_2_4GHZ:
                        band = WIFI_BAND_2_4GHZ;
                        isDetermined = true;
                        break;
                    case ARCOMMANDS_JUMPINGSUMO_NETWORK_WIFISCAN_BAND_5GHZ:
                        band = WIFI_BAND_5GHZ;
                        isDetermined = true;
                        break;
                    default:
                        band = -1;
                        break;
                }

                if (isDetermined) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            notifyWifiBandChanged(band);
                        }
                    });
                }
            }
        }
    }
}
