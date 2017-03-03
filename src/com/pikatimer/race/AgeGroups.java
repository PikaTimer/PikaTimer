/* 
 * Copyright (C) 2016 John Garner
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
package com.pikatimer.race;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 *
 * @author John
 */

@Entity
@DynamicUpdate
@Table(name="race_age_groups")
public class AgeGroups {
    private Integer raceID;

    private IntegerProperty agStartProperty = new SimpleIntegerProperty(9);
    private IntegerProperty agIncrementProperty = new SimpleIntegerProperty(5);
    private IntegerProperty mastersProperty = new SimpleIntegerProperty(40);
    
    private Map<Integer,String> agNameMap = new ConcurrentHashMap();
    private Map<Integer,Integer> agMap = new ConcurrentHashMap();
    
    private Race race;

    public AgeGroups() {
        // if the agIncrementProperty or the agStartProperty change, 
        // invalidate the agMap and agNameMaps
        agIncrementProperty.addListener(listener -> {invalidateMaps();});
        agStartProperty.addListener(listener -> {invalidateMaps();});
        
    }
    
    @Id
    @GenericGenerator(name = "ag_generator", strategy = "foreign", 
	parameters = @Parameter(name = "property", value = "race"))
    @GeneratedValue(generator = "ag_generator")
    @Column(name = "race_id", unique = true, nullable = false)
    public Integer getRaceID() {
        return raceID; 
    }
    public void setRaceID(Integer r) {
        raceID = r;
    }
    
    @OneToOne(mappedBy = "ageGroups")
    @MapsId
    @JoinColumn(name="race_id")  
    public Race getRace() {
        return race;
    }
    public void setRace(Race r) {
        race=r;
    }
    
    @Column(name="ag_increment")
    public Integer getAGIncrement() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return agIncrementProperty.getValue(); 
    }
    public void setAGIncrement(Integer i) {
        System.out.println("AgeGroups.setAGIncrement() with " + i);
        agIncrementProperty.set(i);
    }
    public IntegerProperty agIncrementProperty() {
        return agIncrementProperty;
    }
    
    @Column(name="masters_start")
    public Integer getMasters() {
        return mastersProperty.getValue(); 
    }
    public void setMasters(Integer i) {
        //System.out.println("AgeGroups.setMasters() with " + i);
        mastersProperty.setValue(i);
    }
    public IntegerProperty mastersProperty() {
        return mastersProperty;
    }
    
    @Column(name="ag_start")
    public Integer getAGStart() {
        return agStartProperty.getValue(); 
    }
    public void setAGStart(Integer i) {
        //System.out.println("AgeGroups.setAGStart() with " + i);
        agStartProperty.set(i);
    }
    public IntegerProperty agStartProperty() {
        return agStartProperty;
    }
    

    public String ageToAGString(Integer i){
        // Returns the string representation of the ag given an age
        // e.g., 42 -> 40-44
        // based on the increment and the agStart floor (1->9)
        if (agNameMap.containsKey(ageToAG(i))) return agNameMap.get(ageToAG(i));
        
        if(i <= agStartProperty.get()) {
            agNameMap.put(ageToAG(i), "1-" + (agStartProperty.getValue()));
        } else {
            agNameMap.put(ageToAG(i), ageToAG(i) + "-" + (ageToAG(i)+agIncrementProperty.get()-1));
        }
        
        return agNameMap.get(ageToAG(i));
    }
    
    public Integer ageToAG(Integer i){
        // Returns the base Age for the age group given the participants age.
        // e.g., 42 -> 40
        // based on the increment and the agStart floor
        
        if (agMap.containsKey(i)) return agMap.get(i);
        
        if (i <= agStartProperty.get()) agMap.put(i,1);
        else agMap.put(i,((i/agIncrementProperty.get())*agIncrementProperty.get()));
        
        return agMap.get(i);
        
    }

    private void invalidateMaps() {
        //System.out.println("AgeGroups.invalidateMaps() Called");

        agNameMap = new ConcurrentHashMap();
        agMap = new ConcurrentHashMap();
    }
  
    
}
