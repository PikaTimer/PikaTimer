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

import static com.pikatimer.participant.CustomAttributeType.TIME;
import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.util.Formatters;
import com.pikatimer.util.IntegerEditingCell;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.Pair;
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
    
    @FXML ToggleSwitch visibleAwardsToggleSwitch;
    @FXML ToggleSwitch visibleOverallToggleSwitch;
    
    
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
    
    // Custom Award Stuff
    @FXML GridPane customGridPane;
    
    // Timing location 
    @FXML ComboBox<AwardTimingPoint> timingPointComboBox;
    
    // Filters
    @FXML ToggleSwitch filterToggleSwitch;
    @FXML VBox filterVBox;
    @FXML TableView<AwardFilter> filterTableView;
    ObservableList<Pair<String,String>> customAttributesList = FXCollections.observableArrayList();
    ObservableList<String> customAttributesDisplayList = FXCollections.observableArrayList();
    @FXML TableColumn<AwardFilter,String> filterAttributeTableColumn;
    @FXML TableColumn<AwardFilter,String> filterTypeTableColumn;
    @FXML TableColumn<AwardFilter,String> filterReferenceValueTableColumn;
    @FXML Button filterAddButton;
    @FXML Button filterDeleteButton;
    
    // Groupings
    @FXML ToggleSwitch subdivideToggleSwitch;
    @FXML VBox subdivideVBox;
    @FXML ListView<CustomAttribute> subdivideListView;
    ObservableList<CustomAttribute> availableCustomAttributesList = FXCollections.observableArrayList(CustomAttribute.extractor());
    ObservableList<CustomAttribute> subdivideCustomAttributesList = FXCollections.observableArrayList(CustomAttribute.extractor());
    @FXML Button subdivideAddButton;
    @FXML Button subdivideDeleteButton;
    
    @FXML ToggleSwitch skewToggleSwitch;
    @FXML HBox skewControlHBox;
    @FXML ComboBox<String> skewOpComboBox;
    @FXML ComboBox<String> skewAttributeComboBox;
    ObservableList<String> skewableAttributes = FXCollections.observableArrayList();
    Map<String,Integer> skewableAttributesMap = new HashMap();
    
    
    ObservableList<AwardTimingPoint> availableTimingPointsList = FXCollections.observableArrayList(AwardTimingPoint.extractor());
    
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
        
        visibleAwardsToggleSwitch.setSelected(awardCategory.getVisible());
        visibleOverallToggleSwitch.setSelected(awardCategory.getVisibleOverall());
        
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
        
        visibleAwardsToggleSwitch.selectedProperty().addListener((obs,  prevVal,  newVal) -> {
             awardCategory.setVisible(newVal);
             raceDAO.updateAwardCategory(awardCategory);
        });
        visibleOverallToggleSwitch.selectedProperty().addListener((obs,  prevVal,  newVal) -> {
             awardCategory.setVisibleOverall(newVal);
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
        
        filterVBox.visibleProperty().bind(filterToggleSwitch.selectedProperty());
        filterVBox.managedProperty().bind(filterToggleSwitch.selectedProperty());
        
        filterToggleSwitch.setSelected(awardCategory.getFiltered());
        filterToggleSwitch.selectedProperty().addListener((obs,  prevVal,  newVal) -> {
             awardCategory.setFiltered(newVal);
             raceDAO.updateAwardCategory(awardCategory);
        });
        
        filterDeleteButton.setOnAction(action -> {
            filterDelete();
        });
        filterDeleteButton.disableProperty().bind(filterTableView.getSelectionModel().selectedItemProperty().isNull());
        filterAddButton.setOnAction(action -> {
            filterAdd();
        });
        
        rebuildAttributeLists();
        ParticipantDAO.getInstance().getCustomAttributes().addListener((ListChangeListener) listener -> {
            System.out.println("Custom Attributes changed...");
            rebuildAttributeLists();
        });
        
        filterAttributeTableColumn.setCellValueFactory(value -> {
            String key = value.getValue().attributeProperty().getValue();
            for(Pair<String,String> k: customAttributesList) {
                if (k.getKey().equals(key)) return new SimpleStringProperty(k.getValue());
            };
            return new SimpleStringProperty(key);
        });
        filterAttributeTableColumn.setCellFactory(ComboBoxTableCell.forTableColumn(customAttributesDisplayList));
        filterAttributeTableColumn.setOnEditCommit(e -> {
            String value = e.getNewValue();
            String key = e.getOldValue();
            for(Pair<String,String> k: customAttributesList) {
                if (k.getValue().equals(value)) key = k.getKey() ;
            };
            e.getRowValue().attributeProperty().setValue(key);
            raceDAO.updateAwardCategory(awardCategory);
        });
        
        filterTypeTableColumn.setCellValueFactory(value -> value.getValue().comparisonTypeProperty());
        filterTypeTableColumn.setCellFactory(ComboBoxTableCell.forTableColumn("=",">","<",">=","<=","!=","=~"));
        filterTypeTableColumn.setOnEditCommit(e -> {
            e.getRowValue().comparisonTypeProperty().setValue(e.getNewValue());
            raceDAO.updateAwardCategory(awardCategory);
        });
        filterReferenceValueTableColumn.setCellValueFactory(value -> value.getValue().referenceValueProperty());
        filterReferenceValueTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        filterReferenceValueTableColumn.setOnEditCommit(e -> {
            e.getRowValue().referenceValueProperty().setValue(e.getNewValue());
            raceDAO.updateAwardCategory(awardCategory);
        });
        
        filterTableView.setItems(awardCategory.filtersProperty());
        filterTableView.setPlaceholder(new Label("No filters defined yet..."));
        
        subdivideToggleSwitch.setSelected(awardCategory.getSubdivided());
        subdivideToggleSwitch.selectedProperty().addListener((obs,  prevVal,  newVal) -> {
             awardCategory.setSubdivided(newVal);
             raceDAO.updateAwardCategory(awardCategory);
        });
        
        // build the subdivideCustomAttributesList
        awardCategory.subDivideProperty().forEach(s -> {});
        
        subdivideListView.setItems(subdivideCustomAttributesList);
        subdivideListView.setCellFactory(ComboBoxListCell.forListView(availableCustomAttributesList));
        
        
        awardCategory.subDivideProperty().forEach(s -> {
            availableCustomAttributesList.stream().filter((k) -> (k.key.getValue().equals(s))).forEachOrdered((k) -> {
                subdivideCustomAttributesList.add(k);
            });
        });
        subdivideVBox.visibleProperty().bind(subdivideToggleSwitch.selectedProperty());
        subdivideVBox.managedProperty().bind(subdivideToggleSwitch.selectedProperty());
        subdivideDeleteButton.setOnAction(action -> {
            subDelete();
        });
        subdivideDeleteButton.disableProperty().bind(subdivideListView.getSelectionModel().selectedItemProperty().isNull());
        subdivideAddButton.setOnAction(action -> {
            subAdd();
        });
        
        subdivideListView.setOnEditCommit((ListView.EditEvent<CustomAttribute> t) -> {
            System.out.println("setOnEditCommit " + t.getIndex());
            
            if(t.getIndex() >= 0 && t.getIndex() < t.getSource().getItems().size()) {
                CustomAttribute ca = t.getSource().getItems().get(t.getIndex()); 
                if (t.getNewValue().key.toString().isEmpty()) {
                    //we never saved this so just remove it
                    subdivideCustomAttributesList.remove(ca);
                } else {
                    // rebuild the backing list
                    
                    subdivideCustomAttributesList.remove(ca);
                    subdivideCustomAttributesList.add(t.getIndex(), t.getNewValue());
                    awardCategory.subDivideProperty().clear();
                    awardCategory.updateSubdivideList();
                    subdivideCustomAttributesList.forEach( cca -> {
                        if (cca.key.isEmpty().get()) return;
                        System.out.println("Current splitBy attribute: " + cca.key.getValueSafe());
                        awardCategory.subDivideProperty().add(cca.key.getValueSafe());
                    });
                    awardCategory.updateSubdivideList();
                    raceDAO.updateAwardCategory(awardCategory);
                }
            } else {
                System.out.println("Timing setOnEditCommit event out of index: " + t.getIndex());
            }
        });

        timingPointComboBox.setItems(availableTimingPointsList);
        
        rebuildTimingPointList();
        
        
        // rebuild the list when we add or remove splits or segments
        awardCategory.getRaceAward().getRace().splitsProperty().addListener((ListChangeListener) listener -> {rebuildTimingPointList();});
        awardCategory.getRaceAward().getRace().unsortedSegmentsProperty().addListener((ListChangeListener) listener -> {
            System.out.println("awardCategory: segments changed...");
            rebuildTimingPointList();
        });
        
        timingPointComboBox.getSelectionModel().selectedItemProperty().addListener((obs,  prevVal,  newVal) -> {
            if (newVal != null && !newVal.id.getValue().equals(awardCategory.getTimingPointID())) {
                awardCategory.setTimingPointID(newVal.id.get());
                awardCategory.setTimingPointType(newVal.type.get());
                raceDAO.updateAwardCategory(awardCategory);
            }
        });
        
        
        skewToggleSwitch.setSelected(awardCategory.getSkewed());
        skewToggleSwitch.selectedProperty().addListener((obs,  prevVal,  newVal) -> {
             awardCategory.setSkewed(newVal);
             raceDAO.updateAwardCategory(awardCategory);
        });
        
        skewOpComboBox.setItems(FXCollections.observableArrayList("+","-"));
        skewOpComboBox.getSelectionModel().select(awardCategory.getSkewType());
        skewOpComboBox.getSelectionModel().selectedItemProperty().addListener((obs,  prevVal,  newVal) -> {
            if (newVal != null && !newVal.equals(prevVal)) {
                awardCategory.setSkewType(newVal);
                raceDAO.updateAwardCategory(awardCategory);
            }
        });
        skewAttributeComboBox.setItems(skewableAttributes);
        skewableAttributesMap.keySet().forEach(key -> {
            if (skewableAttributesMap.get(key).equals(awardCategory.getSkewAttribute())) skewAttributeComboBox.getSelectionModel().select(key);
        });
        skewAttributeComboBox.getSelectionModel().selectedItemProperty().addListener((obs,  prevVal,  newVal) -> {
            if (newVal != null && !newVal.equals(skewableAttributesMap.get(newVal))) {
                awardCategory.setSkewAttribute(skewableAttributesMap.get(newVal));
                raceDAO.updateAwardCategory(awardCategory);
            }
        });
        skewToggleSwitch.visibleProperty().bind(Bindings.size(skewableAttributes).greaterThan(0));
        skewToggleSwitch.managedProperty().bind(Bindings.size(skewableAttributes).greaterThan(0));
        skewControlHBox.visibleProperty().bind(skewToggleSwitch.selectedProperty());
        skewControlHBox.managedProperty().bind(skewToggleSwitch.selectedProperty());
    }
    
    public void subAdd(){
        CustomAttribute ca = new CustomAttribute("","Select...");
        subdivideCustomAttributesList.add(ca);
        //subdivideListView.getSelectionModel().select(subdivideCustomAttributesList.indexOf(ca));
        //subdivideListView.edit(subdivideCustomAttributesList.indexOf(ca));
    }
    
    public void subDelete(){
        CustomAttribute ca = subdivideListView.getSelectionModel().getSelectedItem();
        subdivideCustomAttributesList.remove(ca);
        if (ca.key.isEmpty().get()) return;
        awardCategory.subDivideProperty().remove(ca.key.getValue());
        awardCategory.updateSubdivideList();
        raceDAO.updateAwardCategory(awardCategory);
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
    
    public void filterAdd(){
        awardCategory.addFilter(new AwardFilter("","=",""));
        raceDAO.updateAwardCategory(awardCategory);
    }
    
    public void filterDelete(){
        awardCategory.deleteFilter(filterTableView.getSelectionModel().getSelectedItem());
        raceDAO.updateAwardCategory(awardCategory);
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
    
    private void rebuildTimingPointList(){
        availableTimingPointsList.clear();
        
        // Finish line
        AwardTimingPoint finish = new AwardTimingPoint("Finish","FINISH",0);
        availableTimingPointsList.setAll(finish);
        Race r = awardCategory.getRaceAward().getRace();
        // Splits
        r.getSplits().forEach(s -> {
            System.out.println("AwardCategoryController: rebuildTimingPointList: split " + s.getSplitName() + " -> " + s.getPosition());
            if (s.getPosition() == 1 ) return; // Skip the start
            if (s.getPosition() == r.getSplits().size()) return;  // Skip the finish
            availableTimingPointsList.add(new AwardTimingPoint(s.splitNameProperty(),"SPLIT",s.getID()));
        });
        // Segments
        r.getSegments().forEach(s ->{
            System.out.println("AwardCategoryController: rebuildTimingPointList: Segment " + s.getSegmentName()+ " -> " + s.getID());
            availableTimingPointsList.add(new AwardTimingPoint(s.segmentNameProperty(),"SEGMENT",s.getID()));
        });
        
        AwardTimingPoint tmpTp = new AwardTimingPoint("",awardCategory.getTimingPointType(),awardCategory.getTimingPointID());
        availableTimingPointsList.forEach(tp -> {
            if (tp.equals(tmpTp)) timingPointComboBox.getSelectionModel().select(tp);
        });
    }
    
    private void rebuildAttributeLists(){
        availableCustomAttributesList.clear();
        availableCustomAttributesList.add(new CustomAttribute("AG","Age Group"));
        
        customAttributesList.clear();
        customAttributesDisplayList.clear();
        customAttributesList.add(new Pair("AG","Age Group"));
        customAttributesDisplayList.add("Age Group");
        
        skewableAttributes.clear();
        skewableAttributesMap.clear();
        
        Participant.getAvailableAttributes().keySet().stream().sorted().forEach(k -> {
            customAttributesList.add(new Pair(k,Participant.getAvailableAttributes().get(k)));
            customAttributesDisplayList.add(Participant.getAvailableAttributes().get(k));
            availableCustomAttributesList.add(new CustomAttribute(k,Participant.getAvailableAttributes().get(k)));
        });
        ParticipantDAO.getInstance().getCustomAttributes().forEach(ca -> {
            customAttributesList.add(new Pair(ca.getID().toString(),ca.getName()));
            availableCustomAttributesList.add(new CustomAttribute(ca.getID().toString(),ca.nameProperty()));
            customAttributesDisplayList.add(ca.getName());
            if (ca.getAttributeType().equals(TIME)) {
                skewableAttributes.add(ca.getName());
                skewableAttributesMap.put(ca.getName(), ca.getID());
            };
        });
        
        skewableAttributesMap.keySet().forEach(key -> {
            if (skewableAttributesMap.get(key).equals(awardCategory.getSkewAttribute())) skewAttributeComboBox.getSelectionModel().select(key);
        });
    }

    private static class AwardTimingPoint{
        StringProperty name = new SimpleStringProperty("");
        StringProperty type = new SimpleStringProperty("FINISH");
        IntegerProperty id = new SimpleIntegerProperty(0);
        
        public AwardTimingPoint() {
            
        }
        public AwardTimingPoint(String n, String t, Integer i){
            name.setValue(n);
            type.setValue(t);
            id.setValue(i);
        }
    
        public AwardTimingPoint(StringProperty n, String t, Integer i){
            name = n;
            type.setValue(t);
            id.setValue(i);
        }
        
        @Override
        public String toString(){
            return name.getValueSafe();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 73 * hash + Objects.hashCode(this.type.get());
            hash = 73 * hash + Objects.hashCode(this.id.get());
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final AwardTimingPoint other = (AwardTimingPoint) obj;
            if (!Objects.equals(this.type.get(), other.type.get())) {
                return false;
            }
            if (!Objects.equals(this.id.get(), other.id.get())) {
                return false;
            }
            return true;
        }
        
        public static Callback<AwardTimingPoint, Observable[]> extractor() {
            return (AwardTimingPoint ca) -> new Observable[]{ca.name};
        }
    
    }
    
    private static class CustomAttribute {
        StringProperty name = new SimpleStringProperty("");
        StringProperty key = new SimpleStringProperty("");
        
        public CustomAttribute() {
        }
        public CustomAttribute(String k, String v){
            name.setValue(v);
            key.setValue(k);
        }
        public CustomAttribute(String k, StringProperty v){
            name = v;
            key.setValue(k);
        }
        
        @Override
        public String toString(){
            return name.getValueSafe();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + Objects.hashCode(this.key);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CustomAttribute other = (CustomAttribute) obj;
            if (!Objects.equals(this.key, other.key)) {
                return false;
            }
            return true;
        }
        
        
        public static Callback<CustomAttribute, Observable[]> extractor() {
            return (CustomAttribute ca) -> new Observable[]{ca.name};
        }
        
    }
    
}
