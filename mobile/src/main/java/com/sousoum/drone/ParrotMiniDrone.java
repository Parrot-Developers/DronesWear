package com.sousoum.drone;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.parrot.arsdk.arcommands.ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARFeatureMiniDrone;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.sousoum.shared.AccelerometerData;
import com.sousoum.shared.ActionType;
import com.sousoum.shared.JoystickData;

/**
 * Created by d.bertrand on 12/02/16.
 */
public class ParrotMiniDrone extends ParrotFlyingDrone {

    private static final String TAG = "ParrotRSsDrone";

    public ParrotMiniDrone(@NonNull ARDiscoveryDeviceService deviceService, Context ctx) {
        super(deviceService, ctx);
    }

    @Override
    public void pilotWithAcceleroData(AccelerometerData accelerometerData) {
        if (mDeviceController != null) {
            byte pitchVal = (byte) -(Math.max(-100, Math.min(100, ((accelerometerData.getAccY() / 9.0) * 50))));
            byte rollVal = (byte) -(Math.max(-100, Math.min(100, ((accelerometerData.getAccX() / 9.0) * 50))));
            Log.w(TAG, "pitch = " + pitchVal + " | roll = " + rollVal);
            mDeviceController.getFeatureMiniDrone().setPilotingPCMDFlag((byte) 1);
            mDeviceController.getFeatureMiniDrone().setPilotingPCMDRoll(rollVal);
            mDeviceController.getFeatureMiniDrone().setPilotingPCMDPitch(pitchVal);
        }
    }

    @Override
    public void pilotWithJoystickData(JoystickData joystickData) {
        if (mDeviceController != null) {
            byte yawVal = (byte) Math.max(-100, Math.min(100, joystickData.getPercentX() * 50));
            byte gazVal = (byte) Math.max(-100, Math.min(100, joystickData.getPercentY() * 50));
            Log.w(TAG, "yaw = " + yawVal + " | gaz = " + gazVal);
            mDeviceController.getFeatureMiniDrone().setPilotingPCMDYaw(yawVal);
            mDeviceController.getFeatureMiniDrone().setPilotingPCMDGaz(gazVal);
        }
    }

    @Override
    public void stopPiloting() {
        if (mDeviceController != null) {
            mDeviceController.getFeatureMiniDrone().setPilotingPCMD((byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
        }
    }

    @Override
    public void sendAction() {
        if (mDeviceController != null) {
            switch (mCurrentAction) {
                case ActionType.TAKE_OFF:
                    mDeviceController.getFeatureMiniDrone().sendPilotingTakeOff();
                    break;
                case ActionType.LAND:
                    mDeviceController.getFeatureMiniDrone().sendPilotingLanding();
                    break;
            }
        }
    }

    @Override
    public void sendEmergency() {
        mDeviceController.getFeatureMiniDrone().sendPilotingEmergency();
    }

    @Override
    public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary) {
        super.onCommandReceived(deviceController, commandKey, elementDictionary);

        if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED) && (elementDictionary != null)) {
            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (args != null) {
                final int action;
                ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state = ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue((Integer) args.get(ARFeatureMiniDrone.ARCONTROLLER_DICTIONARY_KEY_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE));
                switch (state) {
                    case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        action = ActionType.LAND;
                        break;
                    case ARCOMMANDS_MINIDRONE_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                        action = ActionType.TAKE_OFF;
                        break;
                    default:
                        action = ActionType.NONE;
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
