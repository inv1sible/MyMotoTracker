package anony.mouse.mototracker;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.String.format;

// https://developer.android.com/guide/topics/location/strategies
// https://developer.android.com/training/permissions/requesting

public class MainActivity extends AppCompatActivity implements LocationListener, SensorEventListener {

    private final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;
    private SensorManager mySensorManager;
    private Sensor myAccelerometer;
    private Sensor myMagnetometer;
    private float[] myGravity;
    private float[] myFilteredGravity;
    private float[] myGeomagnetic;
    private LocationManager myLocationManager;
    private TextView myTextViewLocation;
    private TextView myTextViewSpeed;
    private TextView myTextViewSpeedAvg;
    private TextView myTextViewSpeedMax;
    private TextView myTextViewRollingAngle;
    private TextView myTextViewRollingAngleLeft;
    private TextView myTextViewRollingAngleRight;
    private TextView myTextViewAcceleration;
    private TextView myTextViewAcceleration3D;
    private TextView myTextViewAccuracy;
    private TextView myTextViewLocationEntries;
    private Location lastLocation;
    private double rollingAngleRaw;
    private double rollingAngleOffset;
    private double rollingAngleCalibrated;

    Logger logger;
    FileHandler fh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        logger = Logger.getLogger("MyLog");
        try {
            String externalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
            File loggingDir = new File(externalStorage + File.separator + "Logs" );
            if(!loggingDir.exists()){
                loggingDir.mkdir();
            }
            SimpleDateFormat format = new SimpleDateFormat("YYYYMMdd_HHmmss");
            String fileName = "MyMotoTracker"+ format.format(Calendar.getInstance().getTime())+".log";
            fh = new FileHandler(loggingDir + File.separator + fileName);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            logger.setUseParentHandlers(false);
            logger.warning("Test");
        } catch (IOException e) {
            e.printStackTrace();
        }

        myGravity = new float[]{0, 0, 0};
        myFilteredGravity = new float[]{0, 0, 0};

        myTextViewLocation = findViewById(R.id.textViewLocation);
        myTextViewSpeed = findViewById(R.id.textViewSpeed);
        myTextViewSpeedAvg = findViewById(R.id.textViewSpeedAvg);
        myTextViewSpeedMax = findViewById(R.id.textViewSpeedMax);
        myTextViewAcceleration = findViewById(R.id.textViewAcceleration);
        myTextViewAcceleration3D = findViewById(R.id.textViewAcceleration3D);
        myTextViewAccuracy = findViewById(R.id.textViewAccuracy);
        myTextViewRollingAngle = findViewById(R.id.textViewRollingAngle);
        myTextViewRollingAngleLeft = findViewById(R.id.textViewRollingAngleLeft);
        myTextViewRollingAngleRight = findViewById(R.id.textViewRollingAngleRight);
        myTextViewLocationEntries = findViewById(R.id.textViewLocationCount);

        final ToggleButton myLocationManagerToggleButton = findViewById(R.id.toggleButtonLocationManager);
        myLocationManagerToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    enableSensorManager();
                    enableLocationManager();
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
                myTextViewLocation.setText("");
                myTextViewSpeed.setText("0");
                myTextViewSpeedAvg.setText("0");
                myTextViewSpeedMax.setText("0");
                myTextViewAcceleration.setText("0");
                myTextViewAccuracy.setText("0m");
                myTextViewRollingAngle.setText("0°");
                myTextViewRollingAngleLeft.setText("0°");
                myTextViewRollingAngleRight.setText("0°");
                myTextViewLocationEntries.setText("0 entries in cache.db");
            }
        });

        myTextViewRollingAngle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("myTextViewRollingAngle.onClick", "Offset= " + rollingAngleCalibrated);
                rollingAngleOffset = rollingAngleRaw;
            }
        });

        mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        myAccelerometer = mySensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        myMagnetometer = mySensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        myLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void enableLocationManager() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_ACCESS_FINE_LOCATION);
            return;
        }
        myLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    private void disableLocationManager() {
        myLocationManager.removeUpdates(this);
    }

    private void enableSensorManager() {
        mySensorManager.registerListener(this, myAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mySensorManager.registerListener(this, myMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void disableSensorManager() {
        mySensorManager.unregisterListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    enableLocationManager();
                } else {
                    // permission was revoked, booh!
                    disableLocationManager();
                }
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            myGravity = event.values;

            final float alpha = 0.8f;

            myFilteredGravity[0] = alpha * myFilteredGravity[0] + (1 - alpha) * event.values[0];
            myFilteredGravity[1] = alpha * myFilteredGravity[1] + (1 - alpha) * event.values[1];
            myFilteredGravity[2] = alpha * myFilteredGravity[2] + (1 - alpha) * event.values[2];

            double Vx = event.values[0] - myFilteredGravity[0];
            double Vy = event.values[1] - myFilteredGravity[1];
            double Vz = event.values[2] - myFilteredGravity[2];

            double Vges = sqrt(Vx * Vx + Vy * Vy + Vz * Vz);

            String message = format(Locale.GERMANY,
                    "Vx=%.2f m/s \nVy=%.2f m/s \nVz=%.2f m/s \nVges=%.2f m/s^2",
                    Vx, Vy, Vz, Vges);
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
                rollingAngleRaw = roll;
                rollingAngleCalibrated = roll - rollingAngleOffset;
                String message = format(Locale.GERMANY, "azimuth=%.0f, pitch=%.0f, roll=%.0f", azimuth, pitch, roll);
                Log.d("orientation", message);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        String message = "accuracy=" + accuracy;
        Log.d("orientation", message);
    }

    @Override
    public void onLocationChanged(Location location) {
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        double speed = location.getSpeed();
        long timestamp = location.getTime();
        String strTimestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(timestamp);
        double direction = location.getBearing();
        double accuracy = location.getAccuracy();
        double acceleration = 0d;
        if (lastLocation != null) {
            acceleration = (speed - lastLocation.getSpeed()) / ((location.getTime() - lastLocation.getTime()) / 1000d);
        }
        String message = format(Locale.GERMANY, "%s: lat=%.2f, lon=%.2f, \nspeed=%.2f km/h, accel=%.2f m/s^2, \ndir=%.2f, acc=%.2f m",
                strTimestamp,
                latitude,
                longitude,
                toKMH(speed),
                acceleration,
                direction,
                accuracy);
        //Log.d("location", message);
        myTextViewLocation.setText(message);
        myTextViewSpeed.setText(String.format("%.0f", toKMH(speed)));
        // myTextViewSpeedMax needs to be observed from DB (see below)
        myTextViewAcceleration.setText(String.format("%.0f", acceleration));
        myTextViewAccuracy.setText(String.format("%.2f", accuracy) + " m");
        myTextViewRollingAngle.setText(String.format("%.1f", rollingAngleCalibrated) + "°");
        // myTextViewRollingAngleLeft and -Right need to be observed from DB (see below)
        lastLocation = location;
        LocationEntry locationEntry = new LocationEntry(timestamp, latitude, longitude,
                speed, acceleration, accuracy, rollingAngleCalibrated);
        try {
            LocationRepository locationRepository = new LocationRepository(getApplicationContext());
            locationRepository.insertLocation(locationEntry);
            locationRepository.getLocationsCount().observe(this,
                    new Observer<Integer>() {
                        @Override
                        public void onChanged(Integer count) {
                            myTextViewLocationEntries.setText(count + " entries in cache.db");
                        }
                    });
            locationRepository.getAvgSpeed().observe(this,
                    new Observer<Double>() {
                        @Override
                        public void onChanged(Double speed) {
                            if (speed != null) {
                                myTextViewSpeedAvg.setText(String.format("%.0f", toKMH(speed)));
                            }
                        }
                    });
            locationRepository.getMaxSpeed().observe(this,
                    new Observer<Double>() {
                        @Override
                        public void onChanged(Double speed) {
                            if (speed != null) {
                                myTextViewSpeedMax.setText(String.format("%.0f", toKMH(speed)));
                            }
                        }
                    });
            locationRepository.getMinAngle().observe(this,
                    new Observer<Double>() {
                        @Override
                        public void onChanged(Double angle) {
                            if (angle != null) {
                                myTextViewRollingAngleLeft.setText(String.format("%.0f°", Math.abs(angle)));
                            }
                        }
                    });
            locationRepository.getMaxAngle().observe(this,
                    new Observer<Double>() {
                        @Override
                        public void onChanged(Double angle) {
                            if (angle != null) {
                                myTextViewRollingAngleRight.setText(String.format("%.0f°", angle));
                            }
                        }
                    });
        }catch (Exception e){
            Context context = getApplicationContext();
            String text = "Exception: " + e.getLocalizedMessage();
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

            logger.warning(text);
        }
    }

    private double toKMH(double speed) {
        double speedInKMH = speed * 3.6;
        if (Double.isNaN(speedInKMH) || Double.isInfinite(speedInKMH)) {
            speedInKMH = 0d;
        }
        return speedInKMH;
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
