/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.results;

import com.pikatimer.util.DurationFormatter;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
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
@Table(name="results")
public class Result {
    private final StringProperty bib = new SimpleStringProperty();
    private Integer id;
    private Integer raceID ; 
    private Duration startDuration;
    private Duration startWaveStartDuration;
    private Duration finishDuration;
    private Map<Integer,Long> splitMap = new HashMap<>();
    
    // Bib String
    // Race id
    // start Duration (from gun)
    // finish Duration (from start)
    // map<split,Duration (from gun)>
    // race id and bib form the "key" and will never change
    
    public Result() {
        
    }
    public void clearTimes(){
        startDuration = null;
        finishDuration = null;
        splitMap = new HashMap<>();
        startWaveStartDuration = null;
    }
    
    @Id
    @GenericGenerator(name="result_id" , strategy="increment")
    @GeneratedValue(generator="result_id")
    @Column(name="result_id")
    public Integer getID() {
        return id;
    }
    public void setID(Integer id) {
        this.id = id;
    }

    @Column(name="bib")
    public String getBib() {
        return bib.getValueSafe();
    }
    public void setBib(String b){
        bib.setValue(b);
    }
    public StringProperty bibProperty(){
        return bib;
    }
    
    @Column(name="race_id")
    public Integer getRaceID(){
        return raceID;
    }
    public void setRaceID(Integer id) {
        raceID = id;
        
    }
    
    @Column(name="partStart",nullable=true)
    public Long getStart() {
        if( startDuration != null) {
            return startDuration.toNanos();
        } else {
            return 0L;
        }
    }
    public void setStart(Long c) {
        if(c != null) {
            //Fix this to watch for parse exceptions
            startDuration = Duration.ofNanos(c);
        }
    }
    @Transient
    public Duration getStartDuration(){
        return startDuration;
    }
    public void setStartDuration(Duration s){
        startDuration = s;
    }
    
    @Column(name="waveStart",nullable=true)
    public Long getWaveStart() {
        if( startWaveStartDuration != null) {
            return startWaveStartDuration.toNanos();
        } else {
            return 0L;
        }
    }
    public void setWaveStart(Long c) {
        if(c != null) {
            
            startWaveStartDuration = Duration.ofNanos(c);
            if (startDuration == null || startDuration.isZero()) startDuration = startWaveStartDuration;

        }
    }
    @Transient
    public Duration getStartWaveStartDuration(){
        return startWaveStartDuration;
    }
    public void setStartWaveStartDuration(Duration ws) {
        startWaveStartDuration = ws;
        if (startDuration == null || startDuration.isZero()) startDuration = startWaveStartDuration;
    }
    
    @Column(name="partFinish",nullable=true)
    public Long getFinish() {
        if( finishDuration != null) {
            return finishDuration.toNanos();
        } else {
            return 0L;
        }
    }
    public void setFinish(Long c) {
        if(c != null) {
            //Fix this to watch for parse exceptions
            finishDuration = Duration.ofNanos(c);
        }
    }
    @Transient  
    public Duration getFinishDuration(){
        return finishDuration;
    }
    public void setFinishDuration(Duration f){
        finishDuration = f;
    }
    
    
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="split_id", insertable=false,updatable=false)
    @Column(name="split_time")
    @CollectionTable(name="split_results", joinColumns=@JoinColumn(name="result_id"))
    public Map<Integer,Long> getSplitMap(){
        return splitMap;
    }
    public void setSplitMap(Map<Integer,Long> m){
        splitMap = m; 
    }
    
    
    public Duration getSplitTime(Integer splitID) {
        if (splitMap.containsKey(splitID)) {
            return Duration.ofNanos(splitMap.get(splitID));
        } else {
            return Duration.ZERO;
        }
    }
    public void setSplitTime(Integer splitID, Duration t) {
        if(t == null || t.isZero()) {
            splitMap.remove(splitID);
        } else {
            splitMap.put(splitID, t.toNanos());
        }
    }
    
    
    @Transient
    public Boolean isEmpty(){
        return (finishDuration == null || finishDuration.isZero()) 
                && (startDuration == null || startDuration.isZero()) 
                && (splitMap == null || splitMap.isEmpty()); 
    }
    
    @Override
    public int hashCode(){
        int hash = 7;
        
        hash = 37 * hash + Objects.hashCode(this.bib.getValueSafe());
        hash = 37 * hash + Objects.hashCode(this.raceID);
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
        final Result other = (Result) obj;
        if (!Objects.equals(this.bib.getValueSafe(), other.bib.getValueSafe())) {
            return false;
        }
        return Objects.equals(this.raceID, other.raceID);
    }

    
}
