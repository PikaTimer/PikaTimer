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

import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.participant.Status;
import com.pikatimer.results.ResultsDAO;
import com.pikatimer.util.IntegerEditingCell;
import java.io.IOException;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;


public class FXMLAwardDetailsController {
    
    @FXML ComboBox<Race> raceComboBox; 
    @FXML Label selectedRaceLabel;
    
    @FXML ChoiceBox<SexHandling> sexHandlingChoiceBox;
    @FXML TableView<SexCode> sexCodeMapTableView;
    @FXML TableColumn<SexCode,String> sexCodeTableColumn;
    @FXML TableColumn<SexCode,String> sexLabelTableColumn;
    @FXML Button sexCodeAdd;
    @FXML Button sexCodeDelete;
    
    @FXML ChoiceBox<Integer> agIncrementChoiceBox;
    @FXML TextField agStartTextField;
    @FXML ToggleSwitch agCustomToggleSwitch;
    @FXML ToggleSwitch customAGNamesToggleSwitch;
    @FXML GridPane agGridPane;
    @FXML VBox agCustomVBox;
    @FXML TableView<AgeGroupIncrement> customAGTableView;
    @FXML TableColumn<AgeGroupIncrement,Integer> startAGTableColumn;
    @FXML TableColumn<AgeGroupIncrement,String> endAGTableColumn;
    @FXML TableColumn<AgeGroupIncrement,String> nameAGTableColumn;
    @FXML Button agCustomAdd;
    @FXML Button agCustomDelete;
    
    @FXML VBox awardsVBox;
    private final Map<String,VBox> raceAwardUIMap = new HashMap();
    private final Map<String,Node> raceAwardNodeUIMap = new HashMap();
    
//    @FXML ChoiceBox<String> awardOverallPullChoiceBox;
//    @FXML ChoiceBox<String> awardMastersPullChoiceBox;
//    @FXML ChoiceBox<String> awardAGPullChoiceBox;
//    
//    @FXML ChoiceBox<String> awardOverallChipChoiceBox;
//    @FXML ChoiceBox<String> awardMastersChipChoiceBox;
//    @FXML ChoiceBox<String> awardAGChipChoiceBox;
//   
//    @FXML TextField awardOverallMaleDepthTextField;
//    @FXML TextField awardOverallFemaleDepthTextField; 
//    @FXML TextField awardMastersMaleDepthTextField;
//    @FXML TextField awardMastersFemaleDepthTextField;    
//    @FXML TextField awardAGMaleDepthTextField;
//    @FXML TextField awardAGFemaleDepthTextField;
    
    @FXML CheckBox permitTiesCheckBox;

    final RaceDAO raceDAO = RaceDAO.getInstance();
    final ResultsDAO resultsDAO = ResultsDAO.getInstance();
    final ParticipantDAO participantDAO = ParticipantDAO.getInstance();
    
    private final BooleanProperty populateAwardSettingsInProgress = new SimpleBooleanProperty(FALSE);

    private Race activeRace;
    /**
     * Initializes the controller class.
     */
    public void initialize() {
        // TODO
        raceComboBox.setItems(raceDAO.listRaces());
        raceComboBox.visibleProperty().bind(Bindings.size(raceDAO.listRaces()).greaterThan(1));
        raceComboBox.managedProperty().bind(Bindings.size(raceDAO.listRaces()).greaterThan(1));

        selectedRaceLabel.visibleProperty().bind(Bindings.size(raceDAO.listRaces()).greaterThan(1));
        selectedRaceLabel.managedProperty().bind(Bindings.size(raceDAO.listRaces()).greaterThan(1));


        
        initializeAgeGroupSettings();
        initializeRaceSettings();
        initializeSexHandlingSettings();
        
       raceComboBox.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> observableValue, Number number, Number number2) -> {
            if (number2.intValue() == -1 )  {
                raceComboBox.getSelectionModel().clearAndSelect(0);
                return;
            } 

            activeRace = raceComboBox.getItems().get(number2.intValue());
            
            // Populate the AG settings
            populateAgeGroupSettings(activeRace);
            // Populate the Awards Settings
            populateAwardsSettings(activeRace);
            // Populate the race-wide settings
            populateRaceSettings(activeRace);
            
            populateSexHandlingSettings(activeRace);
        });
        
        Platform.runLater(() -> {raceComboBox.getSelectionModel().clearAndSelect(0); });
    }    
    
    private void populateAgeGroupSettings(Race r) {
        System.out.println("populateRaceAGSettings() called...");
        
        AgeGroups ag;
        if (r.getAgeGroups() == null) {
            ag = new AgeGroups();
            r.setAgeGroups(ag);
            raceDAO.updateRace(r);
        } else {
            ag = r.getAgeGroups();
        }

        agStartTextField.setText(ag.getAGStart().toString());
        agIncrementChoiceBox.getSelectionModel().select(ag.getAGIncrement());
        
        agCustomToggleSwitch.setSelected(ag.getUseCustomIncrements());
        customAGNamesToggleSwitch.setSelected(ag.getUseCustomNames());
        nameAGTableColumn.visibleProperty().setValue(ag.getUseCustomNames());
        
        customAGTableView.setItems(ag.ageGroupIncrementProperty());
        
    }

    private void initializeSexHandlingSettings(){
        ObservableList<SexHandling> sexHandlingList = FXCollections.observableArrayList(Arrays.asList(SexHandling.values()));
        sexHandlingChoiceBox.setItems(sexHandlingList);
        sexHandlingChoiceBox.getSelectionModel().select(SexHandling.OFI);
        
        sexHandlingChoiceBox.setOnAction((event) -> {
            Race r = activeRace;
            
            SexHandling sh = sexHandlingChoiceBox.getSelectionModel().getSelectedItem();

            // If no change, bail
            if (sh.equals(r.getSexGroups().getHandling())) return; 

            r.getSexGroups().setHandling(sh);
            raceDAO.updateRace(r);
        });
        
        
        sexCodeTableColumn.setCellValueFactory(value -> value.getValue().codeProperty());
        sexCodeTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        sexCodeTableColumn.setOnEditCommit(e -> {
            e.getRowValue().codeProperty().setValue(e.getNewValue().toUpperCase());
            Race r = activeRace; 
            raceDAO.updateRace(r);
        });
//        TextFormatter<String> sexCodeformatter = new TextFormatter<>( code -> {
//            code.setText(code.getText().toUpperCase());
//            return code; 
//        });
        
        
        sexLabelTableColumn.setCellValueFactory(value -> value.getValue().labelProperty());
        sexLabelTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        sexLabelTableColumn.setOnEditCommit(e -> {
            e.getRowValue().labelProperty().setValue(e.getNewValue());
            Race r = activeRace; 
            raceDAO.updateRace(r);
        });
        
    }
    
    private void initializeRaceSettings(){
        permitTiesCheckBox.selectedProperty().addListener((ob, oldVal, newVal) -> {
            Race r = activeRace;
            if (!newVal.equals(r.getBooleanAttribute("permitTies"))) {
                r.setBooleanAttribute("permitTies", newVal);
                raceDAO.updateRace(r);
            }
        });
    }
    
    private void initializeAgeGroupSettings() {
        //@FXML ChoiceBox agIncrementChoiceBox;
        //@FXML TextField agStartTextField;
        //@FXML TextField agMastersStartTextField;
        System.out.println("initizlizeRaceAGSettings() called...");
        
        agGridPane.visibleProperty().bind(agCustomToggleSwitch.selectedProperty().not());
        agGridPane.managedProperty().bind(agCustomToggleSwitch.selectedProperty().not());
        agCustomVBox.visibleProperty().bind(agCustomToggleSwitch.selectedProperty());
        agCustomVBox.managedProperty().bind(agCustomToggleSwitch.selectedProperty());
        

        TextFormatter<String> AGSformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        agStartTextField.setTooltip(new Tooltip("Sets the max age for the first age group. i.e. 1 -> X"));  
        agStartTextField.setTextFormatter(AGSformatter);
        
        
        agIncrementChoiceBox.setItems(FXCollections.observableArrayList(1, 5, 10));
        

        
        agStartTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            Race r = activeRace; 

            if (!newPropertyValue) {
                //System.out.println("agStart out of focus...");
                
                if (agStartTextField.getText().isEmpty()) 
                    agStartTextField.setText(r.getAgeGroups().getAGStart().toString());
                
                Integer st = Integer.parseUnsignedInt(agStartTextField.getText());
                Integer inc = agIncrementChoiceBox.getSelectionModel().getSelectedItem();
                
                // If no change, bail
                if (st.equals(r.getAgeGroups().getAGStart())) return; 
                
                if (st < (inc - 1)) {
                    st = inc - 1;
                    agStartTextField.setText(st.toString());
                } else if ((st+1)%inc != 0) { // oops, the start is not a good value
                    st = ((st/inc)*inc)-1;
                    agStartTextField.setText(st.toString()); // now it should be ;-)
                }
                r.getAgeGroups().setAGStart(st);
                raceDAO.updateRace(r);

            }
        });
        
        agIncrementChoiceBox.setOnAction((event) -> {
            Race r = activeRace;
            
            Integer st = Integer.parseUnsignedInt(agStartTextField.getText());
            Integer inc = agIncrementChoiceBox.getSelectionModel().getSelectedItem();

            // If no change, bail
            if (inc.equals(r.getAgeGroups().getAGIncrement())) return; 

            if (st < (inc - 1)) {
                st = inc - 1;
                agStartTextField.setText(st.toString());
            } else if ((st+1)%inc != 0) { // oops, the start is not a good value
                st = ((st/inc)*inc)-1;
                agStartTextField.setText(st.toString()); // now it should be ;-)
            }
            r.getAgeGroups().setAGStart(st);
            r.getAgeGroups().setAGIncrement(inc);
            raceDAO.updateRace(r);
        });
        
        agCustomToggleSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean prevVal, Boolean newVal) -> {
             Race r = activeRace; 
             r.getAgeGroups().setUseCustomIncrements(newVal);
             raceDAO.updateRace(r);
        });
        customAGNamesToggleSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean prevVal, Boolean newVal) -> {
             Race r = activeRace; 
             r.getAgeGroups().setUseCustomNames(newVal);
             raceDAO.updateRace(r);
             nameAGTableColumn.visibleProperty().setValue(newVal);
        });
        
        
        
        endAGTableColumn.editableProperty().set(false);
        endAGTableColumn.setCellValueFactory(value -> value.getValue().endAgeProperty());
        
        startAGTableColumn.setCellValueFactory(value -> value.getValue().startAgeProperty().asObject());
        startAGTableColumn.setCellFactory(col -> new IntegerEditingCell());
        startAGTableColumn.setOnEditCommit(e -> {
            try {
                e.getRowValue().startAgeProperty().setValue(e.getNewValue());
                activeRace.getAgeGroups().recalcCustomAGs();
            } catch (Exception ex) {
                System.out.println("startAGTableColumn.setOnEditCommit Oops....");
                e.getRowValue().startAgeProperty().setValue(e.getOldValue());
            }
            Race r = activeRace; 
            raceDAO.updateRace(r);
        });
        
        nameAGTableColumn.setCellValueFactory(value -> value.getValue().nameProperty());
        nameAGTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        nameAGTableColumn.setOnEditCommit(e -> {
            e.getRowValue().nameProperty().setValue(e.getNewValue());
            Race r = activeRace; 
            raceDAO.updateRace(r);
        });
        
        agCustomAdd.setOnAction(action -> {
            Race r = activeRace;
            AgeGroupIncrement agi = new AgeGroupIncrement();
            if (r.getAgeGroups().ageGroupIncrementProperty().isEmpty()) 
                agi.setStartAge(10);
            else if (r.getAgeGroups().ageGroupIncrementProperty().size() == 1){
                agi.setStartAge(r.getAgeGroups().ageGroupIncrementProperty().get(r.getAgeGroups().ageGroupIncrementProperty().size()-1).getStartAge() + 5);
            } else {
                Integer l = r.getAgeGroups().ageGroupIncrementProperty().get(r.getAgeGroups().ageGroupIncrementProperty().size()-1).getStartAge();
                Integer p = r.getAgeGroups().ageGroupIncrementProperty().get(r.getAgeGroups().ageGroupIncrementProperty().size()-2).getStartAge();
                agi.setStartAge(l + l - p);
            }
            r.getAgeGroups().addCustomIncrement(agi);
            raceDAO.updateRace(r);
            activeRace.getAgeGroups().recalcCustomAGs();
        
        });
        agCustomDelete.disableProperty().bind(customAGTableView.getSelectionModel().selectedItemProperty().isNull());
        agCustomDelete.setOnAction(action -> {
            Race r = activeRace;
            r.getAgeGroups().removeCustomIncrement(customAGTableView.getSelectionModel().getSelectedItem());
            raceDAO.updateRace(r);
            activeRace.getAgeGroups().recalcCustomAGs();
        });
    }
    
    private void populateSexHandlingSettings(Race r){
        if (r == null) return;
        if (r.getSexGroups() == null){
            r.setSexGroups(new SexGroups());
            r.getSexGroups().setHandling(SexHandling.OFI);
        }
        if (r.getSexGroups().sexCodeListProperty().isEmpty() ) {
            System.out.println("Empty SexCodeList. Adding defaults");
            r.getSexGroups().addSexCode(new SexCode("F","Female"));
            r.getSexGroups().addSexCode(new SexCode("M","Male"));
            r.getSexGroups().addSexCode(new SexCode("X","Non-Binary"));
            raceDAO.updateRace(r);
        }
        
        sexHandlingChoiceBox.getSelectionModel().select(r.getSexGroups().getHandling());
        
        sexCodeMapTableView.setItems(r.getSexGroups().sexCodeListProperty());
        
    }
    
    private void populateRaceSettings(Race r){
        if (r == null) return; 
        if (r.getBooleanAttribute("permitTies") == null ) {
            r.setBooleanAttribute("permitTies", TRUE);
            raceDAO.updateRace(r);
        }
        permitTiesCheckBox.selectedProperty().set(r.getBooleanAttribute("permitTies"));
    }

    private void populateAwardsSettings(Race r) {
        System.out.println("populateAwardSettings() Called...");
        populateAwardSettingsInProgress.setValue(TRUE);
        RaceAwards a;
        
        // If null, create one and save it
        if (r.getAwards() == null) {
            System.out.println("NULL RaceAwards... Creating one...");
            a = new RaceAwards();
            r.setAwards(a);
            raceDAO.updateRace(r);
        } else {
            a = r.getAwards();
        }
        
        // Create the defaults if they do not exist
        if (a.awardCategoriesProperty().isEmpty()) {
                a.createDefaultCategories();
                a.awardCategoriesProperty().forEach(ac -> raceDAO.updateAwardCategory(ac));
        }
        
        // now show the AwardCategories
                //@FXML VBox outputDetailsVBox;
        
        // Did we already build this?
        if (! raceAwardUIMap.containsKey(r.getUUID())) {
            // No? then let's build this 
            VBox raceAwardVBox = new VBox();
            raceAwardUIMap.put(r.getUUID(), raceAwardVBox);
            System.out.println("Populating raceAwardUIMap() for race  " + r.getUUID() + " with "+ a.awardCategoriesProperty().size() + " awards" );
            
            rebuildAwardDisplay(r);
            
            a.awardCategoriesProperty().addListener((ListChangeListener)l -> {
                rebuildAwardDisplay(r);
            });
            

        }
        // Ok, now lets clear the existing outputDetails
        // the setAll below should take care of this... 
        // outputDetailsVBox.getChildren().clear(); 
        
        // And set it to the new one
        awardsVBox.getChildren().setAll(raceAwardUIMap.get(r.getUUID()));
      
    }
    
    private void rebuildAwardDisplay(Race r){
        System.out.println("rebuildAwardDisplay called for race w/ uuid of " + r.getUUID());
        RaceAwards a = r.getAwards();
        VBox raceAwardVBox = raceAwardUIMap.get(r.getUUID());
        
        raceAwardVBox.getChildren().clear();
        a.awardCategoriesProperty().forEach(ac -> {
            System.out.println("Looking for award category with UUID of " + ac.getUUID());
            if (raceAwardNodeUIMap.containsKey(ac.getUUID())){
                System.out.println("Found an existing on in the UI map...");
                raceAwardVBox.getChildren().add(raceAwardNodeUIMap.get(ac.getUUID()));
            } else {
                System.out.println("Award not found, building UI for " + ac.getName() + "(" + ac.getUUID() + ")" );
                FXMLLoader tlLoader = new FXMLLoader(getClass().getResource("/com/pikatimer/race/FXMLAwardCategory.fxml"));
                try {
                    raceAwardNodeUIMap.put(ac.getUUID(),tlLoader.load());
                    raceAwardVBox.getChildren().add(raceAwardNodeUIMap.get(ac.getUUID()));
                    System.out.println("Showing Award of type " + ac.getType().toString());
                } catch (IOException ex) {
                    System.out.println("Loader Exception for race reports!");
                    ex.printStackTrace();
                    Logger.getLogger(FXMLAwardCategoryController.class.getName()).log(Level.SEVERE, null, ex);
                }
                ((FXMLAwardCategoryController)tlLoader.getController()).setAwardCategory(ac);
            }
        });
        raceAwardVBox.requestLayout();
    }
    
    public void addAwardCategory(ActionEvent fxevent){
        AwardCategory a = new AwardCategory();
        a.setName("New Award");
        a.setRaceAward(activeRace.getAwards());
        a.setType(AwardCategoryType.OVERALL);
        activeRace.getAwards().addAwardCategory(a);
        raceDAO.updateAwardCategory(a);

    }
    
    
    
}
