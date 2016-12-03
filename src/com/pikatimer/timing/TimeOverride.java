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
package com.pikatimer.timing;

import com.pikatimer.race.Wave;
import com.pikatimer.util.DurationFormatter;
import java.time.Duration;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
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
@Table(name="overrides")
public class TimeOverride {
    private Integer timeOverrideID;
    private String chip;
    private Integer splitId;
    private Duration timestamp;
    //private Boolean relativeTime; 
    private StringProperty bibProperty = new SimpleStringProperty();
    private IntegerProperty splitIdProperty = new SimpleIntegerProperty();
    private BooleanProperty relativeProperty = new SimpleBooleanProperty(false);
    private ObjectProperty<Duration> durationProperty = new SimpleObjectProperty();
    private StringProperty timestampStringProperty = new SimpleStringProperty();

    
    public static Callback<TimeOverride, Observable[]> extractor() {
        return (TimeOverride w) -> new Observable[]{w.bibProperty,w.durationProperty,w.splitIdProperty};
    }
    
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
            setTimestamp(Duration.ofNanos(c));
        }
    }
    @Transient
    public Duration getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Duration d) {
        
        this.timestamp = d;
        durationProperty.set(timestamp);
        if (relativeProperty.getValue()) timestampStringProperty.set(DurationFormatter.durationToString(timestamp, 3, Boolean.FALSE).replace(".000", ""));
        else timestampStringProperty.setValue(DurationFormatter.durationToString(timestamp, 3).replace(".000", ""));
    }
    public ObjectProperty<Duration> timestampProperty(){
        return this.durationProperty;
    }
    public StringProperty timestampStringProperty(){
        return this.timestampStringProperty;
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
        splitIdProperty.set(i);
    }
    
    @Column(name="relative_to_start")
    public Boolean getRelative() {
        return relativeProperty.getValue();
        // relativeTime;
    }
    public void setRelative(Boolean r) {
        //relativeTime = r;
        relativeProperty.setValue(r);
        if (relativeProperty.getValue()) timestampStringProperty.set(DurationFormatter.durationToString(timestamp, 3, Boolean.FALSE).replace(".000", ""));
        else timestampStringProperty.setValue(DurationFormatter.durationToString(timestamp, 3).replace(".000", ""));
    }
    public BooleanProperty relativeProperty(){
        return relativeProperty;
    }
    
    
}
