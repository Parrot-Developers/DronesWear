package com.sousoum.shared;

/**
 * Created by d.bertrand on 07/01/16.
 */
public class JoystickData
{
    private final float mJoystickData[];

    public JoystickData(float percentX, float percentY) {
        mJoystickData = new float[2];
        setJoystickData(percentX, percentY);
    }

    public float getPercentX()
    {
        return mJoystickData[0];
    }

    public float getPercentY()
    {
        return mJoystickData[1];
    }

    public void setJoystickData(float percentX, float percentY) {
        mJoystickData[0] = percentX;
        mJoystickData[1] = percentY;
    }

    @Override
    public String toString()
    {
        return "Joystick = [" + getPercentX() + ", " + getPercentY() + "]";
    }
}
