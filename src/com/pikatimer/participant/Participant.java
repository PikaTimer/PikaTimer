/* 
 * Copyright (C) 2016 John Garner
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
package com.pikatimer.participant;

import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import static java.lang.Boolean.FALSE;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
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
@Table(name="PARTICIPANT")
public class Participant {
    

   
    private final StringProperty firstNameProperty = new SimpleStringProperty();
    private final StringProperty middleNameProperty= new SimpleStringProperty();
    private final StringProperty lastNameProperty= new SimpleStringProperty();
    private final StringProperty fullNameProperty= new SimpleStringProperty();
    private final StringProperty emailProperty= new SimpleStringProperty(); 
    private final IntegerProperty IDProperty = new SimpleIntegerProperty();
    private final StringProperty uuidProperty = new SimpleStringProperty(java.util.UUID.randomUUID().toString());
    private final StringProperty bibProperty= new SimpleStringProperty();
    private final IntegerProperty ageProperty = new SimpleIntegerProperty();
    private final StringProperty sexProperty= new SimpleStringProperty(); 
    private final StringProperty cityProperty= new SimpleStringProperty();
    private final StringProperty stateProperty= new SimpleStringProperty();
    private final StringProperty countryProperty= new SimpleStringProperty();
    private LocalDate birthdayProperty; 
    private final ObservableList<Wave> waves = FXCollections.observableArrayList(Wave.extractor());   
    private Set<Integer> waveIDSet = new HashSet(); 
    private final BooleanProperty dnfProperty = new SimpleBooleanProperty(FALSE);
    private final BooleanProperty dqProperty = new SimpleBooleanProperty(FALSE);
    private final StringProperty noteProperty = new SimpleStringProperty();
    private Status status; 
    private final ObjectProperty<Status> statusProperty = new SimpleObjectProperty(Status.GOOD);
   
    public Participant() {
        this("","");
        
    }
 
    public Participant(String firstName, String lastName) {

        
        setFirstName(firstName);
        setLastName(lastName);
        // TODO: Fix this to include the middle name if it is set
        fullNameProperty.bind(new StringBinding(){
            {super.bind(firstNameProperty,middleNameProperty, lastNameProperty);}
            @Override
            protected String computeValue() {
                return (firstNameProperty.getValueSafe() + " " + middleNameProperty.getValueSafe() + " " + lastNameProperty.getValueSafe()).replaceAll("( )+", " ");
            }
        });
        
        //Convenience properties for the getDNF and getDQ status checks
        dnfProperty.bind(new BooleanBinding(){
            {super.bind(statusProperty);}
            @Override
            protected boolean computeValue() {
                if (statusProperty.getValue().equals(Status.DNF)) return true;
                return false; 
            }
        });
        dqProperty.bind(new BooleanBinding(){
            {super.bind(statusProperty);}
            @Override
            protected boolean computeValue() {
                if (statusProperty.getValue().equals(Status.DQ)) return true;
                return false; 
            }
        });
        
    }
    
    public static ObservableMap getAvailableAttributes() {
        ObservableMap<String,String> attribMap = FXCollections.observableHashMap();
        attribMap.put("bib", "Bib");
        attribMap.put("first", "First Name");
        attribMap.put("middle", "Middle Name");
        attribMap.put("last", "Last Name");
        attribMap.put("age", "Age");
        attribMap.put("sex-gender", "Sex");
        attribMap.put("city", "City");
        attribMap.put("state", "State");
        attribMap.put("country", "Country");
        attribMap.put("email", "EMail");
        // routine to add custom attributes based on db lookup
        return attribMap; 
    }
    
    public Participant(Map<String, String> attribMap) {
        // bulk set routine. Everything is a string so convert as needed
        this("","");
        attribMap.entrySet().stream().forEach((Map.Entry<String, String> entry) -> {
            if (entry.getKey() != null) {
                //System.out.println("processing " + entry.getKey() );
             switch(entry.getKey()) {
                 case "bib": this.setBib(entry.getValue()); break; 
                 case "first": this.setFirstName(entry.getValue()); break;
                 case "middle": this.setMiddleName(entry.getValue()); break;
                 case "last": this.setLastName(entry.getValue()); break;
                 
                     
                 // TODO: catch bad integers 
                 case "age": this.setAge(Integer.parseUnsignedInt(entry.getValue())); break; 
                     
                 // TODO: map to selected sex translator
                 case "sex-gender": this.setSex(entry.getValue()); break; 
                     
                 case "city": this.setCity(entry.getValue()); break; 
                 case "state": this.setState(entry.getValue()); break; 
                 case "email": this.setEmail(entry.getValue()); break; 
                     
                 // TODO: Team value
             }
            }
        });
    }
    
    @Id
    @GenericGenerator(name="participant_id" , strategy="increment")
    @GeneratedValue(generator="participant_id")
    @Column(name="PARTICIPANT_ID")
    public Integer getID() {
        return IDProperty.getValue(); 
    }
    public void setID(Integer id) {
        IDProperty.setValue(id);
    }
    public IntegerProperty idProperty() {
        return IDProperty; 
    }
    
    @Column(name="uuid")
    public String getUUID() {
       // System.out.println("Participant UUID is " + uuidProperty.get());
        return uuidProperty.getValue(); 
    }
    public void setUUID(String  uuid) {
        uuidProperty.setValue(uuid);
        //System.out.println("Participant UUID is now " + uuidProperty.get());
    }
    public StringProperty uuidProperty() {
        return uuidProperty; 
    }
    
    @Column(name="FIRST_NAME")
    public String getFirstName() {
        return firstNameProperty.getValueSafe();
    }
    public void setFirstName(String fName) {
        firstNameProperty.setValue(fName);
    }
    public StringProperty firstNameProperty() {
        return firstNameProperty; 
    }
 
    
    @Column(name="LAST_NAME")
    public String getLastName() {
        return lastNameProperty.getValueSafe();
    }
    public void setLastName(String fName) {
        lastNameProperty.setValue(fName);
    }
    public StringProperty lastNameProperty() {
        return lastNameProperty;
    }
    
    @Column(name="MIDDLE_NAME")
    public String getMiddleName() {
        return middleNameProperty.getValueSafe();
    }
    public void setMiddleName(String mName) {
        middleNameProperty.setValue(mName);
    }
    public StringProperty middleNameProperty() {
        return middleNameProperty;
    }
    
    public StringProperty fullNameProperty(){
        return fullNameProperty;
    }
    
    @Column(name="EMAIL")
    public String getEmail() {
        return emailProperty.getValueSafe();
    }
    public void setEmail(String fName) {
        emailProperty.setValue(fName);
    }
    public StringProperty emailProperty() {
        return emailProperty; 
    }
    
    @Column(name="BIB_Number")
    public String getBib() {
        return bibProperty.getValueSafe();
    }
    public void setBib(String b) {
        bibProperty.setValue(b);
    }
    public StringProperty bibProperty() {
        return bibProperty;
    }
    
    @Column(name="AGE")
    public Integer getAge () {
        return ageProperty.getValue();
    }
    public void setAge (Integer a) {
        ageProperty.setValue(a);
    }
    public IntegerProperty ageProperty() {
        return ageProperty; 
    }
    
    @Column(name="SEX")
    public String getSex() {
        return sexProperty.getValueSafe();
    }
    public void setSex(String s) {
        sexProperty.setValue(s);
    }
    public StringProperty sexProperty() {
        return sexProperty;
    }
    
    @Column(name="CITY")
    public String getCity() {
        return cityProperty.getValueSafe();
    }
    public void setCity(String c) {
        cityProperty.setValue(c);
    }
    public StringProperty cityProperty() {
        return cityProperty; 
    }
    
   @Column(name="STATE")
    public String getState() {
        return stateProperty.getValueSafe();
    }
    public void setState(String s) {
        stateProperty.setValue(s);
    }
    public StringProperty stateProperty(){
        return stateProperty;
    }
    
    @Column(name="BIRTHDAY",nullable=true)
    public String getBirthday() {
        if (birthdayProperty != null) {
            //return Date.from(birthdayProperty.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            return birthdayProperty.toString();
        } else {
            return null; 
        }
    }
    public void setBirthday(LocalDate d) {
        if (d != null) {
            birthdayProperty = d;
        }
    }    
    public void setBirthday(String d) {
        if (d != null) {
            birthdayProperty = LocalDate.parse(d,DateTimeFormatter.ISO_LOCAL_DATE);
            //Instant instant = Instant.ofEpochMilli(d.getTime());
            //setBirthday(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate());
        }
    }
    public LocalDate birthdayProperty() {
        return birthdayProperty;
    }
    
    
    @Transient
    public Boolean getDNF() {
        return dnfProperty.getValue();
    }
    public BooleanProperty dnfProperty(){
        return dnfProperty;
    }
            
    @Transient
    public Boolean getDQ() {
        return dqProperty.getValue();
    }
    public BooleanProperty dqProperty(){
        return dqProperty;
    }
            
            
    @Column(name="note", nullable=true)
    public String getNote() {
        return noteProperty.getValueSafe();
    }
    public void setNote(String s) {
        noteProperty.setValue(s);
    }
    public StringProperty noteProperty(){
        return noteProperty;
    }        
    
    @Enumerated(EnumType.STRING)
    @Column(name="status")
    public Status getStatus() {
        return status;
    }
    public void setStatus(Status s) {
        
        if (s != null && (status == null || ! status.equals(s)) ){
            
            status = s;
            statusProperty.set(status);
        }
    }
    public ObjectProperty<Status> statusProperty(){
        return statusProperty;
    }
            
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name="wave_id", nullable=false)
    @CollectionTable(name="part2wave", joinColumns=@JoinColumn(name="participant_id"))
//    @OrderColumn(name = "index_id")
    public Set<Integer> getWaveIDs() {
        return waveIDSet;  
    }
    public void setWaveIDs(Set<Integer> w) {
        waveIDSet = w; 
        
        
    }
    
    public void setWaves(List<Wave> w) {
        System.out.println("SetWaves(List) called with " + w.size());
        waves.setAll(w);
        waveIDSet = new HashSet();
        waves.stream().forEach(n -> {waveIDSet.add(n.getID());});
    }
    public void setWaves(Set<Wave> w){
        System.out.println("SetWaves(Set) called with " + w.size());
        waves.setAll(w);
        waveIDSet = new HashSet();
        waves.stream().forEach(n -> {waveIDSet.add(n.getID());});
    }
    public void setWaves(Wave w) {
        System.out.println("Participant.setWaves(Wave w)");
        waves.setAll(w);
        
        waveIDSet = new HashSet();
        waveIDSet.add(w.getID()); 
    }
    public void addWave(Wave w) {
        //System.out.println("Participant.addWave(Wave w)");
        if(w == null) System.out.println("Wave is NULL!!!");
        else { 
            //System.out.println("Participant.addWave(Wave w) " + w.getID());
            waves.add(w); 
            waveIDSet.add(w.getID()); 
        }
    }
    public ObservableList<Wave> wavesProperty() {
        waves.clear();
        waveIDSet.stream().forEach(id -> {
            if (RaceDAO.getInstance().getWaveByID(id) == null) System.out.println("Null WAVE!!! " + id);
            waves.add(RaceDAO.getInstance().getWaveByID(id)); 
        });
        return waves; 
    }
    
    public static Callback<Participant, Observable[]> extractor() {
        return (Participant p) -> new Observable[]{p.firstNameProperty,p.middleNameProperty,p.lastNameProperty,p.bibProperty,p.ageProperty,p.sexProperty,p.cityProperty,p.stateProperty,p.countryProperty,p.waves,p.statusProperty};
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + Objects.hashCode(this.uuidProperty.getValue());

        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Participant other = (Participant) obj;
        if (!Objects.equals(this.uuidProperty.getValue(),other.uuidProperty.getValue())) {
            return false; 
        }

        return true;
    }

    
    
}