/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.event;



import com.pikatimer.race.FXMLRaceDetailsController;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLEventController  {
    
    private final Event event = Event.getInstance();
    private EventDAO eDAO; 
    private FXMLLoader raceDetailsLoader ;
    @FXML private TextField eventTitle;
    @FXML private DatePicker eventDate;
    @FXML private CheckBox multipleRacesCheckBox;
    @FXML private VBox racesVBox; 
    @FXML private CheckBox multipleTimingCheckBox;
    @FXML private VBox timingVBox;
    @FXML private Pane raceDetailsPane;
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
        
        //event.multipleRacesProperty().bind(singleRaceCheckBox.selectedProperty());
        
        //Setup the races VBox 
        // Bind the multiple Races CheckBox to the races table to automatically 
        // enable / disable it
        timingVBox.managedProperty().bind(multipleTimingCheckBox.selectedProperty());
        timingVBox.visibleProperty().bind(multipleTimingCheckBox.selectedProperty());
        // if we have more than one race then let's set the multipleRacesCheckBox to true.
        multipleTimingCheckBox.setSelected(false);
        
        //Setup the races VBox 
        // Bind the multiple Races CheckBox to the races table to automatically 
        // enable / disable it
        racesVBox.managedProperty().bind(multipleRacesCheckBox.selectedProperty());
        racesVBox.visibleProperty().bind(multipleRacesCheckBox.selectedProperty());
        
        
        // Populate the underlying table with any races.
        // raceDAO.getRaces(); 
        
        // if we have more than one race then let's set the multipleRacesCheckBox to true.
        multipleRacesCheckBox.setSelected(false);
        
        // load up the raceDetailsPane
        // Save the FXMLLoader so that we can send it notes when things change in the races box
        raceDetailsPane.getChildren().clear();
            try {
                raceDetailsLoader = new FXMLLoader(getClass().getResource("/com/pikatimer/race/FXMLRaceDetails.fxml"));
                raceDetailsPane.getChildren().add(raceDetailsLoader.load());
            } catch (IOException ex) {
                Logger.getLogger(FXMLEventController.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        
         // bind the selected race to the
         //FXMLRaceDetailsController raceDetailsController = raceDetailsLoader.<FXMLRaceDetailsController>getController(); 
         //raceDetailsController.selectRace(r);
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
    
//    @FXML
//    protected void toggleSingleRaceCheckBox(ActionEvent fxevent) {
//        // are we enabled or disabled?
//        if ( singleRaceCheckBox.isSelected() ) {
//            System.out.println("Only one race...");
//            
//            // load the single race fxml into the singleRacePane; 
//            
//            event.getMainTabPane().getTabs().removeIf(p -> p.getText().equals("Races"));
//            singleRacePane.getChildren().clear();
//            try {
//                singleRacePane.getChildren().add(FXMLLoader.load(getClass().getResource("/com/pikatimer/race/FXMLSingleRace.fxml")));
//            } catch (IOException ex) {
//                Logger.getLogger(FXMLEventController.class.getName()).log(Level.SEVERE, null, ex);
//                ex.printStackTrace();
//            }
//        } else {
//            // More than one race. Show a placeholder text and open the Races Tab
//            System.out.println("More than one race...");
//            singleRacePane.getChildren().clear();
//            singleRacePane.getChildren().add(new Label("Multiple Races"));
//            Tab raceTab = new Tab("Races");
//            try {
//                raceTab.setContent(FXMLLoader.load(getClass().getResource("/com/pikatimer/race/FXMLMultipleRaces.fxml")));
//            } catch (IOException ex) {
//                Logger.getLogger(FXMLEventController.class.getName()).log(Level.SEVERE, null, ex);
//                ex.printStackTrace();
//            }
//            event.getMainTabPane().getTabs().add(1, raceTab);
//        }
//        
//    }
    
    private void updateEvent() {
        if (eDAO == null) {
            eDAO = new EventDAO(); 
        }
        eDAO.updateEvent();
    }
}
