package anony.mouse.mymototracker;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.String.format;

// https://developer.android.com/guide/topics/location/strategies
// https://developer.android.com/training/permissions/requesting

public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        //     LocationListener,
        SensorEventListener {

    protected static final String EXTRA_MESSAGE = "anony.mouse.mymototracker.MESSAGE";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 0;
    private final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;

    private SensorManager mySensorManager;
    private Sensor myAccelerometer;
    private Sensor myMagnetometer;
    private LocationService myLocationService;
    private float[] myGravity;
    private float[] myAcceleration;
    private float[] myFilteredGravity;
    private float[] myGeomagnetic;
    private LocationManager myLocationManager;
    private ToggleButton myLocationManagerToggleButton;
    private TextView myTextViewTimeTracking;
    private TextView myTextViewLocation;
    private TextView myTextViewSpeed;
    private TextView myTextViewSpeedAvg;
    private TextView myTextViewSpeedMax;
    private TextView myTextViewRollingAngle;
    private TextView myTextViewRollingAngleLeft;
    private TextView myTextViewRollingAngleRight;
    private TextView myTextViewOrientation;
    private TextView myTextViewAcceleration;
    private TextView myTextViewAcceleration3D;
    private TextView myTextViewAccuracy;
    private TextView myTextViewLocationEntries;
    private Location lastLocation;
    private double rollingAngleRaw;
    private double rollingAngleOffset;
    private double rollingAngleCalibrated;
    private List<LocationEntry> wayPoints;
    private float myCurrentSpeed = 0;
    private double myMaxSpeed = 0;
    private double myMinRolling = 0;
    private double myMaxRolling = 0;
    private double myAvgRolling = 0;
    private double myCntRolling = 0;
    private double myAverageSpeed = 0;
    private Long startTimeInMillis;

    protected static double toKMH(double speed) {
        double speedInKMH = speed * 3.6;
        if (Double.isNaN(speedInKMH) || Double.isInfinite(speedInKMH)) {
            speedInKMH = 0d;
        }
        return speedInKMH;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Check if the user revoked runtime permissions.
        if (!checkPermissions()) {
            requestPermissions();
        }

        myAcceleration = new float[]{0, 0, 0};
        myGravity = new float[]{0, 0, 0};
        myFilteredGravity = new float[]{0, 0, 0};
        wayPoints = new ArrayList<LocationEntry>();

        myTextViewTimeTracking = findViewById(R.id.textViewTimeTracking);
        myTextViewLocation = findViewById(R.id.textViewLocation);
        myTextViewSpeed = findViewById(R.id.textViewSpeed);
        myTextViewSpeedAvg = findViewById(R.id.textViewSpeedAvg);
        myTextViewSpeedMax = findViewById(R.id.textViewSpeedMax);
        myTextViewOrientation = findViewById(R.id.textViewOrientation);
        myTextViewAcceleration = findViewById(R.id.textViewAcceleration);
        myTextViewAcceleration3D = findViewById(R.id.textViewAcceleration3D);
        myTextViewAccuracy = findViewById(R.id.textViewAccuracy);
        myTextViewRollingAngle = findViewById(R.id.textViewRollingAngle);
        myTextViewRollingAngleLeft = findViewById(R.id.textViewRollingAngleLeft);
        myTextViewRollingAngleRight = findViewById(R.id.textViewRollingAngleRight);
        myTextViewLocationEntries = findViewById(R.id.textViewLocationCount);

        myLocationManagerToggleButton = findViewById(R.id.toggleButtonLocationManager);
        myLocationManagerToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    enableSensorManager();
                    enableLocationManager();
                    startTimeInMillis = System.currentTimeMillis();
                } else {
                    disableSensorManager();
                    disableLocationManager();
                }
            }
        });

        final Button myClearCacheButton = findViewById(R.id.ButtonClearCache);
        myClearCacheButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LocationRepository locationRepository = new LocationRepository(getApplicationContext());
                locationRepository.clearCache();
                locationRepository.close();
                resetTextViews();
            }
        });

        myClearCacheButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                LocationRepository locationRepository = new LocationRepository(getApplicationContext());
                locationRepository.clearLocations();
                locationRepository.close();
                resetTextViews();
                //updateTextViews();
                Toast toast = Toast.makeText(getApplicationContext(), "All locations deleted", Toast.LENGTH_LONG);
                toast.show();
                return true;
            }
        });

        myTextViewSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAvgMaxSpeedTextViews();
            }
        });

        myTextViewRollingAngle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("myTextViewRollingAngle.onClick", "Offset= " + rollingAngleCalibrated);
                rollingAngleOffset = rollingAngleRaw;
                resetRollingAngleTextViews();
            }
        });

        mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        myAccelerometer = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        myMagnetometer = mySensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        myLocationService = new LocationService();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        switch (requestCode) {
//            case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    // permission was granted, yay!
//                    enableLocationManager();
//                } else {
//                    // permission was revoked, booh!
//                    disableLocationManager();
//                }
//            }
//        }
//    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        Log.d("!!!", s);
        if (s.equals(LocationResultHelper.KEY_LOCATION_UPDATES_RESULT)) {
            //mLocationUpdatesResultView.setText(LocationResultHelper.getSavedLocationResult(this));
        } else if (s.equals(LocationRequestHelper.KEY_LOCATION_UPDATES_REQUESTED)) {
            //updateButtonsState(LocationRequestHelper.getRequesting(this));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String message = "accuracy=" + accuracy;
        Log.d("orientation", message);
    }

    private void resetTextViews() {
        wayPoints.clear();
        myTextViewLocation.setText("");
        myTextViewAccuracy.setText("0 m");
        myTextViewLocationEntries.setText("0 trackpoints");
        resetAvgMaxSpeedTextViews();
        resetRollingAngleTextViews();
    }

    private void resetAvgMaxSpeedTextViews() {
        if (wayPoints.size() > 0) {
            myAverageSpeed = myCurrentSpeed * wayPoints.size();
        } else {
            myAverageSpeed = 0;
        }
        myMaxSpeed = 0;
        myTextViewSpeed.setText("0");
        myTextViewSpeedAvg.setText("0");
        myTextViewSpeedMax.setText("0");
        myTextViewAcceleration.setText("0");
    }

    private void resetRollingAngleTextViews() {
        myMinRolling = 0;
        myMaxRolling = 0;
        myTextViewRollingAngle.setText("0°");
        myTextViewRollingAngleLeft.setText("0°");
        myTextViewRollingAngleRight.setText("0°");
    }

    private void updateTextViews(LocationEntry location) {
        Long currentTimeInMillis = System.currentTimeMillis() - startTimeInMillis;
        int hours = (int) (currentTimeInMillis / (1000 * 60 * 60));
        int mins = (int) (currentTimeInMillis / (1000 * 60)) % 60;
        long secs = (int) (currentTimeInMillis / 1000) % 60;
        myTextViewTimeTracking.setText(String.format("%02d:%02d:%02d", hours, mins, secs));

        myTextViewSpeed.setText(String.format("%.0f", toKMH(location.getSpeed())));
        myTextViewAcceleration.setText(String.format("%.0f", location.getAcceleration()));
        myTextViewAccuracy.setText(String.format("%.2f", location.getAccuracy()) + " m");
        myTextViewRollingAngle.setText(String.format("%.0f", rollingAngleCalibrated) + "°");

        double mySpeed = location.speed;
        double myRolling = location.rolling;
        myAverageSpeed += mySpeed;
        myTextViewSpeedAvg.setText(String.format("%.0f", toKMH(myAverageSpeed / wayPoints.size())));
        //updateAvgSpeed();
        if (mySpeed > myMaxSpeed) {
            myMaxSpeed = mySpeed;
            myTextViewSpeedMax.setText(String.format("%.0f", toKMH(mySpeed)));
        }
        if (myRolling > myMaxRolling) {
            myMaxRolling = myRolling;
            myTextViewRollingAngleRight.setText(String.format("%.0f", myRolling) + "°");
        }
        if (myRolling < myMinRolling) {
            myMinRolling = myRolling;
            myTextViewRollingAngleLeft.setText(String.format("%.0f", Math.abs(myRolling)) + "°");
        }
        myTextViewLocationEntries.setText(wayPoints.size() + " trackpoints");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            myGravity = event.values;

            //https://developer.android.com/guide/topics/sensors/sensors_motion.html#sensors-motion-accel
            final float alpha = 0.8f;

            myFilteredGravity[0] = alpha * myFilteredGravity[0] + (1 - alpha) * event.values[0];
            myFilteredGravity[1] = alpha * myFilteredGravity[1] + (1 - alpha) * event.values[1];
            myFilteredGravity[2] = alpha * myFilteredGravity[2] + (1 - alpha) * event.values[2];

            myAcceleration[0] = event.values[0] - myFilteredGravity[0];
            myAcceleration[1] = event.values[1] - myFilteredGravity[1];
            myAcceleration[2] = event.values[2] - myFilteredGravity[2];

            double Vges = sqrt(myAcceleration[0] * myAcceleration[0]
                    + myAcceleration[1] * myAcceleration[1]
                    + myAcceleration[0] * myAcceleration[0]);

            String message = format(Locale.GERMANY,
                    "Vx=%.2f m/s^2 \nVy=%.2f m/s^2 \nVz=%.2f m/s^2 \nVges=%.2f m/s^2",
                    myAcceleration[0], myAcceleration[1], myAcceleration[2], Vges);
            Log.d("orientation", message);
            myTextViewAcceleration3D.setText(message);

            //myGravity = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            myGeomagnetic = event.values;
        }
        if (myGravity != null && myGeomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, myGravity, myGeomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                double azimuth = toDegrees(orientation[0]);
                double pitch = toDegrees(orientation[1]);
                double roll = toDegrees(orientation[2]);
                myTextViewOrientation.setText(String.format(Locale.GERMANY, "azimuth: %.0f°\npitch: %.0f°\nroll: %.0f°\norientation: na", azimuth, pitch, roll));
                // Calculate rolling angle only when parallel to the ground (0) and -80 degree turned towards viewer
                //if (pitch > -80d && pitch < 0d) {
                rollingAngleRaw = roll;
                rollingAngleCalibrated = roll - rollingAngleOffset;
                if (rollingAngleCalibrated < -90d) {
                    rollingAngleCalibrated = -90d;
                }
                if (rollingAngleCalibrated > 90d) {
                    rollingAngleCalibrated = 90d;
                }
                //} else {
                //    rollingAngleCalibrated = 0d;
                //}
                myAvgRolling += rollingAngleCalibrated;
                myCntRolling += 1;
                String message = format(Locale.GERMANY, "azimuth=%.0f, pitch=%.0f, roll=%.0f", azimuth, pitch, roll);
                Log.d("orientation", message);
            }
        }
    }

    /**
     * Called when the user taps the buttonSaveRoute
     */
    public void onSaveRoute(View view) {
        LocationRepository locationRepository = new LocationRepository(getApplicationContext());
        locationRepository.saveRoute();

    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, LocationUpdatesBroadcastReceiver.class);
        intent.setAction(LocationUpdatesBroadcastReceiver.ACTION_PROCESS_UPDATES);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void enableLocationManager() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_ACCESS_FINE_LOCATION);
            return;
        }
        myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationService);
        myLocationService.start(getPendingIntent());
    }

    private void disableLocationManager() {
        myLocationService.stop();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
//            Snackbar.make(
//                    findViewById(R.id.),
//                    R.string.permission_rationale,
//                    Snackbar.LENGTH_INDEFINITE)
//                    .setAction(R.string.ok, new View.OnClickListener() {
//                        @Override
//                        public void onClick(View view) {
//                            // Request permission
//                            ActivityCompat.requestPermissions(MainActivity.this,
//                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                                    REQUEST_PERMISSIONS_REQUEST_CODE);
//                        }
//                    })
//                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted. Kick off the process of building and connecting
                // GoogleApiClient.
                //buildGoogleApiClient();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
//                Snackbar.make(
//                        findViewById(R.id.activity_main),
//                        R.string.permission_denied_explanation,
//                        Snackbar.LENGTH_INDEFINITE)
//                        .setAction(R.string.settings, new View.OnClickListener() {
//                            @Override
//                            public void onClick(View view) {
//                                // Build intent that displays the App settings screen.
//                                Intent intent = new Intent();
//                                intent.setAction(
//                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                                Uri uri = Uri.fromParts("package",
//                                        BuildConfig.APPLICATION_ID, null);
//                                intent.setData(uri);
//                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                startActivity(intent);
//                            }
//                        })
//                        .show();
            }
        }
    }

    private void enableSensorManager() {
        mySensorManager.registerListener(this, myAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mySensorManager.registerListener(this, myMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void disableSensorManager() {
        mySensorManager.unregisterListener(this);
    }

    /**
     * Called when the user taps the ShowMapsActivity button
     */
    public void onShowMapsActivity(View view) {
        Intent intent = new Intent(this, TracksActivity.class);
//        EditText editText = (EditText) findViewById(R.id.editText);
//        String message = editText.getText().toString();
//        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
