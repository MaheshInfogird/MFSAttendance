package com.mfsattendance;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.List;
import java.util.Locale;

/**
 * Created by infogird128 on 25/01/2017.
 */

public class GPSTracker extends Service
{
    private Context mContext;
    String CompleteAddress;

    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean canGetLocation = false;

    Location location;
    double latitude;
    double longitude;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1000; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute
    protected LocationManager locationManager;
    Activity activity;

    public GPSTracker()
    {
    }

    public GPSTracker(Context context, Activity activity)
    {
        this.mContext = context;
        this.activity = activity;
        getLocation();
        location_addtess();
    }

    public Location getLocation()
    {
        try
        {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled)
            {
                // No network provider is enabled
            }
            else
            {
                this.canGetLocation = true;
                if (isNetworkEnabled)
                {
                    Log.d("Network_Net", "Network_Net");
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, mLocationListener);
                    Log.d("Network_ntwrk", "Network_ntwrk");
                    if (locationManager != null)
                    {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        Log.d("location_ntwrk", ""+location);

                        if (location != null)
                        {
                            latitude = location.getLatitude();
                            Log.d("latitude_ntwrk", ""+latitude);
                            longitude = location.getLongitude();
                            Log.d("longitude_ntwrk", ""+longitude);
                        }
                    }
                }
            }

            if (isGPSEnabled)
            {
                if (location == null)
                {
                    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                    {
                        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 50);
                    }
                    else
                    {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, mLocationListener);
                        Log.d("GPS Enabled", "GPS Enabled");

                        if (locationManager != null)
                        {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            Log.d("location_gps", ""+location);
                            if (location != null)
                            {
                                latitude = location.getLatitude();
                                Log.d("latitude_gps", ""+latitude);
                                longitude = location.getLongitude();
                                Log.d("longitude_gps", ""+longitude);
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return location;
    }

    public String location_addtess()
    {
        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
        String result = null;
        try
        {
            List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
            String address = addressList.get(0).getAddressLine(0);
            Log.i("address", address);
            String area = addressList.get(0).getAddressLine(1);
            Log.i("area", area);
            String city = addressList.get(0).getAddressLine(2);
            Log.i("city", city);
            CompleteAddress = address + ", " + area;
            Log.i("CompleteAddress1",""+address+area);
            Log.i("CompleteAddress",""+CompleteAddress);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return  CompleteAddress;
    }

    private final LocationListener mLocationListener = new LocationListener()
    {
        @Override
        public void onLocationChanged(final Location location)
        {
            if (location != null)
            {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }
        return latitude;
    }

    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }
        return longitude;
    }

    public String getlocation_Address()
    {
        return CompleteAddress;
    }

    public boolean canGetLocation()
    {
        return this.canGetLocation;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
