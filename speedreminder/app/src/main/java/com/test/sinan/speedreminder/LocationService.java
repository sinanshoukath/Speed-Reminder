package com.test.sinan.speedreminder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DecimalFormat;

/**
 * Created by sinan on 31/1/17.
 */

public class LocationService extends Service implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    private static final long INTERVAL = 1000 * 2;
    private static final long FASTEST_INTERVAL = 1000;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location currentLoc;
    Location prevLoc;
    static double distance = 0;
    private static final double EIGHTY_KM = 80.1;

    public static final String PREF_IS_RUNNING = "pref.is.running";
    double speed;


    private final IBinder mBinder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        return mBinder;
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setSmallestDisplacement(0);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onConnected(Bundle bundle) {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }


    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        distance = 0;
    }


    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onLocationChanged(Location location) {
        MainActivity.locate.dismiss();

        if (prevLoc == null) {
            prevLoc = location;
            currentLoc = location;
        } else
            currentLoc = location;
        //Calling the method below updates the  live values of distance and speed to the TextViews.
        updateUI(currentLoc);
        //calculating the speed with getSpeed method it returns speed in m/s so we are converting it into kmph

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public class LocalBinder extends Binder {

        public LocationService getService() {
            return LocationService.this;
        }


    }

    //The live feed of Distance and Speed are being set in the method below .
    private void updateUI(Location currentLoc) {
        distance = distance + (prevLoc.distanceTo(currentLoc) / 1000.00);
        //float distance = currentLoc.distanceTo(prevLoc);

        double timeTaken = ((currentLoc.getTime() - prevLoc.getTime())/1000);

        if (timeTaken > 0) {
            //speed = getAverageSpeed(distance, timeTaken, currentLoc);
            speed = getSpeed(currentLoc, prevLoc);
        }


        if (speed >= 0)
            MainActivity.speed.setText("Current speed: " + new DecimalFormat("#.##").format(speed) + " km/h");
        else
            MainActivity.speed.setText(".......");


        MainActivity.dist.setText(new DecimalFormat("#.###").format(distance) + " km.");

        prevLoc = currentLoc;



        if (speed > EIGHTY_KM) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            Intent launcherIntent = new Intent(this, MainActivity.class);

            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launcherIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Uri sound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.munchausen);

            builder.setContentTitle("Speed Reminder");
            builder.setContentText("Your speed is over 80km/h");

            builder.setPriority(Notification.PRIORITY_HIGH);
            builder.setSound(sound);
            builder.setSmallIcon(R.drawable.reminder);
            builder.setColor(Color.parseColor("#2ebbec"));
            builder.setAutoCancel(true);

            //builder.setContentIntent(contentIntent);
            mNotificationManager.notify(1, builder.build());
        }
    }


    /*private double getAverageSpeed(double distance, double timeTaken, Location location) {
        //float minutes = timeTaken/60;
        //float hours = minutes/60;
        double speed = 0;
        if (location.hasSpeed()) {
            speed = location.getSpeed() * 18 / 5;
        } else {
            if(distance > 0) {
                double distancePerSecond = timeTaken > 0 ? distance/timeTaken : 0;
                double distancePerMinute = distancePerSecond*60;
                double distancePerHour = distancePerMinute*60;
                speed = distancePerHour > 0 ? (distancePerHour/1000) : 0;
            }
        }

        return speed;
    }*/

    private double getSpeed(Location currentLoc, Location prevLoc) {
        double calculatedSpeed = 0;
        if (prevLoc != null) {
            double elapsedTime = (currentLoc.getTime() - prevLoc.getTime()) / 1000; // Convert milliseconds to seconds
            calculatedSpeed = prevLoc.distanceTo(currentLoc) / elapsedTime;
        }

        double speed = currentLoc.hasSpeed() ? currentLoc.getSpeed() : calculatedSpeed;
        speed = speed * 18 / 5;
        return  speed;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopLocationUpdates();
        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
        prevLoc = null;
        currentLoc = null;
        distance = 0;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
