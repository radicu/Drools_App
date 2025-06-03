package com.radicu.ruleengine.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document("Threshold_Timely")
public class ThresholdTimely{
    @Id
    private String id;
    private int spindleId;
    private String ruleType;
    private float accThreshold;
    private float meanThreshold;
    private Date timestamp;

    public ThresholdTimely() {
        this.timestamp = new Date();
    }

    public ThresholdTimely(int spindleId, String ruleType, 
                           float accThreshold, float meanThreshold) {
        this();
        this.spindleId = spindleId;
        this.ruleType = ruleType;
        this.accThreshold = accThreshold;
        this.meanThreshold = meanThreshold;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getSpindleId() { return spindleId; }
    public void setSpindleId(int spindleId) { this.spindleId = spindleId; }
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    public float getAccThreshold() { return accThreshold; }
    public void setAccThreshold(float accThreshold) { this.accThreshold = accThreshold; }
    public float getMeanThreshold() { return meanThreshold; }
    public void setMeanThreshold(float meanThreshold) { this.meanThreshold = meanThreshold; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}