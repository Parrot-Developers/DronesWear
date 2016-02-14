package com.sousoum.drone;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_NETWORK_WIFISCAN_BAND_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARFeatureARDrone3;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.sousoum.shared.AccelerometerData;
import com.sousoum.shared.ActionType;
import com.sousoum.shared.JoystickData;

/**
 * Created by d.bertrand on 12/02/16.
 */
public class ParrotBebopDrone extends ParrotFlyingDrone {

    private static final String TAG = "ParrotBebopsDrone";

    public ParrotBebopDrone(@NonNull ARDiscoveryDeviceService deviceService, Context ctx) {
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
    public void pilotWithJoystickData(JoystickData joystickData) {
        if (mDeviceController != null) {
            byte yawVal = (byte) Math.max(-100, Math.min(100, joystickData.getPercentX() * 50));
            byte gazVal = (byte) Math.max(-100, Math.min(100, joystickData.getPercentY() * 50));
            Log.w(TAG, "yaw = " + yawVal + " | gaz = " + gazVal);
            mDeviceController.getFeatureARDrone3().setPilotingPCMDYaw(yawVal);
            mDeviceController.getFeatureARDrone3().setPilotingPCMDGaz(gazVal);
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
                case ActionType.TAKE_OFF:
                    mDeviceController.getFeatureARDrone3().sendPilotingTakeOff();
                    break;
                case ActionType.LAND:
                    mDeviceController.getFeatureARDrone3().sendPilotingLanding();
                    break;
            }
        }
    }

    @Override
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
                        action = ActionType.LAND;
                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
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
        } else if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_NETWORKSETTINGSSTATE_WIFISELECTIONCHANGED) && (elementDictionary != null)) {
            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (args != null) {
                final int band;
                boolean isDetermined = false;
                ARCOMMANDS_ARDRONE3_NETWORK_WIFISCAN_BAND_ENUM bandEnum = ARCOMMANDS_ARDRONE3_NETWORK_WIFISCAN_BAND_ENUM.getFromValue((Integer) args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_NETWORKSETTINGSSTATE_WIFISELECTIONCHANGED_BAND));
                switch (bandEnum) {
                    case ARCOMMANDS_ARDRONE3_NETWORK_WIFISCAN_BAND_2_4GHZ:
                        band = WIFI_BAND_2_4GHZ;
                        isDetermined = true;
                        break;
                    case ARCOMMANDS_ARDRONE3_NETWORK_WIFISCAN_BAND_5GHZ:
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
