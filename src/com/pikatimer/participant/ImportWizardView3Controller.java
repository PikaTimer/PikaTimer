/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.participant;

import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import com.pikatimer.race.WaveAssignment;
import com.pikatimer.util.AlphanumericComparator;
import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javax.annotation.PostConstruct;
import org.h2.tools.Csv;


@FXMLController("FXMLImportWizardView3.fxml")
public class ImportWizardView3Controller {
    @FXMLViewFlowContext
    private ViewFlowContext context;
    
    @FXML
    ProgressBar progressBar;
    
    @FXML
    Label label; 
    
    @PostConstruct
    public void init() throws FlowException {
        System.out.println("ImportWizardView2Controller.initialize()");
        ImportWizardData model = context.getRegisteredObject(ImportWizardData.class);
        // Take the csv file name and attribute map and start the import
        
        label.setText("Adding Participants...");
        Map<String,String> mapping = model.getAttributeMap(); 
        
        // if we are assigning the race/wave by bib, lets build a list of possible
        // if assigning by an imported attribute
        // if doing a straight assignment, let's just set that up
        
//        
        
        // Itterate over the results set, create the map of attributes -> values, 
        // (optionally) clean up the imported values, 
        // and then call the participantDAO with the map to create the participant.
        
        // We could do this as a direct table -> table import but I'd rather not
        // since in the future we will have custom attributes that will possibly
        // screw things up. 
        Task importTask = new Task<Void>() {
            @Override
            protected Void call() {
                //To change body of generated methods, choose Tools | Templates.
                // get the ParticipantDAO object
                ParticipantDAO participantDAO = ParticipantDAO.getInstance(); 
               int numAdded = 0; 
               int numToAdd = model.getNumToAdd(); 
               updateProgress(numAdded,numToAdd);
               
               // add check to see if we should clear first
               updateMessage("Clearing the existing participants...");
               participantDAO.blockingClearAll(); 
               
               ObservableList<Participant> participantsList =FXCollections.observableArrayList();
                try {
                    ResultSet rs = new Csv().read(model.getFileName(),null,null);
                    ResultSetMetaData meta = rs.getMetaData();
                    
                    while (rs.next()) {
                        numAdded++; 
                        Map<String,String> p = new HashMap<>();
                        
                        for (int i = 0; i < meta.getColumnCount(); i++) {
                            if (mapping.get(meta.getColumnLabel(i+1)) != null) {
                                //System.out.println(rs.getString(i+1) + " -> " + mapping.get(meta.getColumnLabel(i+1)));
                                p.put(mapping.get(meta.getColumnLabel(i+1)),rs.getString(i+1)); 
                            }
                        }
                        Participant newPerson = new Participant(p); 
                        if(model.getWaveAssignByBib()) {
                            newPerson.setWaves(getWaveByBib(p.get("bib")));
                        } else if (model.getWaveAssignByAttribute()) {
                           // todo
                        } else {
                            newPerson.addWave(model.getAssignedWave()); 
                        }
                        
                        //TODO: merge vs add
                        //TODO: Cleanup Name Capitalization (if selected)
                        //TODO: City / State Title Case (if selected)
                        //TODO: Cleanup City/State (if zip specified)
                        
                        //System.out.println("Adding " + newPerson.getFirstName() + " " + newPerson.getLastName() );
                        //participantDAO.addParticipant(newPerson);
                        participantsList.add(newPerson);
                        updateProgress(numAdded,numToAdd);
                        updateMessage("Adding " + newPerson.getFirstName() + " " + newPerson.getLastName() );
                        
                    }
                } catch (SQLException ex) {
                    System.out.println("Something bad happened... ");
                    Logger.getLogger(ImportWizardView3Controller.class.getName()).log(Level.SEVERE, null, ex);
                }
                updateMessage("Saving...");
                participantDAO.addParticipant(participantsList);
                updateMessage("Done! Added " + numAdded + " participants.");
                
                return null; 
            }
        };
        
        
        //TODO: hide the 'done' button until the task is, well, done. 
        progressBar.progressProperty().bind(importTask.progressProperty());
        label.textProperty().bind(importTask.messageProperty());
        new Thread(importTask).start();
                
    }
    
   
    
    private Set<Wave> getWaveByBib(String bib) {
        AlphanumericComparator comp = new AlphanumericComparator(); 
        
        Set<Wave> waves = new HashSet<>();
        Map raceMap = new HashMap(); 
        
        RaceDAO.getInstance().listWaves().forEach(i -> {
            if (i.getWaveAssignmentMethod() == WaveAssignment.BIB) {
                String start = i.getWaveAssignmentStart(); 
                String end = i.getWaveAssignmentEnd(); 
                if (!(start.isEmpty() && end.isEmpty()) && (comp.compare(start, bib) <= 0 || start.isEmpty()) && (comp.compare(end, bib) >= 0 || end.isEmpty())) {
                    if(!raceMap.containsKey(i.getRace())) {
                        //System.out.println("Bib " + bibTextField.getText() + " matched wave " + i.getWaveName() + " results: "+ comp.compare(start, bibTextField.getText()) + " and " + comp.compare(end, bibTextField.getText()) );
                        raceMap.put(i.getRace(), true); 
                        waves.add(i); 
                    }
                }
            }
        });
        
        return waves; 
    }
    
}
