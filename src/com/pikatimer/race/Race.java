/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.race;

import com.pikatimer.util.Unit;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

@Entity
@DynamicUpdate
@Table(name="race")
public class Race {
    

   private final IntegerProperty IDProperty;

   private BigDecimal raceDistance; //
   private Unit raceUnits; 
   private final StringProperty raceUnitsProperty; 
   private final StringProperty raceName;
   private  LocalTime raceStart;
   private  LocalTime raceCutoff; 
   private final StringProperty raceBibStart;
   private final StringProperty raceBibEnd;
   private final BooleanProperty relayRace; 
   private final StringProperty raceDistanceProperty; 
   
   //private final ObservableList<split> raceSplits;
   //private final ObservableList<wave> raceWaves; 
           
        public Race() {

        this.IDProperty = new SimpleIntegerProperty();
        this.raceUnitsProperty = new SimpleStringProperty();
        this.raceName = new SimpleStringProperty();
        this.raceBibStart = new SimpleStringProperty();
        this.raceBibEnd = new SimpleStringProperty();
        this.relayRace = new SimpleBooleanProperty();
        this.raceDistanceProperty = new SimpleStringProperty();
        
        //this.raceCutoff = LocalTime.parse("10:30");
        
        //raceSplits = FXCollections.observableArrayList();
        //raceWaves = FXCollections.observableArrayList();
    }
        
//    public static ObservableList getRaceSplits() {
//        return raceSplits; 
//    }
    
//    public static ObservableList getRaceWaves() {
//        return raceWaves; 
//    }     
        

    
    @Id
    @GenericGenerator(name="race_id" , strategy="increment")
    @GeneratedValue(generator="race_id")
    @Column(name="RACE_ID")
    public Integer getID() {
        return IDProperty.getValue(); 
    }
    public void setID(Integer id) {
        IDProperty.setValue(id);
    }
    public IntegerProperty idProperty() {
        return IDProperty; 
    }
    
    @Column(name="RACE_NAME")
    public String getRaceName() {
        return raceName.getValueSafe();
    }
    public void setRaceName(String n) {
        raceName.setValue(n);
    }
    public StringProperty raceNameProperty() {
        return raceName;
    }
    
    @Column(name="RACE_DISTANCE")
    public BigDecimal getRaceDistance() {
        return raceDistance;
    }
    public void setRaceDistance(BigDecimal d) {
        raceDistance = d; 
        if(raceDistance != null && raceUnits != null)
            raceDistanceProperty.setValue(raceDistance.toPlainString()+raceUnits.toShortString()); 
        //raceDistance = new BigDecimal(d).setScale(3, BigDecimal.ROUND_HALF_UP);
    }
    public StringProperty raceDistanceProperty() {
        return raceDistanceProperty; 
    }

    @Enumerated(EnumType.STRING)
    @Column(name="RACE_DIST_UNIT")
    public Unit getRaceDistanceUnits() {
        return raceUnits;
    }
    public void setRaceDistanceUnits(Unit d) {
        raceUnits=d;
        if (raceUnits != null) raceUnitsProperty.setValue(d.toString());
        if(raceDistance != null && raceUnits != null)
            raceDistanceProperty.setValue(raceDistance.toPlainString() + " " +raceUnits.toShortString()); 
    }
    public StringProperty raceDistanceUnitsProperty() {
        return raceUnitsProperty;
    }
    
    @Column(name="RACE_BIB_START")
    public String getBibStart() {
        return raceBibStart.getValueSafe();
    }
    public void setBibStart(String b) {
        raceBibStart.setValue(b);
    }
    public StringProperty bibStartProperty() {
        return raceBibStart;
    }
    
    @Column(name="RACE_BIB_END")
    public String getBibEnd() {
        return raceBibEnd.getValueSafe();
    }
    public void setBibEnd(String b) {
        raceBibEnd.setValue(b);
    }
    public StringProperty bibEndProperty() {
        return raceBibEnd;
    }
    
    
    @Column(name="RACE_START_TIME",nullable=true)
    public String getRaceStart() {
        if( raceStart != null) {
            return raceStart.format(DateTimeFormatter.ISO_LOCAL_TIME);
        } else {
            return "";
        }
        //return raceCutoff.toString();
    }
    public void setRaceStart(String c) {
        if(! c.isEmpty()) {
            //Fix this to watch for parse exceptions
            raceStart = LocalTime.parse(c, DateTimeFormatter.ISO_LOCAL_TIME );
        }
    }
    public LocalTime raceStartProperty(){
        return raceStart; 
    }
    
    @Column(name="RACE_CUTOFF", nullable=true)
    public String getRaceCutoff() {
        if (raceCutoff != null) {
            // fix this towatch for parse exceptions
            return raceCutoff.format(DateTimeFormatter.ISO_LOCAL_TIME);
        } else {
            return ""; 
        }
        //return raceCutoff.toString();
    }
    public void setRaceCutoff(String c) {
        if(! c.isEmpty()) {
            raceCutoff = LocalTime.parse(c, DateTimeFormatter.ISO_LOCAL_TIME );
        }
    }
    public LocalTime raceCutoffProperty(){
        return raceCutoff; 
    }
    
}