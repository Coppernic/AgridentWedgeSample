package fr.coppernic.samples.agridentwedge;

import android.app.Application;

import timber.log.Timber;

class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}
