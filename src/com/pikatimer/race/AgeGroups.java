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
package com.pikatimer.race;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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

    private final IntegerProperty agStartProperty = new SimpleIntegerProperty(9);
    private final IntegerProperty agIncrementProperty = new SimpleIntegerProperty(5);
    private final IntegerProperty mastersProperty = new SimpleIntegerProperty(40);
    private final BooleanProperty customIncrementsProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty customNamesProperty = new SimpleBooleanProperty(false);
    private final ObservableList<AgeGroupIncrement> customIncrementObservableList = FXCollections.observableArrayList(AgeGroupIncrement.extractor());
    private Map<Integer,String> agNameMap = new ConcurrentHashMap();
    private Map<Integer,String> agShortNameMap = new ConcurrentHashMap();
    private Map<Integer,Integer> agMap = new ConcurrentHashMap();
    
    private List<AgeGroupIncrement> customIncrementList;
    
    private Race race;

    public AgeGroups() {
        // if the agIncrementProperty or the agStartProperty change, 
        // invalidate the agMap and agNameMaps
        agIncrementProperty.addListener(listener -> {invalidateMaps();});
        agStartProperty.addListener(listener -> {invalidateMaps();});
        customIncrementsProperty.addListener(listener -> {invalidateMaps();});
        customIncrementObservableList.addListener((ListChangeListener<AgeGroupIncrement>) c -> {invalidateMaps(); });
        customNamesProperty.addListener(listener -> {invalidateMaps();});       
        
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
        agIncrementProperty.setValue(i);
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
        agStartProperty.setValue(i);
    }
    public IntegerProperty agStartProperty() {
        return agStartProperty;
    }
    
    @Column(name="custom_increments")
    public Boolean getUseCustomIncrements() {
        return customIncrementsProperty.getValue(); 
    }
    public void setUseCustomIncrements(Boolean i) {
        //System.out.println("AgeGroups.setAGStart() with " + i);
        customIncrementsProperty.setValue(i);
    }
    public BooleanProperty useCustomIncrementsProperty() {
        return customIncrementsProperty;
    }
    
    @Column(name="custom_names")
    public Boolean getUseCustomNames() {
        return customNamesProperty.getValue(); 
    }
    public void setUseCustomNames(Boolean i) {
        //System.out.println("AgeGroups.setAGStart() with " + i);
        customNamesProperty.setValue(i);
        
    }
    public BooleanProperty useCustomNamesProperty() {
        return customNamesProperty;
    }
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
          name="race_age_group_increments",
          joinColumns=@JoinColumn(name="ag_id")
    )
    protected List<AgeGroupIncrement> getCustomIncrementsList(){
        return customIncrementList;
    }
    protected void setCustomIncrementsList(List<AgeGroupIncrement> i){
        customIncrementList = i;
    }
    
    public ObservableList<AgeGroupIncrement> ageGroupIncrementProperty(){
        if (customIncrementObservableList.isEmpty() && customIncrementList != null && ! customIncrementList.isEmpty() ) {
            customIncrementObservableList.addAll(customIncrementList);
            recalcCustomAGs();
        }
        return customIncrementObservableList;
    }
    
    public void addCustomIncrement(AgeGroupIncrement i){
        System.out.println("addCustomIncrement called");
        customIncrementObservableList.add(i);
        customIncrementList = customIncrementObservableList;
    }
    
    public void removeCustomIncrement(AgeGroupIncrement i){
        customIncrementObservableList.remove(i);
        customIncrementList = customIncrementObservableList;
    }
    
    public void recalcCustomAGs(){
        customIncrementObservableList.sort((i1, i2) -> i1.getStartAge().compareTo(i2.getStartAge()));
        for (int i=0; i < customIncrementObservableList.size(); i++) {
            if (i == customIncrementObservableList.size() -1) customIncrementObservableList.get(i).endAgeProperty().setValue("∞");
            else customIncrementObservableList.get(i).endAgeProperty().setValue(Integer.toString(customIncrementObservableList.get(i+1).getStartAge() - 1));
        }
    }

    // Identical to the ageToAGString method but it uses 
    // the custom names if they are set
    public String ageToLongAGString(Integer i){
        // Returns the long string representation of the ag given an age
        // e.g., 42 -> 40 to 44
        // based on the increment and the agStart floor (1->9)
        // Zero is a special case
        
        // Step 1: Handle nulls
        if (i == null) i = 0;
        
        // Step 2: Did we deal with this age before?
        if (agNameMap.containsKey(ageToAG(i))) return agNameMap.get(ageToAG(i));

        // Step 3: Figure out what the AG category is
        
        if (customIncrementsProperty.get()) {
            
            if (customIncrementObservableList.isEmpty()) {
                agNameMap.put(ageToAG(i),"0+");
            } else {
                Integer start = ageToAG(i);
                Integer x = customIncrementObservableList.get(0).getStartAge() -1;
                String end = x.toString();
                agNameMap.put(start,start.toString()+" to " + end); // tmp value
                for (AgeGroupIncrement ag: customIncrementObservableList ){
                    if (Objects.equals(ag.getStartAge(), start)) {
                        if (customNamesProperty.get() && ! ag.nameProperty().getValueSafe().isEmpty()) {
                            agNameMap.put(start, ag.nameProperty().getValueSafe());
                        } else {
                            end = ag.endAgeProperty().get();
                            if (!"+".equals(end)) end = " to "+end;
                            agNameMap.put(start,start.toString()+end);
                        }
                    }
                }
            }
        } else {
            if (i == 0) agNameMap.put(ageToAG(i), "0"); //Zero is a special case
            else if(i <= agStartProperty.get()) {
                agNameMap.put(ageToAG(i), "1-" + (agStartProperty.getValue()));
            } else {
                agNameMap.put(ageToAG(i), ageToAG(i) + "-" + (ageToAG(i)+agIncrementProperty.get()-1));
            }
        }
        return agNameMap.get(ageToAG(i));
    }
    
    
    public String ageToAGString(Integer i){
        // Returns the "short" string representation of the ag given an age
        // e.g., 42 -> 40-44
        // based on the increment and the agStart floor (1->9)
        // Zero is a special case
        
        // Step 1: Handle nulls
        if (i == null) i = 0;
        
        // Step 2: Did we deal with this age before?
        if (agShortNameMap.containsKey(ageToAG(i))) return agShortNameMap.get(ageToAG(i));

        // Step 3: Figure out what the AG category is
        
        if (customIncrementsProperty.get()) {
            //System.out.println("AgeGroups::ageToAGString:  CustomIncrements in use");
            if (customIncrementObservableList.isEmpty()) {
                agShortNameMap.put(ageToAG(i),"0+");
            } else {
                Integer start = ageToAG(i);
                Integer x = customIncrementObservableList.get(0).getStartAge() -1;
                String end = x.toString();
                agShortNameMap.put(start,start.toString()+"-" + end); // tmp value
                //System.out.println("AgeGroups::ageToAGString:  tmp set to " + agShortNameMap.get(start));

                for (AgeGroupIncrement ag: customIncrementObservableList ){
                    if (Objects.equals(ag.getStartAge(), start)) {
                        end = ag.endAgeProperty().getValue();
                        if (!"+".equals(end)) end = "-"+end;
                        agShortNameMap.put(start,start.toString()+end);
                        //System.out.println("AgeGroups::ageToAGString:  finally set to " + agShortNameMap.get(start));

                    }
                }
            }
        } else {
            if (i == 0) agShortNameMap.put(ageToAG(i), "0"); //Zero is a special case
            else if(i <= agStartProperty.get()) {
                agShortNameMap.put(ageToAG(i), "1-" + (agStartProperty.getValue()));
            } else {
                agShortNameMap.put(ageToAG(i), ageToAG(i) + "-" + (ageToAG(i)+agIncrementProperty.get()-1));
            }
        }
        
        //System.out.println("AgeGroups::ageToAGString " + i + " -> " + agShortNameMap.get(ageToAG(i)));
        return agShortNameMap.get(ageToAG(i));
    }
    
    public Integer ageToAG(Integer i){
        // Returns the base Age for the age group given the participants age.
        // e.g., 42 -> 40
        // based on the increment and the agStart floor
        // Zero is a special case
        
        // Step 1: deal with null entries
        if (i == null) i = 0;
        
        // Step 2: Did we deal with this age before?
        if (agMap.containsKey(i)) return agMap.get(i);
        
        // Step 3: No? time to figure it out.... 
        Integer a = 0; // start with zero as a default
        if (customIncrementsProperty.get()) {
            for (AgeGroupIncrement ag: customIncrementObservableList ){
                if (i >= ag.getStartAge() ) {
                    a = ag.getStartAge();
                }
            }
            agMap.put(i, a);
        } else {
            if (i == 0) agMap.put(i,0); 
            else if (i <= agStartProperty.get()) agMap.put(i,1);
            else agMap.put(i,((i/agIncrementProperty.get())*agIncrementProperty.get()));
        }
        //System.out.println("AgeGroups::ageToAG " + i + " -> " + agMap.get(i));
        return agMap.get(i);
        
    }

    private void invalidateMaps() {
        //System.out.println("AgeGroups.invalidateMaps() Called");

        agNameMap = new ConcurrentHashMap();
        agMap = new ConcurrentHashMap();
    }

    
  
    
}
