package com.sousoum.droneswear;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.parrot.arsdk.ARSDK;
import com.parrot.arsdk.arcommands.ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM;
import com.parrot.arsdk.arcommands.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DEVICE_STATE_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_DICTIONARY_KEY_ENUM;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_ERROR_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerArgumentDictionary;
import com.parrot.arsdk.arcontroller.ARControllerDictionary;
import com.parrot.arsdk.arcontroller.ARControllerException;
import com.parrot.arsdk.arcontroller.ARDeviceController;
import com.parrot.arsdk.arcontroller.ARDeviceControllerListener;
import com.parrot.arsdk.arcontroller.ARFeatureARDrone3;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_ENUM;
import com.parrot.arsdk.ardiscovery.ARDISCOVERY_PRODUCT_FAMILY_ENUM;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDevice;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceNetService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryDeviceService;
import com.parrot.arsdk.ardiscovery.ARDiscoveryException;
import com.parrot.arsdk.ardiscovery.ARDiscoveryService;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiver;
import com.parrot.arsdk.ardiscovery.receivers.ARDiscoveryServicesDevicesListUpdatedReceiverDelegate;
import com.sousoum.shared.AccelerometerData;
import com.sousoum.shared.Message;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ARDiscoveryServicesDevicesListUpdatedReceiverDelegate, ARDeviceControllerListener, GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener, NodeApi.NodeListener
{
    private static final String TAG = "MobileMainActivity";

    private GoogleApiClient mGoogleApiClient;

    private ARDiscoveryService mArdiscoveryService;
    private ServiceConnection mArdiscoveryServiceConnection;
    private ARDiscoveryServicesDevicesListUpdatedReceiver mArdiscoveryServicesDevicesListUpdatedReceiver;

    private ARDeviceController mDeviceController;

    private ARDISCOVERY_PRODUCT_FAMILY_ENUM mProductFamily;

    private Object mNodeLock;
    private ArrayList<Node> mNodes;

    private int mCurrentAction;

    private Button mEmergencyBt;
    private TextView mTextView;
    private Switch mAcceleroSwitch;

    static {
        ARSDK.loadSDKLibs();
    }

    private boolean mUseWatchAccelero;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = (TextView)findViewById(R.id.textView);
        mAcceleroSwitch = (Switch)findViewById(R.id.acceleroSwitch);
        mAcceleroSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                mUseWatchAccelero = mAcceleroSwitch.isChecked();
                Log.e(TAG, "Accelero is checked = " + mUseWatchAccelero);
                synchronized (this)
                {
                    if (!mUseWatchAccelero && mDeviceController != null)
                    {
                        if (mProductFamily == ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_ARDRONE)
                        {
                            mDeviceController.getFeatureARDrone3().setPilotingPCMD((byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
                        }
                        else if (mProductFamily == ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_JS)
                        {
                            mDeviceController.getFeatureJumpingSumo().setPilotingPCMD((byte) 0, (byte) 0, (byte) 0);
                        }
                    }
                }
            }
        });
        mEmergencyBt = (Button)findViewById(R.id.emergencyBt);
        mEmergencyBt.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                mDeviceController.getFeatureARDrone3().sendPilotingEmergency();
            }
        });

        mUseWatchAccelero = true;
        mAcceleroSwitch.setChecked(true);
        

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addApi(Wearable.API).build();

        mNodes = new ArrayList<>();
        mNodeLock = new Object();

        mCurrentAction = Message.ACTION_TYPE_NONE;

        initBroadcastReceiver();
        initDiscoveryService();
    }

    @Override
    protected void onPause(){
        super.onPause();

        if(mGoogleApiClient != null && mGoogleApiClient.isConnected()){
            mGoogleApiClient.disconnect();
        }

        unregisterReceivers();
        closeDiscoveryService();
    }

    @Override
    protected void onResume(){
        super.onResume();

        mGoogleApiClient.connect();

        onServicesDevicesListUpdated();

        registerReceivers();
        initDiscoveryService();

    }

    private void startStubAnimation()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                mDeviceController.getFeatureJumpingSumo().setPilotingPCMD((byte)1, (byte)50, (byte)50);
                try
                {
                    Thread.sleep(2500);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                mDeviceController.getFeatureJumpingSumo().setPilotingPCMD((byte)1, (byte)-50, (byte)50);
                try
                {
                    Thread.sleep(1500);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                mDeviceController.getFeatureJumpingSumo().setPilotingPCMD((byte)1, (byte)-50, (byte)-50);
                try
                {
                    Thread.sleep(9500);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                mDeviceController.getFeatureJumpingSumo().setPilotingPCMD((byte)1, (byte)50, (byte)-50);
                try
                {
                    Thread.sleep(1500);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                mDeviceController.getFeatureJumpingSumo().setPilotingPCMD((byte)0, (byte)0, (byte)0);
            }
        }).start();
    }

    //region ARSDK discovery
    private void initDiscoveryService()
    {
        // create the service connection
        if (mArdiscoveryServiceConnection == null)
        {
            mArdiscoveryServiceConnection = new ServiceConnection()
            {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service)
                {
                    mArdiscoveryService = ((ARDiscoveryService.LocalBinder) service).getService();

                    startDiscovery();
                }

                @Override
                public void onServiceDisconnected(ComponentName name)
                {
                    mArdiscoveryService = null;
                }
            };
        }

        if (mArdiscoveryService == null)
        {
            // if the discovery service doesn't exists, bind to it
            Intent i = new Intent(getApplicationContext(), ARDiscoveryService.class);
            getApplicationContext().bindService(i, mArdiscoveryServiceConnection, Context.BIND_AUTO_CREATE);
        }
        else
        {
            // if the discovery service already exists, start discovery
            startDiscovery();
        }
    }

    private void closeDiscoveryService()
    {
        Log.d(TAG, "closeServices ...");

        if (mArdiscoveryService != null)
        {
            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    mArdiscoveryService.stop();

                    getApplicationContext().unbindService(mArdiscoveryServiceConnection);
                    mArdiscoveryService = null;
                }
            }).start();
        }
    }

    private void startDiscovery()
    {
        if (mArdiscoveryService != null)
        {
            mArdiscoveryService.startWifiDiscovering();
            mTextView.setText("Searching for Parrot devices...");
        }
    }

    private void initBroadcastReceiver()
    {
        mArdiscoveryServicesDevicesListUpdatedReceiver = new ARDiscoveryServicesDevicesListUpdatedReceiver(this);
    }

    private void registerReceivers()
    {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.registerReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver, new IntentFilter(ARDiscoveryService.kARDiscoveryServiceNotificationServicesDevicesListUpdated));
    }

    private void unregisterReceivers()
    {
        LocalBroadcastManager localBroadcastMgr = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastMgr.unregisterReceiver(mArdiscoveryServicesDevicesListUpdatedReceiver);
    }
    //endregion ARSDK discovery

    //region ARDiscoveryServicesDevicesListUpdatedReceiverDelegate
    @Override
    public void onServicesDevicesListUpdated()
    {
        if (mArdiscoveryService != null)
        {
            List<ARDiscoveryDeviceService> deviceList = mArdiscoveryService.getDeviceServicesArray();

            if(deviceList != null)
            {
                for (ARDiscoveryDeviceService service : deviceList)
                {
                    Log.e(TAG, "service :  "+ service + " name = " + service.getName());
                    final ARDISCOVERY_PRODUCT_ENUM product = ARDiscoveryService.getProductFromProductID(service.getProductID());
                    Log.e(TAG, "product :  "+ product);
                    // only display Bebop drones
                    if (ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_JS.equals(ARDiscoveryService.getProductFamily(product)) ||
                            ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_ARDRONE.equals(ARDiscoveryService.getProductFamily(product)))
                    {
                        final ARDiscoveryDeviceService serviceFinal  = service;
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                mTextView.setText("Connecting to Parrot Device: " + serviceFinal.getName());
                                ARDiscoveryDevice discoveryDevice = createDiscoveryDevice(serviceFinal);
                                ARDeviceController deviceController = null;
                                if (discoveryDevice != null)
                                {
                                    deviceController = createDeviceController(discoveryDevice);
                                }

                                if (deviceController != null)
                                {
                                    deviceController.start();
                                }
                            }
                        });
                        break;

                    }
                }
            }
        }
    }
    //endregion ARDiscoveryServicesDevicesListUpdatedReceiverDelegate

    //region DeviceController creation
    private ARDiscoveryDevice createDiscoveryDevice(@NonNull ARDiscoveryDeviceService service)
    {
        ARDiscoveryDevice device = null;
        try
        {
            device = new ARDiscoveryDevice();

            ARDiscoveryDeviceNetService netDeviceService = (ARDiscoveryDeviceNetService) service.getDevice();
            ARDISCOVERY_PRODUCT_ENUM productType = ARDiscoveryService.getProductFromProductID(service.getProductID());
            mProductFamily = ARDiscoveryService.getProductFamily(productType);

            device.initWifi(productType, netDeviceService.getName(), netDeviceService.getIp(), netDeviceService.getPort());
        }
        catch (ARDiscoveryException e)
        {
            e.printStackTrace();
            Log.e(TAG, "Error: " + e.getError());
        }

        return device;
    }

    private ARDeviceController createDeviceController(@NonNull ARDiscoveryDevice discoveryDevice)
    {
        ARDeviceController deviceController = null;
        try
        {
            deviceController = new ARDeviceController(discoveryDevice);

            deviceController.addListener(this);
        }
        catch (ARControllerException e)
        {
            e.printStackTrace();
        }

        return deviceController;
    }
    //endregion DeviceController creation

    //region ARDeviceControllerListener
    @Override
    public void onStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARCONTROLLER_ERROR_ENUM error)
    {
        if (newState == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_STOPPED)
        {
            Log.e(TAG, "Restarting discovery");
            mDeviceController = null;
            mProductFamily = ARDISCOVERY_PRODUCT_FAMILY_ENUM.eARDISCOVERY_PRODUCT_FAMILY_UNKNOWN_ENUM_VALUE;
            sendActionType(Message.ACTION_TYPE_NONE);

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    startDiscovery();

                    mEmergencyBt.setVisibility(View.GONE);

                    mTextView.setText("Parrot device disconnected. Trying to reconnect.");
                }
            });
        }
        else if (newState == ARCONTROLLER_DEVICE_STATE_ENUM.ARCONTROLLER_DEVICE_STATE_RUNNING)
        {
            Log.e(TAG, "Stopping discovery");
            mArdiscoveryService.stopWifiDiscovering();
            mDeviceController = deviceController;

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (mProductFamily == ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_JS)
                    {
                        sendActionType(Message.ACTION_TYPE_JUMP);
                    }
                    else if (mProductFamily == ARDISCOVERY_PRODUCT_FAMILY_ENUM.ARDISCOVERY_PRODUCT_FAMILY_ARDRONE)
                    {
                        mEmergencyBt.setVisibility(View.VISIBLE);
                    }

                    mTextView.setText("Parrot device connected, you can start using your watch to move it");
                }
            });
        }
    }

    private void sendActionType(int actionType)
    {
        mCurrentAction = actionType;
        synchronized (mNodeLock) {
            if (!mNodes.isEmpty()) {
                Message.sendActionTypeMessage(actionType, mNodes, mGoogleApiClient);
            }
        }
    }

    @Override
    public void onExtensionStateChanged(ARDeviceController deviceController, ARCONTROLLER_DEVICE_STATE_ENUM newState, ARDISCOVERY_PRODUCT_ENUM product, String name, ARCONTROLLER_ERROR_ENUM error) {/*No connection to the SkyController possible*/}

    @Override
    public void onCommandReceived(ARDeviceController deviceController, ARCONTROLLER_DICTIONARY_KEY_ENUM commandKey, ARControllerDictionary elementDictionary)
    {
        if ((commandKey == ARCONTROLLER_DICTIONARY_KEY_ENUM.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED) && (elementDictionary != null)){
            ARControllerArgumentDictionary<Object> args = elementDictionary.get(ARControllerDictionary.ARCONTROLLER_DICTIONARY_SINGLE_KEY);
            if (args != null) {
                ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM state = ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_ENUM.getFromValue((Integer)args.get(ARFeatureARDrone3.ARCONTROLLER_DICTIONARY_KEY_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE));
                switch (state) {
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_FLYING:
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_HOVERING:
                        sendActionType(Message.ACTION_TYPE_LAND);
                        break;
                    case ARCOMMANDS_ARDRONE3_PILOTINGSTATE_FLYINGSTATECHANGED_STATE_LANDED:
                        sendActionType(Message.ACTION_TYPE_TAKE_OFF);
                        break;
                    default:
                        sendActionType(Message.ACTION_TYPE_NONE);
                        break;
                }
            }
        }
    }
    //endregion ARDeviceControllerListener

    //region GoogleApiClient.ConnectionCallbacks
    @Override
    public void onConnected(Bundle bundle)
    {
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);

        PendingResult<NodeApi.GetConnectedNodesResult> results = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        results.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>()
        {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult)
            {
                if (getConnectedNodesResult.getStatus().isSuccess())
                {
                    synchronized (mNodeLock)
                    {
                        mNodes.addAll(getConnectedNodesResult.getNodes());
                    }
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i)
    {

    }
    //endregion GoogleApiClient.ConnectionCallbacks

    //region DataApi.DataListener
    @Override
    public void onMessageReceived(MessageEvent messageEvent)
    {
        switch (Message.getMessageType(messageEvent)) {
            case ACC:
                synchronized (this) {
                if (mDeviceController != null && mUseWatchAccelero)
                {
                    Log.e(TAG, "Message received : " + messageEvent);
                    AccelerometerData accelerometerData = Message.decodeAcceleroMessage(messageEvent);
                    switch (mProductFamily)
                    {
                        case ARDISCOVERY_PRODUCT_FAMILY_JS:
                            byte speedVal = (byte) -(Math.max(-100, Math.min(100, ((accelerometerData.getAccY() / 9.0) * 50))));
                            byte turnVal = (byte) -(Math.max(-100, Math.min(100, ((accelerometerData.getAccX() / 9.0) * 50))));
                            Log.w(TAG, "speed = " + speedVal + " | turn = " + turnVal);
                            mDeviceController.getFeatureJumpingSumo().setPilotingPCMD((byte) 1, speedVal, turnVal);
                            break;
                        case ARDISCOVERY_PRODUCT_FAMILY_ARDRONE:
                            byte pitchVal = (byte) -(Math.max(-100, Math.min(100, ((accelerometerData.getAccY() / 9.0) * 50))));
                            byte rollVal = (byte) -(Math.max(-100, Math.min(100, ((accelerometerData.getAccX() / 9.0) * 50))));
                            Log.w(TAG, "pitch = " + pitchVal + " | roll = " + rollVal);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDFlag((byte) 1);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDRoll(rollVal);
                            mDeviceController.getFeatureARDrone3().setPilotingPCMDPitch(pitchVal);
                            break;
                    }
                }

                }
                break;
            case ACTION:
                if (mDeviceController != null)
                {
                    switch (mCurrentAction)
                    {
                        case Message.ACTION_TYPE_JUMP:
                            mDeviceController.getFeatureJumpingSumo().sendAnimationsJump(ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_ENUM.ARCOMMANDS_JUMPINGSUMO_ANIMATIONS_JUMP_TYPE_HIGH);
                            break;
                        case Message.ACTION_TYPE_TAKE_OFF:
                            mDeviceController.getFeatureARDrone3().sendPilotingTakeOff();
                            break;
                        case Message.ACTION_TYPE_LAND:
                            mDeviceController.getFeatureARDrone3().sendPilotingLanding();
                            break;
                    }
                }
                break;
        }
    }
    //endregion DataApi.DataListener

    @Override
    public void onPeerConnected(Node node)
    {
        synchronized (mNodeLock)
        {
            Log.e(TAG, "Adding node = " + node);
            mNodes.add(node);
        }
    }

    @Override
    public void onPeerDisconnected(Node node)
    {
        synchronized (mNodeLock)
        {
            Log.e(TAG, "removing node = " + node);
            mNodes.remove(node);
        }
    }
}
