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
package com.pikatimer.results;

import com.pikatimer.race.CourseRecord;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
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
    private final ObjectProperty<Duration> startDurationProperty; 
    private final ObjectProperty<Duration> waveStartDurationProperty;
    private final ObjectProperty<Duration> startOffsetProperty;
    private Duration waveStartDuration;
    private Duration finishDuration;
    private final ObjectProperty<Duration> finishTODProperty;
    private final ObjectProperty<Duration> finishDurationProperty;
    private final ObjectProperty<Duration> finishGunDurationProperty;
    private Map<Integer,Long> splitMap = new HashMap();
    private final ObservableMap<Integer,ObjectProperty<Duration>> splitPropertyMap = FXCollections.observableHashMap();
    private final IntegerProperty revision = new SimpleIntegerProperty(1); 
    private List<CourseRecord> courseRecordList = new ArrayList();
    private Boolean pendingRecalc=false;
    
    // Bib String
    // Race id
    // start Duration (from gun)
    // finish Duration (from start)
    // map<split,Duration (from gun)>
    // race id and bib form the "key" and will never change
    
    public Result() {
        startDuration = Duration.ZERO;
        waveStartDuration = Duration.ZERO;
        finishDuration = Duration.ZERO;
        startDurationProperty = new SimpleObjectProperty(startDuration);
        waveStartDurationProperty = new SimpleObjectProperty(waveStartDuration);
        startOffsetProperty = new SimpleObjectProperty(Duration.ZERO);
        finishDurationProperty = new SimpleObjectProperty(finishDuration);
        finishGunDurationProperty = new SimpleObjectProperty(finishDuration);
        finishTODProperty = new SimpleObjectProperty(finishDuration);
    }
    
    public void clearTimes(){
        startDuration = Duration.ZERO;
        //startTimeProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));

        waveStartDuration = Duration.ZERO;
        //startGunTimeProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));
        
        splitMap = new HashMap<>();
        
        finishDuration = Duration.ZERO;
        //finishTimeProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));
        //finishGunTimeProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));
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
        pendingRecalc=true;
    }
    public ObjectProperty<Duration> startTimeProperty(){
        return startDurationProperty; 
    }

    
    @Column(name="waveStart",nullable=true)
    public Long getWaveStart() {
        if( waveStartDuration != null) {
            return waveStartDuration.toNanos();
        } else {
            return 0L;
        }
    }
    public void setWaveStart(Long c) {
        if(c != null) {
            waveStartDuration = Duration.ofNanos(c);
            if (startDuration == null || startDuration.isZero()) setStartDuration(waveStartDuration);
        }
    }
    @Transient
    public Duration getWaveStartDuration(){
        return waveStartDuration;
    }
    public void setWaveStartDuration(Duration ws) {
        waveStartDuration = ws;
        if (startDuration == null || startDuration.isZero()) setStartDuration(waveStartDuration);
    }
    public ObjectProperty<Duration> waveStartTimeProperty(){
        return waveStartDurationProperty; 
    }
    
    public ObjectProperty<Duration> startOffsetProperty(){
        return startOffsetProperty; 
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
            pendingRecalc=true;
        }
    }
    @Transient  
    public Duration getFinishDuration(){
        return finishDuration;
    }
    public void setFinishDuration(Duration f){
        finishDuration = f;
        pendingRecalc=true;
    }
    
    public ObjectProperty<Duration> finishTimeProperty(){
        return finishDurationProperty; 
    }
    public ObjectProperty<Duration> finishGunTimeProperty(){
        return finishGunDurationProperty; 
    }
    public ObjectProperty<Duration> finishTODProperty(){
        return finishTODProperty; 
    }
    
    @Transient
    public List<CourseRecord> getCourseRecords(){
        return courseRecordList;
    }
    public void addCourseRecord(CourseRecord cr){
        courseRecordList.add(cr);
    }
    public void delCourseRecord(CourseRecord cr){
        courseRecordList.remove(cr);
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
        pendingRecalc=true;
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
        pendingRecalc=true;
    }
    
    public ObjectProperty<Duration> splitTimeByIDProperty(Integer splitID) {
        if (!splitPropertyMap.containsKey(splitID)) {
            //System.out.println("Split id " + splitID + " not found, adding one in...");
            splitPropertyMap.put(splitID, new SimpleObjectProperty(Duration.ofNanos(Long.MAX_VALUE)));
        } 
        return splitPropertyMap.get(splitID);
    }
    
    public void recalcTimeProperties(){
        if (pendingRecalc){
            
            //System.out.println("Result::recalcTimeProperties for bib " + bib.get());
//            System.out.println(" StartDuration: " + startDuration.toString());
//            System.out.println(" waveStartDuration: " + waveStartDuration.toString());
//            System.out.println(" finishDuration: " + finishDuration.toString());

            if (!startDuration.equals(startDurationProperty.get())) startDurationProperty.set(startDuration);
            if (!waveStartDuration.equals(waveStartDurationProperty.get())) waveStartDurationProperty.set(waveStartDuration);
            
            Duration startOffset = Duration.ZERO;
            if (splitMap.containsKey(0)) {
                startOffset = Duration.ofNanos(splitMap.get(0)).minus(waveStartDuration);
            }
            
            if (!startOffset.equals(startOffsetProperty.get())) startOffsetProperty.set(startOffset);
            
            if (finishDuration.isZero()) {
                finishDurationProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));
                finishTODProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));
            }
            else {
                finishDurationProperty.setValue(finishDuration.minus(startDuration));
                if (finishDuration.toDays()> 0) {
                    finishTODProperty.setValue(finishDuration.minus(Duration.ofDays(finishDuration.toDays())));
                } else finishTODProperty.setValue(finishDuration);
            }

            if (finishDuration.isZero()) finishGunDurationProperty.setValue(Duration.ofNanos(Long.MAX_VALUE));
            else finishGunDurationProperty.setValue(finishDuration.minus(waveStartDuration));
            
            
//            
//            System.out.println(" chipTime: " + finishDurationProperty.get().toString());
//            System.out.println(" gunTime: " + finishGunDurationProperty.get().toString());
            
            // now loop through and fix the splits...
            // missing splits are set to MAX_VALUE
            splitMap.keySet().forEach(splitID -> {
                Duration d;
                if (splitID != 0){
                    d = Duration.ofNanos(splitMap.get(splitID)).minus(startDuration);
                    if (d.isNegative()) d = Duration.ofNanos(Long.MAX_VALUE);
                } else {
                    d = Duration.ofNanos(splitMap.get(splitID));
                }
                if (splitPropertyMap.containsKey(splitID)) splitPropertyMap.get(splitID).set(d);
                else splitPropertyMap.put(splitID, new SimpleObjectProperty(d));
            });    
            
            splitPropertyMap.keySet().forEach(splitID -> {
                if (!splitMap.containsKey(splitID)) splitPropertyMap.get(splitID).set(Duration.ofNanos(Long.MAX_VALUE));
            
            });
            
            revision.set(revision.get() + 1);
            //System.out.println("Result revision is now " + revision.getValue().toString());
            pendingRecalc = false;
        }
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
