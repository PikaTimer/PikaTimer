/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import java.time.Duration;
import java.util.Objects;
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
@Table(name="raw_timing_data")
public class RawTimeData {
    private Duration timestamp;
    private String chip;
    private Integer timingLocationInputId;
    private Integer rawTimeId; 

    @Id
    @GenericGenerator(name="rawTime_id" , strategy="increment")
    @GeneratedValue(generator="rawTime_id")
    @Column(name="ID")
    public Integer getID() {
        return rawTimeId; 
    }
    public void setID(Integer id) {
        rawTimeId=id;
    }

    @Column(name="raw_time")
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

    @Column(name="CHIP_ID")
    public String getChip() {
        return chip;
    }

    public void setChip(String chip) {
        this.chip = chip;
    }
    
    @Column(name="timing_loc_input_id") 
    public Integer getTimingLocationInputId() {
        //System.out.println("RawTimeData: Returning timingLocationInputId of " + timingLocationInputId);
        return timingLocationInputId;
    }
    public void setTimingLocationInputId(Integer i) {
        //System.out.println("RawTimeData: Setting timingLocationInputId to " + i);
        this.timingLocationInputId = i;
    }

    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.timestamp);
        hash = 67 * hash + Objects.hashCode(this.chip);
        hash = 67 * hash + Objects.hashCode(this.timingLocationInputId);
        
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
       
        final RawTimeData other = (RawTimeData) obj;
        if (!Objects.equals(this.timestamp, other.timestamp)) {
            return false;
        }
        if (!Objects.equals(this.chip, other.chip)) {
            return false;
        }
        if (!Objects.equals(this.timingLocationInputId, other.timingLocationInputId)) {
            return false;
        }
        
        return true;
    }
        
    
    
}
