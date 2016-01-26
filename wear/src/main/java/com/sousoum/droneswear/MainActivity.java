package com.sousoum.droneswear;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;
import com.sousoum.shared.AccelerometerData;
import com.sousoum.shared.ActionType;
import com.sousoum.shared.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, DataApi.DataListener
{

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    private static final String TAG = "WearMainActivity";

    private BoxInsetLayout mContainerView;
    private TextView mClockView;
    private Button mActionBt;
    private TextView mTextview;

    private Handler mHandler;
    private Runnable mSendAccRunnable;
    private SensorManager mManager;

    private final Object mAcceleroLock = new Object();

    private AccelerometerData mAccData;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mClockView = (TextView) findViewById(R.id.clock);
        mActionBt = (Button) findViewById(R.id.button);
        mTextview = (TextView) findViewById(R.id.textview);

        mAccData = new AccelerometerData(0, 0, 0);

        mManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addApi(Wearable.API).build();
        mHandler = new Handler();

        mSendAccRunnable = new Runnable()
        {
            @Override
            public void run()
            {

                sendSensorValues();

                mHandler.postDelayed(mSendAccRunnable, 100);

            }
        };

        mHandler.postDelayed(mSendAccRunnable, 500);
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mManager.unregisterListener(this);
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onPause(){
        super.onPause();

        PendingResult<DataApi.DeleteDataItemsResult> pendingResult = Message.sendEmptyAcceleroMessage(mGoogleApiClient);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>()
        {
            @Override
            public void onResult(DataApi.DeleteDataItemsResult deleteDataItemsResult)
            {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
                {
                    Wearable.DataApi.removeListener(mGoogleApiClient, MainActivity.this);
                    mGoogleApiClient.disconnect();
                }
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        mManager.registerListener(this, mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);

        mGoogleApiClient.connect();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails)
    {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient()
    {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient()
    {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay()
    {
        if (isAmbient())
        {
            mContainerView.setBackgroundResource(android.R.color.black);
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        }
        else
        {
            mContainerView.setBackgroundResource(R.color.primary_light);
            mClockView.setVisibility(View.GONE);
        }
    }

    private void sendSensorValues()
    {
        synchronized (mAcceleroLock)
        {
            Message.sendAcceleroMessage(mAccData, mGoogleApiClient);
        }
    }

    private void onActionTypeChanged(int actionType)
    {
        switch (actionType) {
            case ActionType.ACTION_TYPE_NONE:
                mActionBt.setVisibility(View.GONE);
                mTextview.setVisibility(View.VISIBLE);
                break;
            case ActionType.ACTION_TYPE_JUMP:
                mActionBt.setText(R.string.jump_action);
                mActionBt.setVisibility(View.VISIBLE);
                mTextview.setVisibility(View.GONE);
                break;
            case ActionType.ACTION_TYPE_TAKE_OFF:
                mActionBt.setText(R.string.take_off_action);
                mActionBt.setVisibility(View.VISIBLE);
                mTextview.setVisibility(View.GONE);
                break;
            case ActionType.ACTION_TYPE_LAND:
                mActionBt.setText(R.string.land_action);
                mActionBt.setVisibility(View.VISIBLE);
                mTextview.setVisibility(View.GONE);
                break;
        }
    }

    public void onButtonClicked(View view)
    {
        Message.sendActionMessage(mGoogleApiClient);

        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE,
                getString(R.string.action_sent));
        startActivity(intent);
    }

    //region SensorEventListener
    @Override
    public void onSensorChanged(SensorEvent event)
    {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                synchronized (mAcceleroLock)
                {
                    mAccData.setAccData(
                            event.values[0],
                            event.values[1],
                            event.values[2]);
                }
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }
    //endregion SensorEventListener

    //region DataApi.DataListener
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d("TAG", "DataItem deleted: " + event.getDataItem().getUri());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem dataItem = event.getDataItem();
                switch (Message.getMessageType(dataItem)) {
                    case ACTION_TYPE:
                        int productAction = Message.decodeActionTypeMessage(dataItem);
                        onActionTypeChanged(productAction);
                        break;
                }

            }
        }
    }
    //endregion DataApi.DataListener

    //region GoogleApiClient.ConnectionCallbacks
    @Override
    public void onConnected(Bundle bundle)
    {
        Wearable.DataApi.addListener(mGoogleApiClient, this);

        // get existing data
        PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(mGoogleApiClient);
        results.setResultCallback(new ResultCallback<DataItemBuffer>()
        {
            @Override
            public void onResult(DataItemBuffer dataItems)
            {
                if (dataItems != null)
                {
                    for (DataItem dataItem : dataItems)
                    {
                        switch (Message.getMessageType(dataItem))
                        {
                            case ACTION_TYPE:
                                int productAction = Message.decodeActionTypeMessage(dataItem);
                                onActionTypeChanged(productAction);
                                break;
                        }
                    }
                    dataItems.release();
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        Log.i(TAG, "onConnectionSuspended");
    }
    //endregion GoogleApiClient.ConnectionCallbacks
}
