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
package com.pikatimer.race;

import com.pikatimer.participant.Participant;
import com.pikatimer.util.DurationFormatter;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import javafx.beans.Observable;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
    private final StringProperty raceName = new SimpleStringProperty();
    private final StringProperty waveName;
    private  LocalTime waveStart;
    private final StringProperty waveStartString;
    private  Duration waveMaxStart;
    private final StringProperty waveMaxStartString;
    private WaveAssignment waveAssignmentMethod = WaveAssignment.BIB;
    private final StringProperty waveAssignmentMethodProperty;
    private final StringProperty waveAssignmentStart;
    private final StringProperty waveAssignmentEnd; 
    private final IntegerProperty wavePosition;
    private final ObservableList<Participant> participants; 
    private final StringProperty waveDisplayName = new SimpleStringProperty();
    private StringBinding waveDNBinding;
    

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
        this.participants=FXCollections.observableArrayList();
        //raceSplits = FXCollections.observableArrayList();
        //raceWaves = FXCollections.observableArrayList();
        setWaveStart("07:00:00"); // default start time
        
    }
   
   public static Callback<Wave, Observable[]> extractor() {
        return (Wave w) -> new Observable[]{w.waveName,w.waveStartString,w.raceName,w.waveDisplayName};
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RACE_ID",nullable=false)
    public Race getRace() {
        return race;
    }
    public void setRace(Race r) {
        race=r;
        if (r != null) raceName.bind(r.raceNameProperty());
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
            try {
                waveStart = LocalTime.parse(c, DateTimeFormatter.ISO_LOCAL_TIME );
                waveStartString.set(waveStart.format(DateTimeFormatter.ISO_LOCAL_TIME));
            } catch (Exception  e){
                // noop, we just let the old time rule
            }
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

    /*    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "waves")
    @Fetch(FetchMode.SUBSELECT)
    public Set<Participant> getParticipants() {
    return new HashSet<>(participants);
    }
    public void setParticipants(Set<Participant> p) {
    participants.setAll(p);
    }
    public ObservableList<Participant> participantsProperty() {
    return participants;
    }*/
    
    public StringProperty waveDisplayNameProperty(){
        // setup the bindings for the waveDisplayName
        System.out.println("Wave:waveDisplayNameProperty() called");

        if (waveDNBinding==null && race != null && RaceDAO.getInstance().listRaces().size() > 0) {
            System.out.println("Null WaveDNBinding");


            StringBinding waveDNBinding = new StringBinding() {
                    {
                        super.bind(race.wavesProperty(),RaceDAO.getInstance().listRaces(),waveName,race.raceNameProperty());
                    }

                    @Override
                    protected String computeValue(){
                        System.out.println("WaveDNBinding.computeValue() called");

                        if (race.wavesProperty().size() == 1 ) {
                            System.out.println("WaveDNBinding.computeValue() returning " + race.getRaceName());
                            return race.getRaceName();
                        } else if (RaceDAO.getInstance().listRaces().size() == 1 ) {
                            System.out.println("WaveDNBinding.computeValue() returning " + waveName.getValueSafe());
                            return waveName.getValueSafe();
                        } else {
                            System.out.println("WaveDNBinding.computeValue() returning " + race.getRaceName() + " " + waveName.getValueSafe());
                            return race.getRaceName() + " " + waveName.getValueSafe();
                        } 
                    }
            };

            waveDisplayName.bind(waveDNBinding);
        }

        return waveDisplayName;
    }

    
    @Override
    public String toString(){
        if(race.wavesProperty().size()> 1) {
            //System.out.println("Wave.toString() called: " + race.getRaceName() + " " + waveName.getValueSafe());
            return race.getRaceName() + " " + waveName.getValueSafe(); 
        } else {
            //System.out.println("Wave.toString() called: " + race.getRaceName() );
            return race.getRaceName();
        }
    }


    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.race);
        hash = 67 * hash + Objects.hashCode(this.waveName);
        hash = 67 * hash + Objects.hashCode(this.waveStart);
        hash = 67 * hash + Objects.hashCode(this.waveAssignmentMethod);
        hash = 67 * hash + Objects.hashCode(this.wavePosition);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        //System.out.println("Wave.equals called for " + this.IDProperty.toString());
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Wave other = (Wave) obj;
        //System.out.println("Wave.equals comparing to " + other.IDProperty.toString());
        if (!Objects.equals(this.race, other.race)) {
        //System.out.println("Wave.equals false: race");
        return false;
        }
        if (!Objects.equals(this.waveName.getValue(), other.waveName.getValue())) {
            //System.out.println("Wave.equals false: waveName: " + this.waveName + " vs " + other.waveName);
            return false;
        }
        if (!Objects.equals(this.waveStart, other.waveStart)) {
            //System.out.println("Wave.equals false: waveStart");
            return false;
        }
        if (this.waveAssignmentMethod != other.waveAssignmentMethod) {
            //System.out.println("Wave.equals false: waveAssignmentMethod");
            return false;
        }
        if (!Objects.equals(this.wavePosition.getValue(), other.wavePosition.getValue())) {
            //System.out.println("Wave.equals false: wavePosition");
            return false;
        }
        //System.out.println("Wave.equals true");
        return true;
    }
    
}
