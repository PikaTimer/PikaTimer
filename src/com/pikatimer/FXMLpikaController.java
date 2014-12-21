/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer;

import com.pikatimer.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;


/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLpikaController   {
    
    private final Event event = Event.getInstance();
    @FXML private Label eventName;
    @FXML private Label eventDate;
    @FXML private TabPane mainTabPane; 
    /**
     * Initializes the controller class.
     */
    @FXML
    protected void initialize() {
        // TODO
        //System.out.println("FXMLpikaController initialized!");
        eventName.textProperty().bind(event.getObservableEventName());
        eventDate.textProperty().bind(event.getObservableEventDateString());
        event.setMainTabPane(mainTabPane);
    }
    
    
       
}
