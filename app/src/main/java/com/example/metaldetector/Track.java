package com.example.metaldetector;

import java.util.ArrayList;
import java.util.List;

/**
 * Полноценная GPS-трек-система с точками и метаданными.
 */
public class Track {
    private long id;
    private String name;
    private long startTime;
    private long endTime;
    private List<TrackPoint> points;

    public Track() {
        this.points = new ArrayList<>();
    }

    public Track(String name, long startTime) {
        this.name = name;
        this.startTime = startTime;
        this.points = new ArrayList<>();
    }

    public void addPoint(double lat, double lon, long timestamp, float amplitudeDb, float phase) {
        points.add(new TrackPoint(lat, lon, timestamp, amplitudeDb, phase));
    }

    public void endTrack(long endTime) {
        this.endTime = endTime;
    }

    public int getPointCount() {
        return points.size();
    }

    public double getTotalLengthMeters() {
        double total = 0;
        for (int i = 1; i < points.size(); i++) {
            total += distance(points.get(i - 1), points.get(i));
        }
        return total;
    }

    private double distance(TrackPoint a, TrackPoint b) {
        double latDiff = Math.toRadians(b.getLatitude() - a.getLatitude());
        double lonDiff = Math.toRadians(b.getLongitude() - a.getLongitude());
        double aa = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
                + Math.cos(Math.toRadians(a.getLatitude())) * Math.cos(Math.toRadians(b.getLatitude()))
                * Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(aa), Math.sqrt(1 - aa));
        return 6371000 * c; // Earth radius in meters
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public List<TrackPoint> getPoints() { return points; }
    public void setPoints(List<TrackPoint> points) { this.points = points; }
}
