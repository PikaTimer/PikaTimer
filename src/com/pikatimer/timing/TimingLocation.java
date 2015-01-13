/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import com.pikatimer.participant.Participant;
import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.persistence.CascadeType;
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

   public TimingLocation() {
        this.IDProperty = new SimpleIntegerProperty();
        this.locationName = new SimpleStringProperty("Not Yet Set");
        this.associatedSplits = FXCollections.observableArrayList();
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
    public StringProperty raceLocationProperty() {
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
    
    
    @Override
    public String toString(){
        return getLocationName();
    }
    
    
    @Override
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
}
