/*
 * Copyright (C) 2019 John Garner <segfaultcoredump@gmail.com>
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
package com.pikatimer.race;

import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.results.ProcessedResult;
import com.pikatimer.results.Result;
import com.pikatimer.results.ResultsDAO;
import com.pikatimer.timing.Segment;
import java.time.Duration;
import java.util.Objects;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;
import javax.persistence.Column;
import javax.persistence.Entity;
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
 * @author John Garner <segfaultcoredump@gmail.com>
 */

@Entity
@DynamicUpdate
@Table(name="Course_Records")
public class CourseRecord implements Comparable<CourseRecord>{
    
    //id
    private final IntegerProperty IDProperty = new SimpleIntegerProperty(); 
    
    // race_id
    private Race race; 
        
    // seg_id  
    private Integer segmentID;
    private final ObjectProperty<Segment> segmentProperty = new SimpleObjectProperty();
    // category
    private final StringProperty categoryProperty = new SimpleStringProperty(); // Overall or AG: "OVERALL", "F40-44", etc 
    // sex
    private final StringProperty sexProperty = new SimpleStringProperty(); // M/F
    // age
    private final StringProperty ageProperty = new SimpleStringProperty();  
    //time
    private Duration recordDuration = Duration.ZERO;
    private final ObjectProperty<Duration> recordDurationProperty = new SimpleObjectProperty(Duration.ZERO);

    // Details on the current record Holder
    //year
    private final StringProperty crYearProperty = new SimpleStringProperty(); // Year; 
    //name
    private final StringProperty crNameProperty = new SimpleStringProperty(); // Name;
    //city
    private final StringProperty crCityProperty = new SimpleStringProperty(); // Name;
    //state
    private final StringProperty crStateProperty = new SimpleStringProperty(); // Name;
    //country
    private final StringProperty crCountryProperty = new SimpleStringProperty(); // Name;
    //note
    private final StringProperty crNoteProperty = new SimpleStringProperty(); // Note; 

    // New CR's
    private Duration newDuration ;
    private final ObjectProperty<Duration> newRecordDurationProperty = new SimpleObjectProperty(Duration.ZERO);;       
    private final ObjectProperty<Result> newRecord = new SimpleObjectProperty();

    
    
    public CourseRecord(Race r){
        this(); 
        this.setRace(r);
    }
    public CourseRecord() {
        
    }
    
    public Boolean checkRecordEligible(Result r){
        // checks to see if we are elliglble for this record. 
        //System.out.println("CourseRecord::checkRecordEligible: Checking Bib " + r.getBib());
        Participant p = ParticipantDAO.getInstance().getParticipantByBib(r.getBib());
        if (p.getSex() == null ? sexProperty.getValueSafe() != null : !p.getSex().equals(sexProperty.getValueSafe())) return false;
        if (!categoryProperty.getValue().equals("OVERALL")) {
            if (!categoryProperty.getValue().equals(race.getAgeGroups().ageToAGString(p.getAge()))) return false;
        }
        //System.out.println("CourseRecord::checkRecordEligible: " + p.fullNameProperty().get() + " is eligible!");
        return true;
    }
    
    public void clearNewRecord(){
        if (newRecord.get() != null) newRecord.get().delCourseRecord(this);
        newRecord.set(null);
        newDuration = null;
        newRecordDurationProperty.set(Duration.ZERO);
    }
    
    public void checkRecord(Result r){
        if (!checkRecordEligible(r)) return; 
        
        ProcessedResult pr = ResultsDAO.getInstance().processResult(r, race);
        // is this an overall or a segment CR?
        Duration time;
        if (segmentID == null){
            time = pr.getChipFinish();
        } else {
            time = pr.getSegmentTime(segmentID);
        }
        if (time == null) return;
        
        if (Duration.ZERO.equals(currentRecordTime()) || time.minus(currentRecordTime()).isNegative()) {

            if (newRecord.get() != null) newRecord.get().delCourseRecord(this);
            newRecord.set(r);
            r.addCourseRecord(this);
            newDuration = time;
            newRecordDurationProperty.set(time);

            Participant p = ParticipantDAO.getInstance().getParticipantByBib(r.getBib());
            System.out.println("NEW Course Record!!! Old: " + sexProperty.get() + " " + categoryProperty.get() + " " + recordDuration.toString());
            System.out.println("NEW Course Record!!! New: " + p.fullNameProperty().get() + " " + newDuration.toString());
        }
        
    }
    

    public ObjectProperty<Result> newRecord(){
        return newRecord;
    }
    public ObjectProperty<Duration> newRecordDuration(){
        return newRecordDurationProperty;
    }
    public Duration currentRecordTime(){
        if (newDuration == null) return recordDuration;
        return newDuration;
    }
    
    @Id
    @GenericGenerator(name="cr_id" , strategy="increment")
    @GeneratedValue(generator="cr_id")
    @Column(name="ID")
    public Integer getID() {
        return IDProperty.getValue(); 
    }
    public void setID(Integer id) {
        IDProperty.setValue(id);
    }
    public IntegerProperty idProperty() {
        return IDProperty; 
    }
    
        
       
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RACE_ID",nullable=false)
    public Race getRace() {
        return race;
    }
    public void setRace(Race r) {
        race=r;
    }
    
    
    @Column(name="time",nullable=true)
    public Long getRecord() {
        if( recordDuration != null) {
            return recordDuration.toNanos();
        } else {
            return 0L;
        }
    }
    public void setRecord(Long c) {
        //System.out.println("CourseRecord::setRecord called with " + c);
        if(c != null) {
            setRecordDuration(Duration.ofNanos(c));
        }
    }
    @Transient  
    public Duration getRecordDuration(){
        return recordDuration;
    }
    public void setRecordDuration(Duration f){
        recordDuration = f;
        recordDurationProperty.setValue(f);
        //System.out.println("CourseRecord::setRecordDuration " + recordDuration.toString());

    }
    
    public ObjectProperty<Duration> recordDurationProperty(){
        return recordDurationProperty; 
    }
    
    @Column(name="seg_id",nullable=true)
    public Integer getSegmentID() {
        return segmentID;
    }
    public void setSegmentID(Integer c) {
        segmentID = c;
              
    }
    public ObjectProperty<Segment> segmentProperty(){
        if (segmentID != null) updateSegment();  
        return segmentProperty;
    }
    
    @Column(name="category")
    public String getCategory() {
        return categoryProperty.getValueSafe();
    }
    public void setCategory(String n) {
        categoryProperty.setValue(n);
    }
    public StringProperty categoryProperty() {
        return categoryProperty;
    }
    
    @Column(name="sex")
    public String getSex() {
        return sexProperty.getValueSafe();
    }
    public void setSex(String n) {
        sexProperty.setValue(n);
    }
    public StringProperty sexProperty() {
        return sexProperty;
    }
    
    @Column(name="age")
    public String getAge() {
        return ageProperty.getValueSafe();
    }
    public void setAge(String n) {
        ageProperty.setValue(n);
    }
    public StringProperty ageProperty() {
        return ageProperty;
    }
    
    @Column(name="year")
    public String getYear() {
        return crYearProperty.getValueSafe();
    }
    public void setYear(String n) {
        crYearProperty.setValue(n);
    }
    public StringProperty yearProperty() {
        return crYearProperty;
    }
    @Column(name="name")
    public String getName() {
        return crNameProperty.getValueSafe();
    }
    public void setName(String n) {
        crNameProperty.setValue(n);
    }
    public StringProperty nameProperty() {
        return crNameProperty;
    }
    
    @Column(name="note")
    public String getNote() {
        return crNoteProperty.getValueSafe();
    }
    public void setNote(String n) {
        crNoteProperty.setValue(n);
    }
    public StringProperty noteProperty() {
        return crNoteProperty;
    }
    
    @Column(name="city")
    public String getCity() {
        return crCityProperty.getValueSafe();
    }
    public void setCity(String n) {
        crCityProperty.setValue(n);
    }
    public StringProperty cityProperty() {
        return crCityProperty;
    }
    
    @Column(name="state")
    public String getState() {
        return crStateProperty.getValueSafe();
    }
    public void setState(String n) {
        crStateProperty.setValue(n);
    }
    public StringProperty stateProperty() {
        return crStateProperty;
    }
    
    @Column(name="country")
    public String getCountry() {
        return crCountryProperty.getValueSafe();
    }
    public void setCountry(String n) {
        crCountryProperty.setValue(n);
    }
    public StringProperty countryProperty() {
        return crCountryProperty;
    }
    
    public static Callback<CourseRecord, Observable[]> extractor() {
        return (CourseRecord cr) -> new Observable[]{cr.recordDurationProperty,cr.newRecord};
    }
    
    private void updateSegment(){
        if (segmentProperty.isNotNull().get()) return;
        if (race == null) return; 
        //System.out.println("CR::updateSegment() Called");
        if(segmentID != null) {
            //System.out.println("Looking for segment ID " + segmentID);
            race.getSegments().forEach(s -> {
                
                if (segmentID.equals(s.getID())) {
                    segmentProperty.set(s);
                }
            });
            System.out.println("  Segment is " + segmentProperty.get().getSegmentName());
        } else {
            System.out.println("  Segment is OVERALL");
        }
    }
    
    @Override
    public int compareTo(CourseRecord other) {
        
        // Sort Order:
        // Segment: Overall and then by SegID
        // Sex: Alpha
        // Category: Overall and then by AG
        
        
        if (this.segmentID == null && other.segmentID != null) return -1;
        if (this.segmentID != null && other.segmentID == null) return 1;
        if (this.segmentID != null && other.segmentID != null) {
            if (this.segmentID > other.segmentID) return 1;
            if (this.segmentID < other.segmentID) return -1;
        } 
        //System.out.println("CourseRecord::compareTo() " + this.segmentID + " vs " + other.segmentID);
                
        // If we get here then the segments are the same
        if (!this.sexProperty.get().equals(other.sexProperty.get())) {
            return sexProperty.get().compareTo(other.sexProperty.get()) * -1;
        }
        
        // we can't both be 'overall' at this point
        if ("OVERALL".equals(this.categoryProperty.get()) ) return -1;
        if ("OVERALL".equals(other.categoryProperty.get()) ) return 1;
        return this.categoryProperty.get().compareTo(other.categoryProperty.get()) ;
        
        
        // compare the time for now
//        if (this.recordDuration != null && other.recordDuration == null) return 1;
//        if (this.recordDuration == null && other.recordDuration != null) return -1;
//        return this.recordDuration.compareTo(other.recordDuration) ;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.race.getID());
        hash = 71 * hash + Objects.hashCode(this.segmentID);
        hash = 71 * hash + Objects.hashCode(this.categoryProperty.get());
        hash = 71 * hash + Objects.hashCode(this.sexProperty.get());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CourseRecord other = (CourseRecord) obj;
        if (!Objects.equals(this.race, other.race)) {
            return false;
        }
        if (!Objects.equals(this.segmentProperty.getValue(), other.segmentProperty.getValue())) {
            return false;
        }
        if (!Objects.equals(this.categoryProperty.getValue(), other.categoryProperty.getValue())) {
            return false;
        }
        if (!Objects.equals(this.sexProperty.getValue(), other.sexProperty.getValue())) {
            return false;
        }
        return true;
    }
    
}
