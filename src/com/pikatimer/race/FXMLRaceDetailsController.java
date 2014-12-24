/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.race;

import com.pikatimer.participant.Participant;
import com.pikatimer.util.Unit;
import java.math.BigDecimal;
import java.util.Arrays;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;



/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLRaceDetailsController {
    private RaceDAO raceDAO; 
    
    @FXML private HBox raceNameHBox;
    @FXML private TextField raceNameTextField; 
    @FXML private TextField raceDistanceTextField; 
    @FXML private TextField raceStartTimeTextField; 
    @FXML private ChoiceBox distanceUnitChoiceBox; 
    @FXML private TextField raceCutoffTimeTextField;
    @FXML private Label raceCutoffTimePaceLabel; 
    Race selectedRace; 
    /**
     * Initializes the controller class.
     */
    public void initialize() {
        // TODO

        // get a RaceDAO
        raceDAO = RaceDAO.getInstance(); 
        raceNameHBox.disableProperty().bind(Bindings.size(raceDAO.listRaces()).lessThanOrEqualTo(1));
        ObservableList<Unit> unitList = FXCollections.observableArrayList(Arrays.asList(Unit.values()));
       
        //distanceUnitChoiceBox.setItems(FXCollections.observableArrayList(Arrays.asList(Unit.values()))); 
        distanceUnitChoiceBox.setItems(unitList);
        distanceUnitChoiceBox.setValue(Unit.MILES);
        
                
        
    }    
    
    public void selectRace(Race r) {
        selectedRace = r;
        
        if (selectedRace != null) {
            System.out.println("Non-Null race, populate all fields out");
            //Setup the Race Name
            raceNameTextField.setText(selectedRace.getRaceName());
            if(raceNameTextField.disableProperty().get()) {
                raceNameTextField.requestFocus();
            } else {
                raceStartTimeTextField.requestFocus(); 
            }
            //Setup the start time
            
            //Setup the distance
            raceDistanceTextField.setText(selectedRace.getRaceDistance().toPlainString());
            distanceUnitChoiceBox.setValue(selectedRace.getRaceDistanceUnits()); 
            
            //setup the cutoff and pace

            // setup the cutoff label so that it displace the pace in M/Mi if 
            // it is set, otherwise blank it

            //Setup the splits VBox


            //Setup the wave starts VBOX
        
        
        // if there is only one race, blank out the bib range options
        
        
        } else {
            System.out.println("Null race, de-populate all fields out");

            // blank out everything 
            // the pane will be disabled but let's not confuse things
        }
    }
    
    public void updateRaceName(ActionEvent fxevent){
        selectedRace.setRaceName(raceNameTextField.getText());
        raceDAO.updateRace(selectedRace);
    }
    
    public void updateRaceDistance(ActionEvent fxevent){
        //TODO: If the location is referenced by a split, 
        //prompt to reassign the split to a new location or cancel the edit. 
        //Do we have a parsable number?
        BigDecimal dist = null;
        try {
            dist = new BigDecimal(raceDistanceTextField.getText());
            selectedRace.setRaceDistance(dist);
            selectedRace.setRaceDistanceUnits((Unit)distanceUnitChoiceBox.getValue());
            raceDAO.updateRace(selectedRace);
        } catch (Exception e) {
            // not a number
            dist = selectedRace.getRaceDistance();
            raceDistanceTextField.setText(dist.toPlainString());
        }
        
    }
}
