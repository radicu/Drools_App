package com.radicu.ruleengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SensorStressData {
    private double temperature;
    private double pressure;
    private double vibration;
    private double rpm;
    private String status;

    // Getters and Setters

    @JsonProperty("temperature")
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    
    @JsonProperty("pressure")
    public double getPressure() { return pressure; }
    public void setPressure(double pressure) { this.pressure = pressure; }
    
    @JsonProperty("vibration")
    public double getVibration() { return vibration; }
    public void setVibration(double vibration) { this.vibration = vibration; }
    
    @JsonProperty("rpm")
    public double getRpm() { return rpm; }
    public void setRpm(double rpm) { this.rpm = rpm; }
    
    @JsonProperty("status")
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
