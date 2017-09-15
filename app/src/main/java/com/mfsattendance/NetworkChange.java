package com.mfsattendance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Created by Infogird80 on 9/16/2016.
 */
public abstract class NetworkChange extends BroadcastReceiver
{
    public static boolean isConnected = false;

    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        isNetworkAvailable(context);
    }

    private boolean isNetworkAvailable( final Context context)
    {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null)
        {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
            {
                for (int i = 0; i < info.length; i++)
                {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED)
                    {
                        if(!isConnected)
                        {
                            isConnected = true;
                            Log.i("connected", "connected");
                            onNetworkChange();
                        }
                        return true;
                    }
                }
            }
        }
        isConnected = false;
        onNetworkChange();
        Log.i("no internet connection", "no internet connection");
        return false;
    }

    protected abstract void onNetworkChange();
}


