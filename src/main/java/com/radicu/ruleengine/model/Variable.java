package com.radicu.ruleengine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Variable {

    private String sPINDLE_STATES;
    private float alarmLimit5100;
    private String state;
    private float alarmLimit2;
    private float drillingCondition3;
    private String krpm;
    private String drilling_condition;
    private float spindleCurrent;
    private float drillingCondition2;
    private String spindle_current;
    private float xTableCurrent;
    private float yTableCurrent;
    private float feedRate;
    private float rpm;
    private String spindle;
    private float alarmLimit3200;
    private float alarmLimit4200;
    private float drillingCondition1;


    @JsonProperty("sPINDLE_STATES")
    public String getSPINDLE_STATES() {
        return sPINDLE_STATES;
    }

    public void setSPINDLE_STATES(String sPINDLE_STATES) {
        this.sPINDLE_STATES = sPINDLE_STATES;
    }

    @JsonProperty("alarmLimit5100")
    public float getAlarmLimit5100() {
        return alarmLimit5100;
    }

    public void setAlarmLimit5100(float alarmLimit5100) {
        this.alarmLimit5100 = alarmLimit5100;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @JsonProperty("alarmLimit2")
    public float getAlarmLimit2() {
        return alarmLimit2;
    }

    public void setAlarmLimit2(float alarmLimit2) {
        this.alarmLimit2 = alarmLimit2;
    }

    @JsonProperty("drillingCondition3")
    public float getDrillingCondition3() {
        return drillingCondition3;
    }

    public void setDrillingCondition3(float drillingCondition3) {
        this.drillingCondition3 = drillingCondition3;
    }

    @JsonProperty("krpm")
    public String getKrpm() {
        return krpm;
    }

    public void setKrpm(String krpm) {
        this.krpm = krpm;
    }

    @JsonProperty("drilling_condition")
    public String getDrilling_condition() {
        return drilling_condition;
    }

    public void setDrilling_condition(String drilling_condition) {
        this.drilling_condition = drilling_condition;
    }

    @JsonProperty("spindleCurrent")
    public float getSpindleCurrent() {
        return spindleCurrent;
    }

    public void setSpindleCurrent(float spindleCurrent) {
        this.spindleCurrent = spindleCurrent;
    }

    @JsonProperty("drillingCondition2")
    public float getDrillingCondition2() {
        return drillingCondition2;
    }

    public void setDrillingCondition2(float drillingCondition2) {
        this.drillingCondition2 = drillingCondition2;
    }

    @JsonProperty("spindle_current")
    public String getSpindle_current() {
        return spindle_current;
    }

    public void setSpindle_current(String spindle_current) {
        this.spindle_current = spindle_current;
    }

    @JsonProperty("xTableCurrent")
    public float getXTableCurrent() {
        return xTableCurrent;
    }

    public void setXTableCurrent(float xTableCurrent) {
        this.xTableCurrent = xTableCurrent;
    }

    @JsonProperty("yTableCurrent")
    public float getYTableCurrent() {
        return yTableCurrent;
    }

    public void setYTableCurrent(float yTableCurrent) {
        this.yTableCurrent = yTableCurrent;
    }

    @JsonProperty("feedRate")
    public float getFeedRate() {
        return feedRate;
    }

    public void setFeedRate(float feedRate) {
        this.feedRate = feedRate;
    }

    @JsonProperty("rpm")
    public float getRpm() {
        return rpm;
    }

    public void setRpm(float rpm) {
        this.rpm = rpm;
    }

    @JsonProperty("spindle")
    public String getSpindle() {
        return spindle;
    }

    public void setSpindle(String spindle) {
        this.spindle = spindle;
    }

    @JsonProperty("alarmLimit3200")
    public float getAlarmLimit3200() {
        return alarmLimit3200;
    }

    public void setAlarmLimit3200(float alarmLimit3200) {
        this.alarmLimit3200 = alarmLimit3200;
    }

    @JsonProperty("alarmLimit4200")
    public float getAlarmLimit4200() {
        return alarmLimit4200;
    }

    public void setAlarmLimit4200(float alarmLimit4200) {
        this.alarmLimit4200 = alarmLimit4200;
    }

    @JsonProperty("drillingCondition1")
    public float getDrillingCondition1() {
        return drillingCondition1;
    }

    public void setDrillingCondition1(float drillingCondition1) {
        this.drillingCondition1 = drillingCondition1;
    }
}
