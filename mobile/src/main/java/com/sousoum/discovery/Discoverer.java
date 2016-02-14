package com.sousoum.discovery;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_FAMILY_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by d.bertrand on 13/01/16.
 */
public class Discoverer implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate {
    private static final String TAG = "Discoverer";
    private static final int TIMEOUT_DELAY_MS = 8000;

    public interface DiscovererListener {
        /**
         * Called when a Parrot device has been found on the network
         *
         * @param deviceService The device found
         */
        void onServiceDiscovered(ARDiscoveryDeviceService deviceService);

        /**
         * Called when the discovery took too long
         */
        void onDiscoveryTimedOut();
    }

    private final List<DiscovererListener> mListeners;


    private final Context mCtx;

    private ARDiscoveryService mArdiscoveryService;
    private ServiceConnection mArdiscoveryServiceConnection;
    private ARDiscoveryServicesDevicesListUpdatedReceiver mArdiscoveryServicesDevicesListUpdatedReceiver;

    private final Handler mHandler;

    private Runnable mTimeoutRunnable;

    public Discoverer(Context ctx) {
        mCtx = ctx;

        mListeners = new ArrayList<>();
        mHandler = new Handler(mCtx.getMainLooper());
        mTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                notifyDiscoveryTimedOut();
                mHandler.removeCallbacks(mTimeoutRunnable);
            }
        };

        initBroadcastReceiver();
    }

    public void setup() {
        registerReceivers();
        initDiscoveryService();
    }

    public void cleanup() {
        stopDiscovering();
        closeDiscoveryService();
        unregisterReceivers();
    }

    public void addListener(DiscovererListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(DiscovererListener listener) {
        mListeners.remove(listener);
    }

    public void startDiscovering() {
        if (mArdiscoveryService != null) {
            Log.i(TAG, "Start discovering");
            onServicesDevicesListUpdated();
            mArdiscoveryService.start();
            mHandler.postDelayed(mTimeoutRunnable, TIMEOUT_DELAY_MS);
        }
    }

    public void stopDiscovering() {
        if (mArdiscoveryService != null) {
            Log.i(TAG, "Stop discovering");
            mArdiscoveryService.stop();
            mHandler.removeCallbacks(mTimeoutRunnable);
        }
    }


    private void initBroadcastReceiver() {
        mArdiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
    }

    private void initDiscoveryService() {
        // create the service connection
        if (mArdiscoveryServiceConnection == null) {
            mArdiscoveryServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    mArdiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();

                    startDiscovering();
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    mArdiscoveryService = null;
                }
            };
        }

        if (mArdiscoveryService == null) {
            // if the discovery service doesn't exists, bind to it
            Intent i = new Intent(mCtx, ARDiscoveryService.class);
            mCtx.bindService(i, mArdiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        } else {
            // if the discovery service already exists, start discovery
            startDiscovering();
        }
    }

    private void closeDiscoveryService() {
        Log.d(TAG, "closeServices ...");

        if (mArdiscoveryService != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mArdiscoveryService.stop();

                    mCtx.unbindService(mArdiscoveryServiceConnection);
                    mArdiscoveryService = null;
                }
            }).start();
        }
    }

    private void registerReceivers() {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(mCtx);
        localBroadcastMgr.registerReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver, new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }

    private void unregisterReceivers() {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(mCtx);
        localBroadcastMgr.unregisterReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver);
    }

    @Override
    public void onServicesDevicesListUpdated() {
        if (mArdiscoveryService != null) {
            List<ARDiscoveryDeviceService> deviceList = mArdiscoveryService.getDeviceServicesArray();

            if (deviceList != null) {
                for (ARDiscoveryDeviceService service : deviceList) {
                    Log.i(TAG, "service :  " + service + " name = " + service.getName());
                    final ARDISCOVERY_PRODUCT_ENUM product = ARDiscoveryService.getProductFromProductID(service.getProductID());
                    Log.i(TAG, "product :  " + product);
                    ARDISCOVERY_PRODUCT_FAMILY_ENUM family = ARDiscoveryService.getProductFamily(product);
                    // only display drones from family Bebop, Jumping and MiniDrone (RollingSpider)
                    if (ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_JS.equals(family) ||
                            ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_MINIDRONE.equals(family) ||
                            ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_ARDRONE.equals(family)) {
                        final ARDiscoveryDeviceService serviceFinal = service;
                        mHandler.removeCallbacks(mTimeoutRunnable);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                notifyServiceDiscovered(serviceFinal);
                            }
                        });
                        break;

                    }
                }
            }
        }
    }

    private void notifyServiceDiscovered(ARDiscoveryDeviceService discoveryDeviceService) {
        List<DiscovererListener> listenersCpy = new ArrayList<>(mListeners);
        for (DiscovererListener listener : listenersCpy) {
            listener.onServiceDiscovered(discoveryDeviceService);
        }
    }

    private void notifyDiscoveryTimedOut() {
        List<DiscovererListener> listenersCpy = new ArrayList<>(mListeners);
        for (DiscovererListener listener : listenersCpy) {
            listener.onDiscoveryTimedOut();
        }
    }
}
