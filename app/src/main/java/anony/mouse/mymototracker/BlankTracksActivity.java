package anony.mouse.mymototracker;

import android.app.ListActivity;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;


public class BlankTracksActivity extends AppCompatActivity {

    private List<String> myTracks = new ArrayList<String>();
    private BlankTracksActivity myContext;

    public BlankTracksActivity() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_tracks);

        myContext = this;

        final ListView myListView = (ListView) findViewById(R.id.list);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        LocationRepository locationRepository = new LocationRepository(getApplicationContext());
        locationRepository.getTracks().observe((LifecycleOwner) this,
                new Observer<List<LocationEntry>>() {

                    @Override
                    public void onChanged(@Nullable List<LocationEntry> tracks) {
                        for (LocationEntry track : tracks){
                            long timestamp = track.timestamp;
                            String strTimestamp = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(timestamp);
                            myTracks.add(track.routeID + ", " + strTimestamp);

                            ListAdapter myArrayAdapter = new ArrayAdapter<String>(
                                    myContext,
                                    R.layout.fragment_tracks,
                                    myTracks);

                            // Bind to our new adapter.
                            myListView.setAdapter(myArrayAdapter);
                        }
                    }
                });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }


}
