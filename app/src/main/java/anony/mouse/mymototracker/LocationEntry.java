package anony.mouse.mymototracker;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

class OBDInfos {
    public float machineSpeed;
    public float machinerpm;
    public float machinethrottle;
    public float machineload;

    public OBDInfos(float machineSpeed, float machinerpm, float machinethrottle, float machineload) {
        this.machineSpeed = machineSpeed;
        this.machinerpm = machinerpm;
        this.machinethrottle = machinethrottle;
        this.machineload = machineload;
    }
}

@Entity(tableName = "locations")
public class LocationEntry {
    @PrimaryKey(autoGenerate = true)
    public int uid;
    public String routeID;
    public Long timestamp;
    public double latitude;
    public double longitude;
    public float speed;
    public float acceleration;
    public float accelerationX;
    public float accelerationY;
    public float accelerationZ;
    public float accuracy;
    public double rolling;

    @Embedded
    public OBDInfos obdInfos;

    public LocationEntry(Long timestamp, double latitude, double longitude,
                         float speed, float acceleration, float accelerationX,
                         float accelerationY, float accelerationZ,
                         float accuracy, double rolling) {
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.acceleration = acceleration;
        this.accelerationX = accelerationX;
        this.accelerationY = accelerationY;
        this.accelerationZ = accelerationZ;
        this.accuracy = accuracy;
        this.rolling = rolling;
        OBDInfos obdInfos = new OBDInfos(0, 0, 0, 0);
    }

    public LatLng getLatLng() {
        return new LatLng(this.latitude, this.longitude);
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public double latitude() {
        return this.latitude;
    }

    public double longitude() {
        return this.longitude;
    }

    public float getSpeed() {
        return this.speed;
    }

    public float getAcceleration() {
        return this.acceleration;
    }

    public float getAccelerationX() {
        return this.accelerationX;
    }

    public float getAccelerationY() {
        return this.accelerationY;
    }

    public float getAccelerationZ() {
        return this.accelerationZ;
    }

    public float getAccuracy() {
        return this.accuracy;
    }

    public double getRollingAngle() {
        return this.rolling;
    }
}

