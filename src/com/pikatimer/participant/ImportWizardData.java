/* 
 * Copyright (C) 2017 John Garner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
    private final StringProperty filename = new SimpleStringProperty();
    private final BooleanProperty waveAssignByBib = new SimpleBooleanProperty();
    private final BooleanProperty waveAssignByAttribute = new SimpleBooleanProperty();
    private final BooleanProperty clearExistingAttribute = new SimpleBooleanProperty();
    private final BooleanProperty nextButtonEnabledAttribute = new SimpleBooleanProperty(true);
    private final StringProperty duplicateHandlingAttribute = new SimpleStringProperty();

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
    
    public StringProperty duplicateHandlingProperty(){
        return duplicateHandlingAttribute;
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
    
    public BooleanProperty  clearExistingProperty() {
        return clearExistingAttribute; 
    }

    public BooleanProperty nextButtonDisabledProperty(){
        return nextButtonEnabledAttribute;
    }
    
    public void mapAttrib(String k, String v) {
        attributeMap.put(k, v); 
        System.out.println("ImportWizardData: Setting " + k + " to " + v);
    }
    public Map getAttributeMap() {
        return attributeMap;
    }
    
}
