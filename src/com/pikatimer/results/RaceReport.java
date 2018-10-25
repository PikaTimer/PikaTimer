/*
 * Copyright (C) 2016 jcgarner
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
package com.pikatimer.results;

/**
 *
 * @author jcgarner
 */



import com.pikatimer.race.Race;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javax.persistence.CascadeType;
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
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

@Entity
@DynamicUpdate
@Table(name="race_outputs")
public class RaceReport {
    
    private final IntegerProperty IDProperty = new SimpleIntegerProperty();
    private final StringProperty uuidProperty = new SimpleStringProperty(java.util.UUID.randomUUID().toString());
    private final IntegerProperty raceProperty= new SimpleIntegerProperty();
    private final StringProperty reportTypeProperty = new SimpleStringProperty("UNSET");
    
    private List<RaceOutputTarget> raceOutputTargetList;
    private final ObservableList<RaceOutputTarget> raceOutputTargets = FXCollections.observableArrayList();
    
    private ReportTypes reportType;
    private Race race;
    
    private Map<String,String> attributes = new HashMap();
    private Map<String,Integer> intAttributes = new HashMap();
    private Map<String,Boolean> boolAttributes = new HashMap();
    
    private RaceReportType raceReportType;
    
    private Boolean reportTypeChanged = false;
    
    public RaceReport(){
        
    }
//    id int primary key
    @Id
    @GenericGenerator(name="race_outputs_id" , strategy="increment")
    @GeneratedValue(generator="race_outputs_id")
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
    public Race getRace() {
        return race;
    }
    public void setRace(Race r) {
        race=r;
    }
    
//    output_type varchar,
    @Enumerated(EnumType.STRING)
    @Column(name="output_type")
    public ReportTypes getReportType() {
        return reportType;
    }
    public void setReportType(ReportTypes t) {
        
        if (t != null && (reportType == null || ! reportType.equals(t)) ){
            
            reportType = t;
            reportTypeProperty.setValue(reportTypeProperty.toString());
            reportTypeChanged = true;
        }
    }
    
    @OneToMany(mappedBy="raceReport",cascade={CascadeType.PERSIST, CascadeType.REMOVE},fetch = FetchType.LAZY)
    @Fetch(FetchMode.SELECT)
    public List<RaceOutputTarget> getRaceOutputTargets() {
        return raceOutputTargetList;
    }
    public void setRaceOutputTargets(List<RaceOutputTarget> rr) {
        raceOutputTargetList = rr;
        if (rr == null) {
            System.out.println("RaceReport.setRaceOutputTarget(list) called with null list");
        } else {
            System.out.println("RaceReport.setRaceOutputTargets(list) " + "( " + IDProperty.getValue().toString() + ")" + " now has " + raceOutputTargetList.size() + " Output Destinations");
            raceOutputTargets.setAll(rr);
        }
    }
    public ObservableList<RaceOutputTarget> outputTargets() {
        return raceOutputTargets;
    }
    public void addRaceOutputTarget(RaceOutputTarget t) {
        raceOutputTargets.add(t);
        t.setRaceReport(this);
        raceOutputTargetList = raceOutputTargets.sorted();
        //raceReportsList = raceReports.sorted();
    }
    public void removeRaceOutputTarget(RaceOutputTarget w) {
        raceOutputTargets.remove(w);
        raceOutputTargetList = raceOutputTargets.sorted();
    }
    
    // The map of attributes -> values
    // easier than a really wide table of attributes since this thing will just 
    // grow once we add in custom stuff
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="attribute", insertable=false,updatable=false)
    @Column(name="value")
    @CollectionTable(name="race_output_attributes", joinColumns=@JoinColumn(name="output_id"))
    @OrderColumn(name = "id")
    private Map<String, String> getAttributes() {
        return attributes;
    }
    private void setAttributes(Map<String,String> m) {
        attributes = m;
    } 
    
    @Transient
    public Set<String> getKnownAttributeNames() {
        return attributes.keySet();
    }

    
    public Integer getIntegerAttribute(String key) {
        if (!intAttributes.containsKey(key)) {
            if (attributes.containsKey(key)) {
                intAttributes.put(key,Integer.parseUnsignedInt(attributes.get(key)));
            } else {
                System.out.println("RaceAwards.getIntegerAtrribute key of " + key + " is NULL!");
                return null;
            }
        }
        return intAttributes.get(key);
    }
    public void setIntegerAttribute(String key, Integer n) {
        intAttributes.put(key,n);
        attributes.put(key, n.toString());
    }
    
    
    //Pull, Gun, etc
     public Boolean getBooleanAttribute(String key) {
        if (!boolAttributes.containsKey(key)) {
            if (attributes.containsKey(key)) {
                boolAttributes.put(key,Boolean.parseBoolean(attributes.get(key)));
            } else {
                System.out.println("RaceAwards.getBooleanAtrribute key of " + key + " is NULL!");
                return null;
            }
        }
        return boolAttributes.get(key);
    }
    public void setBooleanAttribute(String key, Boolean n) {
        boolAttributes.put(key,n);
        attributes.put(key, n.toString());
    }
    
    public String getStringAttribute(String key) {
        if (!attributes.containsKey(key)) {
            return null;
        }
        return attributes.get(key);
    }
    public void setStringAttribute(String key, String v) {
        attributes.put(key, v);
    }
    
    public void processResultNow(List<ProcessedResult> r){
        System.out.println("RaceReport.procesResultNow() Called... ");
        if (race != null && reportType != null) {
            if (raceReportType == null || reportTypeChanged) {
                raceReportType = reportType.getReportType();
                raceReportType.init(race);
                reportTypeChanged = false;
            }
            System.out.println("RaceReport.procesResult() calling raceReportType.process()");
            String output = raceReportType.process(r, this);
            
            // for each output portal, ship it...
            raceOutputTargets.forEach(ot -> {
                System.out.println("RaceReport.procesResult() calling ot.saveOutput()");
                ot.saveOutput(output);
            });
        }
    }
    public void processResultIfEnabled(List<ProcessedResult> r){
        // If we are enabled... do something
        System.out.println("RaceReport.procesResult() Called... ");
        if (getBooleanAttribute("enabled")) {
            processResultNow(r);
        }
    }
    
}
