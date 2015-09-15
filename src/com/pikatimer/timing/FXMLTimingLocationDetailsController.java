/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * FXML Controller class
 *
 * @author jcgarner
 */
// Rename this to TiminglocationInput and ditch the fxml/controller crap. 
public class FXMLTimingLocationDetailsController{
    @FXML private TextField timingLocationInputNameTextField; 
    @FXML private Pane parentPane; 
    
    /**
     * Initializes the controller class.
     */
    @FXML
    public void initialize() {
        // TODO
        
        
    }    
    
    public void setTimingLocationInput(TimingLocationInput ti) {
        //timingLocationNameTextField.setText(tl.getLocationName());
        if(ti != null) {
            //timingLocationInputNameTextField.textProperty().setValue(ti.getLocationName());
        }
            
    }
    
    public void removeTimingInput(ActionEvent fxevent){
        VBox parent; 
        //TimingLocationDAO.getInstance().removeTimingLocationInput(this);
        //parent.getChildren().remove(this); 
       
    }

}
