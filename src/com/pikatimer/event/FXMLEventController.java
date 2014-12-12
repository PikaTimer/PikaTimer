/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.event;



import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLEventController  {
    
    private final Event event = Event.getInstance();
    private EventDAO eDAO; 
    @FXML private TextField eventTitle;
    @FXML private DatePicker eventDate;
    /**
     * Initializes the controller class.
     */
   @FXML
    protected void initialize() {
        // TODO
        System.out.println("FXMLpikaController initialize called...");
        
        
        
        eventTitle.setText(event.getEventName());
        System.out.println("FXMLpikaController initialize set title");
        
        
        
        //Watch for text changes... Because setOnInputMethodTextChanged does not work :-( 
        eventTitle.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("Textfield out focus");
                if ( ! eventTitle.getText().equals(event.getEventName()) ) {
                    setEventTitle();
                }
            }
        });
        
        // Use this if ou what keystroke by keystroke monitoring.... 
        //        eventTitle.textProperty().addListener((observable, oldValue, newValue) -> {
        //            //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
        //            event.setEventName(newValue);
        //            updateEvent();
        //        });
        
        eventDate.setValue(event.getLocalEventDate());
        
        
        /** Set the actions after we seed the value, otherwise the eventDate
         * action will fire and call an update... ugh... 
         * 
        */
        eventDate.setOnAction(this::setEventDate);
        //eventDate.onInputMethodTextChangedProperty()
        //eventDate.onInputMethodTextChanged(this::setEventDate);
        System.out.println("FXMLpikaController initialize set date");
        
        System.out.println("FXMLpikaController initialized!");
        
    }
    
    /**
     * Initializes the controller class.
     * @param fxevent
     */
    @FXML
    protected void setEventTitle(ActionEvent fxevent) {
        event.setEventName(eventTitle.getText());
        updateEvent();
    }
    
    protected void setEventTitle() {
        event.setEventName(eventTitle.getText());
        updateEvent();
    }
    
    @FXML
    protected void setEventDate(ActionEvent fxevent) {
        
        event.setEventDate(eventDate.getValue());
        updateEvent();
           
    }
    
    private void updateEvent() {
        if (eDAO == null) {
            eDAO = new EventDAO(); 
        }
        eDAO.updateEvent();
    }
}
