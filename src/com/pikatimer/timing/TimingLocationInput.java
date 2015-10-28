/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import com.pikatimer.event.Event;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
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
    private final StringProperty TimingLocationInputName;
    private TimingLocation timingLocation; // timing_loc_id
    private final StringProperty timingInputString; 
    private TimingInputTypes timingInputType; 
    private final Map<String, String> attributes = new ConcurrentHashMap<>(8, 0.9f, 1);
    private TimingReader timingReader;
    private BooleanProperty tailFileBooleanProperty;
    private BooleanProperty timingReaderInitialized; 
    private static final TimingDAO timingDAO = TimingDAO.getInstance();
    private Button inputButton;
    private TextField inputTextField; 
    private final BooleanProperty skewInput;
    private Duration skewTime; 
    private LocalDateTime firstRead;
    private LocalDateTime lastRead; 
    
    private Set rawTimeSet;
    
    
    public TimingLocationInput() {
        this.IDProperty = new SimpleIntegerProperty();
        this.TimingLocationInputName = new SimpleStringProperty("Not Yet Set");
        this.timingInputString = new SimpleStringProperty();
        tailFileBooleanProperty = new SimpleBooleanProperty();
        timingReaderInitialized = new SimpleBooleanProperty();
        skewInput = new SimpleBooleanProperty();
        //attributes = new ConcurrentHashMap <>();
        
        tailFileBooleanProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if(newValue) {
                
            }
        });
        
        
        
        
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
    
    @Transient
    public String getLocationName() {
    return TimingLocationInputName.getValueSafe();
    }
    public void setLocationName(String n) {
    TimingLocationInputName.setValue(n);
    }
    public StringProperty LocationNameProperty() {
    return TimingLocationInputName;
    }
    
    //@Transient
    public BooleanProperty continueReadingProperty() {
        return tailFileBooleanProperty;
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
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
            System.out.println("TimingLocationInput.setTimingLocation: name=" + timingInputString.getValueSafe()); 
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
        
        if (t != null && (timingInputType == null || ! timingInputType.equals(t)) ){
            
            
            timingReader = t.getNewReader();
            //timingReader.setTimingInput(this);
            
            timingInputType = t;
            
        }
    }
    
    
    public void initializeReader(Pane readerDisplayPane) {
        
        // only do this once to prevent issues
        if (!timingReaderInitialized.getValue()) {
            timingReader.setTimingListener(this);
            
            tailFileBooleanProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if(newValue) {
                    System.out.println("TimingLocationInput: calling timingReader.startReading()");
                    timingReader.startReading();
                } else {
                    System.out.println("TimingLocationInput: calling timingReader.stopReading()");
                    timingReader.stopReading();
                }
            });
            timingReaderInitialized.setValue(Boolean.TRUE);
        
            if (rawTimeSet == null) {
                rawTimeSet = new HashSet(); 
                rawTimeSet.addAll(timingDAO.getRawTimes(this));
                System.out.println("TimingLocationInput.initializeReader: Read in " + rawTimeSet.size() + " existing times"); 
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
        
        attributes.putAll(tli_attributes);
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
        hash = 13 * hash + Objects.hashCode(this.TimingLocationInputName);
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
        if (!Objects.equals(this.TimingLocationInputName, other.TimingLocationInputName)) {
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
        System.out.println("TimingLocationInput.processRead called" );
        
        // Mark it as our own
        r.setTimingLocationInputId(IDProperty.getValue());
        
        
        // is it a duplicate?
        
        // if so, just return
        if (rawTimeSet.contains(r)) {
            System.out.println("TimingLocationInput.processRead: Duplicate " + r.getChip() + " " + r.getTimestamp().toString()); 
            return;
        }
        
        //if not, save it to our local stash of times. 
        timingDAO.saveRawTimes(r); 
        rawTimeSet.add(r);
        
        //timingDAO.getRawTimeQueue().add(r); 
        
        // Create a cooked time
        CookedTimeData c = new CookedTimeData();
                
        // skew it
        //if(skewInput.getValue()) {
        //    r.setTimestamp(r.getTimestamp().plus(skewTime)); 
        //}
        
        // Tag it as a backup if needed
        
        // Swap the chip for a bib
        if (!timingReader.chipIsBib()){
            //r.setChip(timingDAO.getBibFromChip(r.getChip()));
        }
        
        // Send it up to the TimingLocation for further processing...
        //timingLocation.cookTime(c);
        
        // Filter it  (move to the TimingLocation)
        //if (r.getTimestamp().isBefore(firstRead) || r.getTimestamp().isAfter(lastRead)) return; 
        
        // Move to the timing location
        //timingDAO.cookRawTime(r); 
        
    }
}
