package com.radicu.ruleengine.model;

import com.radicu.ruleengine.service.MongoService;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Spindle {
    private int id;
    private String status = "OPERATIONAL";
    private float rpm;
    private float feed;
    private float anc_acc;
    private float anc_mean;
    private float ncs;
    private float ncs_acc;
    private float bwo_acc;
    private float bwo_mean;
    
    // Thresholds with default values
    private float anc_acc_treshold_high = 210.0f;
    private float anc_mean_treshold_high = 4500f;
    private float bwo_acc_treshold_high = 20f;
    private float bwo_mean_treshold_high = 480f;

     // Add MongoDB service reference (via setter injection)
    private static MongoService mongoService;
    
    public static void setMongoService(MongoService service) {
        mongoService = service;
    }

     

    // Action methods

    public void setRpm(float rpm) {
        this.rpm = rpm;
        System.out.println("ANC_acc: " + this.anc_acc + " | ANC_mean: " + this.anc_mean);
        System.out.println("Spindle_" + this.id + " RPM Decrease 10%");
        
    }

    public void setFeed(float feed){
        this.feed = feed;
        System.out.println("BWO_acc: " + this.bwo_acc + " | BWO_mean: " + this.bwo_mean);
        System.out.println("Spindle_" + this.id + " Feed Decreased 5%");
        
    }

    public void changeBit(){
        this.status = "Change Bit";
        System.out.println("Spindle_" + this.id + " Changing Bit");

    }

    
    // ANC metrics threshold Adjustment
    public void adjustRule_ANC(float newAccThreshold, float newMeanThreshold) {
        try {
            this.anc_acc_treshold_high = newAccThreshold;
            this.anc_mean_treshold_high = newMeanThreshold;
            
            if (mongoService != null) {
                mongoService.updateThreshold(this.id, "ANC", newAccThreshold, newMeanThreshold);
                System.out.println("Updated to MongoDB ANC thresholds for spindle " + this.id);
            } else {
                System.err.println("MongoService not initialized for spindle " + this.id);
            }
        } catch (Exception e) {
            System.err.println("Failed to update ANC thresholds: " + e.getMessage());
            // Add retry logic or fallback storage
        }
    }

    // BWO metrics threshold Adjustment
     public void adjustRule_BWO(float newAccThreshold, float newMeanThreshold) {
        try {
            this.bwo_acc_treshold_high = newAccThreshold;
            this.bwo_mean_treshold_high = newMeanThreshold;
            
            if (mongoService != null) {
                mongoService.updateThreshold(this.id, "BWO", newAccThreshold, newMeanThreshold);
                System.out.println("Updated to MongoDB BWO thresholds for spindle " + this.id);
            } else {
                System.err.println("MongoService not initialized for spindle " + this.id);
            }
        } catch (Exception e) {
            System.err.println("Failed to update ANC thresholds: " + e.getMessage());
            // Add retry logic or fallback storage
        }
    }

    public void forceChangeBits(String reason) {
        this.status = "FORCE_CHANGE_REQUIRED_" + reason;
        System.out.println("Spindle_" + this.id + " Forced change bit, Reason: " + reason);

    }
    
    // Explicit getters (Lombok sometimes misses these in rules)
    public float getRpm() { return rpm; }
    public float getFeed() { return feed; }
    public float getAnc_acc() { return anc_acc; }
    public float getAnc_mean() { return anc_mean; }
    public float getAnc_acc_treshold_high() { return anc_acc_treshold_high; }
    public float getAnc_mean_treshold_high() { return anc_mean_treshold_high; }
    public float getBwo_acc_treshold_high() { return bwo_acc_treshold_high; }
    public float getBwo_mean_treshold_high() { return bwo_mean_treshold_high; }
    public int getId() { return id; }
}