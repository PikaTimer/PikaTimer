/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * FXML Controller class
 *
 * @author jcgarner
 */

public class FXMLTimingController {
    @FXML private VBox timingVBox;
    @FXML private ListView<TimingLocation> timingLocListView;
    @FXML private Button timingLocRemoveAllButton;
    @FXML private Button timingLocAddButton;
    @FXML private Button timingLocRemoveButton;  
    @FXML private TextField timingLocationNameTextField; 
    
    private ObservableList<TimingLocation> timingLocationList;
    private TimingLocation selectedTimingLocation;
    private TimingDAO timingLocationDAO; 
    //private FXMLTimingLocationInputController timingLocationDetailsController;
    private FXMLLoader timingLocationDetailsLoader ;
    
    @FXML private VBox timingDetailsVBox;
    /**
     * Initializes the controller class.
     */
    @FXML
    public void initialize() {
        
        selectedTimingLocation = null;
        // TODO
        
        timingLocationDAO=TimingDAO.getInstance();
        timingLocationList=timingLocationDAO.listTimingLocations(); 
        
        timingLocListView.setItems(timingLocationList);
        timingLocListView.setEditable(true);
        
        //timingLocListView.setCellFactory(TextFieldListCell.forListView(null));
        timingLocListView.setCellFactory(TextFieldListCell.forListView(new StringConverter(){
            @Override
            public TimingLocation fromString(String s) {
                TimingLocation t = new TimingLocation();
                t.setLocationName(s);
                return t; 
            }
            @Override
            public String toString(Object t) {
                if (t != null) {
                return ((TimingLocation)t).toString(); 
                } else {
                    System.out.println("Timing StringConverter toString null object detected.");
                    return "";
                }
            }
        }        
        ));		

        timingLocListView.setOnEditCommit((ListView.EditEvent<TimingLocation> t) -> {
            System.out.println("setOnEditCommit " + t.getIndex());
            if(t.getIndex() < t.getSource().getItems().size()) {
                TimingLocation tl = t.getSource().getItems().get(t.getIndex()); 
                if (t.getNewValue().toString().isEmpty()) {
                    //timingLocationDAO.removeTimingLocation(tl);
                    //tl.setLocationName("New Timing Location");
                    timingLocationDAO.removeTimingLocation(tl);
                } else {
                    tl.setLocationName(t.getNewValue().toString());
                    timingLocationDAO.updateTimingLocation(tl);
                }
            } else {
                System.out.println("Timing setOnEditCommit event out of index: " + t.getIndex());
            }
            timingLocAddButton.requestFocus();
            timingLocAddButton.setDefaultButton(true);
        });

        timingLocListView.setOnEditCancel((ListView.EditEvent<TimingLocation> t) ->{
            System.out.println("setOnEditCancel " + t.getIndex());
            if (t.getIndex() >= 0 && t.getIndex() < t.getSource().getItems().size()) {
                TimingLocation tl = t.getSource().getItems().get(t.getIndex());
                if (tl.getLocationName().isEmpty()) {
                    //tl.setLocationName("New Timing Location");
                    timingLocationDAO.removeTimingLocation(tl);
                }
            } else {
                System.out.println("Timing setOnEditCancel event out of index: " + t.getIndex());
            }
            timingLocAddButton.requestFocus();
            timingLocAddButton.setDefaultButton(true);
        });
        
        timingLocRemoveButton.disableProperty().bind(timingLocListView.getSelectionModel().selectedItemProperty().isNull());

        // load up the TimingLocationDetailsPane
        // Save the FXMLLoader so that we can send it notes when things change in the races box
        
        /*        timingDetailsVBox.getChildren().clear();
        try {
        timingLocationDetailsLoader = new FXMLLoader(getClass().getResource("/com/pikatimer/timing/FXMLTimingLocationInput.fxml"));
        timingDetailsVBox.getChildren().add(timingLocationDetailsLoader.load());
        } catch (IOException ex) {
        Logger.getLogger(FXMLTimingController.class.getName()).log(Level.SEVERE, null, ex);
        ex.printStackTrace();
        }
        
        timingLocationDetailsController =(FXMLTimingLocationInputController)timingLocationDetailsLoader.getController(); 
        //timingLocationDetailsController.selectTimingLocation(selectedTimingLocation);
        */
            
         //if there are no timing locations selected in the view then disable the entire right hand side
         timingDetailsVBox.visibleProperty().bind(timingLocListView.getSelectionModel().selectedItemProperty().isNull().not());
         timingLocListView.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Cha‌​nge<? extends TimingLocation> c) -> { 
             System.out.println("timingLocListView changed...");
             //timingLocListView.getSelectionModel().getSelectedItems().forEach(System.out::println); 
             ObservableList<TimingLocation> selectedTimingLocations = timingLocListView.getSelectionModel().getSelectedItems();
             
             timingDetailsVBox.getChildren().clear();
             
             if ( selectedTimingLocations.size() == 0 ) {
                System.out.println("Nothing Selected");
                //timingLocationDetailsController.selectTimingLocation(null);
                if (selectedTimingLocation != null) {
                    timingLocationNameTextField.textProperty().unbindBidirectional(selectedTimingLocation.LocationNameProperty());
                    selectedTimingLocation=null; 
                }
             } else {
                System.out.println("We just selected " + selectedTimingLocations.get(0).getLocationName());
                //timingLocationNameTextField.textProperty().setValue(selectedTimingLocations.get(0).LocationNameProperty().getValue());
                
                if (selectedTimingLocation != null) {
                    System.out.println("Unbinding timingLocationNameTextField");
                    timingLocationNameTextField.textProperty().unbindBidirectional(selectedTimingLocation.LocationNameProperty());
                }
                selectedTimingLocation=selectedTimingLocations.get(0); 
                timingLocationNameTextField.textProperty().bindBidirectional(selectedTimingLocation.LocationNameProperty());
                System.out.println("Selected timing location is now " + selectedTimingLocation.getLocationName());
                //timingLocationDetailsController.setTimingLocationInput(null); // .selectTimingLocation(selectedTimingLocations.get(0));
                if (selectedTimingLocation.getInputs().size() == 0 ) { // no inputs yet
                    addTimingInput(null);
                } else { // display all of the inputs
                    System.out.println("Starting the display of inputs for a timing location");
                    selectedTimingLocation.getInputs().forEach(i -> {
                        System.out.println("showing input for a timing location ");
                        showTimingInput(i);
                    });
                    System.out.println("Done showing inputs for a timing location");
                }
             }
         });
         
        timingLocListView.getSelectionModel().clearAndSelect(0);
        
        timingLocationNameTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
            System.out.println("timingLocationNameTextField out focus");

                if ( timingLocationNameTextField.getText().isEmpty() ) {
                    timingLocationNameTextField.textProperty().setValue("Unnamed");
                    TimingDAO.getInstance().updateTimingLocation(timingLocListView.getSelectionModel().getSelectedItems().get(0));
                }
                
                TimingDAO.getInstance().updateTimingLocation(timingLocListView.getSelectionModel().getSelectedItems().get(0));
                
            }
        });
        
    }    
    
    
        public void resetTimingLocations(ActionEvent fxevent){
        // prompt 
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Resetting all Timing Locations");
        alert.setHeaderText("This action cannot be undone.");
        alert.setContentText("This will reset the timing locations to default values.\nAll splits will be reassigned to one of the default locations.");
        //Label alertContent = new Label("This will reset the timing locations to default values.\nAll splits will be reassigned to one of the default locations.");
        //alertContent.setWrapText(true); 
        //alert.getDialogPane().setContent(alertContent);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK){
            timingLocationDAO.createDefaultTimingLocations();
        } else {
            // ... user chose CANCEL or closed the dialog
        }
        
        timingLocAddButton.requestFocus();
        timingLocAddButton.setDefaultButton(true);
    }
        
    public void addTimingLocation(ActionEvent fxevent){
        // prompt 
        TimingLocation t = new TimingLocation();
        t.setLocationName("New Timing Location");

        
        timingLocationDAO.addTimingLocation(t);

        System.out.println("Setting the timingLocListView.edit to " + timingLocationList.size() + " " + timingLocationList.indexOf(t));
        timingLocListView.getSelectionModel().select(timingLocationList.indexOf(t));
        
        //timingLocListView.edit(timingLocationList.indexOf(t));
        timingLocationNameTextField.requestFocus();
        timingLocationNameTextField.selectAll();
        //Because we call the timingLocListView.edit, we don't want to pull back focus
        //timingLocAddButton.requestFocus();

    }
    public void removeTimingLocation(ActionEvent fxevent){
        //TODO: If the location is referenced by a split, 
        //prompt to reassign the split to a new location or cancel the edit. 
        
        timingLocationDAO.removeTimingLocation(timingLocListView.getSelectionModel().getSelectedItem());
        //timingLocAddButton.requestFocus();
        //timingLocAddButton.setDefaultButton(true);
    }
    
    public void addTimingInput(ActionEvent fxevent){
        TimingLocationInput tli = new TimingLocationInput();
        tli.setTimingLocation(selectedTimingLocation);
        tli.setLocationName(selectedTimingLocation.getLocationName() + " Input " + selectedTimingLocation.getInputs().size()+1);
        timingLocationDAO.addTimingLocationInput(tli);
        showTimingInput(tli);
        //timingLocationDetailsController.selectTimingLocation(selectedTimingLocation);
    }
    
    private void showTimingInput(TimingLocationInput i) {
        System.out.println("showTimingInput called... ");
        FXMLLoader tlLoader = new FXMLLoader(getClass().getResource("/com/pikatimer/timing/FXMLTimingLocationInput.fxml"));
        try {
            timingDetailsVBox.getChildren().add(tlLoader.load());
            
        } catch (IOException ex) {
            Logger.getLogger(FXMLTimingController.class.getName()).log(Level.SEVERE, null, ex);
        }
        ((FXMLTimingLocationInputController)tlLoader.getController()).setTimingLocationInput(i); 
        //timingLocationDetailsController.selectTimingLocation(selectedTimingLocation);
    }
    public void clearAllTimes(ActionEvent fxevent){
        //TODO: Prompt and then remove all times associated with that timing location 
        // _or_ all timing locations
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Clear Timing Data...");
        alert.setHeaderText("Clear Timing Data:");
        alert.setContentText("Do you want to clear the times for just this imput or all inputs?.");

        ButtonType allButtonType = new ButtonType("All");
        
        ButtonType currentButtonType = new ButtonType("Current",ButtonData.YES);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(cancelButtonType, allButtonType,  currentButtonType );

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == allButtonType){
            // ... user chose "One"
        } else if (result.get() == currentButtonType) {
            // ... user chose "Two"

        } else {
            // ... user chose CANCEL or closed the dialog
        }
 
    }


    
}
