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

import com.pikatimer.race.Race;
import com.pikatimer.util.Pace;
import com.pikatimer.util.Unit;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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


/**
 *
 * @author jcgarner
 */
@Entity
@DynamicUpdate
@Table(name="race_split")
public class Split {
    
    //private final Wave self; 
    private final IntegerProperty IDProperty = new SimpleIntegerProperty(); // split_id
    private Race race; // race_id
    private TimingLocation splitLocation; // timing_loc_id
    private final StringProperty splitLocationString = new SimpleStringProperty(); 
    private final IntegerProperty splitPosition  = new SimpleIntegerProperty(); // split_seq_number
    private BigDecimal splitDistance; //
    private final StringProperty splitDistanceString = new SimpleStringProperty(); // split_distance
    private Unit splitDistanceUnit; // split_dist_unit
    private final StringProperty splitDistanceUnitString = new SimpleStringProperty(); 
    private Pace splitPace; // split_pace_unit
    private final StringProperty splitPaceString = new SimpleStringProperty();
    private final StringProperty splitName = new SimpleStringProperty();
    private Duration splitCutoff = Duration.ZERO;
    private Duration splitMinTime = Duration.ZERO;
    private final StringProperty splitCutoffString = new SimpleStringProperty();
    private final BooleanProperty mandatoryProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty ignoreProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty splitCutoffIsRelativeProperty = new SimpleBooleanProperty(true);
    private final ObjectProperty<Duration> splitMinTimeProperty = new SimpleObjectProperty(Duration.ZERO);
    

    public Split(Race r){
        this(); 
        this.setRace(r);
    }
   public Split() {
    }
   
    public static Callback<Split, Observable[]> extractor() {
        return (Split s) -> new Observable[]{s.splitName,s.splitDistanceString,s.splitPosition,s.splitLocationString,s.splitMinTimeProperty};
    }

    @Id
    @GenericGenerator(name="split_id" , strategy="increment")
    @GeneratedValue(generator="split_id")
    @Column(name="SPLIT_ID")
    public Integer getID() {
        return IDProperty.getValue(); 
    }
    public void setID(Integer id) {
        IDProperty.setValue(id);
    }
    public IntegerProperty idProperty() {
        return IDProperty; 
    }
    
    @Column(name="SPLIT_SEQ_NUMBER")
    public Integer getPosition() {
        return splitPosition.getValue(); 
    }
    public void setPosition(Integer id) {
        System.out.println("Split.setPosition: was " + splitPosition + " now " + id);
        splitPosition.setValue(id);
    }
    public IntegerProperty splitPositionProperty() {
        return splitPosition; 
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RACE_ID",nullable=false)
    public Race getRace() {
        return race;
    }
    public void setRace(Race r) {
        race=r;
    }
    
    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "TIMING_LOC_ID",nullable=false)
    @Column(name = "TIMING_LOC_ID",nullable=false)
    public Integer getTimingLocationID() {
        return splitLocation.getID();
    }
    public void setTimingLocationID(Integer id) {
        setTimingLocation(TimingDAO.getInstance().getTimingLocationByID(id));
    }
    @Transient
    public TimingLocation getTimingLocation() {
        return splitLocation;
    }
    public void setTimingLocation(TimingLocation l) {
        if (l != null) {
            System.out.println("Split.setTimingLocation: " + l.getID());
            splitLocation=l;
            splitLocationString.unbind();
            splitLocationString.bind(l.LocationNameProperty()); 
            System.out.println("Split.setTimingLocation: " + splitLocationString.getValueSafe()); 
        } else {
            System.out.println("Split.setTimingLocation: null"); 
        }
    }
    public StringProperty timingLocationProperty() {
        return splitLocationString; 
    }
    
    @Column(name="SPLIT_NAME")
    public String getSplitName() {
        return splitName.getValueSafe();
    }
    public void setSplitName(String n) {
        splitName.setValue(n);
    }
    public StringProperty splitNameProperty() {
        return splitName;
    }
    
    @Column(name="MIN_TIME",nullable=true)
    public Long getSplitMinTime() {
        if( splitMinTime != null) {
            return splitMinTime.toNanos();
        } else {
            return 0L;
        }
    }
    public void setSplitMinTime(Long c) {
        if(c != null) {
            //Fix this to watch for parse exceptions
            System.out.println("splitMinTimeProperty: was :" + splitMinTimeProperty.getValue().toString());
            splitMinTime = Duration.ofNanos(c);
            splitMinTimeProperty.setValue(splitMinTime);
            Platform.runLater(() -> {splitMinTimeProperty.setValue(splitMinTime);});
            System.out.println("splitMinTimeProperty: now :" + splitMinTimeProperty.getValue().toString());
        }
    }
    public Duration splitMinTimeDuration(){
        return splitMinTime; 
    }
    
    
    @Column(name="CUTOFF_TIME",nullable=true)
    public Long getSplitCutoff() {
        if( splitCutoff != null) {
            return splitCutoff.toNanos();
        } else {
            return 0L;
        }
    }
    public void setSplitCutoff(Long c) {
        if(c != null) {
            //Fix this to watch for parse exceptions
            splitCutoff = Duration.ofNanos(c);
        }
    }
    public Duration splitCutoffDuration(){
        return splitCutoff; 
    }
    
    
    
    
    
    @Column(name="SPLIT_DISTANCE")
    public BigDecimal getSplitDistance() {
        return splitDistance;
    }
    public void setSplitDistance(BigDecimal d) {
        splitDistance = d; 
        if(splitDistance != null && splitDistanceUnit != null)
            splitDistanceString.setValue(splitDistance.toPlainString()+ " " + splitDistanceUnit.toShortString()); 
    }
    public StringProperty splitDistanceProperty() {
        return splitDistanceString; 
    }

    @Enumerated(EnumType.STRING)
    @Column(name="SPLIT_DIST_UNIT")
    public Unit getSplitDistanceUnits() {
        return splitDistanceUnit;
    }
    public void setSplitDistanceUnits(Unit d) {
        splitDistanceUnit=d;
        if (splitDistanceUnit != null) splitDistanceUnitString.setValue(d.toString());
        if(splitDistance != null && splitDistanceUnit != null)
            splitDistanceString.setValue(splitDistance.toPlainString() + " " +splitDistanceUnit.toShortString()); 
    }
    public StringProperty splitDistanceUnitsProperty() {
        return splitDistanceUnitString; 
    }
    
    @Column(name="MANDATORY")
    public Boolean getMandatorySplit(){
        return mandatoryProperty.getValue();
    }
    public void setMandatorySplit(Boolean m){
        mandatoryProperty.setValue(m);
    }
    
    @Column(name="IGNORE_TIME")
    public Boolean getIgnoreTime(){
        return ignoreProperty.getValue();
    }
    public void setIgnoreTime(Boolean m){
        ignoreProperty.setValue(m);
    }

    @Column(name="CUTOFF_ABSOLUTE")
    
    public Boolean getSplitCutoffIsRelative(){
        return splitCutoffIsRelativeProperty.getValue();
    }
    public void setSplitCutoffIsRelative(Boolean m){
        splitCutoffIsRelativeProperty.setValue(m);
    }        

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.race);
        hash = 29 * hash + Objects.hashCode(this.splitLocation);
        hash = 29 * hash + Objects.hashCode(this.splitLocationString);
        hash = 29 * hash + Objects.hashCode(this.splitPosition);
        hash = 29 * hash + Objects.hashCode(this.splitDistance);
        hash = 29 * hash + Objects.hashCode(this.splitDistanceString);
        hash = 29 * hash + Objects.hashCode(this.splitDistanceUnit);
        hash = 29 * hash + Objects.hashCode(this.splitDistanceUnitString);
        hash = 29 * hash + Objects.hashCode(this.splitPace);
        hash = 29 * hash + Objects.hashCode(this.splitPaceString);
        hash = 29 * hash + Objects.hashCode(this.splitName);
        hash = 29 * hash + Objects.hashCode(this.splitCutoff);
        hash = 29 * hash + Objects.hashCode(this.splitCutoffString);
        return hash;
    }

    @Override
    public String toString() {
        return splitName.getValueSafe() ;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Split other = (Split) obj;
        if (!Objects.equals(this.race, other.race)) {
            return false;
        }
        if (!Objects.equals(this.splitLocation, other.splitLocation)) {
            return false;
        }
        if (!Objects.equals(this.splitLocationString.getValue(), other.splitLocationString.getValue())) {
            return false;
        }
        if (!Objects.equals(this.splitPosition.getValue(), other.splitPosition.getValue())) {
            return false;
        }
        if (!Objects.equals(this.splitDistance, other.splitDistance)) {
            return false;
        }
        if (this.splitDistanceUnit != other.splitDistanceUnit) {
            return false;
        }

        if (this.splitPace != other.splitPace) {
            return false;
        }

        if (!Objects.equals(this.splitName.getValue(), other.splitName.getValue())) {
            return false;
        }
        if (!Objects.equals(this.splitCutoff, other.splitCutoff)) {
            return false;
        }

        return true;
    }

}
