/*
 * Copyright (C) 2017 John Garner <segfaultcoredump@gmail.com>
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
import com.pikatimer.race.RaceDAO;
import com.pikatimer.util.Pace;
import javafx.beans.Observable;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
 * @author John Garner <segfaultcoredump@gmail.com>
 */
@Entity
@DynamicUpdate
@Table(name="race_segment")
public class Segment implements Comparable<Segment>{
    private final IntegerProperty IDProperty = new SimpleIntegerProperty(); // split_id
    private Race race; // race_id
    private Integer startSplitID; // 
    private Integer endSplitID; 
    private final StringProperty segmentName = new SimpleStringProperty(); 
    private final StringProperty endSplitStringProperty = new SimpleStringProperty();
    private final StringProperty startSplitStringProperty = new SimpleStringProperty();
    private final StringProperty distanceStringProperty = new SimpleStringProperty("0");
    
    private Pace customPace;
    private BooleanProperty useCustomPace = new SimpleBooleanProperty(false);
    private BooleanProperty hideOnResults = new SimpleBooleanProperty(false);
    
    
    public Segment(Race r){

        setRace(r);
    }
    
    public Segment() {

    }
   public static Callback<Segment, Observable[]> extractor() {
        return (Segment s) -> new Observable[]{s.segmentName,s.endSplitStringProperty,s.startSplitStringProperty,s.distanceStringProperty};
    }
   
    @Id
    @GenericGenerator(name="segment_id" , strategy="increment")
    @GeneratedValue(generator="segment_id")
    @Column(name="SEGMENT_ID")
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
   
    @Column(name="SEGMENT_NAME")
    public String getSegmentName() {
        return segmentName.getValueSafe();
    }
    public void setSegmentName(String n) {
        segmentName.setValue(n);
    }
    public StringProperty segmentNameProperty() {
        return segmentName;
    }
    
    @Column(name = "START_SPLIT_ID",nullable=false)
    public Integer getStartSplitID() {
        return startSplitID;
    }
    public void setStartSplitID(Integer id) {
        startSplitID = id;
    }
    @Transient
    public Split getStartSplit() {
        return RaceDAO.getInstance().getSplitByID(startSplitID);
    }
    public void setStartSplit(Split s) {
        if (s != null) {
            System.out.println("Segment.setStartSplit: " + s.getID());
            startSplitID=s.getID();
            startSplitStringProperty.unbind();
            startSplitStringProperty.bind(s.splitNameProperty());
            
            checkSplitOrder();
            
            updateDistanceStringProperty();

        } else {
            System.out.println("Segment.setStartSplit: null"); 
        }
    }
    public StringProperty startSplitStringProperty() {
        if ( ! startSplitStringProperty.isBound() && endSplitID != null && getStartSplit() != null) startSplitStringProperty.bind(getStartSplit().splitNameProperty());
        return startSplitStringProperty; 
    }
    
    @Column(name = "END_SPLIT_ID",nullable=false)
    public Integer getEndSplitID() {
        return endSplitID;
    }
    public void setEndSplitID(Integer id) {
        endSplitID = id;
    }
    
    
    @Column(name="HIDDEN")
    public Boolean getHidden() {
        return hideOnResults.getValue();
    }
    public void setHidden(Boolean n) {
        hideOnResults.setValue(n);
    }
    public BooleanProperty hiddenProperty() {
        return hideOnResults;
    }
    
    @Column(name="use_custom_pace")
    public Boolean getUseCustomPace() {
        return useCustomPace.getValue();
    }
    public void setUseCustomPace(Boolean n) {
        useCustomPace.setValue(n);
    }
    public BooleanProperty useCustomPaceProperty() {
        return useCustomPace;
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name="pace_unit")
    public Pace getCustomPace() {
        return customPace;
    }
    public void setCustomPace(Pace p) {
        customPace = p;
    }
    
    
    @Transient
    public Split getEndSplit() {
        return RaceDAO.getInstance().getSplitByID(endSplitID);
    }
    public void setEndSplit(Split s) {
        if (s != null) {
            System.out.println("Segment.setEndSplit: " + s.getID());
            endSplitID=s.getID();
            endSplitStringProperty.unbind();
            endSplitStringProperty.bind(s.splitNameProperty());
            checkSplitOrder();
            updateDistanceStringProperty();
        } else {
            System.out.println("Segment.setEndSplit: null"); 
        }
    }
    public StringProperty endSplitStringProperty() {
        if ( ! endSplitStringProperty.isBound() && endSplitID != null && getEndSplit() != null) endSplitStringProperty.bind(getEndSplit().splitNameProperty());
        return endSplitStringProperty; 
    }
    
    
    
    
    
    
    @Transient
    public Integer getEndSplitPosition(){
        return getEndSplit().getPosition();
    }
    @Transient
    public Integer getStartSplitPosition(){
        return getStartSplit().getPosition();
    }
    public StringProperty distanceStringProperty(){
        updateDistanceStringProperty();
        return distanceStringProperty;
    }
    
    @Transient
    public Float getSegmentDistance(){
        return getEndSplit().getSplitDistance().subtract(getStartSplit().getSplitDistance()).abs().floatValue();
    }
    private void updateDistanceStringProperty(){
        if (getEndSplit() != null && getStartSplit() != null) {
            distanceStringProperty.unbind();
            distanceStringProperty.bind(new StringBinding(){
                {
                super.bind(getEndSplit().splitDistanceProperty(),getStartSplit().splitDistanceProperty());
                }
                @Override
                protected String computeValue() {
                    return getEndSplit().getSplitDistance().subtract(getStartSplit().getSplitDistance()).abs().toPlainString().concat(" " + getEndSplit().getSplitDistanceUnits().toShortString());
                }
            });
            //distanceStringProperty.setValue(getEndSplit().getSplitDistance().subtract(getStartSplit().getSplitDistance()).abs().toPlainString().concat(" " + getEndSplit().getSplitDistanceUnits().toShortString()));
        } else {
            distanceStringProperty.unbind();
            distanceStringProperty.setValue("0");
        }
            
    }

    private void checkSplitOrder() {
        if (getEndSplit() != null && getStartSplit() != null && getStartSplit().getSplitDistance().compareTo(getEndSplit().getSplitDistance()) > 0 ) {
            // Switch them
            Integer t = endSplitID;
            endSplitID = startSplitID;
            startSplitID = t; 
            
            endSplitStringProperty.unbind();
            endSplitStringProperty.bind(getEndSplit().splitNameProperty());
            
            startSplitStringProperty.unbind();
            startSplitStringProperty.bind(getStartSplit().splitNameProperty());
        }
    }

    @Override
    public int compareTo(Segment other) {
        // compare end split
        if (this.getEndSplit() != null && other.getEndSplit() == null) return 1;
        if (this.getEndSplit() == null && other.getEndSplit() != null) return -1;
        if (this.getEndSplitPosition() > other.getEndSplitPosition()) return 1;
        if (this.getEndSplitPosition() < other.getEndSplitPosition()) return -1;
        
        // compare start split
        if (this.getStartSplit() != null && other.getStartSplit() == null) return 1;
        if (this.getStartSplit() == null && other.getStartSplit() != null) return -1;
        if (this.getStartSplitPosition() > other.getStartSplitPosition()) return 1;
        if (this.getStartSplitPosition() < other.getStartSplitPosition()) return -1;
        
        return 0;
    }
    
    
}
