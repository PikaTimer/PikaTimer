/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import java.time.Duration;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
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
@Table(name="overrides")
public class TimeOverride {
    private Integer timeOverrideID;
    private String chip;
    private Integer splitId;
    private Duration timestamp;
    //private Boolean relativeTime; 
    private StringProperty bibProperty = new SimpleStringProperty();
    private BooleanProperty relativeProperty = new SimpleBooleanProperty(false);

    @Id
    @GenericGenerator(name="override_id" , strategy="increment")
    @GeneratedValue(generator="override_id")
    @Column(name="override_id")
    public Integer getID() {
        return timeOverrideID; 
    }
    public void setID(Integer id) {
        timeOverrideID=id;
    }

    @Column(name="override_time")
    public Long getTimestampLong() {
        if (timestamp != null) {
            return timestamp.toNanos();
        } else {
            return 0L; 
        }
    }
    public void setTimestampLong(Long c) {
        if(c != null) {
            timestamp = Duration.ofNanos(c);
        }
    }
    @Transient
    public Duration getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Duration d) {
        this.timestamp = d;
    }
    
    @Column(name="bib")
    public String getBib() {
        return bibProperty.getValueSafe();
    }

    public void setBib(String bib) {
        bibProperty.setValue(bib);
    }
    public StringProperty bibProperty() {
        return bibProperty;
    }
    
    @Column(name="split_id") 
    public Integer getSplitId() {
        //System.out.println("RawTimeData: Returning timingLocationInputId of " + timingLocationInputId);
        return splitId;
    }
    public void setSplitId(Integer i) {
        //System.out.println("RawTimeData: Setting timingLocationInputId to " + i);
        splitId = i;
    }
    
    @Column(name="relative_to_start")
    public Boolean getRelative() {
        return relativeProperty.getValue();
        // relativeTime;
    }
    public void setRelative(Boolean r) {
        //relativeTime = r;
        relativeProperty.setValue(r);
    }
    public BooleanProperty relativeProperty(){
        return relativeProperty;
    }
    
    
}
