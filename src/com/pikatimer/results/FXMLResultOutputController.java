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
package com.pikatimer.results;

import com.pikatimer.participant.CustomAttribute;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.AwardCategory;
import com.pikatimer.race.RaceDAO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
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
import org.controlsfx.control.CheckComboBox;
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
    @FXML CheckBox showSegmentSplitsCheckBox;
    @FXML CheckBox showAwardsCheckBox;
    @FXML CheckBox showCRsCheckBox;
    @FXML CheckBox hideCustomHeadersCheckBox;
    @FXML CheckBox showFinishTODCheckBox;
    @FXML CheckBox showSplitTODCheckBox;
    @FXML CheckBox showEmailCheckBox;
    
    @FXML HBox customAttributesHBox;
    @FXML CheckBox customAttributesCheckBox;
    @FXML CheckComboBox<CustomAttribute> customAttributesCheckComboBox;
    
    
    @FXML HBox specificAwardsHBox;
    @FXML CheckBox showSpecificAwardsCheckBox;
    @FXML CheckComboBox<AwardCategory> customAwardsCheckComboBox;
    
    @FXML Label noOutputPpathsLabel;
    
    @FXML VBox outputTargetsVBox;
    @FXML FlowPane outputOptionsFlowPane;
            
    @FXML Button outputAddButton;
    
    RaceReport thisRaceReport; 
    final RaceDAO raceDAO = RaceDAO.getInstance();
    final ResultsDAO resultsDAO = ResultsDAO.getInstance();
    final ParticipantDAO participantDAO = ParticipantDAO.getInstance();
    
    final Map<RaceOutputTarget,HBox> rotHBoxMap = new ConcurrentHashMap();
    
    private List<CustomAttribute> selectedCustomAttributesList;
    private final BooleanProperty customAttributeUpdateInProgress = new SimpleBooleanProperty(false);
    
   
    private List<AwardCategory> selectedIndividualAwardList;
    private final BooleanProperty individualAwardUpdateInProgress = new SimpleBooleanProperty(false);
    
    /**
     * Initializes the controller class.
     */
    public void initialize() {
        
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
            
            // @FXML CheckBox showGunTimeCheckBox;
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
            
            // @FXML CheckBox showFinishTODCheckBox; showFinishTOD
            if (rrt.optionSupport("showFinishTOD")) {
                if (r.getBooleanAttribute("showFinishTOD") == null) {
                    r.setBooleanAttribute("showFinishTOD", false);
                    attributeAdded = true;
                }
                showFinishTODCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showFinishTOD"));
                showFinishTODCheckBox.visibleProperty().set(true);
                showFinishTODCheckBox.managedProperty().set(true);
                optionsLabel.visibleProperty().set(true);
            } else {
                showFinishTODCheckBox.visibleProperty().unbind();
                showFinishTODCheckBox.visibleProperty().set(false);
                showFinishTODCheckBox.managedProperty().unbind();
                showFinishTODCheckBox.managedProperty().set(false);
            }
 
            
            // @FXML CheckBox showSplitsCheckBox;
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
            
            // @FXML CheckBox showSplitTODCheckBox; showSplitTOD
            if (rrt.optionSupport("showSplitTOD")) {
                if (r.getBooleanAttribute("showSplitTOD") == null) {
                    r.setBooleanAttribute("showSplitTOD", false);
                    attributeAdded = true;
                }
                showSplitTODCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showSplitTOD"));
                showSplitTODCheckBox.visibleProperty().bind(Bindings.size(r.getRace().splitsProperty()).greaterThan(2));
                showSplitTODCheckBox.managedProperty().bind(Bindings.size(r.getRace().splitsProperty()).greaterThan(2));
                optionsLabel.visibleProperty().set(true);
            } else {
                showSplitTODCheckBox.visibleProperty().unbind();
                showSplitTODCheckBox.visibleProperty().set(false);
                showSplitTODCheckBox.managedProperty().unbind();
                showSplitTODCheckBox.managedProperty().set(false);
            }
            
            // @FXML CheckBox showEmailCheckBox; showEmailCheckBox
            if (rrt.optionSupport("showEmail")) {
                if (r.getBooleanAttribute("showEmail") == null) {
                    r.setBooleanAttribute("showEmail", false);
                    attributeAdded = true;
                }
                showEmailCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showEmail"));
                optionsLabel.visibleProperty().set(true);
            } else {
                showEmailCheckBox.visibleProperty().unbind();
                showEmailCheckBox.visibleProperty().set(false);
                showEmailCheckBox.managedProperty().unbind();
                showEmailCheckBox.managedProperty().set(false);
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
                showSegmentPaceCheckBox.visibleProperty().bind(showSegmentsCheckBox.selectedProperty());
                showSegmentPaceCheckBox.managedProperty().bind(showSegmentsCheckBox.selectedProperty());
                optionsLabel.visibleProperty().set(true);
            } else {
                showSegmentPaceCheckBox.visibleProperty().unbind();
                showSegmentPaceCheckBox.visibleProperty().set(false);
                showSegmentPaceCheckBox.managedProperty().unbind();
                showSegmentPaceCheckBox.managedProperty().set(false);
            }
            if (rrt.optionSupport("showSegmentSplits")) {
                if (r.getBooleanAttribute("showSegmentSplits") == null) {
                    r.setBooleanAttribute("showSegmentSplits", false);
                    attributeAdded = true;
                }
                showSegmentSplitsCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showSegmentSplits"));
                showSegmentSplitsCheckBox.visibleProperty().bind(showSegmentsCheckBox.selectedProperty());
                showSegmentSplitsCheckBox.managedProperty().bind(showSegmentsCheckBox.selectedProperty());
                optionsLabel.visibleProperty().set(true);
            } else {
                showSegmentSplitsCheckBox.visibleProperty().unbind();
                showSegmentSplitsCheckBox.visibleProperty().set(false);
                showSegmentSplitsCheckBox.managedProperty().unbind();
                showSegmentSplitsCheckBox.managedProperty().set(false);
            }
            
            if (rrt.optionSupport("showAwards")) {
                if (r.getBooleanAttribute("showAwards") == null){
                    r.setBooleanAttribute("showAwards", true);
                    attributeAdded = true;
                }
                showAwardsCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showAwards"));
                showAwardsCheckBox.visibleProperty().set(true);
                showAwardsCheckBox.managedProperty().set(true);
                optionsLabel.visibleProperty().set(true);
            } else {
                showAwardsCheckBox.visibleProperty().set(false);
                showAwardsCheckBox.managedProperty().set(false);
            }
            
            if (rrt.optionSupport("showCourseRecords")) {
                if (r.getBooleanAttribute("showCourseRecords") == null){
                    r.setBooleanAttribute("showCourseRecords", true);
                    attributeAdded = true;
                }
                showCRsCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showCourseRecords"));
                showCRsCheckBox.visibleProperty().set(true);
                showCRsCheckBox.managedProperty().set(true);
                optionsLabel.visibleProperty().set(true);
            } else {
                showCRsCheckBox.visibleProperty().set(false);
                showCRsCheckBox.managedProperty().set(false);
            }
            
            //@FXML HBox customAttributesHBox
            //@FXML CheckBox customAttributesCheckBox;
            //@FXML CheckComboBox customAttributesCheckComboBox;
            if (rrt.optionSupport("showCustomAttributes")){
                if (r.getBooleanAttribute("showCustomAttributes") == null){
                    r.setBooleanAttribute("showCustomAttributes", false);
                    attributeAdded = true;
                }
                customAttributesCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showCustomAttributes"));
                customAttributeTracker();
                customAttributesHBox.visibleProperty().set(true);
                customAttributesHBox.managedProperty().set(true);
            } else {
                customAttributesHBox.visibleProperty().set(false);
                customAttributesHBox.managedProperty().set(false);
            }

            //@FXML CheckBox showSpecificAwardsCheckBox;
            //@FXML CheckComboBox customAwardsCheckComboBox;
            if (rrt.optionSupport("showIndividualAwards")){
                if (r.getBooleanAttribute("showIndividualAwards") == null){
                    r.setBooleanAttribute("showIndividualAwards", false);
                    attributeAdded = true;
                }
                showSpecificAwardsCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showIndividualAwards"));
                individualAwardTracker();
                showSpecificAwardsCheckBox.visibleProperty().set(true);
                showSpecificAwardsCheckBox.managedProperty().set(true);
                //awardSelectionTracker();
            } else {
                showSpecificAwardsCheckBox.visibleProperty().set(false);
                showSpecificAwardsCheckBox.managedProperty().set(false);
                showSpecificAwardsCheckBox.selectedProperty().setValue(false);
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
        //        @FXML CheckBox showSegmentPaceCheckBox;
        showSegmentPaceCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (!r.getBooleanAttribute("showSegmentPace").equals(new_val)){
                r.setBooleanAttribute("showSegmentPace", new_val);
                resultsDAO.saveRaceReport(r);
            }
            
        });
//        @FXML CheckBox showSegmentSplitsCheckBox;
        showSegmentSplitsCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (!r.getBooleanAttribute("showSegmentSplits").equals(new_val)){
                r.setBooleanAttribute("showSegmentSplits", new_val);
                resultsDAO.saveRaceReport(r);
            }
            
        });
//        @FXML CheckBox showGunTimeCheckBox;
        showGunTimeCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showGun", new_val);
            resultsDAO.saveRaceReport(r);
        });
        
        // @FXML CheckBox showFinishTODCheckBox;
        showFinishTODCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showFinishTOD", new_val);
            resultsDAO.saveRaceReport(r);
        });
        // @FXML CheckBox showSplitTODCheckBox;
        showSplitTODCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showSplitTOD", new_val);
            resultsDAO.saveRaceReport(r);
        });
        
        // @FXML CheckBox showEmailCheckBox;
        showEmailCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showEmail", new_val);
            resultsDAO.saveRaceReport(r);
        });
        
        hideCustomHeadersCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("hideCustomHeaders", new_val);
            resultsDAO.saveRaceReport(r);
        });
        
        showAwardsCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showAwards", new_val);
            resultsDAO.saveRaceReport(r);
        });
        
        showCRsCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showCourseRecords", new_val);
            resultsDAO.saveRaceReport(r);
        });
        
        //@FXML CheckBox showSpecificAwards;
            //@FXML CheckComboBox customAwardsCheckComboBox;
        customAwardsCheckComboBox.visibleProperty().bind(showSpecificAwardsCheckBox.selectedProperty());
        customAwardsCheckComboBox.managedProperty().bind(showSpecificAwardsCheckBox.selectedProperty());
        showSpecificAwardsCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showIndividualAwards", new_val);
            resultsDAO.saveRaceReport(r);
        });
        //@FXML CheckBox customAttributesCheckBox;
        //@FXML CheckComboBox customAttributesCheckComboBox;
        customAttributesCheckComboBox.visibleProperty().bind(customAttributesCheckBox.selectedProperty());
        customAttributesCheckComboBox.managedProperty().bind(customAttributesCheckBox.selectedProperty());    

        customAttributesCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showCustomAttributes", new_val);
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
            if (t.getID() < 0) return; // deleted
            
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
        
        outputTargetsVBox.getChildren().remove(rotHBoxMap.get(t));
        rotHBoxMap.remove(t);
        
        thisRaceReport.removeRaceOutputTarget(t);
        resultsDAO.removeRaceReportOutputTarget(t);
        t.setID(-1);
    }

    private void individualAwardTracker(){
        System.out.println("ResultOutputController::individualAwardTracker() called...");
        if (selectedIndividualAwardList == null) {
            selectedIndividualAwardList = new ArrayList();
                       
            thisRaceReport.getRace().getAwards().awardCategoriesProperty().addListener((ListChangeListener) listener -> {individualAwardTracker();});
            
            
            customAwardsCheckComboBox.getCheckModel().getCheckedItems().addListener((ListChangeListener.Change<? extends AwardCategory> change) -> {
                
                System.out.println("ResultOutputController::customAttributesCheckComboBox(changeListener) fired...");
                if (individualAwardUpdateInProgress.getValue()) return;
                System.out.println("Changes to process...");
                while (change.next() ) {
                    change.getRemoved().forEach(removed -> {
                        
                        thisRaceReport.setBooleanAttribute(removed.getUUID(), false);
                    });
                    change.getAddedSubList().forEach(added -> {
                        thisRaceReport.setBooleanAttribute(added.getUUID(), true);
                    });
                }
                resultsDAO.saveRaceReport(thisRaceReport);
                
                //System.out.println(waveComboBox.getCheckModel().getCheckedItems());
            });
        }
        
        individualAwardUpdateInProgress.setValue(true);
        // Update the available custom attributes
        customAwardsCheckComboBox.getItems().setAll(thisRaceReport.getRace().getAwards().awardCategoriesProperty());
        
        // Update the checks because the CheckComboBox does not preserve them :-/ 
        selectedIndividualAwardList = thisRaceReport.getRace().getAwards().awardCategoriesProperty().stream().filter(a -> { 
            if (thisRaceReport.getBooleanAttribute(a.getUUID()) != null )
                return thisRaceReport.getBooleanAttribute(a.getUUID());
            return false;
        }).collect(Collectors.toList());
        customAwardsCheckComboBox.getCheckModel().clearChecks();
        selectedIndividualAwardList.forEach (t -> {
            System.out.println("Checking the checkbox for " + t.toString());
            customAwardsCheckComboBox.getCheckModel().check(t);
        });
        individualAwardUpdateInProgress.setValue(false);
    }
    
        
    private void customAttributeTracker() {
        System.out.println("ResultOutputController::customAttributesTracker() called...");
        if (selectedCustomAttributesList == null) {
            selectedCustomAttributesList = new ArrayList();
                       
            customAttributesCheckBox.visibleProperty().bind(Bindings.isNotEmpty(participantDAO.getCustomAttributes()));
            customAttributesCheckBox.managedProperty().bind(Bindings.isNotEmpty(participantDAO.getCustomAttributes()));
            
            
            participantDAO.getCustomAttributes().addListener((ListChangeListener) listener -> {customAttributeTracker();});
            
            
            customAttributesCheckComboBox.getCheckModel().getCheckedItems().addListener((ListChangeListener.Change<? extends CustomAttribute> change) -> {
                
                System.out.println("ResultOutputController::customAttributesCheckComboBox(changeListener) fired...");
                if (customAttributeUpdateInProgress.getValue()) return;
                System.out.println("Changes to process...");
                while (change.next() ) {
                    change.getRemoved().forEach(removed -> {
                        
                        thisRaceReport.setBooleanAttribute(removed.getUUID(), false);
                    });
                    change.getAddedSubList().forEach(added -> {
                        thisRaceReport.setBooleanAttribute(added.getUUID(), true);
                    });
                }
                resultsDAO.saveRaceReport(thisRaceReport);
                
                //System.out.println(waveComboBox.getCheckModel().getCheckedItems());
            });
        }
        
        customAttributeUpdateInProgress.setValue(true);
        // Update the available custom attributes
        customAttributesCheckComboBox.getItems().setAll(participantDAO.getCustomAttributes());
        
        // Update the checks because the CheckComboBox does not preserve them :-/ 
        selectedCustomAttributesList = participantDAO.getCustomAttributes().stream().filter(a -> { 
            if (thisRaceReport.getBooleanAttribute(a.getUUID()) != null )
                return thisRaceReport.getBooleanAttribute(a.getUUID());
            return false;
        }).collect(Collectors.toList());
        customAttributesCheckComboBox.getCheckModel().clearChecks();
        selectedCustomAttributesList.forEach (t -> {
            System.out.println("Checking the checkbox for " + t.toString());
            customAttributesCheckComboBox.getCheckModel().check(t);
        });
        customAttributeUpdateInProgress.setValue(false);
    }
    
}
