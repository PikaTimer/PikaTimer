/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.participant;


import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 * @author jcgarner
 */
class ImportWizardData {
    private final SimpleStringProperty filename = new SimpleStringProperty();
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
    
    public void mapAttrib(String k, String v) {
        attributeMap.put(k, v); 
        System.out.println("ImportWizardData: Setting " + k + " to " + v);
    }
    public Map getAttributeMap() {
        return attributeMap;
    }
    
}
