package anony.mouse.mymototracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.lang.String.format;

public class TracksActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private TextView waypointInfoText;
    private GoogleMap mMap;
    private MapFragment mapView;
    private SeekBar seekBar;
    private PolylineOptions polylineOptions;
    private List<LatLng> trackpoints;
    private List<LocationEntry> waypoints;
    private Marker currentWaypoint;

    public TracksActivity() {
        trackpoints = new ArrayList<LatLng>();
        polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.RED);
        polylineOptions.width(10);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        checkLocationPermission();

        mapView = (MapFragment) getFragmentManager().findFragmentById(R.id.mapView);
        mapView.getMapAsync(this);

        waypointInfoText = findViewById(R.id.textViewWayPointInfo);

        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Toast.makeText(getApplicationContext(), String.valueOf(progress), Toast.LENGTH_LONG).show();
                if (trackpoints.size() > progress) {
                    showWayPoint(progress);
                }
            }
        });
    }

    private void showWayPoint(int position) {
        if (currentWaypoint != null) {
            currentWaypoint.remove();
        }
        LocationEntry location = waypoints.get(position);
        LatLng point = location.getLatLng();
        String pattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String date = simpleDateFormat.format(location.getTimestamp());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.title(date);
        String waypointText = format(Locale.GERMANY, "%.4f, %.4f, %.0fkm/h, %.0fkm/h, %.0f°",
                point.latitude, point.latitude,
                MainActivity.toKMH(location.getSpeed()), MainActivity.toKMH(location.getAcceleration()),
                location.getRollingAngle());
        markerOptions.snippet(waypointText);
        currentWaypoint = mMap.addMarker(markerOptions);
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, mMap.getCameraPosition().zoom));
        String textViewMessage = format(Locale.GERMANY, "%s\n%.6f, %.6f\n%.0fkm/h, %.0fkm/h, %.0f°",
                date, point.latitude, point.longitude,
                MainActivity.toKMH(location.getSpeed()), MainActivity.toKMH(location.getAcceleration()),
                location.getRollingAngle());
        waypointInfoText.setText(textViewMessage);
    }

    private void addTrackPoints() {
        if (waypoints.size() > 0) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LocationEntry point : waypoints) {
                trackpoints.add(point.getLatLng());
                builder.include(point.getLatLng());
            }
            polylineOptions.addAll(trackpoints);
            mMap.addPolyline(polylineOptions);
            LatLngBounds bounds = builder.build();
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.10);
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
            mMap.moveCamera(cu);
        }
    }

    private void addTrackPoints(LatLngBounds bounds) {
        if (waypoints.size() > 0) {
            mMap.clear();
            trackpoints.clear();
            for (LocationEntry point : waypoints) {
                if (bounds.contains(point.getLatLng())) {
                    trackpoints.add(point.getLatLng());
                }
            }
            polylineOptions.addAll(trackpoints);
            seekBar.setMax(trackpoints.size());
            mMap.addPolyline(polylineOptions);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(GoogleMap gMap) {
        mMap = gMap;

        UiSettings uiSettings = mMap.getUiSettings();
        uiSettings.setIndoorLevelPickerEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setZoomControlsEnabled(true);

        GoogleMap.OnCameraIdleListener onCameraIdleListener = new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                if (waypoints != null) {
                    //addTrackPoints(bounds);
                }
            }
        };
        mMap.setOnCameraIdleListener(onCameraIdleListener);

        mapView.onResume();
        onLoadPoints(null);
    }

    public void onLoadPoints(View view) {
        LocationRepository locationRepository = new LocationRepository(getApplicationContext());
        locationRepository.getAllLocations().observe(this,
                new Observer<List<LocationEntry>>() {
                    @Override
                    public void onChanged(@Nullable List<LocationEntry> locationEntries) {
                        waypoints = locationEntries;
                        seekBar.setMax(locationEntries.size());
                        mMap.clear();
                        addTrackPoints();
                        onNextWayPointClick(null);
                    }
                });
    }

    public void onPreviousWayPointClick(View view) {
        int progress = seekBar.getProgress();
        if (progress < 1) {
            progress = 1;
        }
        seekBar.setProgress(progress - 1);
    }

    public void onNextWayPointClick(View view) {
        int progress = seekBar.getProgress();
        if (progress > seekBar.getMax()) {
            progress = seekBar.getMax() - 1;
        }
        seekBar.setProgress(progress + 1);
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                /*
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();

                */
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

}
