/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import com.pikatimer.event.Event;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
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
public class TimingLocationInput {
    private final IntegerProperty IDProperty;
    private final StringProperty TimingLocationInputName;
    private TimingLocation timingLocation; // timing_loc_id
    private final StringProperty timingInputString; 
    private TimingInputTypes timingInputType; 
    private final Map<String, String> attributes = new ConcurrentHashMap<>(8, 0.9f, 1);
    private TimingReader timingReader;
    private BooleanProperty keepReadingBooleanProperty;
    private static final TimingDAO timingDAO = TimingDAO.getInstance();
    private Button inputButton;
    private TextField inputTextField; 
    
    public TimingLocationInput() {
        this.IDProperty = new SimpleIntegerProperty();
        this.TimingLocationInputName = new SimpleStringProperty("Not Yet Set");
        this.timingInputString = new SimpleStringProperty();
        keepReadingBooleanProperty = new SimpleBooleanProperty();
        //attributes = new ConcurrentHashMap <>();
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
        return keepReadingBooleanProperty;
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
    
    public void initializeReader() {
        timingReader.setTimingInput(this);
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
    
    
    public String getAttribute(String key) {
        //System.out.println("TLI.getAttribute called for " + key);
        return attributes.get(key); 
    }
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
    public LocalDate getEventDate() {
        return Event.getInstance().getLocalEventDate();
    }

    public void selectInput() {
        timingReader.selectInput();
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
}
