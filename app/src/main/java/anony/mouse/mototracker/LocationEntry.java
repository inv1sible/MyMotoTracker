package anony.mouse.mototracker;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

class OBDInfos {
    public Double machineSpeed;
    public Double machinerpm;
    public Double machinethrottle;
    public Double machineload;
}

@Entity(tableName = "locations")
public class LocationEntry {
    @PrimaryKey(autoGenerate = true)
    public int uid;
    public String routeID;
    public Long timestamp;
    public Double latitude;
    public Double longitude;
    public Double speed;
    public Double acceleration;
    public Double accuracy;
    public Double rolling;
    public String deviceID;

    @Embedded
    public OBDInfos obdInfos;

    public LocationEntry(long timestamp, double latitude, double longitude,
                              double speed, double acceleration,
                              double accuracy, double rolling){
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.acceleration = acceleration;
        this.accuracy = accuracy;
        this.rolling = rolling;
        this.deviceID = deviceID;
        obdInfos.machineSpeed = null;
        obdInfos.machinerpm = null;
        obdInfos.machinethrottle = null;
        obdInfos.machineload = null;
    }
}

