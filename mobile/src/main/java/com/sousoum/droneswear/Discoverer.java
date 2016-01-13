package com.sousoum.droneswear;

/**
 * Created by d.bertrand on 13/01/16.
 */
public class Discoverer
{
    private void startDiscovery()
    {
        if (mArdiscoveryService != null)
        {
            mArdiscoveryService.startWifiDiscovering();
            mTextView.setText("Searching for Parrot devices...");
        }
    }
}
