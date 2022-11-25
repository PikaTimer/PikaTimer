/* 
 * Copyright (C) 2017 John Garner
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pikatimer.timing;

import com.pikatimer.util.DurationParser;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
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
    //@FXML private Pane readerPane;
    @FXML private VBox readerVBox;
    @FXML private ChoiceBox<TimingInputTypes> inputTypeChoiceBox;
    @FXML private Label readCountLabel;
    @FXML private CheckBox timeSkewCheckBox;
    @FXML private TextField skewTextField;     
    @FXML private CheckBox backupCheckBox;
    
    @FXML private CheckBox announcerCheckBox;
    
    private TimingDAO timingLocationDAO;
    private TimingLocationInput timingLocationInput;
    
    /**
     * Initializes the controller class.
     */
    @FXML
    public void initialize() {

        timingLocationDAO=TimingDAO.getInstance();
        
        inputTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov, o, n) -> {
            if (o == null || ! o.equals(n)) {
                System.out.println("inputTypeChoiceBox event");
                timingLocationInput.setTimingInputType(n);
                timingLocationInput.initializeReader(readerVBox);
                timingLocationDAO.updateTimingLocationInput(timingLocationInput);
            }
        });
    }    
    
    public void setTimingLocationInput(TimingLocationInput ti) {
        System.out.println("setTimingLocationInput called...");
        
        if(ti != null) {
            // Initialize everything.
            //timingLocationInputNameTextField.textProperty().setValue(ti.getLocationName());
            timingLocationInput = ti; 
            locationNameTextField.setText(ti.getLocationName());
            
            //Watch for text changes... Because setOnInputMethodTextChanged does not work :-( 
            locationNameTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                if (!newPropertyValue) {
                    System.out.println("locationNameTextfield out focus");
                    if ( ! locationNameTextField.getText().equals(timingLocationInput.getLocationName()) ) {
                        System.out.println("Name changed from " + timingLocationInput.getLocationName()+ " to " + locationNameTextField.getText());
                        timingLocationInput.setLocationName(locationNameTextField.getText());
                        timingLocationDAO.updateTimingLocationInput(timingLocationInput);
                    } else {
                        System.out.println("No change in name");
                    }
                }
            });
            
            // Get the reader type and wire in the choice box
            ObservableList<TimingInputTypes> readerTypeList = FXCollections.observableArrayList(Arrays.asList(TimingInputTypes.values()));
            inputTypeChoiceBox.setItems(readerTypeList);
            
            if (timingLocationInput.getTimingInputType() != null) {
                inputTypeChoiceBox.setValue(timingLocationInput.getTimingInputType());
            } else {
                inputTypeChoiceBox.setValue(TimingInputTypes.RFIDFile);
            }
            
            timingLocationInput.initializeReader(readerVBox);
            
            readCountLabel.textProperty().bind(Bindings.convert(timingLocationInput.readCountProperty())); 
            
            // flag as a backup time
            backupCheckBox.setSelected(timingLocationInput.getIsBackup()); 
            
            backupCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
                if (!old_val.equals(new_val)) {
                    timingLocationInput.setIsBackup(new_val);
                    timingLocationDAO.updateTimingLocationInput(timingLocationInput);
                    timingLocationInput.reprocessReads();
                }
            });
            
            // flag as an announcer feed
            announcerCheckBox.setSelected(timingLocationInput.getIsAnnouncer()); 
            
            announcerCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
                if (!old_val.equals(new_val)) {
                    timingLocationInput.setIsAnnouncer(new_val);
                    timingLocationDAO.updateTimingLocationInput(timingLocationInput);
                }
            });
            
            //Setup the skew
            skewTextField.disableProperty().bind(timeSkewCheckBox.selectedProperty().not());
            
            // If we are skewing.... 
            timeSkewCheckBox.setSelected(timingLocationInput.getSkewLocationTime()); 
            
            timeSkewCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
                if (!old_val.equals(new_val)) {
                    timingLocationInput.setSkewLocationTime(new_val);
                    timingLocationDAO.updateTimingLocationInput(timingLocationInput);
                    if (!timingLocationInput.getSkew().isZero()) timingLocationInput.reprocessReads();
                }
            });
            
            skewTextField.textProperty().setValue(timingLocationInput.getSkewString());
            
            skewTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                    //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
                if (newValue.isEmpty()) return; 
                if (newValue.matches("^-?\\.\\d+$")) {
                    Platform.runLater(() -> {
                        int caret = skewTextField.getCaretPosition();
                        skewTextField.setText(newValue.replaceFirst("\\.","0."));
                        skewTextField.positionCaret(caret+1);
                    });
                } else if (newValue.matches("^-?(\\d*:)?(\\d*:)?\\d*\\.?\\d*$")){
                    System.out.println("Good skew time: " + newValue);
                } else {
                    Platform.runLater(() -> {
                        int caret = skewTextField.getCaretPosition();
                        skewTextField.setText(oldValue);
                        skewTextField.positionCaret(caret-1);
                    });
                }
            });
            
            skewTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
                if (!newPropertyValue) {
                    System.out.println("skewTextField out focus");
                    if ( ! skewTextField.getText().equals(timingLocationInput.getSkewString()) ) {
                        System.out.println("Skew changed from " + timingLocationInput.getSkewString()+ " to " + skewTextField.getText());
                        Duration oldSkew = timingLocationInput.getSkew();
                        //timingLocationInput.setSkewString(skewTextField.getText());
                        Duration newSkew = DurationParser.parse(skewTextField.getText(), false);
                        if (! oldSkew.equals(newSkew)){
                            timingLocationInput.setSkew(newSkew);
                            timingLocationDAO.updateTimingLocationInput(timingLocationInput);
                            timingLocationInput.reprocessReads();
                            Platform.runLater(() -> {skewTextField.setText(timingLocationInput.getSkewString());});
                        }
                        if (timingLocationInput.getSkew().isZero()) {
                            timeSkewCheckBox.setSelected(false);
                            Platform.runLater(() -> {skewTextField.setText("");});
                        } else Platform.runLater(() -> {skewTextField.setText(timingLocationInput.getSkewString());});
                    } else {
                        System.out.println("No change in skew time");
                    }
                }
            });
        }
    }
    
    public void removeTimingInput(ActionEvent fxevent){
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Input...");
        alert.setHeaderText("Delete Input:");
        alert.setContentText("Do you want to remove the " +timingLocationInput.getLocationName() + " input?\nThis will clear all reads associated with this input.");

        ButtonType removeButtonType = new ButtonType("Remove",ButtonBar.ButtonData.YES);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(cancelButtonType, removeButtonType );

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == removeButtonType) {
            boolean remove = ((VBox) baseGridPane.getParent()).getChildren().remove(baseGridPane);
            if (timingLocationInput != null) {
                timingLocationDAO.removeTimingLocationInput(timingLocationInput);
            }
        }
    }
    
    public void clearReads(ActionEvent fxevent){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear times...");
        alert.setHeaderText("Clear Times:");
        alert.setContentText("Are you sure you want to clear all existing times for this input?");

        ButtonType deleteButtonType = new ButtonType("Clear Times",ButtonBar.ButtonData.YES);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(cancelButtonType, deleteButtonType );

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == deleteButtonType) {
            timingLocationInput.clearReads(); 
        }
    }
    

}
