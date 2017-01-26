/*
 * Copyright (C) 2016 John Garner
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
package com.pikatimer.results;

import com.pikatimer.race.RaceDAO;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;


/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLResultOutputController {

    @FXML GridPane baseGridPane;
    @FXML ChoiceBox<ReportTypes> outputTypeChoiceBox;
    
    @FXML ToggleSwitch reportEnabledToggleSwitch;
            
    @FXML Label optionsLabel;
    @FXML CheckBox inProgressCheckBox;
    @FXML CheckBox showDQCheckBox;
    @FXML CheckBox showDNFCheckBox;
    @FXML CheckBox showPaceCheckBox;
    @FXML CheckBox showSplitsCheckBox;
    @FXML CheckBox showGunTimeCheckBox;
    @FXML CheckBox showSegmentsCheckBox;
    @FXML CheckBox showSegmentPaceCheckBox;
    @FXML CheckBox hideCustomHeadersCheckBox;
    
    @FXML Label noOutputPpathsLabel;
    
    @FXML VBox outputTargetsVBox;
    @FXML FlowPane outputOptionsFlowPane;
            
    @FXML Button outputAddButton;
    
    RaceReport thisRaceReport; 
    final RaceDAO raceDAO = RaceDAO.getInstance();
    final ResultsDAO resultsDAO = ResultsDAO.getInstance();
    
    final Map<RaceOutputTarget,HBox> rotHBoxMap = new ConcurrentHashMap();
    
    /**
     * Initializes the controller class.
     */
    public void initialize() {
        // TODO
        outputTypeChoiceBox.getItems().setAll(ReportTypes.values());
       outputTargetsVBox.setFillWidth(true);
       
       noOutputPpathsLabel.visibleProperty().bind(Bindings.size(outputTargetsVBox.getChildren()).isEqualTo(0));
       noOutputPpathsLabel.managedProperty().bind(Bindings.size(outputTargetsVBox.getChildren()).isEqualTo(0));
    }    
    
    public void setRaceReport(RaceReport r){
        //This should only be called once
        if (thisRaceReport != null) {
            System.out.println("setRaceReport Called more than once!");
            return;
        }
        thisRaceReport=r;
        
        
        
        outputTypeChoiceBox.getSelectionModel().selectedItemProperty().addListener((ov, oldRT, newRT) -> {
            if (newRT == null) return;
            if (!thisRaceReport.getReportType().equals(newRT)) {
                thisRaceReport.setReportType(newRT);
                resultsDAO.saveRaceReport(r);
            }
            RaceReportType rrt = thisRaceReport.getReportType().getReportType();
            boolean attributeAdded = false;
            
            // Show or hide options based on what the RaceReportType is capable of handling
            optionsLabel.visibleProperty().set(false);

            //@FXML ToggleSwitch reportEnabledToggleSwitch;
            if (r.getBooleanAttribute("enabled") == null) {
                r.setBooleanAttribute("enabled", true);
                attributeAdded = true;
            }
            reportEnabledToggleSwitch.selectedProperty().setValue(r.getBooleanAttribute("enabled"));
            
            //        @FXML CheckBox inProgressCheckBox;
            if (rrt.optionSupport("inProgress")) {
                if (r.getBooleanAttribute("inProgress") == null) {
                    r.setBooleanAttribute("inProgress", false);
                    attributeAdded = true;
                }
                inProgressCheckBox.selectedProperty().setValue(r.getBooleanAttribute("inProgress"));
                inProgressCheckBox.visibleProperty().set(true);
                inProgressCheckBox.managedProperty().set(true);
                optionsLabel.visibleProperty().set(true);

            } else {
                inProgressCheckBox.visibleProperty().set(false);
                inProgressCheckBox.managedProperty().set(false);
            }
            
            //        @FXML CheckBox showDQCheckBox;
            if (rrt.optionSupport("showDQ")) {
                if (r.getBooleanAttribute("showDQ") == null) {
                    r.setBooleanAttribute("showDQ", false);
                    attributeAdded = true;
                }
                showDQCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showDQ"));
                showDQCheckBox.visibleProperty().set(true);
                showDQCheckBox.managedProperty().set(true);
                optionsLabel.visibleProperty().set(true);

            } else {
                showDQCheckBox.visibleProperty().set(false);
                showDQCheckBox.managedProperty().set(false);
            }

            //        @FXML CheckBox showDNFCheckBox;
            if (rrt.optionSupport("showDQ")) {
                if (r.getBooleanAttribute("showDNF") == null) {
                    r.setBooleanAttribute("showDNF", false);
                    attributeAdded = true;
                }
                showDNFCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showDNF"));
                showDNFCheckBox.visibleProperty().set(true);
                showDNFCheckBox.managedProperty().set(true);
                optionsLabel.visibleProperty().set(true);
            } else {
                showDNFCheckBox.visibleProperty().set(false);
                showDNFCheckBox.managedProperty().set(false);
            }
            
            //        @FXML CheckBox showPaceCheckBox;
            if (rrt.optionSupport("showPace")) {
                if (r.getBooleanAttribute("showPace") == null) {
                    r.setBooleanAttribute("showPace", false);
                    attributeAdded = true;
                }
                showPaceCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showPace"));
                showPaceCheckBox.visibleProperty().set(true);
                showPaceCheckBox.managedProperty().set(true);
                optionsLabel.visibleProperty().set(true);
            } else {
                showPaceCheckBox.visibleProperty().set(false);
                showPaceCheckBox.managedProperty().set(false);
            }
            
            //        @FXML CheckBox showGunTimeCheckBox;
            if (rrt.optionSupport("showGun")) {
                if (r.getBooleanAttribute("showGun") == null) {
                    r.setBooleanAttribute("showGun", false);
                    attributeAdded = true;
                }
                showGunTimeCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showGun"));
                showGunTimeCheckBox.visibleProperty().set(true);
                showGunTimeCheckBox.managedProperty().set(true);
                optionsLabel.visibleProperty().set(true);
            } else {
                showGunTimeCheckBox.visibleProperty().set(false);
                showGunTimeCheckBox.managedProperty().set(false);
            }
            
            if (rrt.optionSupport("showSplits")) {
                if (r.getBooleanAttribute("showSplits") == null) {
                    r.setBooleanAttribute("showSplits", false);
                    attributeAdded = true;
                }
                showSplitsCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showSplits"));
                showSplitsCheckBox.visibleProperty().bind(Bindings.size(r.getRace().splitsProperty()).greaterThan(2));
                showSplitsCheckBox.managedProperty().bind(Bindings.size(r.getRace().splitsProperty()).greaterThan(2));
                optionsLabel.visibleProperty().set(true);
            } else {
                showSplitsCheckBox.visibleProperty().unbind();
                showSplitsCheckBox.visibleProperty().set(false);
                showSplitsCheckBox.managedProperty().unbind();
                showSplitsCheckBox.managedProperty().set(false);
            }
            
            if (rrt.optionSupport("showSegments")) {
                if (r.getBooleanAttribute("showSegments") == null) {
                    r.setBooleanAttribute("showSegments", false);
                    attributeAdded = true;
                }
                showSegmentsCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showSegments"));
                showSegmentsCheckBox.visibleProperty().bind(Bindings.size(r.getRace().raceSegmentsProperty()).greaterThan(0));
                showSegmentsCheckBox.managedProperty().bind(Bindings.size(r.getRace().raceSegmentsProperty()).greaterThan(0));
                optionsLabel.visibleProperty().set(true);
            } else {
                showSegmentsCheckBox.visibleProperty().unbind();
                showSegmentsCheckBox.visibleProperty().set(false);
                showSegmentsCheckBox.managedProperty().unbind();
                showSegmentsCheckBox.managedProperty().set(false);
            }
            
            if (rrt.optionSupport("showSegmentPace")) {
                if (r.getBooleanAttribute("showSegmentPace") == null) {
                    r.setBooleanAttribute("showSegmentPace", false);
                    attributeAdded = true;
                }
                showSegmentPaceCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showSegmentPace"));
                showSegmentPaceCheckBox.visibleProperty().bind(Bindings.size(r.getRace().raceSegmentsProperty()).greaterThan(0));
                showSegmentPaceCheckBox.managedProperty().bind(Bindings.size(r.getRace().raceSegmentsProperty()).greaterThan(0));
                optionsLabel.visibleProperty().set(true);
            } else {
                showSegmentPaceCheckBox.visibleProperty().unbind();
                showSegmentPaceCheckBox.visibleProperty().set(false);
                showSegmentPaceCheckBox.managedProperty().unbind();
                showSegmentPaceCheckBox.managedProperty().set(false);
            }
            
            if (rrt.optionSupport("hideCustomHeaders")) {
                if (r.getBooleanAttribute("hideCustomHeaders") == null){
                    r.setBooleanAttribute("hideCustomHeaders", false);
                    attributeAdded = true;
                }
                hideCustomHeadersCheckBox.selectedProperty().setValue(r.getBooleanAttribute("hideCustomHeaders"));
                hideCustomHeadersCheckBox.visibleProperty().set(true);
                hideCustomHeadersCheckBox.managedProperty().set(true);
            } else {
                hideCustomHeadersCheckBox.visibleProperty().set(false);
                hideCustomHeadersCheckBox.managedProperty().set(false);
            }


        
            
            if (attributeAdded) resultsDAO.saveRaceReport(r);
            
        });
        outputTypeChoiceBox.getSelectionModel().select(thisRaceReport.getReportType());

        
        thisRaceReport.outputTargets().forEach(rot -> {
            showRaceReportOutputTarget(rot);
          });
        
        // Pull the key-value map from the race report and populate everything
        
        //@FXML ToggleSwitch reportEnabledToggleSwitch;
        reportEnabledToggleSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (!r.getBooleanAttribute("enabled").equals(new_val)){
                r.setBooleanAttribute("enabled", new_val);
                resultsDAO.saveRaceReport(r);
            }
            
        });         
        
        // Setup checkbox listeners
        //        @FXML CheckBox inProgressCheckBox;
        inProgressCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (!r.getBooleanAttribute("inProgress").equals(new_val)){
                r.setBooleanAttribute("inProgress", new_val);
                resultsDAO.saveRaceReport(r);
            }
        });  
        
//        @FXML CheckBox showDQCheckBox;
        showDQCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (!r.getBooleanAttribute("showDQ").equals(new_val)){
                r.setBooleanAttribute("showDQ", new_val);
                resultsDAO.saveRaceReport(r);
            }
        });
        
//        @FXML CheckBox showDNFCheckBox;
        showDNFCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (!r.getBooleanAttribute("showDNF").equals(new_val)){
                r.setBooleanAttribute("showDNF", new_val);
                resultsDAO.saveRaceReport(r);
            }
        });
        
//        @FXML CheckBox showPaceCheckBox;
        showPaceCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (!r.getBooleanAttribute("showPace").equals(new_val)){
                r.setBooleanAttribute("showPace", new_val);
                resultsDAO.saveRaceReport(r);
            }
        });
        
//        @FXML CheckBox showSplitsCheckBox;
        showSplitsCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (!r.getBooleanAttribute("showSplits").equals(new_val)){
                r.setBooleanAttribute("showSplits", new_val);
                resultsDAO.saveRaceReport(r);
            }
            
        });

        //        @FXML CheckBox showSplitsCheckBox;
        showSegmentsCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (!r.getBooleanAttribute("showSegments").equals(new_val)){
                r.setBooleanAttribute("showSegments", new_val);
                resultsDAO.saveRaceReport(r);
            }
            
        });
        showSegmentPaceCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (!r.getBooleanAttribute("showSegmentPace").equals(new_val)){
                r.setBooleanAttribute("showSegmentPace", new_val);
                resultsDAO.saveRaceReport(r);
            }
            
        });
//        @FXML CheckBox showGunTimeCheckBox;
        showGunTimeCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showGun", new_val);
            resultsDAO.saveRaceReport(r);
        });
        
        hideCustomHeadersCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("hideCustomHeaders", new_val);
            resultsDAO.saveRaceReport(r);
        });
        
        
    }
    
    public void removeRaceReport(ActionEvent fxevent){
        System.out.println("FXMLREsultOutputController baseGridPane is a " +  baseGridPane.getParent().getClass().getName());

        boolean remove = ((VBox) baseGridPane.getParent()).getChildren().remove(baseGridPane);
        if (thisRaceReport != null && thisRaceReport.getRace() != null) {
            thisRaceReport.getRace().removeRaceReport(thisRaceReport);
            resultsDAO.removeRaceReport(thisRaceReport);
        } else {
            System.out.println("Either thisRaceReport is null or thisRaceReport.getRace() is null!");
        }
       
    }
    
    public void addRaceReportOututTarget(ActionEvent fxevent){
        RaceOutputTarget t = new RaceOutputTarget();
        thisRaceReport.addRaceOutputTarget(t);
        showRaceReportOutputTarget(t);
        resultsDAO.saveRaceReportOutputTarget(t);
        
    }
    
    public void processReportNow(ActionEvent fxevent){
        resultsDAO.processReport(thisRaceReport);
    }
    
    private void showRaceReportOutputTarget(RaceOutputTarget t){
        HBox rotHBox = new HBox();
        rotHBox.setSpacing(4);
        ComboBox<ReportDestination> destinationChoiceBox = new ComboBox();
        
        //destinationChoiceBox.getItems().setAll(resultsDAO.listReportDestinations());
        
        destinationChoiceBox.setItems(resultsDAO.listReportDestinations());
        
        // ChoserBox for the OutputDestination
        destinationChoiceBox.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> observableValue, Number number, Number number2) -> {
            ReportDestination op = destinationChoiceBox.getItems().get((Integer) number2);
//            if(! Objects.equals(destinationChoiceBox.getSelectionModel().getSelectedItem(),op)) {
//                t.setOutputDestination(op.getID());
//                resultsDAO.saveRaceReportOutputTarget(t);
//            }
            t.setOutputDestination(op.getID());
            resultsDAO.saveRaceReportOutputTarget(t);
        });
        
        if (t.outputDestination() == null) destinationChoiceBox.getSelectionModel().selectFirst();
        else destinationChoiceBox.getSelectionModel().select(t.outputDestination());
        destinationChoiceBox.setPrefWidth(150);
        destinationChoiceBox.setMaxWidth(150);
        
        // TextField for the filename
        TextField filename = new TextField();
        filename.setText(t.getOutputFilename());
        filename.setPrefWidth(200);
        filename.setMaxWidth(400);
        
        filename.textProperty().addListener((observable, oldValue, newValue) -> {
            t.setOutputFilename(newValue);
            resultsDAO.saveRaceReportOutputTarget(t);    
        });
        
        // Remove 
        
        Button remove = new Button("Remove");
        remove.setOnAction((ActionEvent e) -> {
            removeRaceReportOutputTarget(t);
        });
        
        rotHBox.getChildren().addAll(destinationChoiceBox,filename,remove);
        // Add the rotVBox to the outputTargetsVBox
        rotHBoxMap.put(t, rotHBox);
        outputTargetsVBox.getChildren().add(rotHBox);
       
    }
    
    public void removeRaceReportOutputTarget(RaceOutputTarget t){
        resultsDAO.removeRaceReportOutputTarget(t);
        outputTargetsVBox.getChildren().remove(rotHBoxMap.get(t));
    }
    
}
