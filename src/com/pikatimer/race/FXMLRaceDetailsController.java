/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.race;

import com.pikatimer.util.Unit;
import java.util.Arrays;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;



/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLRaceDetailsController {
    private RaceDAO raceDAO; 
    
    @FXML private ChoiceBox distanceUnitChoiceBox; 
    
    /**
     * Initializes the controller class.
     */
    public void initialize() {
        // TODO

        // get a RaceDAO
        raceDAO = RaceDAO.getInstance(); 
        
        distanceUnitChoiceBox.setItems(FXCollections.observableArrayList(Arrays.asList(Unit.values()))); 
        distanceUnitChoiceBox.setValue(Unit.MILES);
        
                
        
    }    
    
    public void selectRace(Race r) {
        //todo
        //Setup the splits VBox
        
        //Setup the wave starts VBOX
        
        //show/hide the race name if there is only one race. 
        // and bind it to the event name
        
        // if there is only one race, blan out the bib range options
        
        // give the distance unit drop down some useful things to select from
        
        // setup the cutoff label so that it displace the pace in M/Mi if 
        // it is set, otherwise blank it
    }
    
}
