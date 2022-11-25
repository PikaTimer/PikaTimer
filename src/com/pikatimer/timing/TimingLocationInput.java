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
package com.pikatimer.timing;

import com.pikatimer.event.Event;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.HTTPServices;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import javafx.util.converter.BigDecimalStringConverter;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OrderColumn;
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
@Table(name="timing_location_input")
public class TimingLocationInput implements TimingListener{
    private final IntegerProperty IDProperty;
    private final StringProperty timingLocationInputName;
    private TimingLocation timingLocation; // timing_loc_id
    private final StringProperty timingInputString; 
    private TimingInputTypes timingInputType; 
    private Map<String, String> attributes = new ConcurrentHashMap<>(8, 0.9f, 1);
    private TimingReader timingReader;
    private final BooleanProperty tailFileBooleanProperty;
    private final BooleanProperty timingReaderInitialized; 
    private static final TimingDAO timingDAO = TimingDAO.getInstance();
    private Button inputButton;
    private TextField inputTextField; 
    private final BooleanProperty isBackup = new SimpleBooleanProperty(false);
    private final BooleanProperty skewInput;
    private Duration skewDuration; 
    private final BooleanProperty isAnnouncer = new SimpleBooleanProperty(false);
    private final Semaphore processRead = new Semaphore(1);

    private final IntegerProperty readCountProperty = new SimpleIntegerProperty();
    private Set<RawTimeData> rawTimeSet;
    
    
    public TimingLocationInput() {
        this.IDProperty = new SimpleIntegerProperty();
        this.timingLocationInputName = new SimpleStringProperty("Not Yet Set");
        this.timingInputString = new SimpleStringProperty();
        tailFileBooleanProperty = new SimpleBooleanProperty();
        timingReaderInitialized = new SimpleBooleanProperty();
        skewInput = new SimpleBooleanProperty();
        //attributes = new ConcurrentHashMap <>();
        skewDuration = Duration.ZERO;
        skewInput.setValue(Boolean.FALSE);

        
   }
    
    @Id
    @GenericGenerator(name="timing_location_input_id" , strategy="increment")
    @GeneratedValue(generator="timing_location_input_id")
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
    
    @Column(name="input_name")
    public String getLocationName() {
        return timingLocationInputName.getValueSafe();
    }
    public void setLocationName(String n) {
        timingLocationInputName.setValue(n);
    }
    public StringProperty LocationNameProperty() {
        return timingLocationInputName;
    }
    
    //@Transient
    public BooleanProperty continueReadingProperty() {
        return tailFileBooleanProperty;
    }
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TIMING_LOCATION_ID",nullable=false)
    public TimingLocation getTimingLocation() {
        return timingLocation;
    }
    public void setTimingLocation(TimingLocation l) {
        if (l != null && (timingLocation == null || !timingLocation.equals(l))) {
            System.out.println("TimingLocationInput.setTimingLocation: id=" + l.getID());
            timingLocation=l;
            timingInputString.unbind();
            timingInputString.bind(l.LocationNameProperty()); 
            //System.out.println("TimingLocationInput.setTimingLocation: name=" + timingInputString.getValueSafe()); 
        } else {
            System.out.println("TimingLocationInput.setTimingLocation: null or unchanged"); 
        }
    }
    public StringProperty timingLocationProperty() {
        return timingInputString; 
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name="timing_location_type")
    public TimingInputTypes getTimingInputType() {
        return timingInputType;
    }
    public void setTimingInputType(TimingInputTypes t) {
        System.out.println("TimingLocationInput::setTimingInputType now " + t);
        if (t != null && (timingInputType == null || ! timingInputType.equals(t)) ){
            
            // If we already have a reader
            if (timingReader != null) {
                // , tell it to stop reading
                timingReader.stopReading();
                // clear out all existing reads
                //clearReads();
            }
                               
            timingReader = t.getNewReader();
            //timingReader.setTimingInput(this);
            
            timingInputType = t;
            timingReaderInitialized.setValue(Boolean.FALSE);
        }
    }
    
    
    public IntegerProperty readCountProperty() {
        return readCountProperty;         
    }
    
    public void initializeReader(Pane readerDisplayPane) {
        
        // only do this once to prevent issues
        if (!timingReaderInitialized.getValue()) {
            timingReader.setTimingListener(this);
            
            
            timingReaderInitialized.setValue(Boolean.TRUE);
        
            if (rawTimeSet == null) {
                
                rawTimeSet = Collections.newSetFromMap(new ConcurrentHashMap<>()); 
                
                rawTimeSet.addAll(timingDAO.getRawTimes(this));
                System.out.println("TimingLocationInput.initializeReader: Read in " + rawTimeSet.size() + " existing times"); 
                readCountProperty.set(rawTimeSet.size());
            } 
        }
        
        timingReader.showControls(readerDisplayPane);
    }

    
    // Where we stash the attributes for the input
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="attribute", insertable=false,updatable=false)
    @Column(name="value")
    @CollectionTable(name="timing_location_input_attributes", joinColumns=@JoinColumn(name="tli_id"))
    @OrderColumn(name = "index_id")
    public Map<String, String> getAttributes() {
        //System.out.println("TLI.getAttributes called, returning " + attributes.size() + " attributes");
        return attributes;
    }
    public void setAttributes(Map<String,String> tli_attributes) {
        //System.out.println("TLI.setAttributes called, adding " + tli_attributes.size() + " attributes");
        //System.out.println("TLI.setAttributes called, we already have " + attributes.size() + " attributes");
        // This really screws things up for some reason, I don't know why. 
        //attributes.clear();  
        attributes = tli_attributes;
        //attributes.putAll(tli_attributes);
        //System.out.println("TLI.setAttributes called, adding " + tli_attributes.size() + " attributes");
        //System.out.println("TLI.setAttributes called, we now have " + attributes.size() + " attributes");
    } 
    
    
    @Override
    public String getAttribute(String key) {
        //System.out.println("TLI.getAttribute called for " + key);
        return attributes.get(key); 
    }
    @Override
    public void setAttribute(String key, String value) {
        //System.out.println("Setting Attribute " + key + " to " + value);
        attributes.put(key, value); 
        
        //System.out.println("TLI.setAttribute called, we now have " + attributes.size() + " attributes");
        timingDAO.updateTimingLocationInput(this);
    }
    
    
    public void addRawTime(RawTimeData t){
        timingDAO.getRawTimeQueue().add(t);
    }
    
    @Transient
    @Override
    public LocalDate getEventDate() {
        return Event.getInstance().getLocalEventDate();
    }

    
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + Objects.hashCode(this.timingLocationInputName);
        hash = 13 * hash + Objects.hashCode(this.timingLocation);
        hash = 13 * hash + Objects.hashCode(this.timingInputString);
        return hash;
    }

    /*   @Override
    public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
    return false;
    }
    return this.IDProperty.getValue().equals(((TimingLocationInput)obj).IDProperty.getValue());
    }
    @Override
    public int hashCode() {
    return 7 + 5*IDProperty.intValue(); // 5 and 7 are random prime numbers
    }*/
    @Override    
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TimingLocationInput other = (TimingLocationInput) obj;
        if (!Objects.equals(this.timingLocationInputName, other.timingLocationInputName)) {
            return false;
        }
        if (!Objects.equals(this.timingLocation, other.timingLocation)) {
            return false;
        }
        if (!Objects.equals(this.timingInputString, other.timingInputString)) {
            return false;
        }
        return true;
    }

    public static Callback<TimingLocationInput, Observable[]> extractor() {
        return (TimingLocationInput tl) -> new Observable[]{tl.LocationNameProperty()};
    }

    @Transient
    public Button getInputButton() {
        return inputButton; 
    }
    public void setInputButton(Button b) {
        inputButton = b;
    }
    
    @Transient
    public TextField getInputTextField() {
        return inputTextField;
    }
    public void setInputTextField(TextField t) {
        inputTextField = t;
    }

    @Override
    public void processRead(RawTimeData r) {
        try {
            //System.out.println("TimingLocationInput.processRead called" );
            //System.out.println("processRead() ProcessRead.aquire()");
            processRead.acquire();
            //System.out.println("Got it... ");

            // Mark it as our own
            r.setTimingLocationInputId(IDProperty.getValue());

            if (rawTimeSet == null) getRawTimeSet();
            // is it a duplicate?

            // if so, just return
            if (rawTimeSet.contains(r)) {
                //System.out.println("TimingLocationInput.processRead: Duplicate " + r.getChip() + " " + r.getTimestamp().toString()); 
                processRead.release();
                return;
            }
            //if not, save it to our local stash of times. 
            timingDAO.saveRawTimes(r); 
            rawTimeSet.add(r);

            Platform.runLater(() -> {
                readCountProperty.set(rawTimeSet.size());
            });

            processRead.release();

            processReadStage2(r);
        } catch (InterruptedException ex) {
            Logger.getLogger(TimingLocationInput.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        
    }
    
    public void processReadStage2(RawTimeData r){
        // Create a cooked time
        //System.out.println("Stage 2 processing of raw id " + r.getID()); 
        CookedTimeData c = new CookedTimeData();
                
        // mark it as our own
        c.setTimingLocationInputId(this.IDProperty.intValue());
        
        // set link the cooked time to the parent raw time
        c.setRawChipID(r.getChip());
        
        // set the backup flag
        c.setBackupTime(getIsBackup());
        
        // skew it
        if(skewInput.getValue()) {
            c.setTimestamp(r.getTimestamp().plus(skewDuration)); 
            //System.out.println("Skewing input from " + r.getTimestamp() + " to " + c.getTimestamp());
        } else {
            c.setTimestamp(r.getTimestamp());
        }
       
        
        // Swap the chip for a bib
        if (!timingReader.chipIsBib()){
            c.setBib(timingDAO.getBibFromChip(r.getChip()));
        } else {
            c.setBib(r.getChip()); 
        }
        
        // if we are an announcer location, announce the arrival of the bib
        if (isAnnouncer.get()) HTTPServices.getInstance().publishEvent("ANNOUNCER", c.getBib());
        
        // Send it up to the TimingLocation for further processing...
        //System.out.println("Cooking time " + c.getBib() + " " + c.getTimestamp()); 
        timingLocation.cookTime(c);
    }
    
    public void clearLocalReads() {
        stopReader();
        // blow away the rawTimeSet
        if (rawTimeSet != null && !rawTimeSet.isEmpty()) {
            rawTimeSet.clear();
            Platform.runLater(() -> {
                readCountProperty.set(rawTimeSet.size());
            });
        }
    }
    
    @Override
    public void clearReads() {
        if(rawTimeSet != null && !rawTimeSet.isEmpty()) {
            // This will orchistrate the clearing of the reads via the timingDAO
            // The timingDAO will first delete all from the DB
            // It will then call this.clearLocalReads() 
            // Finally it will trigger a removal of all cooked times associated with 
            // this instance. 
            timingDAO.clearRawTimes(this); 
        }
    }
    
    @Override
    @Transient
    public Set<RawTimeData> getReads(){
        return rawTimeSet;
    }
    
    public void reprocessReads() {
        System.out.println("TimingLocationInput::reprocessReads() for " + this.getLocationName());
        TimingLocationInput tli = this; 
        

        // start a background thread for this 

        Task task;
        task = new Task<Void>() {
            @Override public Void call() {
                try {
                    // set the processReadSemaphore to pause the processRead()
                    System.out.println("TimingLocationInput::reprocessReads() Task started for " + tli.getLocationName());
                    processRead.acquire();

                    // clear out all cooked times for our location
                    
                    System.out.println("TimingLocationInput::reprocessReads() Task deleting times for " + tli.getLocationName());

                    timingDAO.blockingClearCookedTimes(tli);
                    
                    System.out.println("TimingLocationInput::reprocessReads() Task reprocessing " + getRawTimeSet().size() + " reads at " + tli.getLocationName() + ".");

                    getRawTimeSet().stream().forEach( r -> {
                        // for everything in our rawTimeSet, reprocess the read 

                        tli.processReadStage2(r);
                        // setting the skew or ignore flag as needed

                    });

                    //resume processing of new times. 
                    System.out.println("TimingLocationInput::reprocessReads() Task done for " + tli.getLocationName() + ".");

                    processRead.release();
                } catch (Exception ex) {
                    System.out.println("TimingLocationInput::reprocessReads() exception for " + tli.getLocationName());
                    ex.printStackTrace();
                    Logger.getLogger(TimingLocationInput.class.getName()).log(Level.SEVERE, null, ex);
                }
                //when done, resume processRead()
                return null;
            }
        };
         // Run this in a thread.... 
        Thread reporcessTimes = new Thread(task);
            reporcessTimes.setDaemon(true);
            reporcessTimes.start();
    }

    public void stopReader() {
        timingReader.stopReading();
    }

    @Column(name="announcer")
    public Boolean getIsAnnouncer() {
        //System.out.println("returning isBackup()");
        return isAnnouncer.getValue();
    }
    public void setIsAnnouncer(Boolean i) {
        if (i != null) { 
            isAnnouncer.setValue(i);
        }
    }
     
    public BooleanProperty announcerProperty(){
        return isAnnouncer;
    }
    
    @Column(name="backup")
    public Boolean getIsBackup() {
        //System.out.println("returning isBackup()");
        return isBackup.getValue();
    }
    public void setIsBackup(Boolean i) {
        if (i != null) { 
            isBackup.setValue(i);
        }
    }
     
    public BooleanProperty backupProperty(){
        return isBackup;
    }
    
    @Column(name="skew")
    public Boolean getSkewLocationTime() {
        //System.out.println("returning SkewLocation()");
        return skewInput.getValue();
    }
    public void setSkewLocationTime(Boolean i) {
        if (i != null) { 
            skewInput.setValue(i);
        }
    }
    public BooleanProperty skewLocation(){
        return skewInput;
    }
    
    
    @Column(name="time_skew")
    public Long getSkewNanos(){
        return skewDuration.toNanos();
    }
    public void setSkewNanos(Long s) {
        if (s != null) {
            skewDuration = Duration.ofNanos(s);     
            System.out.println("Skew duration is now " + skewDuration);
        } 
    }
    @Transient
    public String getSkewString() {
        //String durationString = new BigDecimalStringConverter().toString(BigDecimal.valueOf(skewDuration.toNanos()).divide(BigDecimal.valueOf(1000000000L)));
        String skewDurationString = DurationFormatter.durationToString(skewDuration, 3, false);
        System.out.println("Returning skew duration string of " + skewDurationString + " for " + skewDuration);
        return skewDurationString; 
    }
//    public void setSkewString(String text) {
//        
//        // check for null/blank/zero
//        if (text == null || text.isEmpty() || text.equals("0")) {
//            skewDuration = Duration.ZERO;
//        } else {
//            skewDuration = Duration.ofNanos(new BigDecimalStringConverter().fromString(text).multiply(new BigDecimal(1000000000L)).longValue());
//            System.out.println("Skew duration is now " + skewDuration);
//        }
//    }
    @Transient
    public Duration getSkew() {
        return skewDuration; 
    }
    public void setSkew(Duration s){
        skewDuration = s;
    }
    
    @Transient
    private Set<RawTimeData> getRawTimeSet(){
        if (rawTimeSet == null) {
                
            rawTimeSet = Collections.newSetFromMap(new ConcurrentHashMap<>()); 

            rawTimeSet.addAll(timingDAO.getRawTimes(this));
            System.out.println("TimingLocationInput.initializeReader: Read in " + rawTimeSet.size() + " existing times"); 
            readCountProperty.set(rawTimeSet.size());
        } 
        
        return rawTimeSet;
    }
}
