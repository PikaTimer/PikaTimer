/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.race;

import com.pikatimer.util.DurationFormatter;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
@Entity
@DynamicUpdate
@Table(name="race_waves")
public class Wave {
    
    //private final Wave self; 
    private final IntegerProperty IDProperty;
    private Race race; 
    private final StringProperty waveName;
    private  LocalTime waveStart;
    private final StringProperty waveStartString;
    private  Duration waveMaxStart;
    private final StringProperty waveMaxStartString;
    private WaveAssignment waveAssignmentMethod;
    private final StringProperty waveAssignmentMethodProperty;
    private final StringProperty waveAssignmentStart;
    private final StringProperty waveAssignmentEnd; 
    private final IntegerProperty wavePosition;
   
    
    

    public Wave(Race r){
        this(); 
        this.setRace(r);
    }
   public Wave() {
        //this.self = this; 
        this.IDProperty = new SimpleIntegerProperty();
        this.waveName = new SimpleStringProperty();
        this.waveAssignmentMethodProperty = new SimpleStringProperty();
        this.waveAssignmentStart = new SimpleStringProperty();
        this.waveAssignmentEnd = new SimpleStringProperty();
        this.waveStartString = new SimpleStringProperty();
        this.waveMaxStartString = new SimpleStringProperty();
        this.wavePosition = new SimpleIntegerProperty();
        //this.raceCutoff = LocalTime.parse("10:30");
        
        //raceSplits = FXCollections.observableArrayList();
        //raceWaves = FXCollections.observableArrayList();
        
    }
   
//    @Override
//    public boolean equals(Object w) {
//        return true; 
//    }
//    
    @Id
    @GenericGenerator(name="wave_id" , strategy="increment")
    @GeneratedValue(generator="wave_id")
    @Column(name="wave_ID")
    public Integer getID() {
        return IDProperty.getValue(); 
    }
    public void setID(Integer id) {
        IDProperty.setValue(id);
    }
    public IntegerProperty idProperty() {
        return IDProperty; 
    }
    
    public IntegerProperty wavePositionProperty() {
        return wavePosition; 
    }
    
    @ManyToOne
    @JoinColumn(name = "RACE_ID",nullable=false)
    public Race getRace() {
        return race;
    }
    public void setRace(Race r) {
        race=r;
    }
    
    @Column(name="WAVE_NAME")
    public String getWaveName() {
        return waveName.getValueSafe();
    }
    public void setWaveName(String n) {
        waveName.setValue(n);
    }
    public StringProperty waveNameProperty() {
        return waveName;
    }
    
    @Column(name="WAVE_START_TIME",nullable=true)
    public String getWaveStart() {
        if( waveStart != null) {
            return waveStart.format(DateTimeFormatter.ISO_LOCAL_TIME);
        } else {
            return "";
        }
        //return waveCutoff.toString();
    }
    public void setWaveStart(String c) {
        if(! c.isEmpty()) {
            //Fix this to watch for parse exceptions
            waveStart = LocalTime.parse(c, DateTimeFormatter.ISO_LOCAL_TIME );
            waveStartString.set(waveStart.format(DateTimeFormatter.ISO_LOCAL_TIME));
        }
    }
    public LocalTime waveStartProperty(){
        return waveStart; 
    }
    public StringProperty waveStartStringProperty(){
        return waveStartString;
    }
    @Column(name="WAVE_MAX_START_TIME",nullable=true)
    public Long getWaveMaxStart() {
        if( waveMaxStart != null) {
            return waveMaxStart.toNanos();
        } else {
            return 0L;
        }
        //return waveCutoff.toString();
    }
    public void setWaveMaxStart(Long c) {
        if(c != null) {
            //Fix this to watch for parse exceptions
            waveMaxStart = Duration.ofNanos(c);
            waveMaxStartString.set(DurationFormatter.durationToString(waveMaxStart, 0, Boolean.FALSE));
        }
    }
    public Duration waveMaxStartProperty(){
        return waveMaxStart; 
    }
    public StringProperty waveMaxStartStringProperty(){
        return waveMaxStartString;
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name="WAVE_ASSIGNMENT_METHOD")
    public WaveAssignment getWaveAssignmentMethod() {
        return waveAssignmentMethod;
    }
    public void setWaveAssignmentMethod(WaveAssignment d) {
        waveAssignmentMethod=d;
        if (waveAssignmentMethod != null) waveAssignmentMethodProperty.setValue(d.toString());
        
    }
    public void setWaveAssignmentMethod(String d) {
        waveAssignmentMethod=WaveAssignment.valueOf(d); 
        if (waveAssignmentMethod != null) waveAssignmentMethodProperty.setValue(d.toString());
    }
    public StringProperty waveAssignmentMethodProperty() {
        return waveAssignmentMethodProperty;
    }
    
    @Column(name="WAVE_ASSIGNMENT_ATTR1")
    public String getWaveAssignmentStart() {
        return waveAssignmentStart.getValueSafe();
    }
    public void setWaveAssignmentStart(String n) {
        waveAssignmentStart.setValue(n);
    }
    public StringProperty waveAssignmentStartProperty() {
        return waveAssignmentStart;
    }
    
    @Column(name="WAVE_ASSIGNMENT_ATTR2")
    public String getWaveAssignmentEnd() {
        return waveAssignmentEnd.getValueSafe();
    }
    public void setWaveAssignmentEnd(String n) {
        waveAssignmentEnd.setValue(n);
    }
    public StringProperty waveAssignmentEndProperty() {
        return waveAssignmentEnd;
    }


}
