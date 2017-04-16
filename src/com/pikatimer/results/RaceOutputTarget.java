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
package com.pikatimer.results;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javax.persistence.Column;
import javax.persistence.Entity;
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
 * @author John Garner
 */
@Entity
@DynamicUpdate
@Table(name="race_output_targets")
public class RaceOutputTarget {
    
        
    private final IntegerProperty IDProperty = new SimpleIntegerProperty();
    private final StringProperty uuidProperty = new SimpleStringProperty(java.util.UUID.randomUUID().toString());
    private final StringProperty outputFilenameProperty = new SimpleStringProperty();
    
    private Integer outputDestinationID;
    
    private RaceReport raceReport;
    private ReportDestination outputDestination;
    
//    id int primary key, 
//    uuid varchar,
//    output_id int,
//    remote_target_id int,
//    output_filename varchar
    
    RaceOutputTarget(){
        
    }
//    id int primary key
    @Id
    @GenericGenerator(name="race_output_target_id" , strategy="increment")
    @GeneratedValue(generator="race_output_target_id")
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "output_id",nullable=false)
    public RaceReport getRaceReport() {
        return raceReport;
    }
    public void setRaceReport(RaceReport r) {
        raceReport=r;
    }
    
    //    remote_target_id int,

    @Column(name="remote_target_id")
    public Integer getOutputDestination() {
        //if (outputDestination != null)  return outputDestination.getID();
        return outputDestinationID;
    }
    public void setOutputDestination(Integer id) {
        outputDestinationID = id;
        //outputDestination=ResultsDAO.getInstance().getReportDestinationByID(id);
    }
    public ReportDestination outputDestination() {
        //if(outputDestination == null) outputDestination=ResultsDAO.getInstance().getReportDestinationByID(outputDestinationID);
        //return outputDestination; 
        return ResultsDAO.getInstance().getReportDestinationByID(outputDestinationID);
    }
    
    //    output_filename varchar
    @Column(name="output_filename")
    public String getOutputFilename() {
        return outputFilenameProperty.getValueSafe();
    }
    public void setOutputFilename(String s) {
        outputFilenameProperty.setValue(s);
    }
    public StringProperty outputFilenameProperty() {
        return outputFilenameProperty;
    }
    
    public void saveOutput(String s){
        System.out.println("RaceOutputTarget.saveOutput() called");
        outputDestination = ResultsDAO.getInstance().getReportDestinationByID(outputDestinationID);
        
        if (outputDestination != null && ! outputFilenameProperty.isEmpty().getValue()) 
            outputDestination.save(outputFilenameProperty.getValue(), s);
        
        if (outputDestination == null) 
            System.out.println("RaceOutputTarget.saveOutput() outputDestination is NULL!" + outputDestinationID);
        
        if (outputFilenameProperty.isEmpty().getValue()) 
            System.out.println("RaceOutputTarget.saveOutput() outputFilenameProperty is empty!: " + outputFilenameProperty.getValueSafe());
    }
}
