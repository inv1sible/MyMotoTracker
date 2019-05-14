package anony.mouse.mymototracker;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface LocationDao {
    @Query("SELECT COUNT(*) FROM locations")
    LiveData<Integer> getLocationCount();

    @Query("SELECT * FROM locations")
    LiveData<List<LocationEntry>> getAllLocations();

//    @Query("SELECT * FROM locations WHERE uid IN (:deviceIds)")
//    List<LocationEntry> loadAllLocationsByDeviceIds(int[] deviceIds);
//
//    @Query("SELECT * FROM locations WHERE deviceID LIKE :deviceID AND routeID LIKE :routeID")
//    List<LocationEntry> getRoute(String deviceID, String routeID);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(LocationEntry... locations);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LocationEntry location);

    @Query("DELETE FROM locations WHERE routeID IS NULL")
    void clearCache();

    @Query("DELETE FROM locations")
    void clearLocations();

    @Query("UPDATE locations SET routeID = (SELECT CASE WHEN MAX(routeID) IS NULL THEN 0 ELSE (SELECT MAX(routeID) from locations) END + 1 FROM locations) WHERE routeID IS NULL;")
    void saveRoute();

    @Query("SELECT AVG(speed) FROM locations")
    LiveData<Double> getAvgSpeed();

    @Query("SELECT MAX(speed) FROM locations")
    LiveData<Double> getMaxSpeed();

    @Query("SELECT MIN(rolling) FROM locations")
    LiveData<Double> getMinAngle();

    @Query("SELECT MAX(rolling) FROM locations")
    LiveData<Double> getMaxAngle();
}