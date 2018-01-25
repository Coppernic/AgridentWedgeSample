# Agrident Wedge Sample

Introduction
------------

This application demonstrates how to use the Agrident Wedge application on a C-One with Agrident RFID reader.

Prerequisites
-------------

1. CpcSystemServices version 2.2.0 and above must be installed on the device
2. Aghrident Wedge 2.0.0 and above must be installed on the device

What is a keyboard wedge?
-------------------------

A keyboard wedge is an application that can acquire data and send it directlyu in the keyboard buffer, just as if it was typed on a virtual keyboard.

Coppernic's wedge applications add a deeper integration capability by using intents too in order to send reader's events (succesful read or read failure).

Using Agrident Wedge as a regular keyboard wedge
-------------------------------------------------------

- Remap the Agrident Wedge application to one (or more) of the 3 programmable buttons of the C-One
- Push the button
- Data will be sent as keyboard entries directly to the system

Using Agrident Wedge with intents
---------------------------------

- Declare a broadcast receiver in your class, it will receive the intents from the Agrident Wedge application.

``` groovy
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

``` groovy
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

``` groovy
@Override
protected void onStop() {
    // Unregisters agrident wedge receiver
    unregisterReceiver(agridentReceiver);
    super.onStop();
}
```

That's it!
