package me.Lazinq.DeathLocation.objects;

import org.bukkit.Location;

public class Data {

    private Location location;
    private String reason;
    private Long time;

    public Data(Location location, String reason, Long time) {
        this.location = location;
        this.reason = reason;
        this.time = time;
    }

    public Location getLocation(){
        return location;
    }
    public String getReason(){
        return reason;
    }
    public Long getTime(){
        return time;
    }
}