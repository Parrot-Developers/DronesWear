package com.sousoum.drone;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARFeatureARDrone3;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.sousoum.shared.AccelerometerData;
import com.sousoum.shared.ActionType;

/**
 * Created by d.bertrand on 15/01/16.
 */
public class ParrotFlyingDrone extends ParrotDrone {
    private static final String TAG = "ParrotFlyingDrone";

    public ParrotFlyingDrone(@NonNull ARDiscoveryDeviceService deviceService, Context ctx) {
        super(deviceService, ctx);
    }

    @Override
    public void pilotWithAcceleroData(AccelerometerData accelerometerData) {
        if (mDeviceController != null) {
            byte pitchVal = (byte) -(Math.max(-100, Math.min(100, ((accelerometerData.getAccY() / 9.0) * 50))));
            byte rollVal = (byte) -(Math.max(-100, Math.min(100, ((accelerometerData.getAccX() / 9.0) * 50))));
            Log.w(TAG, "pitch = " + pitchVal + " | roll = " + rollVal);
            mDeviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 1);
            mDeviceController.getFeatureARDrone3().setPilotingPCMDRoll(rollVal);
            mDeviceController.getFeatureARDrone3().setPilotingPCMDPitch(pitchVal);
        }
    }

    @Override
    public void stopPiloting() {
        if (mDeviceController != null) {
            mDeviceController.getFeatureARDrone3().setPilotingPCMD((byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
        }
    }

    @Override
    public void sendAction() {
        if (mDeviceController != null) {
            switch (mCurrentAction) {
                case ActionType.ACTION_TYPE_TAKE_OFF:
                    mDeviceController.getFeatureARDrone3().sendPilotingTakeOff();
                    break;
                case ActionType.ACTION_TYPE_LAND:
                    mDeviceController.getFeatureARDrone3().sendPilotingLanding();
                    break;
            }
        }
    }

    public void sendEmergency() {
        mDeviceController.getFeatureARDrone3().sendPilotingEmergency();
    }

    @Override
    public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary) {
        super.onCommandReceived(deviceController, commandKey, elementDictionary);

        if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED) && (elementDictionary != null)) {
            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (args != null) {
                final int action;
                ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue((Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE));
                switch (state) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        action = ActionType.ACTION_TYPE_LAND;
                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                        action = ActionType.ACTION_TYPE_TAKE_OFF;
                        break;
                    default:
                        action = ActionType.ACTION_TYPE_NONE;
                        break;
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setCurrentAction(action);
                    }
                });
            }
        }
    }
}
