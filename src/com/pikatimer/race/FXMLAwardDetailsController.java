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
import com.pikatimer.results.ResultsDAO;
import com.pikatimer.util.IntegerEditingCell;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
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
    
    @FXML ChoiceBox<Integer> agIncrementChoiceBox;
    @FXML TextField agStartTextField;
    @FXML TextField agMastersStartTextField;
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
    
    @FXML ChoiceBox<String> awardOverallPullChoiceBox;
    @FXML ChoiceBox<String> awardMastersPullChoiceBox;
    @FXML ChoiceBox<String> awardAGPullChoiceBox;
    
    @FXML ChoiceBox<String> awardOverallChipChoiceBox;
    @FXML ChoiceBox<String> awardMastersChipChoiceBox;
    @FXML ChoiceBox<String> awardAGChipChoiceBox;
   
    @FXML TextField awardOverallMaleDepthTextField;
    @FXML TextField awardOverallFemaleDepthTextField; 
    @FXML TextField awardMastersMaleDepthTextField;
    @FXML TextField awardMastersFemaleDepthTextField;    
    @FXML TextField awardAGMaleDepthTextField;
    @FXML TextField awardAGFemaleDepthTextField;
    
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
        initializeAwardSettings();
        initializeRaceSettings();
        
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
        agMastersStartTextField.setText(ag.getMasters().toString());
        
        agCustomToggleSwitch.setSelected(ag.getUseCustomIncrements());
        customAGNamesToggleSwitch.setSelected(ag.getUseCustomNames());
        
        customAGTableView.setItems(ag.ageGroupIncrementProperty());
        
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
        
        TextFormatter<String> AGMformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        agMastersStartTextField.setTooltip(new Tooltip("Sets the starting age for the Masters categories."));  
        agMastersStartTextField.setTextFormatter(AGMformatter);
        
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
        
        agMastersStartTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            Race r = activeRace; 

            if (!newPropertyValue) {
                //System.out.println("agStart out of focus...");
                
                if (agMastersStartTextField.getText().isEmpty()) 
                    agMastersStartTextField.setText(r.getAgeGroups().getMasters().toString());
                
                Integer m = Integer.parseUnsignedInt(agMastersStartTextField.getText());
                
                // If no change, bail
                if ( ! m.equals(r.getAgeGroups().getMasters())){
                    r.getAgeGroups().setMasters(m);
                    raceDAO.updateRace(r);
                }
            }
        });
        
//        @FXML ToggleSwitch blankToZeroToggleSwitch;
//        @FXML ToggleSwitch agCustomToggleSwitch;
//        @FXML ToggleSwitch customAGNamesToggleSwitch;
//        @FXML GridPane agGridPane;
//        @FXML VBox agCustomVBox;
//        @FXML TableView<AgeGroupIncrement> customAGTableView;
//        @FXML TableColumn<AgeGroupIncrement,Integer> startAGTableColumn;
//        @FXML TableColumn<AgeGroupIncrement,Integer> endAGTableColumn;
//        @FXML TableColumn<AgeGroupIncrement,String> nameAGTableColumn;
//        @FXML Button agCustomAdd;
//        @FXML Button agCustomDelete;


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
    
    private void initializeAwardSettings() {
        //    @FXML ChoiceBox awardOverallPullChoiceBox;
        awardOverallPullChoiceBox.setItems(FXCollections.observableArrayList("Yes","No"));
        awardOverallPullChoiceBox.setTooltip(new Tooltip("If \"Yes,\" winners are inelligible for Masters and\nAge Group Awards")); 
        awardOverallPullChoiceBox.setOnAction((event) -> {
            Race r = activeRace; 
            RaceAwards a = r.getAwards();
            
            if (awardOverallPullChoiceBox.getSelectionModel().isEmpty()) return;
            if (populateAwardSettingsInProgress.getValue()) return;

            
            Boolean s = awardOverallPullChoiceBox.getSelectionModel().getSelectedItem().equalsIgnoreCase("yes");
            
            if (! s.equals(a.getBooleanAttribute("OverallPull"))) {
                System.out.println("awardOverallPullChoiceBox Changed...");
                System.out.println(" was " + a.getBooleanAttribute("OverallPull").toString() );
                System.out.println(" Set to " + awardOverallPullChoiceBox.getSelectionModel().getSelectedItem());
                a.setBooleanAttribute("OverallPull",s);
                raceDAO.updateRace(r);
            }
        });
        
        //    @FXML ChoiceBox awardMastersPullChoiceBox;
        awardMastersPullChoiceBox.setItems(FXCollections.observableArrayList("Yes","No"));
        awardMastersPullChoiceBox.setOnAction((event) -> {
            Race r = activeRace;
            RaceAwards a = r.getAwards();
            
            if (awardMastersPullChoiceBox.getSelectionModel().isEmpty()) return;
            if (populateAwardSettingsInProgress.getValue()) return;

            Boolean s = awardMastersPullChoiceBox.getSelectionModel().getSelectedItem().equalsIgnoreCase("yes");
            
            if (! s.equals(a.getBooleanAttribute("MastersPull"))) {
                System.out.println("awardMastersPullChoiceBox Changed..");
                a.setBooleanAttribute("MastersPull",s);
                raceDAO.updateRace(r);
            }
        });
        //    @FXML ChoiceBox awardAGPullChoiceBox;
        awardAGPullChoiceBox.visibleProperty().setValue(false);
        awardAGPullChoiceBox.focusTraversableProperty().set(false);
        awardAGPullChoiceBox.setItems(FXCollections.observableArrayList("Yes","No"));
        awardAGPullChoiceBox.setOnAction((event) -> {
            Race r = activeRace;
            RaceAwards a = r.getAwards();
            
            if (awardAGPullChoiceBox.getSelectionModel().isEmpty()) return;
            if (populateAwardSettingsInProgress.getValue()) return;

            Boolean s = awardAGPullChoiceBox.getSelectionModel().getSelectedItem().equalsIgnoreCase("yes");
            
            if (! s.equals(a.getBooleanAttribute("AGPull"))) {
                a.setBooleanAttribute("AGPull",s);
                raceDAO.updateRace(r);
            }
        });

        //    @FXML ChoiceBox awardOverallChipChoiceBox;
        awardOverallChipChoiceBox.setItems(FXCollections.observableArrayList("Gun","Chip"));
        awardOverallChipChoiceBox.setOnAction((event) -> {
            
            if (awardOverallChipChoiceBox.getSelectionModel().isEmpty()) return;
            if (populateAwardSettingsInProgress.getValue()) return;
            
            Race r = raceComboBox.getSelectionModel().getSelectedItem();
            RaceAwards a = r.getAwards();
            
            Boolean s = awardOverallChipChoiceBox.getSelectionModel().getSelectedItem().equalsIgnoreCase("chip");
            
            if (! s.equals(a.getBooleanAttribute("OverallChip"))) {
                a.setBooleanAttribute("OverallChip",s);
                raceDAO.updateRace(r);
            }
        });
        
        //    @FXML ChoiceBox awardMastersChipChoiceBox;
        awardMastersChipChoiceBox.setItems(FXCollections.observableArrayList("Gun","Chip"));
        awardMastersChipChoiceBox.setOnAction((event) -> {
            if (awardMastersChipChoiceBox.getSelectionModel().isEmpty()) return;
            if (populateAwardSettingsInProgress.getValue()) return;

            Race r = activeRace;
            RaceAwards a = r.getAwards();
            
            Boolean s = awardMastersChipChoiceBox.getSelectionModel().getSelectedItem().equalsIgnoreCase("chip");
            
            if (! s.equals(a.getBooleanAttribute("MastersChip"))) {
                a.setBooleanAttribute("MastersChip",s);
                raceDAO.updateRace(r);
            }
        });
        
        //    @FXML ChoiceBox awardAGChipChoiceBox;
        awardAGChipChoiceBox.setItems(FXCollections.observableArrayList("Gun","Chip"));
        awardAGChipChoiceBox.setOnAction((event) -> {
            if (awardAGChipChoiceBox.getSelectionModel().isEmpty()) return;
            if (populateAwardSettingsInProgress.getValue()) return;

            Race r = activeRace;
            RaceAwards a = r.getAwards();
            
            Boolean s = awardAGChipChoiceBox.getSelectionModel().getSelectedItem().equalsIgnoreCase("chip");
            
            if (! s.equals(a.getBooleanAttribute("AGChip"))) {
                a.setBooleanAttribute("AGChip",s);
                raceDAO.updateRace(r);
            }
        });
        //   
        //    @FXML TextField awardOverallMaleDepthTextField;
        TextFormatter<String> OMDformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        awardOverallMaleDepthTextField.setTextFormatter(OMDformatter);
        awardOverallMaleDepthTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (populateAwardSettingsInProgress.getValue()) return;

            if (!newPropertyValue) {
            Race r = activeRace;

            if (awardOverallMaleDepthTextField.getText().isEmpty()) 
                    awardOverallMaleDepthTextField.setText(r.getAwards().getIntegerAttribute("OverallMaleDepth").toString());
                
                Integer m = Integer.parseUnsignedInt(awardOverallMaleDepthTextField.getText());
                
                // If no change, bail
                if ( ! m.equals(r.getAwards().getIntegerAttribute("OverallMaleDepth"))){
                    r.getAwards().setIntegerAttribute("OverallMaleDepth", m);
                    System.out.println("awardOverallMaleDepthTextField Changed..");
                    raceDAO.updateRace(r);
                }
            }
        });
        
        //    @FXML TextField awardOverallFemaleDepthTextField; 
        TextFormatter<String> OFDformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        awardOverallFemaleDepthTextField.setTextFormatter(OFDformatter);
        awardOverallFemaleDepthTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (populateAwardSettingsInProgress.getValue()) return;

            if (!newPropertyValue) {
                Race r = activeRace;

                if (awardOverallFemaleDepthTextField.getText().isEmpty()) 
                    awardOverallFemaleDepthTextField.setText(r.getAwards().getIntegerAttribute("OverallFemaleDepth").toString());
                
                Integer d = Integer.parseUnsignedInt(awardOverallFemaleDepthTextField.getText());
                
                // If no change, bail
                if ( ! d.equals(r.getAwards().getIntegerAttribute("OverallFemaleDepth"))){
                    r.getAwards().setIntegerAttribute("OverallFemaleDepth", d);
                    System.out.println("awardOverallFemaleDepthTextField Changed..");
                    raceDAO.updateRace(r);
                }
            }
        });
        
        //    @FXML TextField awardMastersMaleDepthTextField;
        TextFormatter<String> MMformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        awardMastersMaleDepthTextField.setTextFormatter(MMformatter);
        awardMastersMaleDepthTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (populateAwardSettingsInProgress.getValue()) return;

            if (!newPropertyValue) {
                Race r = activeRace;

                if (awardMastersMaleDepthTextField.getText().isEmpty()) 
                    awardMastersMaleDepthTextField.setText(r.getAwards().getIntegerAttribute("MastersMaleDepth").toString());
                
                Integer d = Integer.parseUnsignedInt(awardMastersMaleDepthTextField.getText());
                
                // If no change, bail
                if ( ! d.equals(r.getAwards().getIntegerAttribute("MastersMaleDepth"))){
                    r.getAwards().setIntegerAttribute("MastersMaleDepth", d);
                    System.out.println("awardMastersMaleDepthTextField Changed..");
                    raceDAO.updateRace(r);
                }
            }
        });
        
        //    @FXML TextField awardMastersFemaleDepthTextField;    
        TextFormatter<String> MFformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        awardMastersFemaleDepthTextField.setTextFormatter(MFformatter);
        awardMastersFemaleDepthTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (populateAwardSettingsInProgress.getValue()) return;

            if (!newPropertyValue) {
                Race r = activeRace;

                if (awardMastersFemaleDepthTextField.getText().isEmpty()) 
                    awardMastersFemaleDepthTextField.setText(r.getAwards().getIntegerAttribute("MastersFemaleDepth").toString());
                
                Integer d = Integer.parseUnsignedInt(awardMastersFemaleDepthTextField.getText());
                
                // If no change, bail
                if ( ! d.equals(r.getAwards().getIntegerAttribute("MastersFemaleDepth"))){
                    r.getAwards().setIntegerAttribute("MastersFemaleDepth", d);
                    System.out.println("awardMastersFemaleDepthTextField Changed..");
                    raceDAO.updateRace(r);
                }
            }
        });
        
        //    @FXML TextField awardAGMaleDepthTextField;
        TextFormatter<String> AGMformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        awardAGMaleDepthTextField.setTextFormatter(AGMformatter);
        awardAGMaleDepthTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (populateAwardSettingsInProgress.getValue()) return;

            if (!newPropertyValue) {
                Race r = activeRace;

                if (awardAGMaleDepthTextField.getText().isEmpty()) 
                    awardAGMaleDepthTextField.setText(r.getAwards().getIntegerAttribute("AGMaleDepth").toString());
                
                Integer d = Integer.parseUnsignedInt(awardAGMaleDepthTextField.getText());
                
                // If no change, bail
                if ( ! d.equals(r.getAwards().getIntegerAttribute("AGMaleDepth"))){
                    r.getAwards().setIntegerAttribute("AGMaleDepth", d);
                    System.out.println("awardAGMaleDepthTextField Changed..");
                    raceDAO.updateRace(r);
                }
            }
        });
        
        //    @FXML TextField awardAGFemaleDepthTextField;
        TextFormatter<String> AGFformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        awardAGFemaleDepthTextField.setTextFormatter(AGFformatter);
        awardAGFemaleDepthTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (populateAwardSettingsInProgress.getValue()) return;

            if (!newPropertyValue) {
                Race r = activeRace;

                if (awardAGFemaleDepthTextField.getText().isEmpty()) 
                    awardAGFemaleDepthTextField.setText(r.getAwards().getIntegerAttribute("AGFemaleDepth").toString());
                
                Integer d = Integer.parseUnsignedInt(awardAGFemaleDepthTextField.getText());
                
                // If no change, bail
                if ( ! d.equals(r.getAwards().getIntegerAttribute("AGFemaleDepth"))){
                    r.getAwards().setIntegerAttribute("AGFemaleDepth", d);
                    System.out.println("awardAGFemaleDepthTextField Changed..");
                    raceDAO.updateRace(r);
                }
            }
        });


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
        populateAwardSettingsInProgress.setValue(TRUE);
        RaceAwards a;
        
        // If null, create one and save it
        if (r.getAwards() == null) {
            a = new RaceAwards();
            System.out.println("NULL Awards, adding some...");
            // Set defaults for all used values
            a.setBooleanAttribute("OverallPull", TRUE);
            a.setBooleanAttribute("MastersPull", TRUE);
            a.setBooleanAttribute("AGPull", TRUE);
            
            a.setBooleanAttribute("OverallChip", TRUE);
            a.setBooleanAttribute("MastersChip", TRUE);
            a.setBooleanAttribute("AGChip", TRUE);
            
            a.setIntegerAttribute("OverallMaleDepth", 5);
            a.setIntegerAttribute("OverallFemaleDepth", 5);
            a.setIntegerAttribute("MastersMaleDepth", 3);
            a.setIntegerAttribute("MastersFemaleDepth", 3);
            a.setIntegerAttribute("AGMaleDepth", 3);
            a.setIntegerAttribute("AGFemaleDepth", 3);


            
            r.setAwards(a);
            raceDAO.updateRace(r);
        } else {
            a = r.getAwards();
        }
      
        //    @FXML ChoiceBox awardOverallPullChoiceBox;
        if (a.getBooleanAttribute("OverallPull")) {   
            awardOverallPullChoiceBox.setValue("Yes");
        } else {
            awardOverallPullChoiceBox.setValue("No");
        }
        //    @FXML ChoiceBox awardMastersPullChoiceBox;
        if (a.getBooleanAttribute("MastersPull")) {   
            awardMastersPullChoiceBox.setValue("Yes");
        } else {
            awardMastersPullChoiceBox.setValue("No");
        }
        //    @FXML ChoiceBox awardAGPullChoiceBox;
        if (a.getBooleanAttribute("AGPull")) {   
            awardAGPullChoiceBox.setValue("Yes");
        } else {
            awardAGPullChoiceBox.setValue("No");
        }
            
        //    @FXML ChoiceBox awardOverallChipChoiceBox;
        if (a.getBooleanAttribute("OverallChip")) {   
            awardOverallChipChoiceBox.setValue("Chip");
        } else {
            awardOverallChipChoiceBox.setValue("Gun");
        }
        //    @FXML ChoiceBox awardMastersChipChoiceBox;
        if (a.getBooleanAttribute("MastersChip")) {   
            awardMastersChipChoiceBox.setValue("Chip");
        } else {
            awardMastersChipChoiceBox.setValue("Gun");
        }
        //    @FXML ChoiceBox awardAGChipChoiceBox;
        if (a.getBooleanAttribute("AGChip")) {   
            awardAGChipChoiceBox.setValue("Chip");
        } else {
            awardAGChipChoiceBox.setValue("Gun");
        }
        //   

        //    @FXML TextField awardOverallMaleDepthTextField;
        awardOverallMaleDepthTextField.setText(a.getIntegerAttribute("OverallMaleDepth").toString());
        //    @FXML TextField awardOverallFemaleDepthTextField; 
        awardOverallFemaleDepthTextField.setText(a.getIntegerAttribute("OverallFemaleDepth").toString());
        //    @FXML TextField awardMastersMaleDepthTextField;
        awardMastersMaleDepthTextField.setText(a.getIntegerAttribute("MastersMaleDepth").toString());
        //    @FXML TextField awardMastersFemaleDepthTextField;    
        awardMastersFemaleDepthTextField.setText(a.getIntegerAttribute("MastersFemaleDepth").toString());
        //    @FXML TextField awardAGMaleDepthTextField;
        awardAGMaleDepthTextField.setText(a.getIntegerAttribute("AGMaleDepth").toString());
        //    @FXML TextField awardAGFemaleDepthTextField;    
        awardAGFemaleDepthTextField.setText(a.getIntegerAttribute("AGFemaleDepth").toString());
        
        populateAwardSettingsInProgress.setValue(FALSE);
    }
}
