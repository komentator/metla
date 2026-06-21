package com.example.metaldetector;

/**
 * Точка GPS-трека с амплитудой и фазой.
 */
public class TrackPoint {
    private double latitude;
    private double longitude;
    private long timestamp;
    private float amplitudeDb;
    private float phase;

    public TrackPoint() {}

    public TrackPoint(double latitude, double longitude, long timestamp, float amplitudeDb, float phase) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.amplitudeDb = amplitudeDb;
        this.phase = phase;
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public float getAmplitudeDb() { return amplitudeDb; }
    public void setAmplitudeDb(float amplitudeDb) { this.amplitudeDb = amplitudeDb; }
    public float getPhase() { return phase; }
    public void setPhase(float phase) { this.phase = phase; }
}
