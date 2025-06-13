package com.radicu.ruleengine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Variable {

    private float sfMaxThreshold2;
    private float sfMaxThreshold1;
    private String state;
    private float ncs;
    private float ss;
    private float ap1;
    private float bs;
    private String krpm;
    private float ph;
    private float sf;
    private float spindleCurrent;
    private String spindle_current;
    private float xTableCurrent;
    private String spindleId;
    private float yTableCurrent;
    private float bwo;
    private float rpm;
    private float anc;
    private String spindle;


    @JsonProperty("sfMaxThreshold2")
    public float getSfMaxThreshold2() {
        return sfMaxThreshold2;
    }

    public void setSfMaxThreshold2(float sfMaxThreshold2) {
        this.sfMaxThreshold2 = sfMaxThreshold2;
    }

    @JsonProperty("sfMaxThreshold1")
    public float getSfMaxThreshold1() {
        return sfMaxThreshold1;
    }

    public void setSfMaxThreshold1(float sfMaxThreshold1) {
        this.sfMaxThreshold1 = sfMaxThreshold1;
    }

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

    @JsonProperty("ap1")
    public float getAp1() {
        return ap1;
    }

    public void setAp1(float ap1) {
        this.ap1 = ap1;
    }

    @JsonProperty("bs")
    public float getBs() {
        return bs;
    }

    public void setBs(float bs) {
        this.bs = bs;
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

    @JsonProperty("rpm")
    public float getRpm() {
        return rpm;
    }

    public void setRpm(float rpm) {
        this.rpm = rpm;
    }

    @JsonProperty("anc")
    public float getAnc() {
        return anc;
    }

    public void setAnc(float anc) {
        this.anc = anc;
    }

    @JsonProperty("spindle")
    public String getSpindle() {
        return spindle;
    }

    public void setSpindle(String spindle) {
        this.spindle = spindle;
    }
}
