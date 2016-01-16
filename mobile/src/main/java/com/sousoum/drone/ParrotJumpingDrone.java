package com.sousoum.drone;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.parrot.arsdk.arcommands.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.sousoum.shared.AccelerometerData;
import com.sousoum.shared.ActionType;

/**
 * Created by d.bertrand on 15/01/16.
 */
public class ParrotJumpingDrone extends ParrotDrone
{
    private static final String TAG = "ParrotJumpingDrone";

    public ParrotJumpingDrone(@NonNull ARDiscoveryDeviceService deviceService, Context ctx)
    {
        super(deviceService, ctx);
    }

    @Override
    public void pilotWithAcceleroData(AccelerometerData accelerometerData)
    {
        byte speedVal = (byte) -(Math.max(-100, Math.min(100, ((accelerometerData.getAccY() / 9.0) * 50))));
        byte turnVal = (byte) -(Math.max(-100, Math.min(100, ((accelerometerData.getAccX() / 9.0) * 50))));
        Log.i(TAG, "speed = " + speedVal + " | turn = " + turnVal);
        mDeviceController.getFeatureJumpingSumo().setPilotingPCMD((byte) 1, speedVal, turnVal);
    }

    @Override
    public void stopPiloting()
    {
        mDeviceController.getFeatureJumpingSumo().setPilotingPCMD((byte) 0, (byte) 0, (byte) 0);
    }

    @Override
    public void sendAction()
    {
        mDeviceController.getFeatureJumpingSumo().sendAnimationsJump(ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_ENUM.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_HIGH);
    }

    @Override
    public void onStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error)
    {
        super.onStateChanged(deviceController, newState, error);

        if (newState == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING) {
            mHandler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    setCurrentAction(ActionType.ACTION_TYPE_JUMP);
                }
            });
        }
    }
}
