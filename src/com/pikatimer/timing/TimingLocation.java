/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import java.util.List;
import java.util.Objects;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Callback;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

/**
 *
 * @author jcgarner
 */
@Entity
@DynamicUpdate
@Table(name="timing_location")
public class TimingLocation {
    
   private final IntegerProperty IDProperty;
   private final StringProperty locationName;
   private final ObservableList<Split> associatedSplits; 
   private final ObservableList<TimingLocationInput> timingInputs; 

   public TimingLocation() {
        this.IDProperty = new SimpleIntegerProperty();
        this.locationName = new SimpleStringProperty("Not Yet Set");
        this.associatedSplits = FXCollections.observableArrayList();
        this.timingInputs = FXCollections.observableArrayList();
   }
   
    @Id
    @GenericGenerator(name="timing_location_id" , strategy="increment")
    @GeneratedValue(generator="timing_location_id")
    @Column(name="TIMING_LOCATION_ID")
    public Integer getID() {
        return IDProperty.getValue(); 
    }
    public void setID(Integer id) {
        IDProperty.setValue(id);
    }
    public IntegerProperty idProperty() {
        return IDProperty; 
    }
    
    @Column(name="TIMING_LOCATION_NAME")
    public String getLocationName() {
        return locationName.getValueSafe();
    }
    public void setLocationName(String n) {
        locationName.setValue(n);
    }
    public StringProperty LocationNameProperty() {
        return locationName;
    }
    
    @OneToMany(mappedBy="timingLocation",fetch = FetchType.LAZY)
    public List<Split> getSplits() {
        //return associatedSplits.sorted((Split o1, Split o2) -> o1.getPosition().compareTo(o2.getPosition()));
        return associatedSplits.sorted(); 
    }
    public void setSplits(List<Split> splits) {
        System.out.println("TimingLocation.setSplits(list) called for " + locationName + " with " + splits.size() + " splits"); 
        associatedSplits.setAll(splits);
        System.out.println(locationName + " now has " + associatedSplits.size() + " splits");
    }
    public ObservableList<Split> splitsProperty() {
        return associatedSplits; 
    }
    
    @OneToMany(mappedBy="timingLocation",fetch = FetchType.LAZY)
    public List<TimingLocationInput> getInputs() {
        //return associatedSplits.sorted((Split o1, Split o2) -> o1.getPosition().compareTo(o2.getPosition()));
        return timingInputs.sorted(); 
    }
    public void setInputs(List<TimingLocationInput> inputs) {
        //System.out.println("TimingLocation.setInputs(list) called for " + locationName + " with " + inputs.size() + " splits"); 
        timingInputs.setAll(inputs);
        //System.out.println(locationName + " now has " + timingInputs.size() + " inputs");   
    }
    public ObservableList<TimingLocationInput> inputsProperty() {
        return timingInputs; 
    }
    public void addInput(TimingLocationInput t){
        System.out.println("TimingLocation.addInput called");
        timingInputs.add(t);
        System.out.println(locationName + " now has " + timingInputs.size() + " inputs");
    }
    public void removeInput(TimingLocationInput t){
        timingInputs.remove(t); 
    }
    
    @Override
    public String toString(){
        return getLocationName();
    }
    
    
    /*    @Override
    public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
    return false;
    }
    //System.out.println("Wave.equals called: " + IDProperty.getValue() + " vs " + ((Wave)obj).IDProperty.getValue() );
    return this.IDProperty.getValue().equals(((TimingLocation)obj).IDProperty.getValue());
    }
    
    @Override
    public int hashCode() {
    return 7 + 5*IDProperty.intValue(); // 5 and 7 are random prime numbers
    }
    */
    public static Callback<TimingLocation, Observable[]> extractor() {
    
    return (TimingLocation tl) -> new Observable[]{tl.LocationNameProperty()};
    
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + Objects.hashCode(this.locationName);
        hash = 17 * hash + Objects.hashCode(this.IDProperty);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TimingLocation other = (TimingLocation) obj;
        if (!Objects.equals(this.locationName.getValue(), other.locationName.getValue())) {
            return false;
        }
        if (!Objects.equals(this.IDProperty.getValue(), other.IDProperty.getValue())) {
            return false;
        }
        return true;
    }

}
