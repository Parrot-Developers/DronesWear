package com.sousoum.shared;

/**
 * Created by d.bertrand on 07/01/16.
 */
public class AccelerometerData
{
    private float mAccX;
    private float mAccY;
    private float mAccZ;

    public AccelerometerData(float accX, float accY, float accZ) {
        mAccX = accX;
        mAccY = accY;
        mAccZ = accZ;
    }

    public float getAccX()
    {
        return mAccX;
    }

    public float getAccY()
    {
        return mAccY;
    }

    public float getAccZ()
    {
        return mAccZ;
    }

    @Override
    public String toString()
    {
        return "Accelero = [" + mAccX + ", " + mAccY + ", " + mAccZ + "]";
    }
}
