/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.event;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLeventController  {
    
    private final Event event = Event.getInstance();
    @FXML private TextField eventTitle;
    @FXML private DatePicker eventDate;
    /**
     * Initializes the controller class.
     */
   
    /**
     * Initializes the controller class.
     * @param fxevent
     */
    @FXML
    protected void setEventTitle(ActionEvent fxevent) {
        event.setEventName(eventTitle.getText());
                  
    }
    
    
    @FXML
    protected void setEventDate(ActionEvent fxevent) {
        
        event.setEventDate(eventDate.getValue());
        
           
    }
}
