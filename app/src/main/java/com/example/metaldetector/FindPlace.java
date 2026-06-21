package com.example.metaldetector;

public class FindPlace {
    private long id;
    private String title;
    private double latitude;
    private double longitude;
    private long timestamp;
    private float amplitudeDb;
    private float phase;
    private float iValue;
    private float qValue;
    private float rxLevel;

    public FindPlace() {}

    public FindPlace(String title, double latitude, double longitude, long timestamp,
                     float amplitudeDb, float phase, float iValue, float qValue, float rxLevel) {
        this.title = title;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.amplitudeDb = amplitudeDb;
        this.phase = phase;
        this.iValue = iValue;
        this.qValue = qValue;
        this.rxLevel = rxLevel;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
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
    public float getIValue() { return iValue; }
    public void setIValue(float iValue) { this.iValue = iValue; }
    public float getQValue() { return qValue; }
    public void setQValue(float qValue) { this.qValue = qValue; }
    public float getRxLevel() { return rxLevel; }
    public void setRxLevel(float rxLevel) { this.rxLevel = rxLevel; }
}
