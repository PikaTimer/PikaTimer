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
package com.pikatimer.participant;

import com.pikatimer.event.Event;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import static java.lang.Boolean.FALSE;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javafx.application.Platform;
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
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
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
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jcgarner
 */

@Entity
@DynamicUpdate
@Table(name="PARTICIPANT")
public class Participant {
    

   
    private final StringProperty firstNameProperty = new SimpleStringProperty("");
    private final StringProperty middleNameProperty= new SimpleStringProperty();
    private final StringProperty lastNameProperty= new SimpleStringProperty("");
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
    private final StringProperty zipProperty = new SimpleStringProperty();
    private LocalDate birthday; 
    private final ObjectProperty<LocalDate> birthdayProperty = new SimpleObjectProperty();
    private final ObservableList<Wave> waves = FXCollections.observableArrayList(Wave.extractor());  
    
    
    
    private final IntegerProperty wavesChangedCounterProperty = new SimpleIntegerProperty(0);
    private final ObjectProperty<ObservableList<Wave>> wavesProperty = new SimpleObjectProperty(waves);
    private Set<Integer> waveIDSet = new HashSet(); 
    private final BooleanProperty dnfProperty = new SimpleBooleanProperty(FALSE);
    private final BooleanProperty dqProperty = new SimpleBooleanProperty(FALSE);
    private final StringProperty noteProperty = new SimpleStringProperty();
    private Status status = Status.GOOD; 
    private final ObjectProperty<Status> statusProperty = new SimpleObjectProperty(Status.GOOD);
    
    private final ObservableMap<Integer,StringProperty> customAttributeObservableMap = FXCollections.observableHashMap();
    private Map<Integer,String> customAttributeMap = new HashMap();
    
    public Participant() {
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
        
        waves.addListener(new ListChangeListener<Wave>() {
            @Override
            public void onChanged(Change<? extends Wave> c) {
            
                Platform.runLater(() -> wavesChangedCounterProperty.setValue(wavesChangedCounterProperty.get()+1));
            }
        });
        
        status = Status.GOOD;
        statusProperty.set(status);
        
    }
    public Participant(Map<String, String> attribMap) {
        this();
        setAttributes(attribMap);
        
    }
    public Participant(String firstName, String lastName) {
        this();
        setFirstName(firstName);
        setLastName(lastName);
        
        
    }
    
    public static ObservableMap<String,String> getAvailableAttributes() {
        ObservableMap<String,String> attribMap = FXCollections.observableMap(new LinkedHashMap() );
        
        attribMap.put("bib", "Bib");
        attribMap.put("first", "First Name");
        attribMap.put("middle", "Middle Name");
        attribMap.put("last", "Last Name");
        attribMap.put("age", "Age");
        attribMap.put("birth","Birthday");
        attribMap.put("sex-gender", "Sex");
        attribMap.put("city", "City");
        attribMap.put("state", "State");
        attribMap.put("zip","Zip Code");
        attribMap.put("country", "Country");
        attribMap.put("status","Status");
        attribMap.put("note","Note");
        attribMap.put("email", "EMail");
        // TODO: routine to add custom attributes based on db lookup
        return attribMap; 
    }
    
    
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="attribute_id")
    @Column(name="attribute_value")
    @CollectionTable(name="participant_attributes", joinColumns=@JoinColumn(name="participant_id"))
    public Map<Integer,String> getCustomAttributes(){
        return customAttributeMap;
    }
    public void setCustomAttributes(Map<Integer,String> attribMap) {
        customAttributeMap = attribMap;
        customAttributeMap.keySet().forEach(k -> {
            if (customAttributeObservableMap.containsKey(k)) {
                customAttributeObservableMap.get(k).set(customAttributeMap.get(k));
            } else {
                customAttributeObservableMap.put(k, new SimpleStringProperty(customAttributeMap.get(k)));
            }
        });
    }
    public ObservableMap<Integer,StringProperty> customAttributesProperty(){
        return customAttributeObservableMap;
    }
    public StringProperty getCustomAttribute(Integer cID) {
        if (! customAttributeObservableMap.containsKey(cID)) {
            customAttributeObservableMap.put(cID, new SimpleStringProperty());
        }
        return customAttributeObservableMap.get(cID);
    }
    public void setCustomAttribute(Integer cID, String value){
        customAttributeMap.put(cID, value);
        if (customAttributeObservableMap.containsKey(cID)) {
            customAttributeObservableMap.get(cID).set(value);
        } else {
            customAttributeObservableMap.put(cID, new SimpleStringProperty(value));
        }
    }
    
    
    public void setAttributes(Map<String, String> attribMap) {
        // bulk set routine. Everything is a string so convert as needed
        
        attribMap.entrySet().stream().forEach((Map.Entry<String, String> entry) -> {
            if (entry.getKey() != null) {
                //System.out.println("processing " + entry.getKey() );
             switch(entry.getKey()) {
                 case "bib": this.setBib(entry.getValue()); break; 
                 case "first": this.setFirstName(entry.getValue()); break;
                 case "middle": this.setMiddleName(entry.getValue()); break;
                 case "last": this.setLastName(entry.getValue()); break;
                 
                 case "birth": 
                     this.setBirthday(entry.getValue()); 
                     //set the age too if we were able to parse the birthdate
                     break;
                 case "age": 
                     //Setting the birthdate will also set the age, so if the age is already set just skip it.
                     try {
                        if (this.birthday == null) this.setAge(Integer.parseUnsignedInt(entry.getValue())); 
                     } catch (Exception e) {
                         System.out.println("Unable to parse age " + entry.getValue() );
                     }
                     break; 
                     
                 // TODO: map to selected sex translator
                 case "sex-gender": this.setSex(entry.getValue()); break; 
                 
                 
                     
                 case "city": this.setCity(entry.getValue()); break; 
                 case "state": this.setState(entry.getValue()); break; 
                 case "country": this.setCountry(entry.getValue()); break;
                 case "zip": this.setZip(entry.getValue()); break;
                 
                 case "note": this.setNote(entry.getValue()); break;
                 
                 case "status": 
                     try {
                         this.setStatus(Status.valueOf(entry.getValue()));
                     } catch (Exception e){
                         
                     }
                         
                 case "email": this.setEmail(entry.getValue()); break; 
                     
                 // TODO: Team value
                 
             }
            }
        });
    }
    
    public String getNamedAttribute(String attribute) {
        
        if (attribute != null) {
                //System.out.println("processing " + entry.getKey() );
            switch(attribute) {
                case "bib": return this.bibProperty.getValueSafe();
                case "first": return this.firstNameProperty.getValueSafe();
                case "middle": return this.middleNameProperty.getValueSafe();
                case "last": return this.lastNameProperty.getValueSafe();

                // TODO: catch bad integers 
                case "birth": if (this.birthday != null) return birthday.format(DateTimeFormatter.ISO_DATE); else return "";
                    
                case "age":  if (this.ageProperty.getValue() != null) return this.ageProperty.getValue().toString(); else return "";
                     

                // TODO: map to selected sex translator
                case "sex-gender": return this.sexProperty.getValueSafe();

                case "city": return this.cityProperty.getValueSafe();
                case "state": return this.stateProperty.getValueSafe();
                case "country": return this.countryProperty.getValueSafe();
                case "zip": return this.zipProperty.getValueSafe();

                case "note": return this.noteProperty.getValueSafe();

                case "status": if (status != null) return status.name(); else return Status.GOOD.name();


                case "email": return this.emailProperty.getValueSafe();  
                
                
                
                // TODO: Team value
            }
        }
        return "";
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
        //Set to an upper case M or F for now
        //TODO: Switch this to the allowable values for a SEX 
        if (s == null) return;
        if (s.startsWith("M") || s.startsWith("m")) sexProperty.setValue("M");
        else if (s.startsWith("F") || s.startsWith("f")) sexProperty.setValue("F");
        else sexProperty.setValue(s);
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
    
    @Column(name="ZIP")
    public String getZip() {
        return zipProperty.getValueSafe();
    }
    public void setZip(String s) {
        zipProperty.setValue(s);
    }
    public StringProperty zipProperty(){
        return zipProperty;
    }
    
    @Column(name="COUNTRY")
    public String getCountry() {
        return countryProperty.getValueSafe();
    }
    public void setCountry(String s) {
        countryProperty.setValue(s);
    }
    public StringProperty countryProperty(){
        return countryProperty;
    }
    
    @Column(name="BIRTHDAY",nullable=true)
    public String getBirthday() {
        if (birthday != null) {
            //return Date.from(birthdayProperty.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            return birthday.toString();
        } else {
            return null; 
        }
    }
    public void setBirthday(LocalDate d) {
        if (d != null) {
            birthday = d;
            birthdayProperty.setValue(d);
        }
    }    
    public void setBirthday(String d) {
        //System.out.println("Birthdate String: " + d);
        if (d != null) {
            //Try and parse the date
            // First try the ISO_LOCAL_DATE (YYYY-MM-DD)
            // Then try and catch localized date strings such as MM/DD/YYYY 
            // finally a last ditch effort for things like MM.DD.YYYY
            try{
                birthday = LocalDate.parse(d,DateTimeFormatter.ISO_LOCAL_DATE);
                //System.out.println("Parsed via ISO_LOCAL_DATE: " + d);
            } catch (Exception e){
                try {
                    birthday = LocalDate.parse(d,DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
                    //System.out.println("FormatStyle.SHORT: " + d);
                    
                } catch (Exception e2){ 
                    try {
                        birthday = LocalDate.parse(d,DateTimeFormatter.ofPattern("M/d/yyyy"));
                        // System.out.println("Parsed via M/d/yyyy: " + d);
                    } catch (Exception e3) {
                        //System.out.println("Unble to parse date: " + d);
                    }
                }
            }
           
            if (this.birthday != null) {
                birthdayProperty.setValue(birthday);
                this.setAge(Period.between(this.birthday, Event.getInstance().getLocalEventDate()).getYears());
                //System.out.println("Parsed Date: " + d + " -> " + getAge());
            }

            //Instant instant = Instant.ofEpochMilli(d.getTime());
            //setBirthday(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate());
        }
    }
    public ObjectProperty<LocalDate> birthdayProperty() {
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
        Platform.runLater(() -> wavesChangedCounterProperty.setValue(wavesChangedCounterProperty.get()+1));
    }
    public void setWaves(Set<Wave> w){
        System.out.println("SetWaves(Set) called with " + w.size());
        waves.setAll(w);
        waveIDSet = new HashSet();
        waves.stream().forEach(n -> {waveIDSet.add(n.getID());});
        Platform.runLater(() -> wavesChangedCounterProperty.setValue(wavesChangedCounterProperty.get()+1));

    }
    public void setWaves(Wave w) {
        System.out.println("Participant.setWaves(Wave w)");
        waves.setAll(w);
        
        waveIDSet = new HashSet();
        waveIDSet.add(w.getID()); 
        Platform.runLater(() -> wavesChangedCounterProperty.setValue(wavesChangedCounterProperty.get()+1));

    }
    public void addWave(Wave w) {
        //System.out.println("Participant.addWave(Wave w)");
        if(w == null) System.out.println("Wave is NULL!!!");
        else { 
            //System.out.println("Participant.addWave(Wave w) " + w.getID());
            waves.add(w); 
            waveIDSet.add(w.getID()); 
        }
        Platform.runLater(() -> wavesChangedCounterProperty.setValue(wavesChangedCounterProperty.get()+1));
    }
    public ObservableList<Wave> wavesObservableList() {
        if (waves.size() != waveIDSet.size()){
            waves.clear();
            waveIDSet.stream().forEach(id -> {
                if (RaceDAO.getInstance().getWaveByID(id) == null) System.out.println("Null WAVE!!! " + id);
                waves.add(RaceDAO.getInstance().getWaveByID(id)); 
            });
        }
        return waves; 
    }
    
    public ObjectProperty<ObservableList<Wave>> wavesProperty(){
        return wavesProperty;
    }
    public IntegerProperty wavesChangedCounterProperty(){
        return wavesChangedCounterProperty;
    }
    
    public static Callback<Participant, Observable[]> extractor() {
        return (Participant p) -> new Observable[]{p.firstNameProperty,p.middleNameProperty,p.lastNameProperty,p.bibProperty,p.ageProperty,p.sexProperty,p.cityProperty,p.stateProperty,p.countryProperty,p.wavesProperty,p.wavesChangedCounterProperty,p.statusProperty};
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

    @Transient
    public JSONObject getJSONObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("ID", this.IDProperty.getValue());
            json.put("Bib",this.bibProperty.getValue());
            json.put("FirstName", this.firstNameProperty.getValueSafe());
            json.put("MiddleName", this.middleNameProperty.getValueSafe());
            json.put("LastName", this.lastNameProperty.getValueSafe());
            json.put("Sex", this.sexProperty.getValueSafe());
            json.put("Age", this.ageProperty.getValue());
            json.put("City", this.cityProperty.getValueSafe());
            json.put("State", this.stateProperty.getValueSafe());
            json.put("Country", this.countryProperty.getValueSafe());
            
        } catch (JSONException e){
            
        }
        return json; 
    }

    
    
}