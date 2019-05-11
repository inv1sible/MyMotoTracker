package anony.mouse.mymototracker;

import android.arch.persistence.room.Room;
import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LocationDatabaseTest {
    private LocationDao locationDao;
    private LocationDatabase db;
    private Context ApplicationProvider;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, LocationDatabase.class).build();
        locationDao = db.daoAccess();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void writeLocationAndReadInList() throws Exception {
        LocationEntry l1 = new LocationEntry(1556811407000L,
                50.32122544, 7.60364487,
                40.3800010681152,2.81000137329102,
                3.21600008010864, -1.17695557277497);
        LocationEntry l2 = new LocationEntry(1556811865000L,
                50.30278353, 7.62184339,
                18.3299999237061,0.340000152587891,
                3.21600008010864, 7.59759934307777);
        LocationEntry l3 = new LocationEntry(1556811948000L,
                50.32504965, 7.60518213,
                30.5799999237061, 0.309999465942383,
                3.21600008010864, 1.40544279614994);
        locationDao.insert(l1);
        locationDao.insert(l2);
        locationDao.insert(l3);
        List<LocationEntry> locationEntryList = locationDao.getAllLocations();
        assertEquals(locationEntryList.size(), 3);
        assertEquals(locationDao.getMinAngle(), new Double(-1.17695557277497));
        assertEquals(locationDao.getMaxAngle(), new Double(7.59759934307777));
        assertEquals(locationDao.getAvgSpeed(), new Double(40.3800010681152));
        assertEquals(locationDao.getMaxSpeed(), new Double(40.3800010681152));

    }
}