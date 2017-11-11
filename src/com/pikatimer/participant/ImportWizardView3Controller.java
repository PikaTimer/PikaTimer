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

import com.pikatimer.race.Race;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import com.pikatimer.race.WaveAssignment;
import com.pikatimer.util.AlphanumericComparator;
import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
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
    
    @FXML ProgressBar progressBar;
    
    @FXML Label label; 
    
    @PostConstruct
    public void init() throws FlowException {
        System.out.println("ImportWizardView3Controller.initialize()");
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
                
                Boolean existingRunners = false;
                Boolean dupeCheck = false;
                Boolean mergeDupes = false;
                
                Map<String,Wave> waveMap = new HashMap();
                Map<String,Race> raceMap = new HashMap();
                
                if (model.getWaveAssignByAttribute()) {
                    System.out.println("Setting up raceMap and waveMap");
                    RaceDAO.getInstance().listRaces().forEach(race -> {
                        raceMap.put(race.getRaceName(), race);
                        System.out.println("Race \"" + race.getRaceName() + "\" added");
                    });
                    RaceDAO.getInstance().listWaves().forEach(wave -> {
                        waveMap.put(wave.getWaveName(), wave);
                        System.out.println("Wave \"" + wave.getWaveName() + "\" added");

                    });
                    
                }
                    
                
                if (!participantDAO.listParticipants().isEmpty()) {
                    existingRunners = true;
                    // add check to see if we should clear first
                    System.out.println("Import: dup property is " + model.duplicateHandlingProperty().getValue());
                     if (model.clearExistingProperty().get()) {
                         updateMessage("Clearing the existing participants...");
                         participantDAO.blockingClearAll(); 
                     } else {
                         if (model.duplicateHandlingProperty().getValueSafe().equals("Ignore")) {
                             System.out.println("Import: Ignore Duplicates");
                             dupeCheck = true;
                             mergeDupes = false;
                         }
                         else if (model.duplicateHandlingProperty().getValueSafe().equals("Merge")) {
                             System.out.println("Import: Merge Duplicates");
                             dupeCheck = true;
                             mergeDupes = true;
                         }
                         else if (model.duplicateHandlingProperty().getValueSafe().equals("Import")) {
                             System.out.println("Import: Import Duplicates");
                             dupeCheck = false;
                         }
                     }
                }
               //ObservableList<Participant> participantsList =FXCollections.observableArrayList();
               
               
               // Let's play the "What type of text file is this..." game
               // Try UTF-8 and see if it blows up on the decode. If it does, default down to a platform specific type and then hope for the best
               // TODO: fix the "platform specific" part to not assume Windows in the US
               CharsetDecoder uft8Decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
               String charset = "UTF-8"; 
               try {
                    String result = new BufferedReader(new InputStreamReader(new FileInputStream(model.getFileName()),uft8Decoder)).lines().collect(Collectors.joining("\n"));
                } catch (Exception ex) {
                    System.out.println("Not UTF-8: " + ex.getMessage());
                    charset = "Cp1252"; // Windows standard txt file stuff
                }
                
               try {
                    ResultSet rs = new Csv().read(model.getFileName(),null,charset);
                    ResultSetMetaData meta = rs.getMetaData();
                    
                    Map<String,Participant> existingMap = new HashMap();
                    if (dupeCheck) {
                        participantDAO.listParticipants().forEach(p -> {
                            String key = p.getFirstName()+p.getLastName() + p.getAge()+p.getSex();
                            key = key.toLowerCase();
                            existingMap.put(key, p);
                            System.out.println("Added Key " + key);
                        });
                        
                    }
                    System.out.println("ExistingMap size: " + existingMap.size());
                    
                    while (rs.next()) {
                        numAdded++; 
                        
                        Map<String,String> attributes = new HashMap();
                        Map<Integer,String> customAttributes = new HashMap();
                        
                        String pendingWave = "";
                        String pendingRace = ""; 
                        for (int i = 0; i < meta.getColumnCount(); i++) {
                            if (mapping.get(meta.getColumnLabel(i+1)) != null && !"".equals(rs.getString(i+1))) {
                                String key = mapping.get(meta.getColumnLabel(i+1));
                                System.out.println(rs.getString(i+1) + " -> " + key);
                                if (key.equals("WAVE")) {
                                    pendingWave = rs.getString(i+1);
                                } else if (key.equals("RACE")) {
                                    pendingRace = rs.getString(i+1);
                                } else if (key.matches("^\\d$")) {
                                    customAttributes.put(Integer.parseInt(key), rs.getString(i+1));
                                } else {
                                    attributes.put(key,rs.getString(i+1));
                                } 
                            }
                        }
                        Participant p = new Participant(attributes); 
                        p.setCustomAttributes(customAttributes);
                        
                        String key = p.getFirstName()+p.getLastName() + p.getAge()+p.getSex();
                        key = key.toLowerCase();
                        System.out.println("Looking for key " + key);
                        if (dupeCheck && existingMap.containsKey(key)){
                            System.out.println("Found existing key");
                            if (mergeDupes) {
                                p = existingMap.get(key);
                                if (attributes.containsKey("bib") && !p.getBib().equals(attributes.get("bib"))) {
                                    if (participantDAO.getParticipantByBib(attributes.get("bib")) != null) {
                                        System.out.println("Duplicate bib found!");
                                        attributes.put("bib", "Dupe: " + attributes.get("bib"));
                                    }
                                }
                                
                                // This is ugly since updating an existing participant off of the 
                                // Platform thread will cause issues. 
                                Participant np = p; // cuz lambdas don't like changes
                                Platform.runLater(() -> {
                                    np.setAttributes(attributes);
                                    np.setCustomAttributes(customAttributes);
                                    participantDAO.updateParticipant(np);
                                });
                                
                                updateMessage("Merging " + p.getFirstName() + " " + p.getLastName() );
                                updateProgress(numAdded,numToAdd);
                                continue;
                            } else {
                                System.out.println("Duplicate participant found, skipping");
                                updateMessage("Skipping " + p.getFirstName() + " " + p.getLastName() );
                                updateProgress(numAdded,numToAdd);
                                continue;
                            }
                        } else {
                            System.out.println("Did not find an existing person");
                        }
                        
                        if (participantDAO.getParticipantByBib(p.getBib()) != null) {
                            p.setBib("Dupe " + p.getBib());
                        }
                        
                        if(model.getWaveAssignByBib()) {
                            p.setWaves(participantDAO.getWaveByBib(attributes.get("bib")));
                        } else if (model.getWaveAssignByAttribute()) {
                            System.out.println("Assigning wave by attribute: \"" + pendingWave + "\" / \"" + pendingRace + "\"");
                           if (pendingWave.isEmpty() && !pendingRace.isEmpty()){
                               if (raceMap.containsKey(pendingRace)){
                                   p.setWaves(raceMap.get(pendingRace).getWaves().get(0));
                               }
                           } else if (!pendingWave.isEmpty() && pendingRace.isEmpty()){
                               if (waveMap.containsKey(pendingWave)){
                                   p.setWaves(waveMap.get(pendingWave));
                               }
                           } else if (!pendingWave.isEmpty() && !pendingRace.isEmpty()) {
                               // Well crap, we have both....
                               if (waveMap.containsKey(pendingWave)){
                                   p.setWaves(waveMap.get(pendingWave));
                               } else if (raceMap.containsKey(pendingRace)){
                                   p.setWaves(raceMap.get(pendingRace).getWaves().get(0));
                               } // else they are screwed..... Sorry.... 
                           }
                           if (p.wavesObservableList().size() > 0 ) System.out.println("Now in wave " + p.wavesObservableList().get(0).getWaveName());
                           else System.out.println("Not in any wave/race!!!");
                        } else {
                            p.addWave(model.getAssignedWave()); 
                        }
                        
                        participantDAO.addParticipant(p);
                        //TODO: Cleanup Name Capitalization (if selected)
                        //TODO: City / State Title Case (if selected)
                        //TODO: Cleanup City/State (if zip specified)
                        
                        //System.out.println("Adding " + newPerson.getFirstName() + " " + newPerson.getLastName() );
                        //participantDAO.addParticipant(newPerson);
                        
                        updateProgress(numAdded,numToAdd);
                        updateMessage("Adding " + p.getFirstName() + " " + p.getLastName() );
                        
                    }
                } catch (Exception ex) {
                    System.out.println("Something bad happened... ");
                    Logger.getLogger(ImportWizardView3Controller.class.getName()).log(Level.SEVERE, null, ex);
                }
                updateMessage("Saving...");
                
                // TODO: pass the task object down to the DAO so that it can run updateProgress
                
                
                updateMessage("Done! Added " + numAdded + " participants.");
                
                return null; 
            }
        };
        
        
        //TODO: hide the 'done' button until the task is, well, done. 
        progressBar.progressProperty().bind(importTask.progressProperty());
        label.textProperty().bind(importTask.messageProperty());
        new Thread(importTask).start();
                
    }
    
   
    
    
    
}
