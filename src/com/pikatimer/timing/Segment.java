/*
 * Copyright (C) 2016 John Garner <segfaultcoredump@gmail.com>
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
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
@Table(name="race_segment")
public class Segment {
    private final IntegerProperty IDProperty = new SimpleIntegerProperty(); // split_id
    private Race race; // race_id
    private Integer startSplitID; // 
    private Integer endSplitID; 
    private final StringProperty segmentName = new SimpleStringProperty(); 
    
    public Segment(Race r){

        setRace(r);
    }
    
    public Segment() {

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
    public String getSplitName() {
        return segmentName.getValueSafe();
    }
    public void setSplitName(String n) {
        segmentName.setValue(n);
    }
    public StringProperty splitNameProperty() {
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
        } else {
            System.out.println("Segment.setStartSplit: null"); 
        }
    }
    
    @Column(name = "END_SPLIT_ID",nullable=false)
    public Integer getEndSplitID() {
        return endSplitID;
    }
    public void setEndSplitID(Integer id) {
        endSplitID = id;
    }
    @Transient
    public Split getEndSplit() {
        return RaceDAO.getInstance().getSplitByID(endSplitID);
    }
    public void setEndSplit(Split s) {
        if (s != null) {
            System.out.println("Segment.setEndSplit: " + s.getID());
            endSplitID=s.getID();
        } else {
            System.out.println("Segment.setEndSplit: null"); 
        }
    }
    
}
