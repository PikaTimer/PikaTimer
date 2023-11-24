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
import com.pikatimer.timing.FXMLTimingController;
import com.pikatimer.timing.Segment;
import com.pikatimer.timing.Split;
import com.pikatimer.timing.TimingLocation;
import com.pikatimer.timing.TimingDAO;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.DurationParser;
import com.pikatimer.util.Pace;
import com.pikatimer.util.Unit;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.ToggleSwitch;
import org.controlsfx.control.table.TableRowExpanderColumn;




/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLRaceDetailsController {
    private RaceDAO raceDAO; 
    
    @FXML private HBox raceNameHBox;
    @FXML private TextField raceNameTextField; 
    @FXML private TextField raceDistanceTextField; 
    @FXML private ChoiceBox distanceUnitChoiceBox; 
    @FXML private TextField raceCutoffTimeTextField;
    @FXML private Label raceCutoffTimePaceLabel; 
    @FXML private TableView<Wave> waveStartsTableView;
    @FXML private TableColumn<Wave, String> waveIDTableColumn;
    @FXML private TableColumn<Wave, String> waveNameTableColumn;
    @FXML private TableColumn<Wave, String> waveStartTimeTableColumn;                
    @FXML private TableColumn<Wave, String> waveMaxStartTimeTableColumn; 
    @FXML private TableColumn<Wave, WaveAssignment> waveAssignmentMethodTableColumn;
    @FXML private TableColumn<Wave, String> waveAssignmentStartTableColumn;
    @FXML private TableColumn<Wave, String> waveAssignmentEndTableColumn;
    @FXML private TableView<Split> raceSplitsTableView;
    @FXML private TableColumn<Split, String> splitNameTableColumn;
    @FXML private TableColumn<Split,TimingLocation> splitLocationTableColumn; 
    @FXML private TableColumn<Split, String> splitDistanceTableColumn;
    @FXML private Button deleteSplitButton;
    @FXML private CheckBox waveStartsCheckBox; 
    @FXML private HBox startTimeHBox; 
    @FXML private VBox waveStartsVBox; 
    @FXML private Button deleteWaveButton;
    @FXML private TextField raceStartTimeTextField; 
    @FXML private VBox splitsVBox;
    @FXML private CheckBox splitsCheckBox; 
    @FXML private HBox bibRangeHBox;
    @FXML private TextField startBibTextField;
    @FXML private TextField endBibTextField;
    @FXML private VBox segmentsVBox;
    @FXML private Button splitUpdateResultsButton;
    @FXML private TableView<Segment> raceSegmentsTableView;
    @FXML private TableColumn<Segment,String> segmentNameTableColumn;
    @FXML private TableColumn<Segment,Split> segmentStartSplitTableColumn;
    @FXML private TableColumn<Segment,Split> segmentEndSplitTableColumn;
    @FXML private TableColumn<Segment,String> segmentDistanceTableColumn;
    @FXML private Button deleteSegmentButton;
    @FXML private VBox startFinishLocationVBox;
    //@FXML private ChoiceBox<TimingLocation> startLocationChoiceBox;
    @FXML private ComboBox<TimingLocation> startLocationComboBox;
    //@FXML private ChoiceBox<TimingLocation>  finishLocationChoiceBox;
    @FXML private ComboBox<TimingLocation> finishLocationComboBox;
    @FXML private HBox minFinishTimeHBox;
    @FXML private ToggleButton finishToggleButton;
    @FXML private TextField minFromLastSplitTextField;
    @FXML private Button courseRecordSetupButton;
    
    
    //@FXML private Button courseRecordsButton;

    
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
        raceNameHBox.disableProperty().bind(Bindings.size(raceDAO.listRaces()).lessThanOrEqualTo(1));
        raceNameHBox.managedProperty().bind(Bindings.size(raceDAO.listRaces()).greaterThan(1));
        raceNameHBox.visibleProperty().bind(Bindings.size(raceDAO.listRaces()).greaterThan(1));
        ObservableList<Unit> unitList = FXCollections.observableArrayList(Arrays.asList(Unit.values()));
        raceWaves = FXCollections.observableArrayList(); 
        raceSegments = FXCollections.observableArrayList(); 
        //distanceUnitChoiceBox.setItems(FXCollections.observableArrayList(Arrays.asList(Unit.values()))); 
        distanceUnitChoiceBox.setItems(unitList);
        distanceUnitChoiceBox.setValue(Unit.MILES);
        
        distanceUnitChoiceBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Unit>() {
            @Override
            public void changed(ObservableValue<? extends Unit> observableValue, Unit o, Unit n) {
                System.out.println("distanceUnitChoiceBox event");
                if (!n.equals(selectedRace.getRaceDistanceUnits())){
                    System.out.println("distanceUnitChoiceBox event triggered update...");
                    selectedRace.setRaceDistanceUnits(n);
                    updateRaceCutoffPace();
                    raceDAO.updateRace(selectedRace);  
                }
            }
        });
        
        raceNameTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("raceNameTextField out focus");
                if ( ! raceNameTextField.getText().equals(selectedRace.getRaceName()) ) {
                    updateRaceName(null);
                }
            }
        });
        
        // Use this if you whant keystroke by keystroke monitoring.... Reject any non digit attempts
        raceDistanceTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                    //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
                try {
                    if (!newValue.isEmpty()) {
                        new BigDecimal(raceDistanceTextField.getText());
                        if (newValue.matches("^0*([0-9]+\\.?[0-9]*)")) {
                            Platform.runLater(() -> { 
                                int c = raceDistanceTextField.getCaretPosition();
                                raceDistanceTextField.setText(newValue.replaceFirst("^0*([0-9]+\\.?[0-9]*)", "$1"));
                                raceDistanceTextField.positionCaret(c);
                            });
                        }
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> { 
                        int c = raceDistanceTextField.getCaretPosition();
                        raceDistanceTextField.setText(oldValue);
                        raceDistanceTextField.positionCaret(c);
                    }); 
                }
                
        });
        // but only update when the textfield focus changes. 
        raceDistanceTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("raceDistanceTextField out focus");
                if ( ! raceDistanceTextField.getText().equals(selectedRace.getRaceDistance().toPlainString()) ) {
                    updateRaceDistance();
                }
            }
        });
        
        
        // Race (wave) Time stuff
        // Use this if you whant keystroke by keystroke monitoring.... Reject any non digit attempts
        raceStartTimeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                    //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
            if (newValue.matches("([3-9]|[012]:)")) {
                //Integer pos = raceStartTimeTextField.getCaretPosition();
                
                Platform.runLater(() -> {
                    raceStartTimeTextField.setText("0" + newValue);
                    raceStartTimeTextField.positionCaret(newValue.length()+2);
                });
                
            } else if (    newValue.isEmpty() || 
                    newValue.matches("([012]|[01][0-9]|2[0-3])") || 
                    newValue.matches("([01][0-9]|2[0-3]):[0-5]?") || 
                    newValue.matches("([01][0-9]|2[0-3]):[0-5][0-9]:[0-5]?") ){
                System.out.println("Possiblely good Race Cutoff Time (newValue: " + newValue + ")");
            } else if(newValue.matches("([01][0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9](\\.[0-9]*)?)?") ) { // Looks like a time, lets check
                System.out.println("Testing Race Start Time (newValue: " + newValue + ")");
            
                try {
                    if (!newValue.isEmpty()) {
                        //LocalTime.parse(raceStartTimeTextField.getText(), DateTimeFormatter.ISO_LOCAL_TIME );
                        LocalTime.parse(raceStartTimeTextField.getText(), DateTimeFormatter.ISO_LOCAL_TIME);
                    }
                } catch (Exception e) {
                    raceStartTimeTextField.setText(oldValue);
                    System.out.println("Exception Bad Race Start Time (newValue: " + newValue + ")");
                    e.printStackTrace();
                }
            } else {
                raceStartTimeTextField.setText(oldValue);
                System.out.println("Bad Race Start Time (newValue: " + newValue + ")");
            }
                
        });
        // but only update when the textfield focus changes. 
        raceStartTimeTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("raceStartTimeTextField out focus");
                try {
                    if (!raceStartTimeTextField.getText().isEmpty()) {
                        LocalTime.parse(raceStartTimeTextField.getText(), DateTimeFormatter.ISO_LOCAL_TIME );
                    }
                } catch (Exception e) {
                    raceStartTimeTextField.setText(raceWaves.get(0).getWaveStart());
                    System.out.println("Bad Race Start Time (newValue: " + raceStartTimeTextField.getText() + ")");
                }
                if ( ! raceStartTimeTextField.getText().equals(raceWaves.get(0).getWaveStart()) ) {
                    updateRaceStartTime();
                } else {
                    System.out.println("Unchaged Race Start, not saving: \"" + raceWaves.get(0).getWaveStart() + "\" vs " + raceStartTimeTextField.getText() );
                }
            } else {
                
            }
        });
        
        startTimeHBox.visibleProperty().bind(waveStartsCheckBox.selectedProperty().not());
        startTimeHBox.managedProperty().bind(waveStartsCheckBox.selectedProperty().not());
        
        // Race (wave) Time stuff
        waveNameTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        waveNameTableColumn.setOnEditCommit((CellEditEvent<Wave, String> t) -> {
            Wave w = (Wave) t.getTableView().getItems().get(t.getTablePosition().getRow());
            w.setWaveName(t.getNewValue());
            raceDAO.updateWave(w);
        });
        waveNameTableColumn.setComparator(new AlphanumericComparator());
        
        waveStartTimeTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());     
        waveStartTimeTableColumn.setOnEditCommit((CellEditEvent<Wave, String> t) -> {
            Wave w = (Wave) t.getTableView().getItems().get(t.getTablePosition().getRow());
            System.out.println("waveStartTimeTextField out focus");
            if (t.getNewValue().isEmpty()) {
                w.setWaveStart(t.getOldValue());
            }
            else if (DurationParser.parsable(t.getNewValue())) {
                Duration newD = DurationParser.parse(t.getNewValue());
                w.setWaveStart(LocalTime.MIDNIGHT.plus(newD).format(DateTimeFormatter.ISO_LOCAL_TIME));
                raceDAO.updateWave(w);
                ResultsDAO.getInstance().reprocessWaveResults(w);
            } else {
                w.setWaveStart(t.getOldValue());
            }
                
        });
                
        waveMaxStartTimeTableColumn.setCellFactory(TextFieldTableCell.forTableColumn()); 
        waveMaxStartTimeTableColumn.setOnEditCommit((CellEditEvent<Wave, String> t) -> {
            
            Wave w = (Wave) t.getTableView().getItems().get(t.getTablePosition().getRow());
            if (t.getNewValue().matches("[0-9]+") ) {
                int minutes = Integer.valueOf(t.getNewValue());
                w.setWaveMaxStart(Duration.ofSeconds(minutes * 60L).toNanos());
                raceDAO.updateWave(w); 
            } else if (t.getNewValue().matches("[0-9][0-9]*:[0-5][0-9]") ) {
                String[] split = t.getNewValue().split(":");
                int minutes = Integer.valueOf(split[0]);
                int seconds = Integer.valueOf(split[1]);
                w.setWaveMaxStart(Duration.ofSeconds(minutes * 60L + seconds).toNanos());
                raceDAO.updateWave(w);
            } else if ( t.getNewValue().isEmpty() && ! t.getOldValue().isEmpty()) {
                w.setWaveMaxStart(0L);
                raceDAO.updateWave(w);
            } else {
                t.consume();
                w.waveMaxStartStringProperty().setValue(t.getOldValue());
            }
        });
        
        // Bib start/stop for the wave
        //ObservableList<String> waveAssignmentList = FXCollections.observableArrayList(Arrays.asList(WaveAssignment.values().toString()));
        waveAssignmentMethodTableColumn.setCellFactory(ComboBoxTableCell.<Wave, WaveAssignment>forTableColumn(WaveAssignment.values()));
        waveAssignmentMethodTableColumn.setOnEditCommit((CellEditEvent<Wave, WaveAssignment> t) -> {
            Wave w = (Wave) t.getTableView().getItems().get(t.getTablePosition().getRow());
            w.setWaveAssignmentMethod(t.getNewValue());
            raceDAO.updateWave(w);
        });
        waveAssignmentStartTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        waveAssignmentStartTableColumn.setOnEditCommit((CellEditEvent<Wave, String> t) -> {
            Wave w = (Wave) t.getTableView().getItems().get(t.getTablePosition().getRow());
            w.setWaveAssignmentStart(t.getNewValue());
            raceDAO.updateWave(w);
        });
        waveAssignmentStartTableColumn.setComparator(new AlphanumericComparator());

        
        waveAssignmentEndTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        waveAssignmentEndTableColumn.setOnEditCommit((CellEditEvent<Wave, String> t) -> {
            Wave w = (Wave) t.getTableView().getItems().get(t.getTablePosition().getRow());
            w.setWaveAssignmentEnd(t.getNewValue());
            raceDAO.updateWave(w);
        });
        waveAssignmentEndTableColumn.setComparator(new AlphanumericComparator());
        
        
        
        
        // Use this if you whant keystroke by keystroke monitoring.... Reject any non digit attempts
//        raceCutoffTimeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
//            if ( newValue.isEmpty() || newValue.matches("([0-9]*|[0-9]*:[0-5]?)") ) {
//                System.out.println("Possiblely good Race Cutoff Time (newValue: " + newValue + ")");
//            } else if(newValue.matches("[0-9]*:[0-5][0-9]") ) { // Looks like a HH:MM time, lets check
//                System.out.println("Looks like a valid Race Cutoff Time (newValue: " + newValue + ")");
//            } else {
//                raceCutoffTimeTextField.setText(oldValue);
//                System.out.println("Bad Race Cutoff Time (newValue: " + newValue + ")");
//            }
//                
//        });
        raceCutoffTimeTextField.setPromptText("HH:MM:SS");
        raceCutoffTimeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                    //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
            

            if ( newValue.isEmpty() || newValue.matches("^[0-9]+(:?([0-5]?([0-5][0-9]?(:([0-5]?([0-5][0-9]?(\\.\\d*)?)?)?)?)?)?)?") ){
                System.out.println("Possiblely good Time (newValue: " + newValue + ")");
            } else {
                Platform.runLater(() -> {
                    int c = raceCutoffTimeTextField.getCaretPosition();
                    if (oldValue.length() > newValue.length()) c++;
                    else c--;
                    raceCutoffTimeTextField.setText(oldValue);
                    raceCutoffTimeTextField.positionCaret(c);
                });
                System.out.println("Bad Cutoff Time (newValue: " + newValue + ")");
            }
                
        });
        // but only update when the textfield focus changes. 
        raceCutoffTimeTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("raceCutoffTimeTextField out focus");

                if ( ! raceCutoffTimeTextField.getText().equals(selectedRace.raceCutoffProperty().getValueSafe()) ) {
                    if (DurationParser.parsable(raceCutoffTimeTextField.getText()) || raceCutoffTimeTextField.getText().isEmpty() ) {
                        updateRaceCutoffTime(); 
                    } else {
                        System.out.println("raceCutoffTimeTextField out focus with bad time, reverting to " + selectedRace.raceCutoffProperty().getValueSafe());
                        raceCutoffTimeTextField.setText(selectedRace.raceCutoffProperty().getValueSafe());
                    }
                } else {
                    System.out.println("Unchaged Cutoff time, not saving: \"" + selectedRace.raceCutoffProperty().getValueSafe() + "\" vs " + raceCutoffTimeTextField.getText() );
                }
            } else {
                
            }
        });
        
        // Start/Finish Stuff
        startLocationComboBox.setItems(TimingDAO.getInstance().listTimingLocations());
        startLocationComboBox.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends TimingLocation> observableValue, TimingLocation o, TimingLocation n) -> {
            System.out.println("startLocationChoiceBox event");
            Split s = selectedRace.getSplits().get(0);
            if (s.getTimingLocation().equals(n)) return;
            s.setTimingLocation(n);
            raceDAO.updateSplit(s);
        });
        
        finishLocationComboBox.setItems(TimingDAO.getInstance().listTimingLocations());
        finishLocationComboBox.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends TimingLocation> observableValue, TimingLocation o, TimingLocation n) -> {
            System.out.println("startLocationChoiceBox event");
            Split s = selectedRace.getSplits().get(selectedRace.getSplits().size()-1);
            if (s.getTimingLocation().equals(n)) return;
            s.setTimingLocation(n);
            raceDAO.updateSplit(s);
        });
        
        // Split table stuff
        splitNameTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        splitNameTableColumn.setOnEditCommit((CellEditEvent<Split, String> t) -> {
            Split w = (Split) t.getTableView().getItems().get(t.getTablePosition().getRow());
            w.setSplitName(t.getNewValue());
            raceDAO.updateSplit(w);
        });
        
        splitLocationTableColumn.setCellFactory(ComboBoxTableCell.<Split, TimingLocation>forTableColumn(TimingDAO.getInstance().listTimingLocations()));
        splitLocationTableColumn.setOnEditCommit((CellEditEvent<Split, TimingLocation> t) -> {
            Split s = (Split) t.getTableView().getItems().get(t.getTablePosition().getRow());
            s.setTimingLocation(t.getNewValue());
            raceDAO.updateSplit(s);
        });
        
        splitDistanceTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        splitDistanceTableColumn.setOnEditCommit((CellEditEvent<Split, String> t) -> {
            BigDecimal dist;
            Split s = t.getRowValue();
            //Split s = (Split) t.getTableView().getItems().get(t.getTablePosition().getRow());
            try {
                dist = new BigDecimal(t.getNewValue().replaceAll("[^\\.0123456789]",""));
                s.setSplitDistance(dist);
                if (s.getPosition().equals(s.getRace().getSplits().size())) {
                    //we are the last split
                    s.getRace().setRaceDistance(dist);
                    raceDistanceTextField.setText(dist.toString());
                    raceDAO.updateRace(s.getRace());
                }
                raceDAO.updateSplit(s);
            } catch (Exception e) {
                // not a number
                s.setSplitDistance(s.getSplitDistance());
            }
        });
        splitDistanceTableColumn.setComparator(new AlphanumericComparator());
        
//        Label splitCutoffTableColumnLabel = new Label("Cutoff");
//        splitCutoffTableColumnLabel.setTooltip(new Tooltip("Optional Cutoff time in HH:MM"));
//        splitCutoffTableColumn.setGraphic(splitCutoffTableColumnLabel);
//        splitCutoffTableColumn.setText("");
//        splitCutoffTableColumn.setCellFactory(TextFieldTableCell.forTableColumn()); 
//        splitCutoffTableColumn.setOnEditCommit((CellEditEvent<Split, String> t) -> {
//            Split s = (Split) t.getTableView().getItems().get(t.getTablePosition().getRow());
//            if (t.getNewValue().matches("[0-9]+") ) {
//                int hours = Integer.valueOf(t.getNewValue());
//                s.setSplitCutoff(Duration.ofSeconds(hours * 3600L).toNanos());
//                raceDAO.updateSplit(s); 
//            } else if (t.getNewValue().matches("[0-9][0-9]*:[0-5][0-9]") ) {
//                String[] split = t.getNewValue().split(":");
//                int hours = Integer.valueOf(split[0]);
//                int minutes = Integer.valueOf(split[1]);
//                s.setSplitCutoff(Duration.ofSeconds(hours * 3600L + minutes * 60L).toNanos());
//                raceDAO.updateSplit(s);
//            } else if ( t.getNewValue().isEmpty() && ! t.getOldValue().isEmpty()) {
//                s.setSplitCutoff(0L);
//                raceDAO.updateSplit(s);
//            } else {
//                t.consume();
//                s.splitCutoffStringProperty().setValue(t.getOldValue());
//            }
//        });
        
        startBibTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            startBibTextField.setText(newValue.replaceFirst("^[ 0]*", "").replaceFirst(" *$", ""));
        });
        startBibTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("startBibTextField out focus");
                updateRaceStartBib();
            }
        });
        
        endBibTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            endBibTextField.setText(newValue.replaceFirst("^[ 0]*", "").replaceFirst(" *$", ""));
        });
        endBibTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("endBibTestField out focus");
                updateRaceEndBib();
            }
        });
        

        
        splitUpdateResultsButton.visibleProperty().set(false);
        
        splitUpdateResultsButton.setOnAction((event) -> {
            ResultsDAO.getInstance().reprocessRaceResults(selectedRace);
            splitUpdateResultsButton.visibleProperty().set(false);
        });
        
        
        // Segment table stuff
        raceSegmentsTableView.setPlaceholder(new Label("No race segments have been defined yet"));
        
        segmentNameTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        segmentNameTableColumn.setOnEditCommit((CellEditEvent<Segment, String> t) -> {
            Segment s = (Segment) t.getTableView().getItems().get(t.getTablePosition().getRow());
            s.setSegmentName(t.getNewValue());
            raceDAO.updateSegment(s);
        });
        
        segmentDistanceTableColumn.setCellValueFactory(new PropertyValueFactory<>("distanceString"));
        
        // Advanced Split Dropdown Stuff
        TableRowExpanderColumn<Split> advancedSplitOptionsTableRowExpanderColumn = new TableRowExpanderColumn<>(param -> {
            Split s = param.getValue();
            if (s == null) return new Label("");
            
            
            if (s.getPosition() == 1 ) { // start split
                Label errorLabel = new Label("There are no advanced options for the start split");
                return errorLabel;
            } 
            
            Integer colWidth = 200;
            VBox editor = new VBox();
            editor.setSpacing(2);
            editor.setPadding(new Insets(0,0,5,0));
            
            Label advLabel = new Label("Advanced Options:");
            advLabel.setStyle("-fx-font-size: 14px;");
            advLabel.setPrefWidth(200);
            

            // Min time from previous 
            // If NOT Start or Finish
            HBox minTimeHBox = new HBox();
            minTimeHBox.setSpacing(5);
            Label splitMinTimeLabel = new Label("Minimum time from previous split: ");
            splitMinTimeLabel.setPrefWidth(colWidth);
            TextField minTimeTextField = new TextField(DurationFormatter.durationToString(s.splitMinTimeDuration()));
            minTimeTextField.setPromptText("[HH:]MM:SS");
            minTimeTextField.setPrefWidth(75);
            minTimeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
                if ( newValue.isEmpty() || newValue.matches("^[0-9]+(:?([0-5]?([0-5][0-9]?(:([0-5]?([0-5][0-9]?(\\.\\d*)?)?)?)?)?)?)?") ){
                    System.out.println("Possiblely good Time (newValue: " + newValue + ")");
                } else {
                    Platform.runLater(() -> {
                        int c = minTimeTextField.getCaretPosition();
                        if (oldValue.length() > newValue.length()) c++;
                        else c--;
                        minTimeTextField.setText(oldValue);
                        minTimeTextField.positionCaret(c);
                    });
                    System.out.println("Bad Cutoff Time (newValue: " + newValue + ")");
                }
            });
            minTimeHBox.getChildren().setAll(splitMinTimeLabel,minTimeTextField);
            
            Button save = new Button("Save");
            save.setOnAction(event -> {
                // Min Time Time
                if (DurationParser.parsable(minTimeTextField.getText(),Boolean.FALSE))
                    s.setSplitMinTime(DurationParser.parse(minTimeTextField.getText(),Boolean.FALSE).toNanos());
                else System.out.println("Min Split time of " +minTimeTextField.getText() + " is not parsable!");
                
                raceDAO.updateSplit(s);
                param.toggleExpanded();
            });
            
            editor.getChildren().addAll(advLabel,minTimeHBox,save);
            
            if (s.getPosition() != s.getRace().getSplits().size()) {

            // Cutoff Time
            // If NOT Start or Finish
            HBox cutoffHBox = new HBox();
            cutoffHBox.setSpacing(5);
            Label splitCutoffLabel = new Label("Cutoff Time to this split");
            splitCutoffLabel.setPrefWidth(colWidth);
            TextField cutoffTimeTextField = new TextField(DurationFormatter.durationToString(s.splitCutoffDuration()));
            cutoffTimeTextField.setPromptText("HH:MM:SS");
            cutoffTimeTextField.setPrefWidth(75);
            cutoffTimeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
                if ( newValue.isEmpty() || newValue.matches("^[0-9]+(:?([0-5]?([0-5][0-9]?(:([0-5]?([0-5][0-9]?(\\.\\d*)?)?)?)?)?)?)?") ){
                    System.out.println("Possiblely good Time (newValue: " + newValue + ")");
                } else {
                    Platform.runLater(() -> {
                        int c = cutoffTimeTextField.getCaretPosition();
                        if (oldValue.length() > newValue.length()) c++;
                        else c--;
                        cutoffTimeTextField.setText(oldValue);
                        cutoffTimeTextField.positionCaret(c);
                    });
                    System.out.println("Bad Cutoff Time (newValue: " + newValue + ")");
                }
            });
            ToggleSwitch absoluteToggleSwitch = new ToggleSwitch("Relative to Start");
            absoluteToggleSwitch.setSelected(s.getSplitCutoffIsRelative());
            cutoffHBox.getChildren().setAll(splitCutoffLabel,cutoffTimeTextField,absoluteToggleSwitch);
            
            colWidth = 130;
            
            // Ignore split time toggle
            // If NOT Start or Finish
            HBox ignoreHBox = new HBox();
            ignoreHBox.setSpacing(5);
            Label ignoreLabel = new Label("Ignore Time to this split");
            ignoreLabel.setPrefWidth(colWidth);
            ToggleSwitch ignoreToggleSwitch = new ToggleSwitch();
            ignoreToggleSwitch.setSelected(s.getIgnoreTime());
            ignoreHBox.getChildren().setAll(ignoreLabel,ignoreToggleSwitch);
            
            
            // Mandatory toggle
            // If NOT Start or Finish
            HBox mandatoryHBox = new HBox();
            mandatoryHBox.setSpacing(5);
            Label mandatoryLabel = new Label("Mandatory Split");
            mandatoryLabel.setPrefWidth(colWidth);
            ToggleSwitch mandatoryToggleSwitch = new ToggleSwitch();
            mandatoryToggleSwitch.setSelected(s.getMandatorySplit());
            mandatoryHBox.getChildren().setAll(mandatoryLabel,mandatoryToggleSwitch);

            
            
            
            save.setOnAction(event -> {
                // Min Time Time
                if (DurationParser.parsable(minTimeTextField.getText(),Boolean.FALSE))
                    s.setSplitMinTime(DurationParser.parse(minTimeTextField.getText(),Boolean.FALSE).toNanos());
                else System.out.println("Min Split time of " +minTimeTextField.getText() + " is not parsable!");
                // Cutoff Time
                if (DurationParser.parsable(cutoffTimeTextField.getText(),Boolean.TRUE))
                    s.setSplitCutoff(DurationParser.parse(cutoffTimeTextField.getText(),Boolean.TRUE).toNanos());
                s.setSplitCutoffIsRelative(absoluteToggleSwitch.selectedProperty().getValue());
                
                s.setMandatorySplit(mandatoryToggleSwitch.selectedProperty().getValue());
                s.setIgnoreTime(ignoreToggleSwitch.selectedProperty().getValue());

                
                raceDAO.updateSplit(s);
                param.toggleExpanded();
            });
            
            editor.getChildren().setAll(advLabel,minTimeHBox,cutoffHBox,mandatoryHBox,ignoreHBox,save);
            }
            return editor;
        });
        advancedSplitOptionsTableRowExpanderColumn.setMinWidth(50);
        advancedSplitOptionsTableRowExpanderColumn.setPrefWidth(50);
        advancedSplitOptionsTableRowExpanderColumn.setMaxWidth(50);
        advancedSplitOptionsTableRowExpanderColumn.setResizable(false);
        advancedSplitOptionsTableRowExpanderColumn.setText("Adv");
        raceSplitsTableView.getColumns().add(advancedSplitOptionsTableRowExpanderColumn);
        
        // Advanced Segment Stuff
        TableRowExpanderColumn<Segment> advancedSegmentOptionsTableRowExpanderColumn = new TableRowExpanderColumn<>(param -> {
            Segment s = param.getValue();
            
            VBox editor = new VBox();
            
            Integer colWidth = 120;
            
            // Intro Label
            Label advLabel = new Label("Advanced Options:");
            advLabel.setStyle("-fx-font-size: 14px;");
            advLabel.setPrefWidth(200);
            
            //Hide on results
            HBox hideHBox = new HBox();
            hideHBox.setSpacing(5);
            Label hideLabel = new Label("Hide on Results");
            hideLabel.setPrefWidth(colWidth);
            ToggleSwitch hideToggleSwitch = new ToggleSwitch();
            hideToggleSwitch.setSelected(s.getHidden());
            hideHBox.getChildren().setAll(hideLabel,hideToggleSwitch);
            
            //Pace Display
            HBox paceHBox = new HBox();
            paceHBox.setSpacing(5);
            Label paceLabel = new Label("Override Pace Display");
            paceLabel.setPrefWidth(colWidth);
            ToggleSwitch customPaceToggleSwitch = new ToggleSwitch();
            customPaceToggleSwitch.setSelected(s.getUseCustomPace());
            ChoiceBox<Pace> paceFormatChoiceBox = new ChoiceBox();
            paceFormatChoiceBox.setItems(FXCollections.observableArrayList(Pace.values()));
            paceFormatChoiceBox.getSelectionModel().select(Pace.MPM);
            if (s.getUseCustomPace()) paceFormatChoiceBox.getSelectionModel().select(s.getCustomPace());
            paceFormatChoiceBox.visibleProperty().bind(customPaceToggleSwitch.selectedProperty());

            paceHBox.getChildren().setAll(paceLabel,customPaceToggleSwitch,paceFormatChoiceBox);
            
            Button save = new Button("Save");
            save.setOnAction(event -> {

                // Hidden
                s.setHidden(hideToggleSwitch.selectedProperty().getValue());
                
                // Custom Pace Display
                s.setUseCustomPace(customPaceToggleSwitch.selectedProperty().getValue());
                if (customPaceToggleSwitch.selectedProperty().getValue())
                    s.setCustomPace(paceFormatChoiceBox.getSelectionModel().getSelectedItem());

                
                raceDAO.updateSegment(s);
                param.toggleExpanded();
            });
            
            editor.getChildren().addAll(advLabel,hideHBox,paceHBox,save);
            
            return editor;
            
        });
        advancedSegmentOptionsTableRowExpanderColumn.setMinWidth(50);
        advancedSegmentOptionsTableRowExpanderColumn.setPrefWidth(50);
        advancedSegmentOptionsTableRowExpanderColumn.setMaxWidth(50);
        advancedSegmentOptionsTableRowExpanderColumn.setResizable(false);
        advancedSegmentOptionsTableRowExpanderColumn.setText("Adv");
        raceSegmentsTableView.getColumns().add(advancedSegmentOptionsTableRowExpanderColumn);
        
        minFinishTimeHBox.visibleProperty().bind(finishToggleButton.selectedProperty());
        minFinishTimeHBox.managedProperty().bind(finishToggleButton.selectedProperty());
        minFromLastSplitTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
            if ( newValue.isEmpty() || newValue.matches("^[0-9]+(:?([0-5]?([0-5][0-9]?(:([0-5]?([0-5][0-9]?(\\.\\d*)?)?)?)?)?)?)?") ){
                System.out.println("Possiblely good Time (newValue: " + newValue + ")");
            } else {
                Platform.runLater(() -> {
                    int c = minFromLastSplitTextField.getCaretPosition();
                    if (oldValue.length() > newValue.length()) c++;
                    else c--;
                    minFromLastSplitTextField.setText(oldValue);
                    minFromLastSplitTextField.positionCaret(c);
                });
                System.out.println("Bad Cutoff Time (newValue: " + newValue + ")");
            }
        });
        minFromLastSplitTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            
            if (!newPropertyValue) {
                Split s = raceSplits.get(raceSplits.size()-1);
                System.out.println("minFromLastSplitTextField out focus");
                
                if (!minFromLastSplitTextField.getText().isEmpty() && DurationParser.parsable(minFromLastSplitTextField.getText(),Boolean.FALSE))
                    s.setSplitMinTime(DurationParser.parse(minFromLastSplitTextField.getText(),Boolean.FALSE).toNanos());
                else if (minFromLastSplitTextField.getText().isEmpty()){
                    s.setSplitMinTime(Duration.ZERO.toNanos()); // empty is stored as zero which is treated as 5 minutes
                }
                else {
                    minFromLastSplitTextField.setText(DurationFormatter.durationToString(s.splitMinTimeDuration()));
                    System.out.println("Min Split time of " +minFromLastSplitTextField.getText() + " is not parsable!");
                }
                raceDAO.updateSplit(s);
            } else {
                // don't do anything
            }
        });
        
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
                Logger.getLogger(FXMLTimingController.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        
        
        });

    }    
    
    public void selectRace(Race r) {
        
        if (selectedRace != null) {
            
            //Unbind any existing listeners to the table views or check boxes
            raceSplitsTableView.getSelectionModel().selectedItemProperty().removeListener(raceSplitsTableViewListener);
            waveStartsCheckBox.selectedProperty().removeListener(waveStartsCheckBoxListener);
            raceSplits.removeListener(raceSplitsListener);
            raceWaves.removeListener(raceWaveListener);
            
        }
        
        selectedRace = r;
        
        if (selectedRace != null) {
            System.out.println("Non-Null race, populate all fields out");
            //Setup the Race Name
            raceNameTextField.setText(selectedRace.getRaceName());
            if(raceNameTextField.disableProperty().get()) {
                raceNameTextField.requestFocus();
            } else {
                raceStartTimeTextField.requestFocus(); 
            }
            
            //Setup the distance
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
           
            deleteWaveButton.disableProperty().bind(Bindings.or(
                waveStartsTableView.getSelectionModel().selectedItemProperty().isNull(),
                Bindings.size(waveStartsTableView.getItems()).lessThan(2))
            );
            waveStartsVBox.managedProperty().bind(waveStartsCheckBox.selectedProperty());
            waveStartsVBox.visibleProperty().bind(waveStartsCheckBox.selectedProperty());
            bibRangeHBox.managedProperty().bind(Bindings.and(
                    Bindings.size(raceDAO.listRaces()).greaterThanOrEqualTo(2), 
                    waveStartsCheckBox.selectedProperty().not()
            ));
            bibRangeHBox.visibleProperty().bind(Bindings.and(
                    Bindings.size(raceDAO.listRaces()).greaterThanOrEqualTo(2), 
                    waveStartsCheckBox.selectedProperty().not()
            ));
                    
            // if we have more than one wave then let's set the waveStartsCheckBox to true.
            if (raceWaves.size() > 1) {
                waveStartsCheckBox.setSelected(true); 
            } else {
                waveStartsCheckBox.setSelected(false);
            }
            waveStartsCheckBox.disableProperty().bind(Bindings.size(waveStartsTableView.getItems()).greaterThan(1));
            //Setup the start time
            raceStartTimeTextField.setText(raceWaves.get(0).getWaveStart());
            
            if (raceWaves.get(0).getWaveAssignmentMethod().equals(WaveAssignment.BIB)) {
                endBibTextField.setText(raceWaves.get(0).getWaveAssignmentEnd());
                startBibTextField.setText(raceWaves.get(0).getWaveAssignmentStart());
            } else {
                endBibTextField.setText("");
                startBibTextField.setText("");
            }
        
            
            
            splitsVBox.managedProperty().bind(splitsCheckBox.selectedProperty());
            splitsVBox.visibleProperty().bind(splitsCheckBox.selectedProperty());
            
            raceSplits=selectedRace.splitsProperty(); 
            FilteredList<Split> filteredSplits = new FilteredList<>(raceSplits, s -> {
                return !(s.getPosition() == 1 || s.getPosition() == raceSplits.size());
            });
            raceSplitsTableView.setItems(filteredSplits);
            if (raceSplits.isEmpty()) {
                System.out.println("No Splits found, creating two...");
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
                splitsCheckBox.setSelected(true); 
            } else {
                splitsCheckBox.setSelected(false);
            }
            splitsCheckBox.disableProperty().bind(Bindings.size(raceSplitsTableView.getItems()).greaterThan(2));
            
            
            startLocationComboBox.getSelectionModel().select(raceSplits.get(0).getTimingLocation());
            finishLocationComboBox.getSelectionModel().select(raceSplits.get(raceSplits.size()-1).getTimingLocation());
            minFromLastSplitTextField.setText(DurationFormatter.durationToString(raceSplits.get(raceSplits.size()-1).splitMinTimeDuration()));
            
            
            //Setup the start time
            raceStartTimeTextField.setText(raceWaves.get(0).getWaveStart());
            
            
            
            
            
            // Cutoff stuff
            raceCutoffTimeTextField.setText(selectedRace.raceCutoffProperty().getValueSafe()); 
            
            updateRaceCutoffPace();
            
            
            
            
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
            
            segmentStartSplitTableColumn.setCellFactory(ComboBoxTableCell.<Segment, Split>forTableColumn(selectedRace.splitsProperty()));
            segmentStartSplitTableColumn.setOnEditCommit((CellEditEvent<Segment, Split> t) -> {
                Segment s = (Segment) t.getTableView().getItems().get(t.getTablePosition().getRow());
                s.setStartSplit(t.getNewValue());
                raceDAO.updateSegment(s);
            });
            
            segmentEndSplitTableColumn.setCellFactory(ComboBoxTableCell.<Segment, Split>forTableColumn(selectedRace.splitsProperty()));
            segmentEndSplitTableColumn.setOnEditCommit((CellEditEvent<Segment, Split> t) -> {
                Segment s = (Segment) t.getTableView().getItems().get(t.getTablePosition().getRow());
                s.setEndSplit(t.getNewValue());
                raceDAO.updateSegment(s);
            });
                
            deleteSegmentButton.disableProperty().bind(raceSegmentsTableView.getSelectionModel().selectedItemProperty().isNull());
            
            // Need to UN-Register the old listeners before setting up the new ones...
           deleteSplitButton.disableProperty().set(true);
           splitDistanceTableColumn.setEditable(false);
           raceSplitsTableViewListener=(obs, oldSelection, newSelection) -> {
                System.out.println("Selected splits changed... now " + newSelection);
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
                        splitDistanceTableColumn.setEditable(true);
//                    }
                } else {
                    deleteSplitButton.disableProperty().set(true);
                    splitDistanceTableColumn.setEditable(false);
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
            waveStartsCheckBox.selectedProperty().addListener(waveStartsCheckBoxListener);
            
            raceWaveListener=(ListChangeListener.Change<? extends Wave> w) -> {
                raceStartTimeTextField.setText(raceWaves.get(0).getWaveStart());
            };
            raceWaves.addListener(raceWaveListener);

        
            raceSplitsListener=(ListChangeListener.Change<? extends Split> c) -> {
                System.out.println("Splits have changed");
                if (ResultsDAO.getInstance().getResults(selectedRace.getID()).size() > 0)splitUpdateResultsButton.visibleProperty().set(true);
            };
            raceSplits.addListener(raceSplitsListener);
            
        } else {
            System.out.println("Null race, de-populate all fields out");

            // blank out everything 
            // the pane will be disabled but let's not confuse things
        }
    }
    
    public void updateRaceName(ActionEvent fxevent){
        selectedRace.setRaceName(raceNameTextField.getText());
        raceDAO.updateRace(selectedRace);
    }
    
    public void updateRaceDistance(ActionEvent fxevent){
        updateRaceDistance();
    }
    
    public void updateRaceDistance() {
        //Do we have a parsable number?
        BigDecimal dist;
        try {
            dist = new BigDecimal(raceDistanceTextField.getText());
            if (!dist.equals(selectedRace.getRaceDistance())) {
                selectedRace.setRaceDistance(dist);
                selectedRace.setRaceDistanceUnits((Unit)distanceUnitChoiceBox.getValue());
                selectedRace.getSplits().get(selectedRace.getSplits().size()-1).setSplitDistance(dist);
                raceDAO.updateRace(selectedRace);
                raceDAO.updateSplit(selectedRace.getSplits().get(selectedRace.getSplits().size()-1));
            }
        } catch (Exception e) {
            // not a number
            dist = selectedRace.getRaceDistance();
            raceDistanceTextField.setText(dist.toPlainString());
        }
        updateRaceCutoffPace();
    }
    
//    public void updateRaceCutoffTime(){
//        selectedRace.setRaceCutoff(raceCutoffTimeTextField.getText());
//        raceDAO.updateRace(selectedRace);
//    }
    
    public void updateRaceStartTime(ActionEvent fxevent){
        //updateRaceStartTime();
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
    
    public void updateRaceCutoffTime(ActionEvent fxevent){
        updateRaceCutoffTime();
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
    
    public void addWave(ActionEvent fxevent){
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
    
    public void deleteWave(ActionEvent fxevent){
        // Make sure the wave is not assigned toanybody first
        final Wave w = waveStartsTableView.getSelectionModel().getSelectedItem();
        
        BooleanProperty inUse = new SimpleBooleanProperty(false);
        
        ParticipantDAO.getInstance().listParticipants().forEach(x ->{
            x.getWaveIDs().forEach(rw -> {
                if (w.getID().equals(rw)) {
                    inUse.setValue(Boolean.TRUE);
                    //System.out.println("Wave " + w.getWaveName() + " is in use by " + x.fullNameProperty().getValueSafe());
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
    
    public void addSplit(ActionEvent fxevent){
        System.out.println("Adding a split...");
        Split newSplit = new Split(selectedRace);
        newSplit.setSplitName("New Split");
        newSplit.setSplitDistanceUnits(selectedRace.getRaceDistanceUnits());
        newSplit.setSplitDistance(BigDecimal.valueOf(0));
        newSplit.setTimingLocation(TimingDAO.getInstance().listTimingLocations().get(1));
        System.out.println("   SelectedItems().size = " + raceSplitsTableView.getSelectionModel().getSelectedItems().size());
        if(raceSplitsTableView.getSelectionModel().getSelectedItems().size()> 0 ) {
            Integer pos = raceSplitsTableView.getSelectionModel().getSelectedItem().getPosition() +1; 
            //pos++; //adjust for the hidden start split
            System.out.println("   pos is now " + pos);
            if (pos > 1) {
                BigDecimal a = selectedRace.getSplits().get(pos-2).getSplitDistance();
                BigDecimal b = selectedRace.getSplits().get(pos-1).getSplitDistance();
                BigDecimal c = a.add( (b.subtract(a)).divide(BigDecimal.valueOf(2)) );
                System.out.println("  new split: " + a + " and " + b + " avg: " + c);
                newSplit.setSplitDistance(a.add( (b.subtract(a)).divide(BigDecimal.valueOf(2)) ) );
                newSplit.setPosition(pos);
            }
        } else { // nothing selected... Add to the end
            BigDecimal a = selectedRace.getSplits().get(0).getSplitDistance();
                BigDecimal b = selectedRace.getSplits().get(1).getSplitDistance();
                BigDecimal c = a.add( (b.subtract(a)).divide(BigDecimal.valueOf(2)) );
                System.out.println("  1st split: " + a + " and " + b + " avg: " + c);
                newSplit.setSplitDistance(a.add( (b.subtract(a)).divide(BigDecimal.valueOf(2)) ) );
                newSplit.setPosition(2); // 1st split after start
        }
        raceDAO.addSplit(newSplit);
    }
    
    public void deleteSplit(ActionEvent fxevent){
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
    
    
    public void addSegment(ActionEvent fxevent){
        Segment s = new Segment();
        s.setRace(selectedRace);
        s.setSegmentName("New Segment");
        s.setStartSplit(selectedRace.getSplits().get(0));
        s.setEndSplit(selectedRace.getSplits().get(1));
        raceDAO.updateSegment(s);
        selectedRace.addRaceSegment(s);
    }
    
    public void deleteSegment(ActionEvent fxevent){
        ObservableList deleteMe = FXCollections.observableArrayList(raceSegmentsTableView.getSelectionModel().getSelectedItems());
        Segment s;
        Iterator<Segment> deleteMeIterator = deleteMe.iterator();
        while (deleteMeIterator.hasNext()) {
            s = deleteMeIterator.next();
            raceDAO.removeSegment(s); 
        }
    }
}
