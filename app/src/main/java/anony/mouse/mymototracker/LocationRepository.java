package anony.mouse.mymototracker;

import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.os.AsyncTask;

public class LocationRepository {

    private String DB_NAME = "cache.db";

    private LocationDatabase locationDatabase;

    public LocationRepository(Context context) {
        locationDatabase = Room.databaseBuilder(context, LocationDatabase.class, DB_NAME).build();
    }

    @SuppressLint("StaticFieldLeak")
    public void insertLocation(final LocationEntry locationEntry) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                locationDatabase.daoAccess().insert(locationEntry);
                return null;
            }
        }.execute();
    }

    public LiveData<Integer> getLocationsCount(){
        return locationDatabase.daoAccess().getLocationCount();
    }

    @SuppressLint("StaticFieldLeak")
    public void clearCache() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                locationDatabase.daoAccess().clearCache();
                return null;
            }
        }.execute();
    }

    public LiveData<Double> getMaxAngle() {
        return locationDatabase.daoAccess().getMaxAngle();
    }

    public LiveData<Double> getMinAngle() {
        return locationDatabase.daoAccess().getMinAngle();
    }

    public LiveData<Double> getMaxSpeed() {
        return locationDatabase.daoAccess().getMaxSpeed();
    }

    public LiveData<Double> getAvgSpeed() {
        return locationDatabase.daoAccess().getAvgSpeed();
    }

    public void close() {
        if(locationDatabase != null){ locationDatabase.close(); }
    }
}
