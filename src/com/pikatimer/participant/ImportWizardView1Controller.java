/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.participant;

import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import java.io.File;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javax.annotation.PostConstruct;


@FXMLController("FXMLImportWizardView1.fxml")
public class ImportWizardView1Controller {
    
    @FXMLViewFlowContext private ViewFlowContext context;
    
    @FXML private Label fileNameLabel;
    @FXML private Button fileChooserButton;
    @FXML private CheckBox clearExistingCheckBox; 
    @FXML private CheckBox cleanupCityCheckBox; 
    @FXML private CheckBox cleanupNamesCheckBox; 
    @FXML private VBox bibAssignmentVBox; 
    @FXML private CheckBox waveByBibCheckBox; 
    @FXML private CheckBox waveHardCodeCheckBox;
    @FXML private CheckBox waveByAttributeCheckBox; 
    @FXML private ComboBox<Wave> waveComboBox;
    
    
    
    ImportWizardData model;
    
    @PostConstruct
    public void init() throws FlowException {
        System.out.println("ImportWizardView1Controller.initialize()");
        
        model = context.getRegisteredObject(ImportWizardData.class);
        fileNameLabel.textProperty().bind(model.fileNameProperty());
        
        fileChooserButton.setOnAction(this::chooseFile);
        
        
        // Wave assignment options:
        // if only one race, hide it all and just do a straight assignment
        ObservableList<Wave> waves = RaceDAO.getInstance().listWaves(); 
        if (waves.size() == 1) {
            bibAssignmentVBox.setVisible(false);
            bibAssignmentVBox.setManaged(false);
            model.waveAssignByBibProperty().setValue(false);
            model.waveAssignByAttributeProperty().setValue(false); 
            model.setAssignedWave(waves.get(0));
        } else {
            model.waveAssignByAttributeProperty().bind(waveByAttributeCheckBox.selectedProperty());
            waveByAttributeCheckBox.selectedProperty().addListener((ov, old_val, new_val) -> {
                if (new_val) { // checked
                    waveHardCodeCheckBox.setSelected(false); 
                    waveByBibCheckBox.setSelected(false);
                }
            });
            model.waveAssignByBibProperty().bind(waveByBibCheckBox.selectedProperty());
            waveByBibCheckBox.selectedProperty().addListener((ov, old_val, new_val) -> {
                if (new_val) { // checked
                    waveHardCodeCheckBox.setSelected(false); 
                    waveByAttributeCheckBox.setSelected(false);
                }
            });
            waveHardCodeCheckBox.selectedProperty().addListener((ov, old_val, new_val) -> {
                if (new_val) { // checked
                    waveByBibCheckBox.setSelected(false);
                    waveByAttributeCheckBox.setSelected(false);
                }
            });
            waveComboBox.getSelectionModel().selectedItemProperty().addListener((ov, old_val, new_val) -> {
                model.setAssignedWave(new_val);
            });
            waveComboBox.getItems().addAll(waves); 
            waveComboBox.setValue(waves.get(0));
            waveByBibCheckBox.setSelected(true);

            
        }
        // assigning the race/wave by bib
        // assigning by an imported attribute
        // if doing a straight assignment
        // if only one race, hide it all and just do a straight assignment
        
        
        
    }
    
    @FXML
    protected void chooseFile(ActionEvent fxevent){
        
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open CSV File");
        
        if (model.getFileName().equals("")) {
            fileChooser.setInitialDirectory(
                new File(System.getProperty("user.home"))
            );
            
        } else {
            fileChooser.setInitialFileName(model.getFileName()); 
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PikaTimer Events", "*.csv"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
        File file = fileChooser.showOpenDialog(fileChooserButton.getScene().getWindow());
        if (file != null) {
            model.setFileName(file.getAbsolutePath());
        }        
    }
}
