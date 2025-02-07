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
package com.pikatimer.race;

import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.results.ResultsDAO;
import com.pikatimer.timing.Segment;
import com.pikatimer.timing.Split;
import com.pikatimer.timing.TimingLocation;
import com.pikatimer.timing.TimingDAO;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.DurationParser;
import com.pikatimer.util.Pace;
import com.pikatimer.util.TextFieldFormatters;
import com.pikatimer.util.Unit;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableRow;
//import javafx.scene.control.cell.ComboBoxTableCell;
//import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.ToggleSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLRaceDetailsController {
    private static final Logger logger = LoggerFactory.getLogger(FXMLRaceDetailsController.class);
    
    private RaceDAO raceDAO; 
    
    @FXML private HBox raceNameHBox;
    @FXML private TextField raceNameTextField; 
    @FXML private TextField raceDistanceTextField; 
    @FXML private ChoiceBox<Unit> distanceUnitChoiceBox; 
    @FXML private TextField raceCutoffTimeTextField;
    @FXML private Label raceCutoffTimePaceLabel; 
    
    
    @FXML private VBox waveStartsVBox; 
    @FXML private ToggleSwitch waveStartsToggleSwitch; 
    @FXML private TableView<Wave> waveStartsTableView;
    @FXML private TableColumn<Wave, String> waveNameTableColumn;
    @FXML private TableColumn<Wave, String> waveStartTimeTableColumn;                
    @FXML private TableColumn<Wave, String> waveAssignmentStartTableColumn;
    @FXML private TableColumn<Wave, String> waveAssignmentEndTableColumn;
    @FXML private Button deleteWaveButton;
    @FXML private Button editWaveButton;
    @FXML private Button addWaveButton;
    
    @FXML private VBox splitsVBox;
    @FXML private ToggleSwitch splitsToggleSwitch; 
    @FXML private TableView<Split> raceSplitsTableView;
    @FXML private TableColumn<Split, String> splitNameTableColumn;
    @FXML private TableColumn<Split,String> splitLocationTableColumn; 
    @FXML private TableColumn<Split, String> splitDistanceTableColumn;
    @FXML private Button deleteSplitButton;
    @FXML private Button addSplitButton;
    @FXML private Button editSplitButton;
    @FXML private Button splitUpdateResultsButton;
        
    @FXML private VBox segmentsVBox;
    @FXML private TableView<Segment> raceSegmentsTableView;
    @FXML private TableColumn<Segment,String> segmentNameTableColumn;
    @FXML private TableColumn<Segment,String> segmentStartSplitTableColumn;
    @FXML private TableColumn<Segment,String> segmentEndSplitTableColumn;
    @FXML private TableColumn<Segment,String> segmentDistanceTableColumn;
    @FXML private Button deleteSegmentButton; 
    @FXML private Button addSegmentButton;  
    @FXML private Button editSegmentButton;    
    
    
    @FXML private HBox startTimeHBox; 
    
    
    @FXML private TextField raceStartTimeTextField; 
    
    
    @FXML private HBox bibRangeHBox;
    @FXML private TextField startBibTextField;
    @FXML private TextField endBibTextField;
    
    @FXML private Button updateResultsButton;


    @FXML private VBox startFinishLocationVBox;
    @FXML private ComboBox<TimingLocation> startLocationComboBox;
    @FXML private ComboBox<TimingLocation> finishLocationComboBox;
    @FXML private HBox maxStartHBox;
    @FXML private TextField maxStartTextField;
    

    @FXML private ToggleSwitch lapRaceToggleSwitch;
    @FXML private VBox lapOptionsVBox;
    @FXML private ComboBox<TimingLocation> lapExitLocationComboBox;
    @FXML private TextField minLapTimeTextField;
      
    @FXML private Button courseRecordSetupButton;


    
    Race selectedRace; 
    ObservableList<Wave> raceWaves;
    ObservableList<Split> raceSplits; 
    ObservableList<Segment> raceSegments;
    private ChangeListener<? super Split>  raceSplitsTableViewListener;
    private ChangeListener<? super Boolean> waveStartsCheckBoxListener;
    private ListChangeListener<? super Split> raceSplitsListener;
    private ListChangeListener<? super Wave> raceWaveListener;

    /**
     * Initializes the controller class.
     */
    public void initialize() {
        

        // get a RaceDAO
        raceDAO = RaceDAO.getInstance(); 

        ObservableList<Unit> unitList = FXCollections.observableArrayList(Arrays.asList(Unit.values()));
        raceWaves = FXCollections.observableArrayList(); 
        raceSegments = FXCollections.observableArrayList(); 

        distanceUnitChoiceBox.setItems(unitList);
        distanceUnitChoiceBox.setValue(Unit.MILES);
        
        distanceUnitChoiceBox.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends Unit> observableValue, Unit o, Unit n) -> {
            logger.debug("distanceUnitChoiceBox event");
            if (!n.equals(selectedRace.getRaceDistanceUnits())){
                logger.debug("distanceUnitChoiceBox event triggered update...");
                selectedRace.setRaceDistanceUnits(n);
                updateRaceCutoffPace();
                raceDAO.updateRace(selectedRace);
            }
        });
        
        raceNameTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                logger.debug("raceNameTextField out focus");
                if ( ! raceNameTextField.getText().equals(selectedRace.getRaceName()) ) {
                    updateRaceName();
                }
            }
        });
        
        raceDistanceTextField.setTextFormatter(TextFieldFormatters.getPositiveBigDecimalFormatter());

        // Update when the textfield focus changes. 
        raceDistanceTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                logger.debug("raceDistanceTextField out focus, saving...");
                if (raceDistanceTextField.getText().isBlank()) {
                    raceDistanceTextField.setText(selectedRace.getRaceDistance().toPlainString());
                }
                if ( ! raceDistanceTextField.getText().equals(selectedRace.getRaceDistance().toPlainString()) ) {
                    updateRaceDistance();
                }
            }
        });
        
        
        // Race (wave) Time stuff
        raceStartTimeTextField.setTextFormatter(TextFieldFormatters.getLocalTimeFormatter());

        // Update when the textfield focus changes. 
        raceStartTimeTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                logger.debug("raceStartTimeTextField out focus");
                try {
                    if (!raceStartTimeTextField.getText().isEmpty()) {
                        LocalTime.parse(raceStartTimeTextField.getText(), DateTimeFormatter.ISO_LOCAL_TIME );
                    }
                } catch (Exception e) {
                    raceStartTimeTextField.setText(raceWaves.get(0).getWaveStart());
                    logger.debug("Bad Race Start Time (newValue: " + raceStartTimeTextField.getText() + ")");
                }
                if ( ! raceStartTimeTextField.getText().equals(raceWaves.get(0).getWaveStart()) ) {
                    updateRaceStartTime();
                } else {
                    logger.debug("Unchaged Race Start, not saving: \"" + raceWaves.get(0).getWaveStart() + "\" vs " + raceStartTimeTextField.getText() );
                }
            } else {
                
            }
        });
        
        startTimeHBox.visibleProperty().bind(waveStartsToggleSwitch.selectedProperty().not());
        startTimeHBox.managedProperty().bind(waveStartsToggleSwitch.selectedProperty().not());
        
        // Race (wave) Time stuff
        //waveNameTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        waveNameTableColumn.setCellValueFactory(w -> w.getValue().waveNameProperty());
        waveNameTableColumn.setComparator(new AlphanumericComparator());
        
        //waveStartTimeTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        waveStartTimeTableColumn.setCellValueFactory((w -> w.getValue().waveStartStringProperty()));

        //waveAssignmentStartTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        waveAssignmentStartTableColumn.setCellValueFactory(w -> w.getValue().waveAssignmentStartProperty());
        waveAssignmentStartTableColumn.setComparator(new AlphanumericComparator());

        
        //waveAssignmentEndTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        waveAssignmentEndTableColumn.setCellValueFactory(w -> w.getValue().waveAssignmentEndProperty());
        waveAssignmentEndTableColumn.setComparator(new AlphanumericComparator());
        
        waveStartsTableView.setRowFactory(t -> {
            final TableRow<Wave> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    editWave();
                }
            });
            return row;
        } );
        
        addWaveButton.setOnAction(action -> addWave());
        deleteWaveButton.setOnAction(action -> deleteWave());
        editWaveButton.setOnAction(action -> editWave());
        
        
        
        raceCutoffTimeTextField.setPromptText("HH:MM:SS");
        raceCutoffTimeTextField.setTextFormatter(TextFieldFormatters.getPositiveDurationFormatter());
        raceCutoffTimeTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                logger.debug("raceCutoffTimeTextField out focus");

                if ( ! raceCutoffTimeTextField.getText().equals(selectedRace.raceCutoffProperty().getValueSafe()) ) {
                    if (DurationParser.parsable(raceCutoffTimeTextField.getText()) || raceCutoffTimeTextField.getText().isEmpty() ) {
                        updateRaceCutoffTime(); 
                    } else {
                        logger.debug("raceCutoffTimeTextField out focus with bad time, reverting to " + selectedRace.raceCutoffProperty().getValueSafe());
                        raceCutoffTimeTextField.setText(selectedRace.raceCutoffProperty().getValueSafe());
                    }
                } else {
                    logger.debug("Unchaged Cutoff time, not saving: \"" + selectedRace.raceCutoffProperty().getValueSafe() + "\" vs " + raceCutoffTimeTextField.getText() );
                }
            } else {
                
            }
        });
        
        // Start/Finish Stuff
        startLocationComboBox.setItems(TimingDAO.getInstance().listTimingLocations());
        startLocationComboBox.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends TimingLocation> observableValue, TimingLocation o, TimingLocation n) -> {
            logger.debug("startLocationChoiceBox event");
            Split s = selectedRace.getSplits().get(0);
            if (s.getTimingLocation().equals(n)) return;
            s.setTimingLocation(n);
            raceDAO.updateSplit(s);
            
            if (s.getTimingLocation().equals(selectedRace.getSplits().getLast().getTimingLocation())) {
                maxStartHBox.setVisible(true);
                updateRaceMaxStartTime();
            } else maxStartHBox.setVisible(false);
        });
        
        finishLocationComboBox.setItems(TimingDAO.getInstance().listTimingLocations());
        finishLocationComboBox.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends TimingLocation> observableValue, TimingLocation o, TimingLocation n) -> {
            logger.debug("startLocationChoiceBox event");
            Split s = selectedRace.getSplits().getLast();
            if (s.getTimingLocation().equals(n)) return;
            s.setTimingLocation(n);
            raceDAO.updateSplit(s);
            
            if (s.getTimingLocation().equals(selectedRace.getSplits().getFirst().getTimingLocation())) {
                maxStartHBox.setVisible(true);
                updateRaceMaxStartTime(); 
            } else maxStartHBox.setVisible(false);
        });
        
        maxStartTextField.setPromptText("HH:MM:SS");
        maxStartTextField.setTextFormatter(TextFieldFormatters.getPositiveDurationFormatter());
        maxStartTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                logger.debug("maxStartTimeTextField out focus");

                if ( ! maxStartTextField.getText().equals(selectedRace.raceMaxStartProperty().getValueSafe()) ) {
                    if (DurationParser.parsable(maxStartTextField.getText()) || maxStartTextField.getText().isEmpty() ) {
                        updateRaceMaxStartTime();
                    } else {
                        logger.debug("maxStartTimeTextField out focus with bad time, reverting to " + selectedRace.raceMaxStartProperty().getValueSafe());
                        maxStartTextField.setText(selectedRace.raceMaxStartProperty().getValueSafe());
                    }
                } else {
                    logger.debug("Unchaged Cutoff time, not saving: \"" + selectedRace.raceMaxStartProperty().getValueSafe() + "\" vs " + maxStartTextField.getText() );
                }
            } else {
                
            }
        });
        
        // Lap Race Stuff
        lapOptionsVBox.visibleProperty().bind(lapRaceToggleSwitch.selectedProperty());
        lapOptionsVBox.managedProperty().bind(lapRaceToggleSwitch.selectedProperty());
        lapRaceToggleSwitch.selectedProperty().addListener((arg0,  oldVal,  newVal) -> {
            logger.debug("Changing the lap race from {} to {}",oldVal,newVal);
            if (selectedRace != null && ! selectedRace.getLapRace().equals(newVal)){
                selectedRace.setLapRace(newVal);
                raceDAO.updateRace(selectedRace);
            }
        });
        
        lapExitLocationComboBox.setItems(TimingDAO.getInstance().listTimingLocations());
        lapExitLocationComboBox.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends TimingLocation> observableValue, TimingLocation o, TimingLocation n) -> {
            logger.debug("lapExitLocationComboBox event");
            if (selectedRace != null && n != null) {
                if (! n.equals(selectedRace.getLapExitLocation())){
                    selectedRace.setLapExitLocation(n);
                    raceDAO.updateRace(selectedRace);
                }
            }
        });
        
        minLapTimeTextField.setPromptText("HH:MM:SS");
        minLapTimeTextField.setTextFormatter(TextFieldFormatters.getPositiveDurationFormatter());
     
        minLapTimeTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                logger.debug("minLapTimeTextField out focus");

                if ( ! minLapTimeTextField.getText().equals(selectedRace.minLapTimeDurationStringProperty().getValueSafe()) ) {
                    if (DurationParser.parsable(minLapTimeTextField.getText()) || minLapTimeTextField.getText().isEmpty() ) {
                        if (minLapTimeTextField.getText().isEmpty()) {
                            selectedRace.setLapMinTime(0L);
                        } else { 
                            selectedRace.setLapMinTime(DurationParser.parse(minLapTimeTextField.getText()).toNanos());
                        }
                        minLapTimeTextField.setText(selectedRace.minLapTimeDurationStringProperty().getValueSafe());
                        raceDAO.updateRace(selectedRace);
                    } else {
                        logger.debug("minLapTimeTextField out focus with bad time, reverting to " + selectedRace.minLapTimeDurationStringProperty().getValueSafe());
                        minLapTimeTextField.setText(selectedRace.minLapTimeDurationStringProperty().getValueSafe());
                    }
                } else {
                    logger.debug("Unchaged lap min time, not saving: \"" + selectedRace.minLapTimeDurationStringProperty().getValueSafe() + "\" vs " + minLapTimeTextField.getText() );
                }
            } else {
                
            }
        });
        
        
        // Split table stuff
        //splitNameTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        splitNameTableColumn.setCellValueFactory((c) -> {return c.getValue().splitNameProperty();});
        splitLocationTableColumn.setCellValueFactory((c) -> {return c.getValue().timingLocationProperty();});
        splitDistanceTableColumn.setCellValueFactory((c) -> {return c.getValue().splitDistanceProperty();});
        splitDistanceTableColumn.setComparator(new AlphanumericComparator());
        
        raceSplitsTableView.setPlaceholder(new Label("No race splits have been defined yet"));
        
        // Double Click to fire off the edit dialog
        raceSplitsTableView.setRowFactory(t -> {
            final TableRow<Split> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    editSplit();
                }
            });
            return row;
        } );
        
        addSplitButton.setOnAction(event -> addSplit());
        deleteSplitButton.setOnAction(event -> deleteSplit());
        editSplitButton.setOnAction(event -> editSplit());
        
        splitUpdateResultsButton.visibleProperty().set(false);
        
        splitUpdateResultsButton.setOnAction((event) -> {
            ResultsDAO.getInstance().reprocessRaceResults(selectedRace);
            splitUpdateResultsButton.visibleProperty().set(false);
        });
        
        
        startBibTextField.setTextFormatter(TextFieldFormatters.integerFormatter(false));
        startBibTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                logger.debug("startBibTextField out focus");
                updateRaceStartBib();
            }
        });
        
        endBibTextField.setTextFormatter(TextFieldFormatters.integerFormatter(false));
        endBibTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                logger.debug("endBibTestField out focus");
                updateRaceEndBib();
            }
        });
        

        

        
        updateResultsButton.visibleProperty().set(false);
        
        updateResultsButton.setOnAction((event) -> {
            ResultsDAO.getInstance().reprocessRaceResults(selectedRace);
            updateResultsButton.visibleProperty().set(false);
        });
        
        
        // Segment table stuff
        raceSegmentsTableView.setPlaceholder(new Label("No race segments have been defined yet"));
                
        segmentNameTableColumn.setCellValueFactory(s -> s.getValue().segmentNameProperty());
        segmentDistanceTableColumn.setCellValueFactory(s -> s.getValue().distanceStringProperty());
        segmentStartSplitTableColumn.setCellValueFactory(s -> s.getValue().startSplitStringProperty());
        segmentEndSplitTableColumn.setCellValueFactory(s -> s.getValue().endSplitStringProperty());
        
        raceSegmentsTableView.setRowFactory(t -> {
            final TableRow<Segment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    editSegment();
                }
            });
            return row;
        } );
        

        
        
        addSegmentButton.setOnAction(event -> addSegment());
        deleteSegmentButton.setOnAction(event -> deleteSegment());
        editSegmentButton.setOnAction(event -> editSegment());
        
    
        
        
        courseRecordSetupButton.setOnAction(r -> {
        
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FXMLCourseRecords.fxml"));
            Parent crRoot;
            try {
                crRoot = (Parent) fxmlLoader.load();
                Stage stage = new Stage();
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.setTitle("Course Record Setup");
                stage.setScene(new Scene(crRoot));  
                ((FXMLCourseRecordsController)fxmlLoader.getController()).setRace(selectedRace);
                stage.showAndWait();
            } catch (IOException ex) {
                logger.warn("Error in FXML",ex);
            }
            
        
        
        });

    }    
    
    // TODO:  Move stuff out of here that can be in the initialize section
    public void selectRace(Race r) {
        
        if (selectedRace != null) {
            
            //Unbind any existing listeners to the table views or check boxes
            raceSplitsTableView.getSelectionModel().selectedItemProperty().removeListener(raceSplitsTableViewListener);
            waveStartsToggleSwitch.selectedProperty().removeListener(waveStartsCheckBoxListener);
            raceSplits.removeListener(raceSplitsListener);
            raceWaves.removeListener(raceWaveListener);
            
        }
        
        selectedRace = r;
        
        if (selectedRace != null) {
            logger.debug("Non-Null race, populate all fields out");
            //Setup the Race Name
            raceNameTextField.setText(selectedRace.getRaceName());
            if(raceNameTextField.disableProperty().get()) {
                raceNameTextField.requestFocus();
            } else {
                raceStartTimeTextField.requestFocus(); 
            }
            
            //Setup the distance
            logger.debug("Setting race distance to {}",selectedRace.getRaceDistance().toPlainString());
            raceDistanceTextField.setText(selectedRace.getRaceDistance().toPlainString());
            distanceUnitChoiceBox.setValue(selectedRace.getRaceDistanceUnits()); 
            
            
            //setup the cutoff and pace

            // setup the cutoff label so that it displace the pace in M/Mi if 
            // it is set, otherwise blank it

            


            //Setup the wave starts VBOX
            raceWaves=selectedRace.wavesProperty(); 
            waveStartsTableView.setItems(raceWaves);
            if (raceWaves.isEmpty()) {
                // no waves. Let's create one with some default values
               Wave wave = new Wave(selectedRace);
               wave.setWaveName("Wave 1");
               wave.setWaveAssignmentMethod(WaveAssignment.BIB);
               
               raceDAO.addWave(wave);
            } 
            editWaveButton.disableProperty().bind(waveStartsTableView.getSelectionModel().selectedItemProperty().isNull());
            
            deleteWaveButton.disableProperty().bind(Bindings.or(
                waveStartsTableView.getSelectionModel().selectedItemProperty().isNull(),
                Bindings.size(waveStartsTableView.getItems()).lessThan(2))
            );
            waveStartsVBox.managedProperty().bind(waveStartsToggleSwitch.selectedProperty());
            waveStartsVBox.visibleProperty().bind(waveStartsToggleSwitch.selectedProperty());
            bibRangeHBox.managedProperty().bind(Bindings.and(
                    Bindings.size(raceDAO.listRaces()).greaterThanOrEqualTo(2), 
                    waveStartsToggleSwitch.selectedProperty().not()
            ));
            bibRangeHBox.visibleProperty().bind(Bindings.and(
                    Bindings.size(raceDAO.listRaces()).greaterThanOrEqualTo(2), 
                    waveStartsToggleSwitch.selectedProperty().not()
            ));
                    
            // if we have more than one wave then let's set the waveStartsCheckBox to true.
            if (raceWaves.size() > 1) {
                waveStartsToggleSwitch.setSelected(true); 
            } else {
                waveStartsToggleSwitch.setSelected(false);
            }
            waveStartsToggleSwitch.disableProperty().bind(Bindings.size(waveStartsTableView.getItems()).greaterThan(1));
            //Setup the start time
            raceStartTimeTextField.setText(raceWaves.get(0).getWaveStart());
            
            if (raceWaves.get(0).getWaveAssignmentMethod().equals(WaveAssignment.BIB)) {
                endBibTextField.setText(raceWaves.get(0).getWaveAssignmentEnd());
                startBibTextField.setText(raceWaves.get(0).getWaveAssignmentStart());
            } else {
                endBibTextField.setText("");
                startBibTextField.setText("");
            }
        
            
            
            splitsVBox.managedProperty().bind(splitsToggleSwitch.selectedProperty());
            splitsVBox.visibleProperty().bind(splitsToggleSwitch.selectedProperty());
            
            raceSplits=selectedRace.splitsProperty(); 
            FilteredList<Split> filteredSplits = new FilteredList<>(raceSplits, s -> {
                return !(s.getPosition() == 1 || s.getPosition() == raceSplits.size());
            });
            raceSplitsTableView.setItems(filteredSplits);
            if (raceSplits.isEmpty()) {
                logger.debug("No Splits found, creating two...");
                // no waves. Let's create one with some default values
               Split startSplit = new Split(selectedRace);
               startSplit.setSplitName("Start");
               startSplit.setSplitDistance(BigDecimal.ZERO);
               startSplit.setSplitDistanceUnits(selectedRace.getRaceDistanceUnits());
               startSplit.setTimingLocation(TimingDAO.getInstance().listTimingLocations().get(0));
               //startSplit.setPosition(1);
               raceDAO.addSplit(startSplit);
               
               Split finishSplit = new Split(selectedRace);
               finishSplit.setSplitName("Finish");
               finishSplit.setSplitDistance(selectedRace.getRaceDistance());
               finishSplit.setSplitDistanceUnits(selectedRace.getRaceDistanceUnits());
               finishSplit.setSplitCutoff(selectedRace.getRaceCutoff());
               finishSplit.setTimingLocation(TimingDAO.getInstance().listTimingLocations().get(1));
               //finishSplit.setPosition(2);
               raceDAO.addSplit(finishSplit);
            } 
            if (raceSplits.size() > 2) {
                splitsToggleSwitch.setSelected(true); 
            } else {
                splitsToggleSwitch.setSelected(false);
            }
            splitsToggleSwitch.disableProperty().bind(Bindings.size(raceSplitsTableView.getItems()).greaterThan(0));
            
            
            startLocationComboBox.getSelectionModel().select(raceSplits.getFirst().getTimingLocation());
            finishLocationComboBox.getSelectionModel().select(raceSplits.getLast().getTimingLocation());
            //minFromLastSplitTextField.setText(DurationFormatter.durationToString(raceSplits.get(raceSplits.size()-1).splitMinTimeDuration()));
            
            if (raceSplits.getFirst().getTimingLocation().equals(raceSplits.getLast().getTimingLocation())) maxStartHBox.setVisible(true);
            else maxStartHBox.setVisible(false);
            
            maxStartTextField.setText(selectedRace.raceMaxStartProperty().getValueSafe());
            
            //Setup the start time
            raceStartTimeTextField.setText(raceWaves.getFirst().getWaveStart());
            
            
            
            
            
            // Cutoff stuff
            raceCutoffTimeTextField.setText(selectedRace.raceCutoffProperty().getValueSafe()); 
            
            updateRaceCutoffPace();
            
            // Lap race stuff
            minLapTimeTextField.setText(selectedRace.minLapTimeDurationStringProperty().getValueSafe());
            lapExitLocationComboBox.getSelectionModel().select(selectedRace.getLapExitLocation());
            lapRaceToggleSwitch.selectedProperty().set(selectedRace.getLapRace());
            
            
            //Segments
            raceSegments=selectedRace.raceSegmentsProperty();
            raceSegmentsTableView.setItems(raceSegments);
            
            segmentsVBox.managedProperty().bind(Bindings.or(
                    Bindings.size(raceSplits).greaterThanOrEqualTo(3), 
                    Bindings.size(raceSegments).greaterThanOrEqualTo(1)
            ));
            segmentsVBox.visibleProperty().bind(Bindings.or(
                    Bindings.size(raceSplits).greaterThanOrEqualTo(3), 
                    Bindings.size(raceSegments).greaterThanOrEqualTo(1)
            ));
            
            //segmentStartSplitTableColumn.setCellFactory(ComboBoxTableCell.<Segment, Split>forTableColumn(selectedRace.splitsProperty()));
//            segmentStartSplitTableColumn.setOnEditCommit((CellEditEvent<Segment, Split> t) -> {
//                Segment s = (Segment) t.getTableView().getItems().get(t.getTablePosition().getRow());
//                s.setStartSplit(t.getNewValue());
//                raceDAO.updateSegment(s);
//            });
            
            //segmentEndSplitTableColumn.setCellFactory(ComboBoxTableCell.<Segment, Split>forTableColumn(selectedRace.splitsProperty()));
//            segmentEndSplitTableColumn.setOnEditCommit((CellEditEvent<Segment, Split> t) -> {
//                Segment s = (Segment) t.getTableView().getItems().get(t.getTablePosition().getRow());
//                s.setEndSplit(t.getNewValue());
//                raceDAO.updateSegment(s);
//            });
                
            deleteSegmentButton.disableProperty().bind(raceSegmentsTableView.getSelectionModel().selectedItemProperty().isNull());
            editSegmentButton.disableProperty().bind(raceSegmentsTableView.getSelectionModel().selectedItemProperty().isNull());
            
            // Need to UN-Register the old listeners before setting up the new ones...
           deleteSplitButton.disableProperty().set(true);
           editSplitButton.disableProperty().set(true);
           //splitDistanceTableColumn.setEditable(false);
           raceSplitsTableViewListener=(obs, oldSelection, newSelection) -> {
                logger.debug("Selected splits changed... now " + newSelection);
                if (newSelection != null ) {
//                    if (newSelection.splitPositionProperty().getValue().equals(1)) {
//                        deleteSplitButton.disableProperty().set(true);
//                        splitDistanceTableColumn.setEditable(false);
//                    }
//                    else if (newSelection.splitPositionProperty().getValue().equals(raceSplitsTableView.getItems().size())) {
//                        deleteSplitButton.disableProperty().set(true);
//                        splitDistanceTableColumn.setEditable(true);
//                    }
//                    else {
                        deleteSplitButton.disableProperty().set(false);
                        editSplitButton.disableProperty().set(false);
                        //splitDistanceTableColumn.setEditable(true);
//                    }
                } else {
                    deleteSplitButton.disableProperty().set(true);
                    editSplitButton.disableProperty().set(true);
                    //splitDistanceTableColumn.setEditable(false);
                }
            };
           raceSplitsTableView.getSelectionModel().selectedItemProperty().addListener(raceSplitsTableViewListener);
           raceSplitsTableView.getSelectionModel().clearSelection();
        
            waveStartsCheckBoxListener=(arg0,  oldPropertyValue,  newPropertyValue) -> {
                if (!newPropertyValue) {
                    if (raceWaves.get(0).getWaveAssignmentMethod().equals(WaveAssignment.BIB)) {
                        endBibTextField.setText(raceWaves.get(0).getWaveAssignmentEnd());
                        startBibTextField.setText(raceWaves.get(0).getWaveAssignmentStart());
                    } else {
                        endBibTextField.setText("");
                        startBibTextField.setText("");
                    }

                    raceStartTimeTextField.setText(raceWaves.get(0).getWaveStart());
                }
            };
            waveStartsToggleSwitch.selectedProperty().addListener(waveStartsCheckBoxListener);
            
            raceWaveListener=(ListChangeListener.Change<? extends Wave> w) -> {
                raceStartTimeTextField.setText(raceWaves.get(0).getWaveStart());
            };
            raceWaves.addListener(raceWaveListener);

        
            raceSplitsListener=(ListChangeListener.Change<? extends Split> c) -> {
                logger.debug("Splits have changed");
                if (!ResultsDAO.getInstance().getResults(selectedRace.getID()).isEmpty())updateResultsButton.visibleProperty().set(true);
                
                if (raceSplits.getFirst().getTimingLocation().equals(raceSplits.getLast().getTimingLocation())) maxStartHBox.setVisible(true);
                else maxStartHBox.setVisible(false);
            };
            raceSplits.addListener(raceSplitsListener);
            
        } else {
            logger.debug("Null race, de-populate all fields out");

            // blank out everything 
            // the pane will be disabled but let's not confuse things
        }
    }
    
    public void updateRaceName(){
        selectedRace.setRaceName(raceNameTextField.getText());
        raceDAO.updateRace(selectedRace);
    }
    
//    public void updateRaceDistance(ActionEvent fxevent){
//        updateRaceDistance();
//    }
    
    public void updateRaceDistance() {
        //Do we have a parsable number?
        try {
            BigDecimal dist = new BigDecimal(raceDistanceTextField.getText());
            logger.debug("updateRaceDistance() -> {} -> {} ",raceDistanceTextField.getText(),dist.toPlainString());
            if (!dist.equals(selectedRace.getRaceDistance())) {
                // Make sure that the new distance is not shorter than the longest on-course split.
                Boolean distOK = true;
                if (selectedRace.getSplits().size() > 2){
                    int i = selectedRace.getSplits().size() - 2;
                    logger.debug("Checking distance of split #{}",i);
                    if (selectedRace.getSplits().get(i).getSplitDistance().compareTo(dist) >= 0 ){
                        distOK = false;
                        logger.debug("New distance of {} <= split distance of {}",dist,selectedRace.getSplits().get(i).getSplitDistance());
                    }
                }
                if (distOK) {
                    selectedRace.setRaceDistance(dist);
                    selectedRace.setRaceDistanceUnits(distanceUnitChoiceBox.getValue());
                    selectedRace.getSplits().getLast().setSplitDistance(dist);
                    raceDAO.updateRace(selectedRace);
                    raceDAO.updateSplit(selectedRace.getSplits().getLast());
                } else {
                    raceDistanceTextField.setText(selectedRace.getRaceDistance().toPlainString());
                    Alert alert = new Alert(AlertType.WARNING);
                    alert.setTitle("Error");
                    alert.setHeaderText("Error setting new distance.");
                    alert.setContentText("The event distance cannot be shorter than the longest on-course split.");

                    alert.showAndWait();
                }
            }
        } catch (Exception e) {
            // not a number
            BigDecimal dist = selectedRace.getRaceDistance();
            raceDistanceTextField.setText(dist.toPlainString());
        }
        updateRaceCutoffPace();
    }
    
    

    public void updateRaceStartTime(){ // really set the 1st wave's start time
        raceWaves.get(0).setWaveStart(raceStartTimeTextField.getText());
        //selectedRace.setRaceStart(raceStartTimeTextField.getText());
        //raceDAO.updateRace(selectedRace);
        raceDAO.updateWave(raceWaves.get(0));
        ResultsDAO.getInstance().reprocessWaveResults(raceWaves.get(0));
    }
    
    
    private void updateRaceStartBib() {
        
        if (!startBibTextField.getText().equals(raceWaves.get(0).getWaveAssignmentStart())){
            raceWaves.get(0).setWaveAssignmentStart(startBibTextField.getText());
            if (raceWaves.get(0).getWaveAssignmentMethod() != WaveAssignment.BIB) raceWaves.get(0).setWaveAssignmentMethod(WaveAssignment.BIB);
            raceDAO.updateWave(raceWaves.get(0));
        }
    }
    private void updateRaceEndBib() {
        if (!endBibTextField.getText().equals(raceWaves.get(0).getWaveAssignmentEnd())){
            raceWaves.get(0).setWaveAssignmentEnd(endBibTextField.getText());
            if (raceWaves.get(0).getWaveAssignmentMethod() != WaveAssignment.BIB) raceWaves.get(0).setWaveAssignmentMethod(WaveAssignment.BIB);
            raceDAO.updateWave(raceWaves.get(0));
        }
    }
    
    private void updateRaceMaxStartTime(){
        if (maxStartTextField.getText().isEmpty()) {
            selectedRace.setRaceMaxStart(Duration.ofMinutes(10).toNanos()); // 10 minute default
        } else { 
            selectedRace.setRaceMaxStart(DurationParser.parse(maxStartTextField.getText(),false).toNanos());
        }
        maxStartTextField.setText(selectedRace.raceMaxStartProperty().getValueSafe());
        raceDAO.updateRace(selectedRace); 
    }
    
    public void updateRaceCutoffTime(){
        if (raceCutoffTimeTextField.getText().isEmpty()) {
            selectedRace.setRaceCutoff(0L);
        } else { 
            selectedRace.setRaceCutoff(DurationParser.parse(raceCutoffTimeTextField.getText()).toNanos());
        }
        raceCutoffTimeTextField.setText(selectedRace.raceCutoffProperty().getValueSafe());
        raceDAO.updateRace(selectedRace);
        // caclulate the MM:SS/mi 
       updateRaceCutoffPace();
    }
    
    public void updateRaceCutoffPace(){
        if (selectedRace.getRaceCutoff().equals(0L)) {
            raceCutoffTimePaceLabel.setText("");
            return;
        }
        Pace pace;
        try {
            pace = Pace.valueOf(selectedRace.getStringAttribute("PaceDisplayFormat"));
        } catch (Exception ex) {
            pace = Pace.MPM;
        }
        pace.getPace(selectedRace.getRaceDistance().floatValue(), selectedRace.getRaceDistanceUnits(), Duration.ofNanos(selectedRace.getRaceCutoff()));
        raceCutoffTimePaceLabel.setText(pace.getPace(selectedRace.getRaceDistance().floatValue(), selectedRace.getRaceDistanceUnits(), Duration.ofNanos(selectedRace.getRaceCutoff())));
    }
    
    public void addWave(){
        Wave wave = new Wave(selectedRace);
        wave.setWaveName("Wave " + (raceWaves.size()+1));
        
        // Bib assignemnts
        Wave pw = raceWaves.get(raceWaves.size()-1);
        wave.setWaveAssignmentMethod(WaveAssignment.BIB);
        Boolean numericBibs = false;
        Integer start = 1;
        Integer end = 100;
        try{
            start = Integer.parseUnsignedInt(pw.getWaveAssignmentStart());
            end = Integer.parseUnsignedInt(pw.getWaveAssignmentEnd());
            if (start == 1) start = 0;
            Integer diff = end - start;
            start = end +1;
            end = start + diff;
            numericBibs = true;
        } catch (Exception ex){
            numericBibs = false;
        }
        if (numericBibs) {
            wave.setWaveAssignmentStart(start.toString());
            wave.setWaveAssignmentEnd(end.toString());
        }
        
        if (raceWaves.size() > 1) {
            Duration delta = Duration.between(pw.waveStartProperty(), raceWaves.get(raceWaves.size()-2).waveStartProperty()).abs();
            wave.setWaveStart(pw.waveStartProperty().plus(delta).format(DateTimeFormatter.ISO_LOCAL_TIME));
        } else wave.setWaveStart(pw.waveStartProperty().plusMinutes(5).format(DateTimeFormatter.ISO_LOCAL_TIME));
        
        
        raceDAO.addWave(wave);
    }
    
    public void editWave(){
        final Wave w = waveStartsTableView.getSelectionModel().getSelectedItem();
        
        Dialog<ButtonType> dialog = new Dialog();
        
        GridPane dialogGrid = new GridPane();
        dialogGrid.setVgap(5);
        dialogGrid.setHgap(5);
        dialogGrid.setPadding(new Insets(5));
        int row = 0;
        
        
        // Wave Name
        Label nameLabel = new Label("Wave Name");
        TextField nameTextField = new TextField(w.getWaveName());
        
        dialogGrid.add(nameLabel, 0, row);
        dialogGrid.add(nameTextField, 1, row);
        GridPane.setHalignment(nameLabel, HPos.LEFT);
        GridPane.setHalignment(nameTextField, HPos.LEFT);
        
        row++;
        
        // Starting Time
        Label startLabel = new Label("Start Time");
        TextField waveStartTextField = new TextField();
        BooleanProperty startTimeOKBooleanProperty = new SimpleBooleanProperty(false);
        
        waveStartTextField.setPromptText("HH:MM:SS[.sss]");
        
        waveStartTextField.setTextFormatter(TextFieldFormatters.getLocalTimeFormatter());
        waveStartTextField.textProperty().addListener((obs, oldVal, newVal) -> {
                try {
                    if (!newVal.isEmpty()) {
                        LocalTime.parse(newVal, DateTimeFormatter.ISO_LOCAL_TIME);
                        startTimeOKBooleanProperty.setValue(Boolean.TRUE);
                    }
                } catch (Exception e) {
                    logger.debug("Exception Bad Race Start Time (newValue: " + newVal + ")");
                    startTimeOKBooleanProperty.setValue(Boolean.FALSE);
                }
        });
        waveStartTextField.setText(w.getWaveStart());
        
        dialogGrid.add(startLabel, 0, row);
        dialogGrid.add(waveStartTextField, 1, row);
        GridPane.setHalignment(startLabel, HPos.LEFT);
        GridPane.setHalignment(waveStartTextField, HPos.LEFT);
        
        row++;
        
        // 1st Bib
        Label startBibLabel = new Label("Start Bib");
        TextField startBibTextField = new TextField();
        
        startBibTextField.setTextFormatter(TextFieldFormatters.integerFormatter(false));
        startBibTextField.setText(w.getWaveAssignmentStart());
        
        dialogGrid.add(startBibLabel, 0, row);
        dialogGrid.add(startBibTextField, 1, row);
        GridPane.setHalignment(startBibLabel, HPos.LEFT);
        GridPane.setHalignment(startBibTextField, HPos.LEFT);
        
        row++;
        
        // Last Bib
        Label endBibLabel = new Label("Start Bib");
        TextField endBibTextField = new TextField();
        
        endBibTextField.setTextFormatter(TextFieldFormatters.integerFormatter(false));
        endBibTextField.setText(w.getWaveAssignmentEnd());
        
        dialogGrid.add(endBibLabel, 0, row);
        dialogGrid.add(endBibTextField, 1, row);
        GridPane.setHalignment(endBibLabel, HPos.LEFT);
        GridPane.setHalignment(endBibTextField, HPos.LEFT);
        
        
        
        
        dialog.getDialogPane().setContent(dialogGrid);
        
        // Set the button types.
        ButtonType saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Validators
        dialog.getDialogPane().lookupButton(saveButtonType).disableProperty().bind(startTimeOKBooleanProperty.not());
        

        Optional<ButtonType> result = dialog.showAndWait();
        
        if (result.get() == saveButtonType ) {
            // Name
            w.setWaveName(nameTextField.textProperty().getValueSafe());
            // start time
            w.setWaveStart(waveStartTextField.textProperty().getValueSafe());
            // start bib
            w.setWaveAssignmentStart(startBibTextField.textProperty().getValueSafe());
            // end bib
            w.setWaveAssignmentEnd(endBibTextField.textProperty().getValueSafe());
       
            raceDAO.updateWave(w);
        }
    }
    
    public void deleteWave(){
        // Make sure the wave is not assigned toanybody first
        final Wave w = waveStartsTableView.getSelectionModel().getSelectedItem();
        
        BooleanProperty inUse = new SimpleBooleanProperty(false);
        
        ParticipantDAO.getInstance().listParticipants().forEach(x ->{
            x.getWaveIDs().forEach(rw -> {
                if (w.getID().equals(rw)) {
                    inUse.setValue(Boolean.TRUE);
                    logger.trace("Wave " + w.getWaveName() + " is in use by " + x.fullNameProperty().getValueSafe());
                }
            });
        });
        
        if (inUse.get()) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Unable to Remove Wave");
            alert.setHeaderText("Unable to remove the selected wave.");
            alert.setContentText("The wave currently has assigned runners.\nPlease assign them to a different wave before removing.");

            alert.showAndWait();
        } else {
            raceDAO.removeWave(w); 
        }
    }
    
    public void addSplit(){
        logger.debug("Adding a split...");
        Split newSplit = new Split(selectedRace);
        newSplit.setSplitName("New Split");
        newSplit.setSplitDistanceUnits(selectedRace.getRaceDistanceUnits());
        newSplit.setSplitDistance(BigDecimal.valueOf(0));
        newSplit.setTimingLocation(TimingDAO.getInstance().listTimingLocations().getLast());
        logger.debug("   SelectedItems().size = " + raceSplitsTableView.getSelectionModel().getSelectedItems().size());
        if(!raceSplitsTableView.getSelectionModel().getSelectedItems().isEmpty() ) {
            Integer pos = raceSplitsTableView.getSelectionModel().getSelectedItem().getPosition(); 
            
            logger.debug("Adding new split at position {}",pos);

            BigDecimal a = selectedRace.getSplits().get(pos-2).getSplitDistance();
            BigDecimal b = selectedRace.getSplits().get(pos-1).getSplitDistance();
            BigDecimal c = a.add( (b.subtract(a)).divide(BigDecimal.valueOf(2)) );
            logger.debug("  new split: " + a + " and " + b + " avg: " + c);
            newSplit.setSplitDistance(a.add( (b.subtract(a)).divide(BigDecimal.valueOf(2)) ) );
            newSplit.setPosition(pos);

        } else { // nothing selected... Add to the end
            int numSplits = selectedRace.getSplits().size();
            BigDecimal a = selectedRace.getSplits().get(numSplits -2).getSplitDistance();
            BigDecimal b = selectedRace.getSplits().getLast().getSplitDistance();
            BigDecimal c = a.add( (b.subtract(a)).divide(BigDecimal.valueOf(2)) );
            logger.debug("  1st split: " + a + " and " + b + " avg: " + c);
            newSplit.setSplitDistance(a.add( (b.subtract(a)).divide(BigDecimal.valueOf(2)) ) );
            newSplit.setPosition(numSplits -1); 
        }
        raceDAO.addSplit(newSplit);
    }
    
    public void deleteSplit(){
        //removeParticipants(FXCollections.observableArrayList(waveStartsTableView.getSelectionModel().getSelectedItems()));
        ObservableList<Split> deleteMe = FXCollections.observableArrayList(raceSplitsTableView.getSelectionModel().getSelectedItems());
        
        // If the split is referenced by a segment, 
        // toss up a warning and leave it alone
        final StringProperty segmentsUsing = new SimpleStringProperty();
        deleteMe.forEach(sp -> {
            sp.getRace().getSegments().forEach(s -> {
                if (s.getStartSplit().equals(sp)) segmentsUsing.set(segmentsUsing.getValueSafe() + sp.getRace().getRaceName() + " " + s.getSegmentName() + " Start\n");

                if (s.getEndSplit().equals(sp)) segmentsUsing.set(segmentsUsing.getValueSafe() +  sp.getRace().getRaceName() + " " + s.getSegmentName() + " End\n");
            });
        });
        
        if (segmentsUsing.isEmpty().get()) {
            Split s;
            Iterator<Split> deleteMeIterator = deleteMe.iterator();
            while (deleteMeIterator.hasNext()) {
                s = deleteMeIterator.next();
                raceDAO.removeSplit(s); 
            }
        } else {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Unable to Remove Split");
            alert.setHeaderText("Unable to remove the selected split");
            alert.setContentText("One or more of the selected splits is in use by the following segments:\n" + segmentsUsing.getValueSafe());

            alert.showAndWait();
        }

    }
    
    private void editSplit() {
        
        // TODO: Validation and UI cleanup
        // Max distance is < race distance
        // Error label
        // red border for invalid fields: 
        // --  theTextField.setBorder(new Border(new BorderStroke(Color.RED, BorderStrokeStyle.SOLID, new CornerRadii(3), new BorderWidths(2), new Insets(-2))));
        
        Split s = raceSplitsTableView.getSelectionModel().getSelectedItem();
        
        Dialog<ButtonType> dialog = new Dialog();
        
        GridPane dialogGrid = new GridPane();
        dialogGrid.setVgap(5);
        dialogGrid.setHgap(5);
        dialogGrid.setPadding(new Insets(5));
        int row = 0;
        
        // Split name

        Label nameLabel = new Label("Split Name");
        TextField nameTextField = new TextField(s.getSplitName());
        
        dialogGrid.add(nameLabel, 0, row);
        dialogGrid.add(nameTextField, 1, row);
        GridPane.setHalignment(nameLabel, HPos.LEFT);
        GridPane.setHalignment(nameTextField, HPos.LEFT);
        
        row++;
        
        // Timing Location

        Label timingLabel = new Label("Timing Location");
        
        ComboBox<TimingLocation> timingLocationComboBox = new ComboBox();
        timingLocationComboBox.setItems(TimingDAO.getInstance().listTimingLocations());
        timingLocationComboBox.getSelectionModel().select(s.getTimingLocation());
        
        dialogGrid.add(timingLabel, 0, row);
        dialogGrid.add(timingLocationComboBox,1, row);
        GridPane.setHalignment(timingLabel, HPos.LEFT);
        GridPane.setHalignment(timingLocationComboBox, HPos.LEFT);

        row++;
        
        
        // Split Distance
        HBox distanceHBox = new HBox();
        distanceHBox.setSpacing(5);
        
        Label distanceLabel = new Label("Cumulative Distance");
        
        TextField distanceTextField = new TextField();
        distanceTextField.setTextFormatter(TextFieldFormatters.getPositiveBigDecimalFormatter());
        
        // Split Distance must be greater than 0 and less than the race distance
        BooleanProperty distOK = new SimpleBooleanProperty(false);
        distanceTextField.textProperty().addListener((obs,oldVal,newVal) -> {
            distOK.setValue(false);
            try {
                BigDecimal dist = new BigDecimal(newVal);
                logger.trace("distOK checK:  Dist: {} zero: {}  race: {}",dist.toPlainString(),dist.compareTo(BigDecimal.ZERO),dist.compareTo(s.getRace().getRaceDistance()));
                if (dist.compareTo(BigDecimal.ZERO) > 0 && dist.compareTo(s.getRace().getRaceDistance()) < 0)
                    distOK.setValue(true);
            } catch (Exception e){
                
            }
        });
        
        distanceTextField.setText(s.getSplitDistance().toPlainString());
        Unit unit = s.getRace().getRaceDistanceUnits();
        Label distanceUnitLabel = new Label(unit.toShortString());
        
        distanceHBox.getChildren().addAll(distanceTextField,distanceUnitLabel);
        
        dialogGrid.add(distanceLabel, 0, row);
        dialogGrid.add(distanceHBox,1, row);
        GridPane.setHalignment(distanceLabel, HPos.LEFT);
        GridPane.setHalignment(distanceHBox, HPos.LEFT);

        row++;
        
        // Advanced Options Section
        Label advLabel = new Label("Advanced Options:");
        advLabel.setStyle("-fx-font-size: 18px;");
        advLabel.setPadding(new Insets(5,0,0,0));

        dialogGrid.add(advLabel,0, row);
        GridPane.setColumnSpan(advLabel, 2);
        GridPane.setHalignment(distanceLabel, HPos.LEFT);
        
        row++;
        
        
        // Min time from previous split
        

        Label splitMinTimeLabel = new Label("Minimum time from previous split: ");
        
        TextField minTimeTextField = new TextField(DurationFormatter.durationToString(s.splitMinTimeDuration()));
        minTimeTextField.setPromptText("[HH:]MM:SS");
        minTimeTextField.setPrefWidth(75);
        minTimeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            logger.trace("TextField Text Changed (newValue: " + newValue + ")");
            if (newValue.isEmpty() || newValue.matches("^[0-9]+(:?([0-5]?([0-5][0-9]?(:([0-5]?([0-5][0-9]?(\\.\\d*)?)?)?)?)?)?)?")) {
                logger.debug("Possiblely good Time (newValue: " + newValue + ")");
            } else {
                Platform.runLater(() -> {
                    int c = minTimeTextField.getCaretPosition();
                    if (oldValue.length() > newValue.length()) {
                        c++;
                    } else {
                        c--;
                    }
                    minTimeTextField.setText(oldValue);
                    minTimeTextField.positionCaret(c);
                });
                logger.debug("Bad Cutoff Time (newValue: " + newValue + ")");
            }
        });
        
        dialogGrid.add(splitMinTimeLabel, 0, row);
        dialogGrid.add(minTimeTextField,1, row);
        GridPane.setHalignment(splitMinTimeLabel, HPos.LEFT);
        GridPane.setHalignment(minTimeTextField, HPos.LEFT);

        row++;
        

        

        // Cutoff Time
        HBox cutoffHBox = new HBox();
        cutoffHBox.setSpacing(5);
        Label splitCutoffLabel = new Label("Cutoff Time to this split");

        TextField cutoffTimeTextField = new TextField(DurationFormatter.durationToString(s.splitCutoffDuration()));
        cutoffTimeTextField.setPromptText("HH:MM:SS");
        cutoffTimeTextField.setPrefWidth(75);
        cutoffTimeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            logger.trace("TextField Text Changed (newValue: " + newValue + ")");
            if (newValue.isEmpty() || newValue.matches("^[0-9]+(:?([0-5]?([0-5][0-9]?(:([0-5]?([0-5][0-9]?(\\.\\d*)?)?)?)?)?)?)?")) {
                logger.debug("Possiblely good Time (newValue: " + newValue + ")");
            } else {
                Platform.runLater(() -> {
                    int c = cutoffTimeTextField.getCaretPosition();
                    if (oldValue.length() > newValue.length()) {
                        c++;
                    } else {
                        c--;
                    }
                    cutoffTimeTextField.setText(oldValue);
                    cutoffTimeTextField.positionCaret(c);
                });
                logger.debug("Bad Cutoff Time (newValue: " + newValue + ")");
            }
        });
        ToggleSwitch absoluteToggleSwitch = new ToggleSwitch("Relative to Start");
        absoluteToggleSwitch.setSelected(s.getSplitCutoffIsRelative());
        cutoffHBox.getChildren().setAll(cutoffTimeTextField, absoluteToggleSwitch);

        dialogGrid.add(splitCutoffLabel, 0, row);
        dialogGrid.add(cutoffHBox,1, row);
        GridPane.setHalignment(splitCutoffLabel, HPos.LEFT);
        GridPane.setHalignment(cutoffHBox, HPos.LEFT);

        row++;

        
        // Ignore split time toggle

        Label ignoreLabel = new Label("Ignore Time to this split");

        ToggleSwitch ignoreToggleSwitch = new ToggleSwitch();
        ignoreToggleSwitch.setSelected(s.getIgnoreTime());

        dialogGrid.add(ignoreLabel, 0, row);
        dialogGrid.add(ignoreToggleSwitch,1, row);
        GridPane.setHalignment(ignoreLabel, HPos.LEFT);
        GridPane.setHalignment(ignoreToggleSwitch, HPos.LEFT);

        row++;

        // Mandatory toggle

        Label mandatoryLabel = new Label("Mandatory Split");

        ToggleSwitch mandatoryToggleSwitch = new ToggleSwitch();
        mandatoryToggleSwitch.setSelected(s.getMandatorySplit());

        dialogGrid.add(mandatoryLabel, 0, row);
        dialogGrid.add(mandatoryToggleSwitch,1, row);
        GridPane.setHalignment(mandatoryLabel, HPos.LEFT);
        GridPane.setHalignment(mandatoryToggleSwitch, HPos.LEFT);

        row++;

        
        dialog.getDialogPane().setContent(dialogGrid);
        
        // Set the button types.
        ButtonType saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Validators
        // SplitName must not be empty
        
        
        
        BooleanBinding valid = Bindings.and(distOK, nameTextField.textProperty().isEmpty().not());
       
        dialog.getDialogPane().lookupButton(saveButtonType).disableProperty().bind(valid.not());

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.get() == saveButtonType ) {
            
            // Split Name
            if (!nameTextField.getText().isBlank()) s.setSplitName(nameTextField.getText());
            
            // Timing Location
            s.setTimingLocation(timingLocationComboBox.getValue()); 
            
            // Cumulative Distance
            try {
                BigDecimal distance = new BigDecimal(distanceTextField.getText());
                s.setSplitDistance(distance);
            } catch (Exception e){
                logger.debug("Invalid distance text field {}",distanceTextField.getText());
            }
            
            // Min time from previous split
            if (DurationParser.parsable(minTimeTextField.getText(), Boolean.FALSE)) {
                s.setSplitMinTime(DurationParser.parse(minTimeTextField.getText(), Boolean.FALSE).toNanos());
            } else {
                logger.debug("Min Split time of " + minTimeTextField.getText() + " is not parsable!");
            }
            
            // Cutoff Time
            if (DurationParser.parsable(cutoffTimeTextField.getText(), Boolean.TRUE)) {
                s.setSplitCutoff(DurationParser.parse(cutoffTimeTextField.getText(), Boolean.TRUE).toNanos());
            }
            
            s.setSplitCutoffIsRelative(absoluteToggleSwitch.selectedProperty().getValue());

            // Mandatory Split
            s.setMandatorySplit(mandatoryToggleSwitch.selectedProperty().getValue());
            
            // Ignore Split Time
            s.setIgnoreTime(ignoreToggleSwitch.selectedProperty().getValue());
            
            raceDAO.updateSplit(s);
        }
    }
    
    
    public void addSegment(){
        Segment s = new Segment();
        s.setRace(selectedRace);
        s.setSegmentName("New Segment");
        s.setStartSplit(selectedRace.getSplits().get(0));
        s.setEndSplit(selectedRace.getSplits().get(1));
        raceDAO.updateSegment(s);
        selectedRace.addRaceSegment(s);
    }
    
    public void deleteSegment(){
        ObservableList deleteMe = FXCollections.observableArrayList(raceSegmentsTableView.getSelectionModel().getSelectedItems());
        Segment s;
        Iterator<Segment> deleteMeIterator = deleteMe.iterator();
        while (deleteMeIterator.hasNext()) {
            s = deleteMeIterator.next();
            raceDAO.removeSegment(s); 
        }
    }
    
    public void editSegment(){
        Segment s = raceSegmentsTableView.getSelectionModel().getSelectedItem();
        
        Dialog<ButtonType> dialog = new Dialog();
        
        GridPane dialogGrid = new GridPane();
        dialogGrid.setVgap(5);
        dialogGrid.setHgap(5);
        dialogGrid.setPadding(new Insets(5));
        int row = 0;
        
        // Split name

        Label nameLabel = new Label("Segment Name");
        TextField nameTextField = new TextField(s.getSegmentName());
        
        dialogGrid.add(nameLabel, 0, row);
        dialogGrid.add(nameTextField, 1, row);
        GridPane.setHalignment(nameLabel, HPos.LEFT);
        GridPane.setHalignment(nameTextField, HPos.LEFT);
        
        row++;
        
        // Segment Start Split

        Label startSplitLabel = new Label("Start Split");
        
        ComboBox<Split> startSplitLocationComboBox = new ComboBox();
        startSplitLocationComboBox.setItems(selectedRace.splitsProperty());
        startSplitLocationComboBox.getSelectionModel().select(s.getStartSplit());
        
        dialogGrid.add(startSplitLabel, 0, row);
        dialogGrid.add(startSplitLocationComboBox,1, row);
        GridPane.setHalignment(startSplitLabel, HPos.LEFT);
        GridPane.setHalignment(startSplitLocationComboBox, HPos.LEFT);

        row++;
        
        // SegmentEnd Split

        Label endSplitLabel = new Label("End Split");
        
        ComboBox<Split> endSplitLocationComboBox = new ComboBox();
        endSplitLocationComboBox.setItems(selectedRace.splitsProperty());
        endSplitLocationComboBox.getSelectionModel().select(s.getEndSplit());
        
        dialogGrid.add(endSplitLabel, 0, row);
        dialogGrid.add(endSplitLocationComboBox,1, row);
        GridPane.setHalignment(endSplitLabel, HPos.LEFT);
        GridPane.setHalignment(endSplitLocationComboBox, HPos.LEFT);

        row++;
        
        // Segment Distance
        HBox distanceHBox = new HBox();
        distanceHBox.setSpacing(5);
        
        Label distanceLabel = new Label("Segment Distance");
        
        BigDecimal segmentDistance = s.getEndSplit().getSplitDistance().subtract(s.getStartSplit().getSplitDistance()).stripTrailingZeros();
        Label distanceTextField = new Label(segmentDistance.toPlainString());
        
        Unit unit = s.getRace().getRaceDistanceUnits();
        Label distanceUnitLabel = new Label(unit.toShortString());
        
        distanceHBox.getChildren().addAll(distanceTextField,distanceUnitLabel);
        
        dialogGrid.add(distanceLabel, 0, row);
        dialogGrid.add(distanceHBox,1, row);
        GridPane.setHalignment(distanceLabel, HPos.LEFT);
        GridPane.setHalignment(distanceHBox, HPos.LEFT);
        
        // Update when we change things
        ChangeListener updateDistListener = (observable, oldValue, newValue) -> {
            BigDecimal endDist = endSplitLocationComboBox.getValue().getSplitDistance();
            BigDecimal startDist = startSplitLocationComboBox.getValue().getSplitDistance();
            distanceTextField.setText(endDist.subtract(startDist).stripTrailingZeros().abs().toPlainString());
        
        };
        endSplitLocationComboBox.valueProperty().addListener(updateDistListener);
        startSplitLocationComboBox.valueProperty().addListener(updateDistListener);

        row++;
        
        // Advanced Options Section
        Label advLabel = new Label("Advanced Options:");
        advLabel.setStyle("-fx-font-size: 18px;");
        advLabel.setPadding(new Insets(5,0,0,0));

        dialogGrid.add(advLabel,0, row);
        GridPane.setColumnSpan(advLabel, 2);
        GridPane.setHalignment(distanceLabel, HPos.LEFT);
        
        row++;
        
        
        //Hide on results
        
        Label hideLabel = new Label("Hide on Results");
        
        ToggleSwitch hideToggleSwitch = new ToggleSwitch();
        hideToggleSwitch.setSelected(s.getHidden());
        
        dialogGrid.add(hideLabel, 0, row);
        dialogGrid.add(hideToggleSwitch, 1, row);
        GridPane.setHalignment(hideLabel, HPos.LEFT);
        GridPane.setHalignment(hideToggleSwitch, HPos.LEFT);

                
        row++;

        //Pace Display
        Label paceLabel = new Label("Override Pace Display");
         
        ToggleSwitch customPaceToggleSwitch = new ToggleSwitch();
        customPaceToggleSwitch.setSelected(s.getUseCustomPace());
        ChoiceBox<Pace> paceFormatChoiceBox = new ChoiceBox();
        paceFormatChoiceBox.setItems(FXCollections.observableArrayList(Pace.values()));
        Pace p = s.getCustomPace() != null ?s.getCustomPace():Pace.MPM;
        paceFormatChoiceBox.getSelectionModel().select(p);
        
        paceFormatChoiceBox.disableProperty().bind(customPaceToggleSwitch.selectedProperty().not());

        dialogGrid.add(paceLabel, 0, row);
        dialogGrid.add(customPaceToggleSwitch, 1, row);
        GridPane.setHalignment(paceLabel, HPos.LEFT);
        GridPane.setHalignment(customPaceToggleSwitch, HPos.LEFT);
        
        row++;
        dialogGrid.add(paceFormatChoiceBox, 1, row);
        GridPane.setHalignment(paceFormatChoiceBox, HPos.LEFT);
        
        

        

        
        dialog.getDialogPane().setContent(dialogGrid);
        
        // Set the button types.
        ButtonType saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Validators
        dialog.getDialogPane().lookupButton(saveButtonType).disableProperty().bind(nameTextField.textProperty().isEmpty());
        

        Optional<ButtonType> result = dialog.showAndWait();
        
        // Unregister the listeners to prevent an NPE 
        // if somebody updates a split distance after we close the dialog
  
        endSplitLocationComboBox.valueProperty().removeListener(updateDistListener);
        startSplitLocationComboBox.valueProperty().removeListener(updateDistListener);
            
        if (result.get() == saveButtonType ) {
            // Name
            s.setSegmentName(nameTextField.textProperty().getValueSafe());
            // Start and End
            Split ss = startSplitLocationComboBox.getValue();
            Split es = endSplitLocationComboBox.getValue();
            if (ss.getSplitDistance().compareTo(es.getSplitDistance()) < 0){
                s.setStartSplit(ss);
                s.setEndSplit(es);
            } else {
                s.setStartSplit(es);
                s.setEndSplit(ss);
            }
            
            // Show on results?
            s.setHidden(hideToggleSwitch.selectedProperty().getValue());
            // Custom pace?
            s.setUseCustomPace(customPaceToggleSwitch.selectedProperty().getValue());
            // Custom Pace
            if(s.getUseCustomPace()) s.setCustomPace(paceFormatChoiceBox.getValue());
       
            raceDAO.updateSegment(s);
        }
    }
    
}
