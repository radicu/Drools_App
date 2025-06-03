package com.radicu.ruleengine.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("Threshold")
public class Threshold {
    @Id
    private String id;
    private int spindleId;
    private String ruleType;
    private float accThreshold;
    private float meanThreshold;

    // Constructors, Getters, Setters
    public Threshold() {}
    
    public Threshold(int spindleId, String ruleType, float accThreshold, float meanThreshold) {
        this.spindleId = spindleId;
        this.ruleType = ruleType;
        this.accThreshold = accThreshold;
        this.meanThreshold = meanThreshold;
    }
    
    // Getters and Setters
}