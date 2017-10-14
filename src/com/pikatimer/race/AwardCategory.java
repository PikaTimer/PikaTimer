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
package com.pikatimer.race;

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
import javax.persistence.Embeddable;
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
 * @author John Garner <segfaultcoredump@gmail.com>
 */
@Entity
@DynamicUpdate
@Table(name="race_award_categories")
public class AwardCategory {
    
    private final IntegerProperty IDProperty = new SimpleIntegerProperty();
    private final StringProperty uuidProperty = new SimpleStringProperty(java.util.UUID.randomUUID().toString());
    private final StringProperty nameProperty = new SimpleStringProperty("New Award");
    private final IntegerProperty priorityProperty = new SimpleIntegerProperty();
    private final ObjectProperty<AwardCategoryType> typeProperty = new SimpleObjectProperty(AwardCategoryType.OVERALL);
    private final ObjectProperty<AwardDepthType> depthTypeProperty = new SimpleObjectProperty(AwardDepthType.FIXED);
    private final BooleanProperty pullProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty chipProperty = new SimpleBooleanProperty(true);
    private final IntegerProperty depthProperty = new SimpleIntegerProperty(3);

    private RaceAwards raceAward;

    public AwardCategory() {
        
    }
    
        @Id
    @GenericGenerator(name="award_category_id" , strategy="increment")
    @GeneratedValue(generator="award_category_id")
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
 
    //    uuid varchar,
    @Column(name="uuid")
    public String getUUID() {
       // System.out.println("RaceReport UUID is " + uuidProperty.get());
        return uuidProperty.getValue(); 
    }
    public void setUUID(String  uuid) {
        uuidProperty.setValue(uuid);
        //System.out.println("RaceReport UUID is now " + uuidProperty.get());
    }
    public StringProperty uuidProperty() {
        return uuidProperty; 
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RACE_ID",nullable=false)
    public RaceAwards getRaceAward() {
        return raceAward;
    }
    public void setRaceAward(RaceAwards r) {
        raceAward=r;
    }
    
    @Column(name="category_name")
    public String getName() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return nameProperty.getValueSafe(); 
    }
    public void setName(String i) {
        nameProperty.setValue(i);
    }
    public StringProperty nameProperty() {
        return nameProperty;
    }
    
    @Column(name="category_priority")
    public Integer getPriority() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return priorityProperty.getValue();
    }
    public void setPriority(Integer i) {
        priorityProperty.setValue(i);
    }
    public IntegerProperty priorityProperty() {
        return priorityProperty;
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name="award_type")
    public AwardCategoryType getType(){
        return typeProperty.getValue();
    }
    public void setType(AwardCategoryType t) {
        typeProperty.setValue(t); 
    }
    
    public ObjectProperty<AwardCategoryType> typeProperty(){
        return typeProperty;
    }
    
    @Column(name="pull")
    public Boolean getPull(){
        return pullProperty.getValue();
    }
    public void setPull(Boolean t) {
        pullProperty.setValue(t); 
    }
    
    public BooleanProperty pullProperty(){
        return pullProperty;
    }
    
    @Column(name="chip")
    public Boolean getChip(){
        return chipProperty.getValue();
    }
    public void setChip(Boolean t) {
        chipProperty.setValue(t); 
    }
    
    public BooleanProperty chipProperty(){
        return chipProperty;
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name="depth_type")
    public AwardDepthType getDepthType(){
        return depthTypeProperty.getValue();
    }
    public void setDepthType(AwardDepthType t) {
        depthTypeProperty.setValue(t); 
    }
    
    public ObjectProperty<AwardDepthType> depthTypeProperty(){
        return depthTypeProperty;
    }
    
    @Column(name="category_depth")
    public Integer getDepth() {
        System.out.println("AwardCategory.getDepth(): returning " + depthProperty.getValue());
        return depthProperty.getValue();
    }
    public void setDepth(Integer i) {
        System.out.println("AwardCategory.setDepth(): " + i);
        depthProperty.setValue(i);
    }
    public IntegerProperty depthProperty() {
        return depthProperty;
    }
    
    
    public static Callback<AwardCategory, Observable[]> extractor() {
        return (AwardCategory ac) -> new Observable[]{ac.priorityProperty};
    }

    
}
