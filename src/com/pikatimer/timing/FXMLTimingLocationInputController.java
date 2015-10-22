/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import java.util.Arrays;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * FXML Controller class
 *
 * @author jcgarner
 */
// Rename this to TiminglocationInput and ditch the fxml/controller crap. 
public class FXMLTimingLocationInputController{
    @FXML private TextField locationNameTextField; 
    @FXML private GridPane baseGridPane; 
    @FXML private ProgressIndicator locationProgressIndicator;
    @FXML private ToggleButton startToggleButton;
    @FXML private Pane readerPane;
    @FXML private ChoiceBox inputTypeChoiceBox;
    //@FXML private Button inputChooserButton;
    //@FXML private TextField timingLocationInputDataTextField;
    @FXML private Label readCountLabel;
    
    //private VBox parentPane;  
    private TimingDAO timingLocationDAO;
    private TimingLocationInput timingLocationInput;
    //private TimingReader timingReader;
    
    /**
     * Initializes the controller class.
     */
    @FXML
    public void initialize() {
        // TODO
        //parentPane = (VBox) baseGridPane.getParent();
        //System.out.println("parentPane is a " +  parentPane.getClass().getName());

        timingLocationDAO=TimingDAO.getInstance();
        locationProgressIndicator.visibleProperty().bind(startToggleButton.selectedProperty());
        locationProgressIndicator.setProgress(-1.0);
        
        inputTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TimingInputTypes>() {
                @Override
                public void changed(ObservableValue<? extends TimingInputTypes> observableValue, TimingInputTypes o, TimingInputTypes n) {
                    if (o == null || ! o.equals(n)) {
                        System.out.println("inputTypeChoiceBox event");
                        timingLocationInput.setTimingInputType(n);
                        timingLocationDAO.updateTimingLocationInput(timingLocationInput);
                        
                        // TODO: clear out any existing reads for this input
                    }
                }
            });
        
    }    
    
    public void setTimingLocationInput(TimingLocationInput ti) {
        System.out.println("setTimingLocationInput called...");
        //timingLocationNameTextField.setText(tl.getLocationName());
        if(ti != null) {
            // Initialize everything.
            //timingLocationInputNameTextField.textProperty().setValue(ti.getLocationName());
            timingLocationInput = ti; 
            locationNameTextField.textProperty().setValue(ti.getTimingLocation().getLocationName() + " " + ti.getID().toString());
            
            //Init the input and wire itinto the select button and display
            //timingLocationInput.setInputButton(inputChooserButton);
            //timingLocationInput.setInputTextField(timingLocationInputDataTextField);
            
            // Get the reader type and wire in the choice box
            ObservableList<TimingInputTypes> readerTypeList = FXCollections.observableArrayList(Arrays.asList(TimingInputTypes.values()));
            inputTypeChoiceBox.setItems(readerTypeList);
            
            
            
            
            
            if(timingLocationInput.getTimingInputType() != null) {
                inputTypeChoiceBox.setValue(timingLocationInput.getTimingInputType());
            } else {
                inputTypeChoiceBox.setValue(TimingInputTypes.RFIDFile);
            }

            timingLocationInput.initializeReader(readerPane);
            
            // get the current status of the reader
            startToggleButton.selectedProperty().setValue(timingLocationInput.continueReadingProperty().getValue());
            timingLocationInput.continueReadingProperty().bind(startToggleButton.selectedProperty());
            
            //Get a count for the reader and wire into the readCountLabel
            
            // Wire in the optional readerPane
            
            
            // Wire in the counter
            
            
        
        
        
        
        
        }
            
    }
    
    public void removeTimingInput(ActionEvent fxevent){
        System.out.println("parentPane is a " +  baseGridPane.getParent().getClass().getName());

        boolean remove = ((VBox) baseGridPane.getParent()).getChildren().remove(baseGridPane);
        if (timingLocationInput != null) {
            timingLocationDAO.removeTimingLocationInput(timingLocationInput);
        }
        //TimingLocationDAO.getInstance().removeTimingLocationInput(this);
        //parent.getChildren().remove(this); 
       
    }
    
    

}
