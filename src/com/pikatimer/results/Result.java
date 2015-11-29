/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.results;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;
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
    private final ObjectProperty<Duration> startTimeProperty; 
    private final ObjectProperty<Duration> startGunTimeProperty;
    private Duration startWaveStartDuration;
    private Duration finishDuration;
    private final ObjectProperty<Duration> finishTimeProperty;
    private final ObjectProperty<Duration> finishGunTimeProperty;
    private Map<Integer,Long> splitMap = new HashMap();
    private Map<Integer,ObjectProperty<Duration>> splitPropertyMap = new HashMap<>();
    private final IntegerProperty revision = new SimpleIntegerProperty(1); 
    
    // Bib String
    // Race id
    // start Duration (from gun)
    // finish Duration (from start)
    // map<split,Duration (from gun)>
    // race id and bib form the "key" and will never change
    
    public Result() {
        startDuration = Duration.ZERO;
        finishDuration = Duration.ZERO;
        startWaveStartDuration = Duration.ZERO;
        startTimeProperty = new SimpleObjectProperty(startDuration);
        startGunTimeProperty = new SimpleObjectProperty(startDuration);
        finishTimeProperty = new SimpleObjectProperty(finishDuration);
        finishGunTimeProperty = new SimpleObjectProperty(finishDuration);
    }
    
    public void clearTimes(){
        startDuration = Duration.ZERO;
        startTimeProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));

        startWaveStartDuration = Duration.ZERO;
        startGunTimeProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));
        
        splitMap = new HashMap<>();
        
        finishDuration = Duration.ZERO;
        finishTimeProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));
        finishGunTimeProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));
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
            setStartDuration(Duration.ofNanos(c));
        }
    }
    @Transient
    public Duration getStartDuration(){
        return startDuration;
    }
    public void setStartDuration(Duration s){
        startDuration = s;
        //revision.setValue(revision.get()+1);
        recalcTimeProperties();
    }
    public ObjectProperty<Duration> startTimeProperty(){
        if (startDuration.isZero()) startTimeProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));
        
        else startTimeProperty.setValue(startDuration);
        return finishTimeProperty; 
    }
    public ObjectProperty<Duration> startTimeGunTimeProperty(){
        if (startDuration.isZero()) startGunTimeProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));
        
        else startGunTimeProperty.setValue(startDuration.minus(startWaveStartDuration));
        return startGunTimeProperty; 
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
            if (startDuration == null || startDuration.isZero()) setStartDuration(startWaveStartDuration);
        }
    }
    @Transient
    public Duration getStartWaveStartDuration(){
        return startWaveStartDuration;
    }
    public void setStartWaveStartDuration(Duration ws) {
        startWaveStartDuration = ws;
        if (startDuration == null || startDuration.isZero()) setStartDuration(startWaveStartDuration);
        //revision.setValue(revision.get()+1);
        
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
            setFinishDuration(Duration.ofNanos(c));
        }
    }
    @Transient  
    public Duration getFinishDuration(){
        return finishDuration;
    }
    public void setFinishDuration(Duration f){
        finishDuration = f;
        //revision.setValue(revision.get()+1);
        recalcTimeProperties();
    }
    
    public ObjectProperty<Duration> finishTimeProperty(){
        return finishTimeProperty; 
    }
    public ObjectProperty<Duration> finishGunTimeProperty(){
        return finishGunTimeProperty; 
    }
    
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="split_id", insertable=false,updatable=false)
    @Column(name="split_time",nullable=false)
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
        //revision.setValue(revision.get()+1);
    }
    
    public ObjectProperty<Duration> splitTimeByIDProperty(Integer splitID) {
        if (splitPropertyMap.containsKey(splitID)) {
            return splitPropertyMap.get(splitID);
        } 
        return null;
    }
    
    private void recalcTimeProperties(){
        if (finishDuration.isZero()) finishTimeProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));
        else finishTimeProperty.setValue(finishDuration.minus(startDuration));
        
        if (finishDuration.isZero()) finishGunTimeProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));
        else finishGunTimeProperty.setValue(finishDuration.minus(startWaveStartDuration));
    }
    
    public void setUpdated(){
        Platform.runLater(() -> {
            revision.setValue(revision.get()+1);
        });
    }
    
    public static Callback<Result, Observable[]> extractor() {
        return (Result r) -> new Observable[]{r.revision};
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
