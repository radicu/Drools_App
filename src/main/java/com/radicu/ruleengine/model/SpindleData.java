package com.radicu.ruleengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpindleData {

    private float spindleCurrent;
    private float alarmLimit3200;
    private float alarmLimit4200;
    private float alarmLimit5100;
    private float alarmLimit2;
    private float xTableCurrent;
    private float yTableCurrent;
    private float drillingCondition1;
    private float drillingCondition2;
    private float drillingCondition3;
    private String state;

    @JsonProperty("spindleCurrent")
    public float getSpindleCurrent() {
        return spindleCurrent;
    }

    public void setSpindleCurrent(float spindleCurrent) {
        this.spindleCurrent = spindleCurrent;
    }

    @JsonProperty("alarmLimit2")
    public float getAlarmLimit2(){
        return alarmLimit2;
    }

    public void setAlarmLimit2(float alarmLimit2){
        this.alarmLimit2 = alarmLimit2;
    }

    @JsonProperty("alarmLimit3200")
    public float getAlarmLimit3200() {
        return alarmLimit3200;
    }

    public void setAlarmLimit3200(float alarmLimit3200) {
        this.alarmLimit3200 = alarmLimit3200;
    }

    @JsonProperty("alarmLimit4200")
    public float getAlarmLimit4200(){
        return alarmLimit4200;
    }

    public void setAlarmLimit4200(float alarmLimit4200){
        this.alarmLimit4200 = alarmLimit4200;
    }

    @JsonProperty("alarmLimit5100")
    public float getAlarmLimit5100(){
        return alarmLimit5100;
    }

    public void setAlarmLimit5100(float alarmLimit5100){
        this.alarmLimit5100 = alarmLimit5100;
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

    @JsonProperty("drillingCondition1")
    public float getDrillingCondition1() {
        return drillingCondition1;
    }

    public void setDrillingCondition1(float drillingCondition1) {
        this.drillingCondition1 = drillingCondition1;
    }

    @JsonProperty("drillingCondition2")
    public float getDrillingCondition2(){
        return drillingCondition2;
    }

    public void setDrillingCondition_2(float drillingCondition2){
        this.drillingCondition2 = drillingCondition2;
    }

    @JsonProperty("drillingCondition3")
    public float getDrillingCondition3(){
        return drillingCondition3;
    }

    public void setDrillingCondition_3(float drillingCondition3){
        this.drillingCondition3 = drillingCondition3; 
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
