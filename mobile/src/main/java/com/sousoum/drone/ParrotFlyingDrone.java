package com.sousoum.drone;

import android.content.Context;
import android.support.annotation.NonNull;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;

/**
 * Created by d.bertrand on 15/01/16.
 */
public abstract class ParrotFlyingDrone extends ParrotDrone {

    public ParrotFlyingDrone(@NonNull ARDiscoveryDeviceService deviceService, Context ctx) {
        super(deviceService, ctx);
    }

    public abstract void sendEmergency();
}
