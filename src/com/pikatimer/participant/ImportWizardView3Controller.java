/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.participant;

import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
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
        
        // add in a progress bar
        
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
                    Logger.getLogger(ImportWizardView2Controller.class.getName()).log(Level.SEVERE, null, ex);
                }
                updateMessage("Saving...");
                participantDAO.addParticipant(participantsList);
                updateMessage("Done! Added " + numAdded + " participants.");
                
                return null; 
            }
        };
        
        //TODO: Update the progress bar
        //TODO: hide the 'done' button until the task is, well, done. 
        progressBar.progressProperty().bind(importTask.progressProperty());
        label.textProperty().bind(importTask.messageProperty());
        new Thread(importTask).start();
                
    }
}
