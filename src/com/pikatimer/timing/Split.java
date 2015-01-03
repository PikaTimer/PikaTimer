/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import com.pikatimer.race.Race;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.Pace;
import com.pikatimer.util.Unit;
import java.math.BigDecimal;
import java.time.Duration;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
    private final IntegerProperty IDProperty; // split_id
    private Race race; // race_id
    private TimingLocation splitLocation; // timing_loc_id
    private final StringProperty splitLocationString; 
    private final IntegerProperty splitPosition; // split_seq_number
    private BigDecimal splitDistance; //
    private final StringProperty splitDistanceString; // split_distance
    private Unit splitDistanceUnit; // split_dist_unit
    private final StringProperty splitDistanceUnitString; 
    private Pace splitPace; // split_pace_unit
    private final StringProperty splitPaceString; 
    private final StringProperty splitName;
    private  Duration splitCutoff;
    private final StringProperty splitCutoffString;
    
    
//   split_id int primary key, 
//    race_id int, 
//    timing_loc_id int, 
//    split_seq_number int, 
//    split_distance numeric,
//    split_dist_unit varchar, 
//    split_pace_unit varchar, 
//    split_name varchar, 
//    short_name varchar, 
//    cutoff_time bigint
//    
    

    public Split(Race r){
        this(); 
        this.setRace(r);
    }
   public Split() {
        //this.self = this; 
        this.IDProperty = new SimpleIntegerProperty();
        this.splitName = new SimpleStringProperty();
        this.splitDistanceString = new SimpleStringProperty();
        this.splitCutoffString = new SimpleStringProperty();
        this.splitPosition = new SimpleIntegerProperty();
        this.splitDistanceUnitString = new SimpleStringProperty(); 
        this.splitPaceString = new SimpleStringProperty();
        this.splitLocationString = new SimpleStringProperty();
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
    
    @ManyToOne
    @JoinColumn(name = "RACE_ID",nullable=false)
    public Race getRace() {
        return race;
    }
    public void setRace(Race r) {
        race=r;
    }
    
    @ManyToOne
    @JoinColumn(name = "TIMING_LOC_ID",nullable=false)
    public TimingLocation getTimingLocation() {
        return splitLocation;
    }
    public void setTimingLocation(TimingLocation l) {
        if (l != null) {
            System.out.println("Split.setTimingLocation: " + l.getID());
            splitLocation=l;
            splitLocationString.unbind();
            splitLocationString.bind(l.raceLocationProperty()); 
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
            splitCutoffString.set(DurationFormatter.durationToString(splitCutoff, 0, Boolean.TRUE));
        }
    }
    public Duration splitCutoffProperty(){
        return splitCutoff; 
    }
    public StringProperty splitCutoffStringProperty(){
        return splitCutoffString;
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

}
