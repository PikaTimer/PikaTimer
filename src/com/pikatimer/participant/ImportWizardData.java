/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.participant;


import com.pikatimer.race.Wave;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author jcgarner
 */
class ImportWizardData {
    private final SimpleStringProperty filename = new SimpleStringProperty();
    private BooleanProperty waveAssignByBib = new SimpleBooleanProperty();
    private BooleanProperty waveAssignByAttribute = new SimpleBooleanProperty();

    private Wave assignedWave; 
    private final Map<String, String> attributeMap = new HashMap<>();
    private ResultSet rs = null;
    private int numToAdd = 0; 
    
    public int getNumToAdd() {
        return numToAdd;
    }
    public void setNumToAdd(int i) {
        numToAdd = i; 
    }
    public String getFileName() {
        return filename.getValueSafe();
    }
    public void setFileName(String fName) {
        System.out.println("setFileName: from " + filename.getValueSafe() + " to " + fName);
        filename.setValue(fName);
        
    }
    public StringProperty fileNameProperty() {
        return filename; 
    }        
    
    public void setResultsSet(ResultSet r) {
        rs = r;
    }
    public ResultSet getResultSet() {
        return rs; 
    }
    
    public BooleanProperty  waveAssignByAttributeProperty() {
        return waveAssignByAttribute; 
    }
    public Boolean getWaveAssignByAttribute() {
        return waveAssignByAttribute.getValue(); 
    }
    public BooleanProperty  waveAssignByBibProperty() {
        return waveAssignByBib; 
    }
    public Boolean getWaveAssignByBib() {
        return waveAssignByBib.getValue(); 
    }
    public void setAssignedWave(Wave w) {
        assignedWave=w; 
    }
    public Wave getAssignedWave(){
        return assignedWave; 
    }
    
    public void mapAttrib(String k, String v) {
        attributeMap.put(k, v); 
        System.out.println("ImportWizardData: Setting " + k + " to " + v);
    }
    public Map getAttributeMap() {
        return attributeMap;
    }
    
}
