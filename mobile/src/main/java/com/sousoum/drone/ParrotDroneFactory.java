package com.sousoum.drone;

import android.content.Context;
import android.support.annotation.NonNull;

import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;

/**
 * Created by d.bertrand on 15/01/16.
 */
public class ParrotDroneFactory {
    public static ParrotDrone createParrotDrone(@NonNull ARDiscoveryDeviceService deviceService, Context ctx) {
        ParrotDrone drone = null;
        switch (ARDiscoveryService.getProductFamily(ARDiscoveryService.getProductFromProductID(deviceService.getProductID()))) {
            case ARDISCOVERY_PRODUCT_FAMILY_ARDRONE:
                drone = new ParrotBebopDrone(deviceService, ctx);
                break;
            case ARDISCOVERY_PRODUCT_FAMILY_JS:
                drone = new ParrotJumpingDrone(deviceService, ctx);
                break;
            case ARDISCOVERY_PRODUCT_FAMILY_MINIDRONE:
                drone = new ParrotMiniDrone(deviceService, ctx);
                break;
        }

        return drone;
    }
}
