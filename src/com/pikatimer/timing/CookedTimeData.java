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
package com.pikatimer.timing;

import com.pikatimer.util.DurationFormatter;
import java.time.Duration;
import java.util.Objects;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;
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
@Table(name="cooked_timing_data")
public class CookedTimeData {
    private Duration timestamp;
    private final StringProperty timestampStringProperty = new SimpleStringProperty();
    private final StringProperty bibProperty = new SimpleStringProperty();
    private final StringProperty chipProperty = new SimpleStringProperty();
    //private StringProperty participantNameProperty = new SimpleStringProperty();
    private Integer timingLocationId;
    private Integer timingLocationInputId;
    //private StringProperty timingLocationNameProperty = new SimpleStringProperty();
    private Integer cookedTimeId; 
    private final BooleanProperty ignoreTimeBoolean = new SimpleBooleanProperty(Boolean.FALSE);
    private final BooleanProperty backupTimeBoolean = new SimpleBooleanProperty(Boolean.FALSE);

    @Id
    @GenericGenerator(name="cookedTime_id" , strategy="increment")
    @GeneratedValue(generator="cookedTime_id")
    @Column(name="ID")
    public Integer getID() {
        return cookedTimeId; 
    }
    public void setID(Integer id) {
        cookedTimeId=id;
    }

    @Column(name="cooked_time")
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
            timestampStringProperty.setValue(DurationFormatter.durationToString(timestamp,3)); 
        }
    }
    
    @Transient
    public Duration getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Duration d) {
        this.timestamp = d;
        timestampStringProperty.setValue(DurationFormatter.durationToString(timestamp,3)); 
        
    }
    public StringProperty timestampStringProperty(){
        return timestampStringProperty;
    }

    @Column(name="bib_id")
    public String getBib() {
        return bibProperty.getValue();
    }

    public void setBib(String b) {
        bibProperty.setValue(b);
        //participantNameProperty.setValue("Not yet Implemented");
    }
    public StringProperty bibProperty() {
        return bibProperty;
    }
    
    @Column(name="raw_chip_id")
    public String getRawChipID() {
        return chipProperty.getValue();
    }

    public void setRawChipID(String  b) {
        chipProperty.setValue(b);
        //participantNameProperty.setValue("Not yet Implemented");
    }
    public StringProperty rawChipIDProperty() {
        return chipProperty;
    }
    
    @Column(name="timing_loc_id") 
    public Integer getTimingLocationId() {
        //System.out.println("RawTimeData: Returning timingLocationInputId of " + timingLocationInputId);
        return timingLocationId;
    }
    public void setTimingLocationId(Integer i) {
        //System.out.println("RawTimeData: Setting timingLocationInputId to " + i);
        this.timingLocationId = i;
    }
//    public StringProperty timingLocationNameProperty() {
//        return timingLocationNameProperty;
//    }
    
    @Column(name="timing_loc_input_id") 
    public Integer getTimingLocationInputId() {
        //System.out.println("RawTimeData: Returning timingLocationInputId of " + timingLocationInputId);
        return timingLocationInputId;
    }
    public void setTimingLocationInputId(Integer i) {
        //System.out.println("RawTimeData: Setting timingLocationInputId to " + i);
        this.timingLocationInputId = i;
    }
    
    @Column(name="ignore_time") 
    public Boolean getIgnoreTime() {
        //System.out.println("RawTimeData: Returning timingLocationInputId of " + timingLocationInputId);
        return ignoreTimeBoolean.getValue();
    }
    public void setIgnoreTime(Boolean i) {
        //System.out.println("RawTimeData: Setting timingLocationInputId to " + i);
        this.ignoreTimeBoolean.setValue(i);
    }
    public BooleanProperty ignoreTimeProperty() {
        return ignoreTimeBoolean; 
    }
    
    @Column(name="backup_time") 
    public Boolean getBackupTime() {
        //System.out.println("RawTimeData: Returning timingLocationInputId of " + timingLocationInputId);
        return backupTimeBoolean.getValue();
    }
    public void setBackupTime(Boolean i) {
        //System.out.println("RawTimeData: Setting timingLocationInputId to " + i);
        this.backupTimeBoolean.setValue(i);
    }
    public BooleanProperty backupTimeProperty() {
        return backupTimeBoolean; 
    }

    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.timestamp);
        hash = 67 * hash + Objects.hashCode(this.bibProperty.getValue());
        hash = 67 * hash + Objects.hashCode(this.timingLocationId);
        
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
       
        final CookedTimeData other = (CookedTimeData) obj;
        if (!Objects.equals(this.timestamp, other.timestamp)) {
            return false;
        }
        if (!Objects.equals(this.bibProperty.getValue(), other.bibProperty.getValue())) {
            return false;
        }
        if (!Objects.equals(this.timingLocationId, other.timingLocationId)) {
            return false;
        }
        
        return true;
    }
    
    public static Callback<CookedTimeData, Observable[]> extractor() {
        return (CookedTimeData ct) -> new Observable[]{ct.bibProperty,ct.ignoreTimeBoolean};
    }

//    @Transient
//    public String getParticipantName() {
//        return participantNameProperty.getValue(); 
//    }
//    public StringProperty participantNameProperty() {
//        return participantNameProperty;
//    }    

}
