/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer;

import com.pikatimer.event.Event;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;


/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLpikaController   {
    
    private final Event event = Event.getInstance();
    @FXML private Label eventName;
    @FXML private Label eventDate;
    /**
     * Initializes the controller class.
     */
    @FXML
    protected void initialize() {
        // TODO
        //System.out.println("FXMLpikaController initialized!");
        eventName.textProperty().bind(event.getEventName());
        eventDate.textProperty().bind(event.getEventDateString());
    }
    
    
       
}
