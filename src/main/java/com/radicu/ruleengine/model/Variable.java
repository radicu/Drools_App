package com.radicu.ruleengine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Variable {

    private String state;
    private float ncs;
    private float ss;
    private String krpm;
    private float ph;
    private float sf;
    private float yTableCurrentMax;
    private float spindleCurrent;
    private String spindle_current;
    private float xTableCurrent;
    private float xTableCurrentMax;
    private String spindleId;
    private float yTableCurrent;
    private float bwo;
    private float anc;
    private float rpm;
    private String spindle;


    @JsonProperty("state")
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @JsonProperty("ncs")
    public float getNcs() {
        return ncs;
    }

    public void setNcs(float ncs) {
        this.ncs = ncs;
    }

    @JsonProperty("ss")
    public float getSs() {
        return ss;
    }

    public void setSs(float ss) {
        this.ss = ss;
    }

    @JsonProperty("krpm")
    public String getKrpm() {
        return krpm;
    }

    public void setKrpm(String krpm) {
        this.krpm = krpm;
    }

    @JsonProperty("ph")
    public float getPh() {
        return ph;
    }

    public void setPh(float ph) {
        this.ph = ph;
    }

    @JsonProperty("sf")
    public float getSf() {
        return sf;
    }

    public void setSf(float sf) {
        this.sf = sf;
    }

    @JsonProperty("yTableCurrentMax")
    public float getYTableCurrentMax() {
        return yTableCurrentMax;
    }

    public void setYTableCurrentMax(float yTableCurrentMax) {
        this.yTableCurrentMax = yTableCurrentMax;
    }

    @JsonProperty("spindleCurrent")
    public float getSpindleCurrent() {
        return spindleCurrent;
    }

    public void setSpindleCurrent(float spindleCurrent) {
        this.spindleCurrent = spindleCurrent;
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

    @JsonProperty("xTableCurrentMax")
    public float getXTableCurrentMax() {
        return xTableCurrentMax;
    }

    public void setXTableCurrentMax(float xTableCurrentMax) {
        this.xTableCurrentMax = xTableCurrentMax;
    }

    @JsonProperty("spindleId")
    public String getSpindleId() {
        return spindleId;
    }

    public void setSpindleId(String spindleId) {
        this.spindleId = spindleId;
    }

    @JsonProperty("yTableCurrent")
    public float getYTableCurrent() {
        return yTableCurrent;
    }

    public void setYTableCurrent(float yTableCurrent) {
        this.yTableCurrent = yTableCurrent;
    }

    @JsonProperty("bwo")
    public float getBwo() {
        return bwo;
    }

    public void setBwo(float bwo) {
        this.bwo = bwo;
    }

    @JsonProperty("anc")
    public float getAnc() {
        return anc;
    }

    public void setAnc(float anc) {
        this.anc = anc;
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
}
