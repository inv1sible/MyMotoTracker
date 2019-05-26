package anony.mouse.mymototracker;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.location.LocationResult;

import java.text.DateFormat;
import java.util.List;
import java.util.Locale;

import static anony.mouse.mymototracker.MainActivity.toKMH;
import static java.lang.String.format;

class LocationService extends IntentService implements android.location.LocationListener {

    static final String ACTION_PROCESS_UPDATES =
            "com.google.android.gms.location.sample.backgroundlocationupdates.action" +
                    ".PROCESS_UPDATES";
    private static final String TAG = LocationService.class.getSimpleName();
    private final LocationManager myLocationManager;

    private float myCurrentSpeed;
    private Location lastLocation;

    public LocationService() {
        super(TAG);
        myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }

    public void start(PendingIntent intent) {

    }

    public void stop() {
        myLocationManager.removeUpdates(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PROCESS_UPDATES.equals(action)) {
                LocationResult result = LocationResult.extractResult(intent);
                if (result != null) {
                    List<Location> locations = result.getLocations();
                    LocationResultHelper locationResultHelper = new LocationResultHelper(this,
                            locations);
                    // Save the location data to SharedPreferences.
                    locationResultHelper.saveResults();
                    // Show notification with the location data.
                    locationResultHelper.showNotification();
                    Log.i(TAG, LocationResultHelper.getSavedLocationResult(this));
                }
            }
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        myCurrentSpeed = location.getSpeed();
        long timestamp = location.getTime();
        String strTimestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(timestamp);
        float direction = location.getBearing();
        float accuracy = location.getAccuracy();
        float acceleration = 0;
        if (lastLocation != null) {
            acceleration = (myCurrentSpeed - lastLocation.getSpeed()) / ((location.getTime() - lastLocation.getTime()) / 1000);
        }
        if (Float.isInfinite(acceleration) || Float.isNaN(acceleration)) {
            acceleration = 0;
        }
        String message = format(Locale.GERMANY,
                "%s: lat=%.2f, lon=%.2f, \nspeed=%.2f km/h, accel=%.2f m/s^2, \ndir=%.2f, acc=%.2f m",
                strTimestamp,
                latitude,
                longitude,
                toKMH(myCurrentSpeed),
                acceleration,
                direction,
                accuracy);
        Log.d("location", message);

            /* LocationEntry locationEntry = new LocationEntry(timestamp, latitude, longitude,
                    myCurrentSpeed, acceleration, myAcceleration[0], myAcceleration[1], myAcceleration[2],
                    accuracy, (myAvgRolling / myCntRolling));

            // only record locations if speed is greater than 2 km/h
            if (toKMH(myCurrentSpeed) >= 2) {
                lastLocation = location;
                LocationRepository locationRepository = new LocationRepository(getApplicationContext());
                wayPoints.add(locationEntry);
                locationRepository.insertLocation(locationEntry);
                locationRepository.close();
            }
            myAvgRolling = 0;
            myCntRolling = 0;
            updateTextViews(locationEntry);
            */
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        String message = format(Locale.GERMANY, "provider=%s, status=%d", provider, status);
        Log.d("location", message);
    }

    @Override
    public void onProviderEnabled(String provider) {
        String message = format("provider %s enabled", provider);
        Log.d("location", message);
    }

    @Override
    public void onProviderDisabled(String provider) {
        String message = format("provider %s disabled", provider);
        Log.d("location", message);
    }
}
