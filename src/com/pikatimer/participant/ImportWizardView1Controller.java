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
package com.pikatimer.participant;

import com.pikatimer.PikaPreferences;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import io.datafx.controller.FXMLController;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import java.io.File;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javax.annotation.PostConstruct;


@FXMLController("FXMLImportWizardView1.fxml")
public class ImportWizardView1Controller {
    
    @FXMLViewFlowContext private ViewFlowContext context;
    
    @FXML private Label fileStatusLabel;
    @FXML private TextField fileTextField;
    @FXML private Button fileChooserButton;
    @FXML private CheckBox clearExistingCheckBox; 
    @FXML private CheckBox cleanupCityCheckBox; 
    @FXML private CheckBox cleanupNamesCheckBox; 
    @FXML private VBox bibAssignmentVBox; 
    @FXML private CheckBox waveByBibCheckBox; 
    @FXML private CheckBox waveHardCodeCheckBox;
    @FXML private CheckBox waveByAttributeCheckBox; 
    @FXML private ComboBox<Wave> waveComboBox;
    @FXML private ComboBox<String> duplicateHandlingComboBox;
    @FXML private HBox duplicateHandlingHBox;
    
    
    ImportWizardData model;
    
    @PostConstruct
    public void init() throws FlowException {
        System.out.println("ImportWizardView1Controller.initialize()");
        
        // TODO: 
        cleanupCityCheckBox.disableProperty().set(true);
        cleanupCityCheckBox.visibleProperty().set(false);
        cleanupCityCheckBox.managedProperty().set(false);
        cleanupNamesCheckBox.disableProperty().set(true);
        cleanupNamesCheckBox.visibleProperty().set(false);
        cleanupNamesCheckBox.managedProperty().set(false);

        model = context.getRegisteredObject(ImportWizardData.class);
        //fileNameLabel.textProperty().bind(model.fileNameProperty());
        
        fileChooserButton.setOnAction(this::chooseFile);
        
        model.clearExistingProperty().bind(clearExistingCheckBox.selectedProperty());
        
        if (ParticipantDAO.getInstance().listParticipants().isEmpty()) {
            clearExistingCheckBox.visibleProperty().set(false);
            clearExistingCheckBox.managedProperty().set(false);
            
            duplicateHandlingHBox.visibleProperty().set(false);
            duplicateHandlingHBox.managedProperty().set(false);
            clearExistingCheckBox.selectedProperty().set(true);
            
        } else {
            duplicateHandlingHBox.disableProperty().bind(clearExistingCheckBox.selectedProperty());
            duplicateHandlingComboBox.setItems(FXCollections.observableArrayList("Ignore","Merge","Import"));
            duplicateHandlingComboBox.getSelectionModel().selectFirst();
            model.duplicateHandlingProperty().bind(duplicateHandlingComboBox.getSelectionModel().selectedItemProperty());
        }
        
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
        // TODO:
        // Cleanup Names
        // Cleanup City / State (by zip?)
        
        fileTextField.textProperty().addListener((ob, oldT, newT) -> {
            File file = new File(fileTextField.getText());
            if (file.exists() && file.isFile() && file.canRead()) {
                System.out.println("  The file is good...");
                fileStatusLabel.setText("");

                model.nextButtonDisabledProperty().set(false);
                model.setFileName(file.getAbsolutePath());
            } else {
                System.out.println("  Unable to use this file");
                if (! file.exists()) fileStatusLabel.setText("File does not exist");
                else if (! file.isFile()) fileStatusLabel.setText("The path entered is not a regular file");
                else if (!file.canRead()) fileStatusLabel.setText("Unable to read the file");


                model.setFileName(fileTextField.getText());
                model.nextButtonDisabledProperty().set(true);
            }


        });
        
        
    }
    
    @FXML
    protected void chooseFile(ActionEvent fxevent){
        
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open CSV File");
        
        if (model.getFileName().equals("")) {
            fileChooser.setInitialDirectory(PikaPreferences.getInstance().getCWD());
        } else {
            fileChooser.setInitialFileName(model.getFileName()); 
        }
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV/TXT Files", "*.csv", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
        File file = fileChooser.showOpenDialog(fileChooserButton.getScene().getWindow());
        if (file != null && file.exists() && file.isFile() && file.canRead()) {
           // model.setFileName(file.getAbsolutePath());
            fileTextField.setText(file.getAbsolutePath());
            //model.nextButtonDisabledProperty().set(false);
        }        
    }
}
