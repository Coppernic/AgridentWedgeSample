# Agrident Wedge Sample

Introduction
------------

This application demonstrates how to use the Agrident Wedge application on a C-One with Agrident RFID reader.

Prerequisites
-------------

### C-One

1. CpcSystemServices version 2.2.0 and above must be installed on the device
2. Agrident Wedge 2.0.0 and above must be installed on the device

### C-One²

1. CoreServices version 1.8.0 and above must be installed on the device
2. Agrident Wedge 2.2.0 and above must be installed on the device

What is a keyboard wedge?
-------------------------

A keyboard wedge is an application that can acquire data and send it directly in the keyboard buffer, just as if it was typed on a virtual keyboard.

Coppernic's wedge applications add a deeper integration capability by using intents too in order to send reader's events (succesful read or read failure).

Using Agrident Wedge as a regular keyboard wedge
-------------------------------------------------------

- Remap the Agrident Wedge application to one (or more) of the 3 programmable buttons of the C-One
- Push the button
- Data will be sent as keyboard entries directly to the system

Using Agrident Wedge with intents
---------------------------------

- For this example, Coppernic Utility library is used. You must declare it in build.gradle.

``` groovy
// At project level
allprojects {
    repositories {
        google()
        jcenter()
        maven { url "https://artifactory.coppernic.fr/artifactory/libs-release" }
    }
}
```

``` groovy
// At module level
implementation(group: 'fr.coppernic.sdk.cpcutils', name: 'CpcUtilsLib', version: '6.13.0', ext: 'aar')
```


- Declare a broadcast receiver in your class, it will receive the intents from the Agrident Wedge application.

``` java
private BroadcastReceiver agridentReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {        
        if (intent.getAction().equals(CpcDefinitions.ACTION_AGRIDENT_SUCCESS)) {
            // Data is available as a String
            String dataRead = intent.getStringExtra(CpcDefinitions.KEY_BARCODE_DATA);           
        } else if (intent.getAction().equals(CpcDefinitions.ACTION_AGRIDENT_ERROR)) {
            // Read failed (main cause is timeout)
        }
    }
};
```

- Register the receiver, for example in onStart

``` java
@Override
protected void onStart() {
    super.onStart();
    // Registers agrident wedge intent receiver
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(CpcDefinitions.ACTION_AGRIDENT_SUCCESS);
    intentFilter.addAction(CpcDefinitions.ACTION_AGRIDENT_ERROR);
    registerReceiver(agridentReceiver, intentFilter);
}    
```

- And unregister it, in onStop for example

``` java
@Override
protected void onStop() {
    // Unregisters agrident wedge receiver
    unregisterReceiver(agridentReceiver);
    super.onStop();
}
```

- Trig a read

```java
private static final String AGRIDENT_WEDGE = "fr.coppernic.tools.cpcagridentwedge";

// Starts Agrident wedge
Intent launchIntent = getPackageManager().getLaunchIntentForPackage(AGRIDENT_WEDGE);
if (launchIntent != null) {
    startActivity(launchIntent);//null pointer check in case package name was not found
}
```

if you don't want to declare CpcUtilsLib in your build, then here are
string values : 

```java
public static final String ACTION_AGRIDENT_SUCCESS = "fr.coppernic.intent.agridentsuccess";
public static final String ACTION_AGRIDENT_ERROR = "fr.coppernic.intent.agridentfailed";
public static final String ACTION_AGRIDENT_SERVICE_STOP = "fr.coppernic.intent.action.stop.agrident.service";
public static final String ACTION_AGRIDENT_SERVICE_START = "fr.coppernic.intent.action.start.agrident.service";
public static final String ACTION_AGRIDENT_READ = "fr.coppernic.tools.agrident.wedge.READ";
public static final String KEY_BARCODE_DATA = "BarcodeData";
```
