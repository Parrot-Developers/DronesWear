# Privacy Policy for the Android app `DronesWear`

The `READ_PHONE_STATE` permission is required by the ARSDK since
[libmux](https://github.com/Parrot-Developers/libmux/blob/dd3657f82c822a655d9fb79a65cafe72585b7e27/android/build.gradle)
does not mention a `minSdkVersion` nor a `targetSdkVersion`.<br/>
As explained [here](https://developer.android.com/reference/android/Manifest.permission.html#READ_PHONE_STATE),

> If both your minSdkVersion and targetSdkVersion values are set to 3 or lower,
the system implicitly grants your app this permission.


No use of this permission is done in the application.