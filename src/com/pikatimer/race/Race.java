/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.race;

import com.pikatimer.timing.Split;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.Unit;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
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
   private  Duration raceCutoff; 
   private final StringProperty raceCutoffProperty; 
   private final StringProperty raceBibStart;
   private final StringProperty raceBibEnd;
   private final BooleanProperty relayRace; 
   private final StringProperty raceDistanceProperty; 
   private final ObservableList<Wave> raceWaves; 
   private final ObservableList<Split> raceSplits; 
   private final Race self; 
           
    public Race() {
        this.self = this; 
        this.IDProperty = new SimpleIntegerProperty();
        this.raceUnitsProperty = new SimpleStringProperty();
        this.raceName = new SimpleStringProperty();
        this.raceBibStart = new SimpleStringProperty();
        this.raceBibEnd = new SimpleStringProperty();
        this.raceCutoffProperty = new SimpleStringProperty();
        this.relayRace = new SimpleBooleanProperty();
        this.raceDistanceProperty = new SimpleStringProperty();
        this.raceWaves = FXCollections.observableArrayList();
        this.raceSplits = FXCollections.observableArrayList();

        // Keep the waves updated as to their position in the list
//        raceWaves.addListener((Change<? extends Wave> change) -> {
//            System.out.println("Race::raceWaves(changeListener) for: " + self.getRaceName());
//            raceWaves.stream().forEach((item) -> {
//                System.out.println(self.getRaceName() + " has " + item.getWaveName() + " at " + raceWaves.indexOf(item));
//                item.wavePositionProperty().set(raceWaves.indexOf(item)+1);
//                //RaceDAO.getInstance().updateWave(item);
//            });
//        });
//        // Keep the splits updated as to their position in the list
//        raceSplits.addListener((Change<? extends Split> change) -> {
//            System.out.println("Race::raceSplits(changeListener) for: " + self.getRaceName());
////            raceSplits.stream().forEach((item) -> {
////                System.out.println(self.getRaceName() + " has " + item.getSplitName() + " at " + raceSplits.indexOf(item));
////                item.splitPositionProperty().set(raceSplits.indexOf(item)+1);
////                Task importTask = new Task<Void>() {
////                    @Override
////                    protected Void call() {
////                        RaceDAO.getInstance().updateSplit(item);
////                        return null; 
////                    }
////                };
////                new Thread(importTask).start();
////            });
//        });
        
        
    }
        
    
        

    
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
    
    
   
    @Column(name="RACE_CUTOFF", nullable=true)
    public Long getRaceCutoff() {
        if (raceCutoff != null) {
            return raceCutoff.toNanos();
        } else {
            return 0L; 
        }
    }
    public void setRaceCutoff(Long c) {
        if(c != null) {
            System.out.println("setRaceCutoff " + c.toString());
            raceCutoff = Duration.ofNanos(c);
            raceCutoffProperty.set(DurationFormatter.durationToString(raceCutoff,0)); 
        }
    }
    public StringProperty raceCutoffProperty(){
        return raceCutoffProperty;  
    }
    
    @OneToMany(mappedBy="race",cascade={CascadeType.PERSIST, CascadeType.REMOVE},fetch = FetchType.LAZY)
    public List<Wave> getWaves() {
        return raceWaves.sorted(); 
    }
    public void setWaves(List<Wave> waves) {
        //System.out.println("Race.setWaves(list) called for " + raceName + " with " + waves.size() + " waves"); 
        raceWaves.setAll(waves);
        //System.out.println("Race.setWaves(list) " + raceName + " now has " + raceWaves.size() + " waves");
    }
    public ObservableList<Wave> wavesProperty() {
        return raceWaves; 
    }
    public void addWave(Wave w) {
        raceWaves.add(w);
    }
    public void removeWave(Wave w) {
        raceWaves.remove(w); 
    }
    
    @OneToMany(mappedBy="race",cascade={CascadeType.PERSIST, CascadeType.REMOVE},fetch = FetchType.LAZY)
    @OrderBy("split_seq_number")
    public List<Split> getSplits() {
        return raceSplits.sorted(); 
        //return raceSplits.sorted((Split o1, Split o2) -> o1.getPosition().compareTo(o2.getPosition()));
    }
    public void setSplits(List<Split> splits) {
//        System.out.println("Race.setSplits(list) called for " + raceName + " with " + splits.size() + " splits"); 
//        splits.stream().forEach(e -> System.out.println(e.getSplitName() + " " + e.getPosition()));
//        splits
//            .stream()
//            .sorted((e1, e2) -> Integer.compare(e1.getPosition(),
//                    e2.getPosition()))
//            .forEach(e -> System.out.println(e.getSplitName()));
        raceSplits.setAll(splits);
//        System.out.println("Race.setSplits(list) " + raceName + " now has " + raceSplits.size() + " splits");
    }
    public ObservableList<Split> splitsProperty() {
        return raceSplits; 
    }
    public void addSplit(Split s) {
        if (s.getPosition() > 0) {
            raceSplits.add(s.getPosition()-1,s); 
        } else if (raceSplits.size() < 2 ) {
            raceSplits.add(s);
        } else {
            raceSplits.add(raceSplits.size()-1, s);
        }
    }
    public void removeSplit(Split s) {
        raceSplits.remove(s); 
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        //System.out.println("Wave.equals called: " + IDProperty.getValue() + " vs " + ((Wave)obj).IDProperty.getValue() ); 
        return this.IDProperty.getValue().equals(((Race)obj).IDProperty.getValue());
    }

    @Override
    public int hashCode() {
        return 7 + 5*IDProperty.intValue(); // 5 and 7 are random prime numbers
    }
}