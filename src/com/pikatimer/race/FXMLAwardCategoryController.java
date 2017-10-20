/*
 * Copyright (C) 2017 John Garner <segfaultcoredump@gmail.com>
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
package com.pikatimer.race;

import com.pikatimer.util.Formatters;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;

/**
 * FXML Controller class
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class FXMLAwardCategoryController {

    @FXML TextField awardTitleTextField;
    
    @FXML ComboBox<AwardCategoryType> awardTypeComboBox;
    
    @FXML ToggleSwitch chipToggleSwitch;
    @FXML ToggleSwitch pullToggleSwitch;
    
    @FXML ComboBox<AwardDepthType> depthTypeComboBox;
    @FXML TextField depthTextField;
    @FXML VBox depthVBox;
    
    AwardCategory awardCategory;
    
    @FXML GridPane customGridPane;
    
    private final RaceDAO raceDAO = RaceDAO.getInstance();
    
    public void initialize() {
        // TODO
        System.out.println("FXMLAwardCategoryController initialized");
        awardTypeComboBox.setItems(FXCollections.observableArrayList(AwardCategoryType.values()));
        depthTypeComboBox.setItems(FXCollections.observableArrayList(AwardDepthType.values()));
    }    
    
    public void setAwardCategory(AwardCategory a){
        awardCategory = a;
        
        // Populate the existing values
        awardTitleTextField.textProperty().set(awardCategory.getName());
        chipToggleSwitch.setSelected(awardCategory.getChip());
        pullToggleSwitch.setSelected(awardCategory.getPull());
        
        depthTypeComboBox.getSelectionModel().select(awardCategory.getDepthType());
        
        depthTextField.setTextFormatter(Formatters.integerFormatter());
        depthTextField.setText(awardCategory.getDepth().toString());
        
        // Hide the custom box if we are not custom
        awardTypeComboBox.getSelectionModel().select(awardCategory.getType());
        if (awardCategory.getType().equals(AwardCategoryType.CUSTOM)){
            customGridPane.setVisible(true);
            customGridPane.setManaged(true);
        } else {
            customGridPane.setVisible(false);
            customGridPane.setManaged(false);
        }
        
        // Hide the depth VBox if we are using a fixed award depth
        if (awardCategory.getDepthType().equals(AwardDepthType.FIXED)){
            depthVBox.setVisible(false);
            depthVBox.setManaged(false);
            depthTextField.setVisible(true);
            depthTextField.setManaged(true);        
        } else {
            depthVBox.setVisible(true);
            depthVBox.setManaged(true);
            depthTextField.setVisible(false);
            depthTextField.setManaged(false);
        }
        
        
        // Now set the controls up so that they update when things change
        awardTitleTextField.textProperty().addListener((obs, prevVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()){
                awardCategory.setName(newVal);
                raceDAO.updateAwardCategory(awardCategory);
            }
        });
        
        chipToggleSwitch.selectedProperty().addListener((obs,  prevVal,  newVal) -> {
             awardCategory.setChip(newVal);
             raceDAO.updateAwardCategory(awardCategory);
        });
        pullToggleSwitch.selectedProperty().addListener((obs,  prevVal,  newVal) -> {
             awardCategory.setPull(newVal);
             raceDAO.updateAwardCategory(awardCategory);
        });
        
        awardTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs,  prevVal,  newVal) -> {
            if (AwardCategoryType.CUSTOM.equals(newVal)) {
                customGridPane.setVisible(true);
                customGridPane.setManaged(true);
            } else {
                customGridPane.setVisible(false);
                customGridPane.setManaged(false);
            }
            awardCategory.setType(newVal);
            raceDAO.updateAwardCategory(awardCategory);
        });
        
        depthTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs,  prevVal,  newVal) -> {
            awardCategory.setDepthType(newVal);
            if (AwardDepthType.FIXED.equals(awardCategory.getDepthType())){
                depthVBox.setVisible(false);
                depthVBox.setManaged(false);
                depthTextField.setVisible(true);
                depthTextField.setManaged(true);        
            } else {
                depthVBox.setVisible(true);
                depthVBox.setManaged(true);
                depthTextField.setVisible(false);
                depthTextField.setManaged(false);
            }
            
            raceDAO.updateAwardCategory(awardCategory);
        });
        
        //
        
        depthTextField.textProperty().addListener((obs, prevVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty() ){
                try {
                    awardCategory.setDepth(Integer.parseUnsignedInt(newVal));
                    raceDAO.updateAwardCategory(awardCategory);
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        depthTextField.textProperty().set(prevVal);
                    });
                }
            }
        });
        
        
    }
    
    
    public void upPriority(ActionEvent fxevent){
        if (awardCategory.getPriority()> 0) {
            awardCategory.getRaceAward().awardCategoriesProperty().remove(awardCategory);
            awardCategory.getRaceAward().awardCategoriesProperty().add(awardCategory.getPriority()-1, awardCategory);
            awardCategory.getRaceAward().recalcPriorities();
            awardCategory.getRaceAward().awardCategoriesProperty().forEach(a -> raceDAO.updateAwardCategory(a));
        }
    }
    public void lowerPriority(ActionEvent fxevent){
        awardCategory.getRaceAward().awardCategoriesProperty().remove(awardCategory);
        awardCategory.getRaceAward().awardCategoriesProperty().add(awardCategory.getPriority()+1, awardCategory);
        awardCategory.getRaceAward().recalcPriorities();
        awardCategory.getRaceAward().awardCategoriesProperty().forEach(a -> raceDAO.updateAwardCategory(a));

    }
    
}
