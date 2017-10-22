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
import com.pikatimer.util.IntegerEditingCell;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
    
    @FXML TableView<AwardDepth> depthTableView;
    @FXML TableColumn<AwardDepth,Integer> depthStartTableColumn;
    @FXML TableColumn<AwardDepth,String> depthEndTableColumn;
    @FXML TableColumn<AwardDepth,Integer> depthTableColumn;
    
    
    @FXML Button depthDeleteButton;
    @FXML Button depthAddButton;
    
    @FXML HBox mastersHBox;
    @FXML TextField mastersAgeTextField;
    
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
        
        TextFormatter<String> AGMformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        mastersAgeTextField.setTooltip(new Tooltip("Sets the starting age for the Masters categories."));  
        mastersAgeTextField.setTextFormatter(AGMformatter);
        
        mastersAgeTextField.setText(awardCategory.getMastersAge().toString());
        
//        @FXML TableColumn<AwardDepth,Integer> depthStartTableColumn;
//        @FXML TableColumn<AwardDepth,String> depthEndTableColumn;
//        @FXML TableColumn<AwardDepth,Integer> depthTableColumn;
        depthEndTableColumn.editableProperty().set(false);
        depthEndTableColumn.setCellValueFactory(value -> value.getValue().endCountProperty());
        
        depthStartTableColumn.setCellValueFactory(value -> value.getValue().startCountProperty().asObject());
        depthStartTableColumn.setCellFactory(col -> new IntegerEditingCell());
        depthStartTableColumn.setOnEditCommit(e -> {
            try {
                e.getRowValue().startCountProperty().setValue(e.getNewValue());
                awardCategory.recalcCustomDepths();
            } catch (Exception ex) {
                System.out.println("depthStartTableColumn.setOnEditCommit Oops....");
                e.getRowValue().startCountProperty().setValue(e.getOldValue());
            }
            
            raceDAO.updateAwardCategory(awardCategory);
        });
        
        depthTableColumn.setCellValueFactory(value -> value.getValue().depthProperty().asObject());
        depthTableColumn.setCellFactory(col -> new IntegerEditingCell());
        depthTableColumn.setOnEditCommit(e -> {
            try {
                e.getRowValue().depthProperty().setValue(e.getNewValue());
            } catch (Exception ex) {
                System.out.println("depthStartTableColumn.setOnEditCommit Oops....");
                e.getRowValue().depthProperty().setValue(e.getOldValue());
            }
            raceDAO.updateAwardCategory(awardCategory);
        });
        
        // Hide the custom box if we are not custom
        awardTypeComboBox.getSelectionModel().select(awardCategory.getType());
        switch (awardCategory.getType()) {
            case CUSTOM:
                customGridPane.setVisible(true);
                customGridPane.setManaged(true);
                mastersHBox.setVisible(false);
                mastersHBox.setManaged(false);
                break;
            case MASTERS:
                customGridPane.setVisible(false);
                customGridPane.setManaged(false);
                mastersHBox.setVisible(true);
                mastersHBox.setManaged(true);
                break;
            default:
                customGridPane.setVisible(false);
                customGridPane.setManaged(false);
                mastersHBox.setVisible(false);
                mastersHBox.setManaged(false);
                break;
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
        
        depthTableView.setItems(awardCategory.customDepthProperty());

        
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
            switch (newVal) {
                case CUSTOM:
                    customGridPane.setVisible(true);
                    customGridPane.setManaged(true);
                    mastersHBox.setVisible(false);
                    mastersHBox.setManaged(false);
                    break;
                case MASTERS:
                    customGridPane.setVisible(false);
                    customGridPane.setManaged(false);
                    mastersHBox.setVisible(true);
                    mastersHBox.setManaged(true);
                    break;
                default:
                    customGridPane.setVisible(false);
                    customGridPane.setManaged(false);
                    mastersHBox.setVisible(false);
                    mastersHBox.setManaged(false);
                    break;
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
                 if(awardCategory.customDepthProperty().isEmpty()){
                    AwardDepth ad = new AwardDepth();
                    ad.setDepth(3);
                    ad.setStartCount(1);
                    awardCategory.addCustomDepth(ad);
                }
                depthVBox.setVisible(true);
                depthVBox.setManaged(true);
                depthTextField.setVisible(false);
                depthTextField.setManaged(false);
            }
            
            raceDAO.updateAwardCategory(awardCategory);
        });
        
        depthDeleteButton.setOnAction(action -> {
            depthDelete();
        });
        depthAddButton.setOnAction(action -> {
            depthAdd();
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
        mastersAgeTextField.textProperty().addListener((obs, prevVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty() ){
                try {
                    awardCategory.setMastersAge(Integer.parseUnsignedInt(newVal));
                    raceDAO.updateAwardCategory(awardCategory);
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        mastersAgeTextField.textProperty().set(prevVal);
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
    
    public void removeAward(ActionEvent fxevent){
        awardCategory.getRaceAward().removeAwardCategory(awardCategory);
        raceDAO.removeAwardCategory(awardCategory);
    }
    
    public void depthAdd(){
        AwardDepth ad = new AwardDepth();
        int size = awardCategory.getCustomDepthList().size();
        if (size>1) {
            Integer p1 = awardCategory.getCustomDepthList().get(size-2).getStartCount();
            Integer p2 = awardCategory.getCustomDepthList().get(size-1).getStartCount();
            if (p1 == 1) p1=0;
            ad.setStartCount(p2 + p2-p1);
            ad.setDepth(awardCategory.getCustomDepthList().get(size-1).getDepth());
        } else if (size == 1) {
            Integer p1 = awardCategory.getCustomDepthList().get(size-1).getStartCount();
            if (p1 == 1) p1=0;
            ad.setStartCount(p1 + 10);
            ad.setDepth(awardCategory.getCustomDepthList().get(size-1).getDepth());
        } else {
            ad.setDepth(3);
            ad.setStartCount(1);
        }
        
        awardCategory.addCustomDepth(ad);
        raceDAO.updateAwardCategory(awardCategory);
    }
    public void depthDelete(){
        awardCategory.removeCustomDepth(depthTableView.getSelectionModel().getSelectedItem());
        raceDAO.updateAwardCategory(awardCategory);
    }
}
