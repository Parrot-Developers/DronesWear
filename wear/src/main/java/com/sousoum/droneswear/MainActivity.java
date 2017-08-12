package com.sousoum.droneswear;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.GridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.sousoum.shared.InteractionType;
import com.sousoum.shared.JoystickData;
import com.sousoum.shared.Message;
import com.sousoum.views.JoystickView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, DataApi.DataListener, JoystickView.JoystickListener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    private static final String TAG = "WearMainActivity";

    private BoxInsetLayout mContainerView;
    private TextView mClockView;
    private TextView mTextview;

    private Handler mHandler;
    private Runnable mSendAccRunnable;
    private SensorManager mManager;

    private final Object mAcceleroLock = new Object();
    private final Object mJoystickLock = new Object();

    private AccelerometerData mAccData;
    private JoystickData mJoystickData;

    private GoogleApiClient mGoogleApiClient;

    private GridViewPager mGridViewPager;
    private DWGridPagerAdapter mAdapter;
    private DotsPageIndicator mDotsPageIndicator;

    private boolean mShowAction;
    private boolean mShowJoystick;

    private int mCurrentAction;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mShowAction = false;
        mShowJoystick = false;

        mContainerView = findViewById(R.id.container);
        mGridViewPager = findViewById(R.id.gridViewPager);
        mDotsPageIndicator = findViewById(R.id.dotsPageIndicator);
        mAdapter = new DWGridPagerAdapter(this);
        mGridViewPager.setAdapter(mAdapter);
        mDotsPageIndicator.setPager(mGridViewPager);
        mDotsPageIndicator.setOnPageChangeListener(mAdapter);

        mClockView = findViewById(R.id.clock);
        mTextview = findViewById(R.id.textview);

        mCurrentAction = ActionType.NONE;

        mAccData = new AccelerometerData(0, 0, 0);
        mJoystickData = new JoystickData(0, 0);

        mManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addApi(Wearable.API).build();
        mHandler = new Handler();

        mSendAccRunnable = new Runnable()
        {
            @Override
            public void run()
            {

                sendSensorValues();
                sendJoystickValues();

                mHandler.postDelayed(mSendAccRunnable, 100);

            }
        };

        mHandler.postDelayed(mSendAccRunnable, 500);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mManager.unregisterListener(this);
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onPause(){
        super.onPause();

        PendingResult<DataApi.DeleteDataItemsResult> pendingResult = Message.sendEmptyAcceleroMessage(mGoogleApiClient);
        if (pendingResult != null) {
            pendingResult.setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
                @Override
                public void onResult(@NonNull DataApi.DeleteDataItemsResult deleteDataItemsResult) {
                    PendingResult<DataApi.DeleteDataItemsResult> pendingResult = Message.sendEmptyJoystickMessage(mGoogleApiClient);
                    if (pendingResult != null) {
                        pendingResult.setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
                            @Override
                            public void onResult(@NonNull DataApi.DeleteDataItemsResult deleteDataItemsResult) {
                                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                                    Wearable.DataApi.removeListener(mGoogleApiClient, MainActivity.this);
                                    mGoogleApiClient.disconnect();
                                }
                            }
                        });
                    }
                }
            });
        }
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

    private void sendSensorValues() {
        synchronized (mAcceleroLock)
        {
            Message.sendAcceleroMessage(mAccData, mGoogleApiClient);
        }
    }

    private void sendJoystickValues() {
        synchronized (mJoystickLock)
        {
            Message.sendJoystickMessage(mJoystickData, mGoogleApiClient);
        }
    }

    private void onActionTypeChanged(int actionType)
    {
        mCurrentAction = actionType;
        switch (actionType) {
            case ActionType.NONE:
                mTextview.setVisibility(View.VISIBLE);
                break;
            case ActionType.JUMP:
                mAdapter.notifyDataSetChanged();
                break;
            case ActionType.TAKE_OFF:
                mAdapter.notifyDataSetChanged();
                break;
            case ActionType.LAND:
                mAdapter.notifyDataSetChanged();
                break;
        }

        if (actionType != ActionType.NONE) {
            mTextview.setVisibility(View.GONE);
        }
    }

    private void onInteractionTypeChanged(int interactionType) {
        if ((interactionType & InteractionType.NONE) == InteractionType.NONE) {
            mShowJoystick = false;
            mShowAction = false;
        } else {
            mShowAction = ((interactionType & InteractionType.ACTION) == InteractionType.ACTION);
            mShowJoystick = ((interactionType & InteractionType.JOYSTICK) == InteractionType.JOYSTICK);
        }
        mAdapter.notifyDataSetChanged();
    }

    private void onButtonClicked()
    {
        PendingResult<DataApi.DataItemResult> pendingResult = Message.sendActionMessage(mGoogleApiClient);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                Message.emptyActionMessage(mGoogleApiClient);
            }
        });

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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    //endregion SensorEventListener

    //region DataApi.DataListener
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem dataItem = event.getDataItem();
                switch (Message.getMessageType(dataItem)) {
                    case ACTION_TYPE:
                        int productAction = Message.decodeActionTypeMessage(dataItem);
                        onActionTypeChanged(productAction);
                        break;
                    case INTERACTION_TYPE:
                        int interactionType = Message.decodeInteractionTypeMessage(dataItem);
                        onInteractionTypeChanged(interactionType);
                        break;
                }

            }
        }
    }
    //endregion DataApi.DataListener

    //region GoogleApiClient.ConnectionCallbacks
    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);

        // get existing data
        PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(mGoogleApiClient);
        results.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(@NonNull DataItemBuffer dataItems) {
                for (DataItem dataItem : dataItems) {
                    switch (Message.getMessageType(dataItem)) {
                        case ACTION_TYPE:
                            int productAction = Message.decodeActionTypeMessage(dataItem);
                            onActionTypeChanged(productAction);
                            break;
                        case INTERACTION_TYPE:
                            int interactionType = Message.decodeInteractionTypeMessage(dataItem);
                            onInteractionTypeChanged(interactionType);
                            break;
                    }
                }
                dataItems.release();
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended");
    }
    //endregion GoogleApiClient.ConnectionCallbacks

    @Override
    public void onValuesUpdated(float percentX, float percentY) {
        synchronized (mJoystickLock)
        {
            mJoystickData.setJoystickData(percentX, percentY);
        }
    }

    private class DWGridPagerAdapter extends GridPagerAdapter implements GridViewPager.OnPageChangeListener  {

        private final LayoutInflater mLayoutInflater;

        DWGridPagerAdapter(Context ctx) {
            mLayoutInflater = LayoutInflater.from(ctx);
        }

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public int getColumnCount(int i) {
            int nbColumns = 1;
            if (mShowJoystick) nbColumns++;
            return nbColumns;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int row, final int column) {
            View view = null;
            switch (column) {
                case 0:
                    if (mShowAction) {
                        view = instantiateActionView(container);
                    }
                    break;
                case 1:
                    if (mShowJoystick) {
                        view = instantiateJoystickView();
                    }
                    break;
            }

            if (view != null) {
                container.addView(view);
            }
            return view;
        }

        private View instantiateActionView(ViewGroup container) {
            final View actionView = mLayoutInflater.inflate(
                    R.layout.action_layout, container, false);

            Button actionBt = actionView.findViewById(R.id.button);
            actionBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onButtonClicked();
                }
            });

            switch (mCurrentAction) {
                case ActionType.NONE:
                    actionBt.setText(null);
                    break;
                case ActionType.JUMP:
                    actionBt.setText(R.string.jump_action);
                    break;
                case ActionType.TAKE_OFF:
                    actionBt.setText(R.string.take_off_action);
                    break;
                case ActionType.LAND:
                    actionBt.setText(R.string.land_action);
                    break;
            }
            return actionView;
        }

        private View instantiateJoystickView() {
            final JoystickView joystickView = new JoystickView(MainActivity.this);
            joystickView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            joystickView.addListener(MainActivity.this);

            return joystickView;
        }

        @Override
        public void destroyItem(ViewGroup viewGroup, int row, int column, Object object) {
            viewGroup.removeView((View)object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void onPageScrolled(int i, int i1, float v, float v1, int i2, int i3) {

        }

        @Override
        public void onPageSelected(int i, int i1) {
        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }
    }
}
