package com.tezos.ui.utils;

import android.app.Application;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.lifecycle.ProcessLifecycleOwner;

/**
 * Created by nfillion on 3/5/18.
 */

public class ArchLifecycleApp extends Application implements LifecycleObserver
{
    private boolean started;

    public boolean isStarted()
    {
        return started;
    }

    private void setStarted(boolean started)
   {
        this.started = started;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void connectListener()
    {
        setStarted(false);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void disconnectListener()
    {
        setStarted(false);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void createSomething()
    {
        setStarted(false);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void stopSomething()
    {
        setStarted(false);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void startSomething()
    {
        setStarted(true);
    }
}
