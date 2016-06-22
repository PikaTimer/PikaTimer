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



import javafx.beans.property.IntegerProperty;
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
@Table(name="race_outputs")
public class RaceReport {
    
    private final IntegerProperty IDProperty = new SimpleIntegerProperty();
    private final StringProperty uuidProperty = new SimpleStringProperty(java.util.UUID.randomUUID().toString());
    private final IntegerProperty raceProperty= new SimpleIntegerProperty();
    private final StringProperty reportTypeProperty = new SimpleStringProperty("UNSET");
    private ReportTypes reportType;
    
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
    

//    race_id int,
    @Column(name="race_id")
    public Integer getRace() {
       // System.out.println("Participant UUID is " + uuidProperty.get());
        return raceProperty.getValue(); 
    }
    public void setRace(Integer  n) {
        raceProperty.setValue(n);
        //System.out.println("Participant UUID is now " + uuidProperty.get());
    }
    public IntegerProperty nameProperty() {
        return raceProperty; 
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
        }
    }
    
    
}
