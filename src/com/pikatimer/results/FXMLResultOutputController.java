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
            
    @FXML CheckBox inProgressCheckBox;
    @FXML CheckBox showDQCheckBox;
    @FXML CheckBox showDNFCheckBox;
    @FXML CheckBox showPaceCheckBox;
    @FXML CheckBox showSplitsCheckBox;
    @FXML CheckBox showGunTimeCheckBox;
    
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
    }    
    
    public void setRaceReport(RaceReport r){
        //This should only be called once
        if (thisRaceReport != null) {
            System.out.println("setRaceReport Called more than once!");
            return;
        }
        thisRaceReport=r;
        outputTypeChoiceBox.getSelectionModel().select(thisRaceReport.getReportType());
        outputTypeChoiceBox.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> observableValue, Number number, Number number2) -> {
            ReportTypes rt = outputTypeChoiceBox.getItems().get((Integer) number2);
            thisRaceReport.setReportType(rt);
            resultsDAO.saveRaceReport(r);
            
            // show any report specific options... 
            // TODO
        });
        
        thisRaceReport.outputTargets().forEach(rot -> {
            showRaceReportOutputTarget(rot);
          });
        
        // Pull the key-value map from the race report and populate everything
        
        //@FXML ToggleSwitch reportEnabledToggleSwitch;
        if (r.getBooleanAttribute("enabled") == null) r.setBooleanAttribute("enabled", true);
        reportEnabledToggleSwitch.selectedProperty().setValue(r.getBooleanAttribute("enabled"));
        reportEnabledToggleSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("enabled", new_val);
            resultsDAO.saveRaceReport(r);
        });         
        
//        @FXML CheckBox inProgressCheckBox;
        if (r.getBooleanAttribute("inProgress") == null) r.setBooleanAttribute("inProgress", false);
        inProgressCheckBox.selectedProperty().setValue(r.getBooleanAttribute("inProgress"));
        inProgressCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("inProgress", new_val);
            resultsDAO.saveRaceReport(r);
        });  
        
//        @FXML CheckBox showDQCheckBox;
        if (r.getBooleanAttribute("showDQ") == null) r.setBooleanAttribute("showDQ", false);
        showDQCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showDQ"));
        showDQCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showDQ", new_val);
            resultsDAO.saveRaceReport(r);
        });
        
//        @FXML CheckBox showDNFCheckBox;
        if (r.getBooleanAttribute("showDNF") == null) r.setBooleanAttribute("showDNF", false);
        showDNFCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showDNF"));
        showDNFCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showDNF", new_val);
            resultsDAO.saveRaceReport(r);
        });
        
//        @FXML CheckBox showPaceCheckBox;
        if (r.getBooleanAttribute("showPace") == null) r.setBooleanAttribute("showPace", true);
        showPaceCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showPace"));
        showPaceCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showPace", new_val);
            resultsDAO.saveRaceReport(r);
        });
        
//        @FXML CheckBox showSplitsCheckBox;
        if (r.getBooleanAttribute("showSplits") == null) r.setBooleanAttribute("showSplits", true);
        showSplitsCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showSplits"));
        showSplitsCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showSplits", new_val);
            resultsDAO.saveRaceReport(r);
        });
        showSplitsCheckBox.visibleProperty().bind(Bindings.size(r.getRace().splitsProperty()).greaterThan(2));
        showSplitsCheckBox.managedProperty().bind(Bindings.size(r.getRace().splitsProperty()).greaterThan(2));
        
//        @FXML CheckBox showGunTimeCheckBox;
        if (r.getBooleanAttribute("showGun") == null) r.setBooleanAttribute("showGun", false);
        showGunTimeCheckBox.selectedProperty().setValue(r.getBooleanAttribute("showGun"));
        showGunTimeCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            r.setBooleanAttribute("showGun", new_val);
            resultsDAO.saveRaceReport(r);
        });
        
        // Just in case we added any defaults 
        resultsDAO.saveRaceReport(r);
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
    
    private void showRaceReportOutputTarget(RaceOutputTarget t){
        HBox rotHBox = new HBox();
        ComboBox<OutputPortal> destinationChoiceBox = new ComboBox();
        
        //destinationChoiceBox.getItems().setAll(resultsDAO.listOutputPortals());
        
        destinationChoiceBox.setItems(resultsDAO.listOutputPortals());
        
        // ChoserBox for the OutputDestination
        destinationChoiceBox.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> observableValue, Number number, Number number2) -> {
            OutputPortal op = destinationChoiceBox.getItems().get((Integer) number2);
//            if(! Objects.equals(destinationChoiceBox.getSelectionModel().getSelectedItem(),op)) {
//                t.setOutputDestination(op.getID());
//                resultsDAO.saveRaceReportOutputTarget(t);
//            }
            t.setOutputDestination(op.getID());
            resultsDAO.saveRaceReportOutputTarget(t);
        });
        
        destinationChoiceBox.getSelectionModel().select(t.outputDestination());
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
