/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

/**
 *
 * @author jcgarner
 */
@Entity
@DynamicUpdate
@Table(name="timing_location_input")
public class TimingLocationInput {
    private final IntegerProperty IDProperty;
    private final StringProperty TimingLocationInputName;
    private TimingLocation timingLocation; // timing_loc_id
    private final StringProperty timingInputString; 
    
    public TimingLocationInput() {
        this.IDProperty = new SimpleIntegerProperty();
        this.TimingLocationInputName = new SimpleStringProperty("Not Yet Set");
        this.timingInputString = new SimpleStringProperty();
   }
    
    @Id
    @GenericGenerator(name="timing_location_input_id" , strategy="increment")
    @GeneratedValue(generator="timing_location_input_id")
    @Column(name="ID")
    public Integer getID() {
        return IDProperty.getValue(); 
    }
    public void setID(Integer id) {
        IDProperty.setValue(id);
    }
    public IntegerProperty idProperty() {
        return IDProperty; 
    }
    
    @Transient
    public String getLocationName() {
    return TimingLocationInputName.getValueSafe();
    }
    public void setLocationName(String n) {
    TimingLocationInputName.setValue(n);
    }
    public StringProperty LocationNameProperty() {
    return TimingLocationInputName;
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TIMING_LOCATION_ID",nullable=false)
    public TimingLocation getTimingLocation() {
        return timingLocation;
    }
    public void setTimingLocation(TimingLocation l) {
        if (l != null) {
            System.out.println("Split.setTimingLocation: " + l.getID());
            timingLocation=l;
            timingInputString.unbind();
            timingInputString.bind(l.LocationNameProperty()); 
            System.out.println("TimingLocationInput.setTimingLocation: " + timingInputString.getValueSafe()); 
        } else {
            System.out.println("TimingLocationInput.setTimingLocation: null"); 
        }
    }
    public StringProperty timingLocationProperty() {
        return timingInputString; 
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        return this.IDProperty.getValue().equals(((TimingLocationInput)obj).IDProperty.getValue());
    }

    @Override
    public int hashCode() {
        return 7 + 5*IDProperty.intValue(); // 5 and 7 are random prime numbers
    }
    
    public static Callback<TimingLocationInput, Observable[]> extractor() {
        return (TimingLocationInput tl) -> new Observable[]{tl.LocationNameProperty()};
    }
}
