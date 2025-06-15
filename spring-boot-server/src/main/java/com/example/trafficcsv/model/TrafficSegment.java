package com.example.trafficcsv.model;

public class TrafficSegment {

    private String id;
    private String time;
    private String locationDescription;
    private double length;
    private double speed;
    private double speedUncapped;
    private double freeFlow;
    private double jamFactor;
    private double confidence;
    private String traversability;

    public TrafficSegment() {}

    public TrafficSegment(
        String id,
        String time,
        String locationDescription,
        double length,
        double speed,
        double speedUncapped,
        double freeFlow,
        double jamFactor,
        double confidence,
        String traversability
    ) {
        this.id = id;
        this.time = time;
        this.locationDescription = locationDescription;
        this.length = length;
        this.speed = speed;
        this.speedUncapped = speedUncapped;
        this.freeFlow = freeFlow;
        this.jamFactor = jamFactor;
        this.confidence = confidence;
        this.traversability = traversability;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getLocationDescription() { return locationDescription; }
    public void setLocationDescription(String locationDescription) { this.locationDescription = locationDescription; }
    public double getLength() { return length; }
    public void setLength(double length) { this.length = length; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public double getSpeedUncapped() { return speedUncapped; }
    public void setSpeedUncapped(double speedUncapped) { this.speedUncapped = speedUncapped; }
    public double getFreeFlow() { return freeFlow; }
    public void setFreeFlow(double freeFlow) { this.freeFlow = freeFlow; }
    public double getJamFactor() { return jamFactor; }
    public void setJamFactor(double jamFactor) { this.jamFactor = jamFactor; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public String getTraversability() { return traversability; }
    public void setTraversability(String traversability) { this.traversability = traversability; }
}