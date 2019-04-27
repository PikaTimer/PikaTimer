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
package com.pikatimer.timing;

import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Race;
import com.pikatimer.race.Wave;
import com.pikatimer.results.ResultsDAO;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.DurationParser;
import com.pikatimer.util.DurationStringConverter;
import com.pikatimer.util.WaveStringConverter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import static javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ChoiceBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.PrefixSelectionComboBox;
import org.controlsfx.control.ToggleSwitch;

/**
 * FXML Controller class
 *
 * @author jcgarner
 */

public class FXMLTimingController {
    @FXML private VBox timingVBox;
    @FXML private ListView<TimingLocation> timingLocListView;
    @FXML private Button timingLocRemoveAllButton;
    @FXML private Button timingLocAddButton;
    @FXML private Button timingLocRemoveButton;  
    
    @FXML private VBox timingLocVBox;
    @FXML private TextField timingLocationNameTextField; 
    
    @FXML private TextField filterStartTextField;
    @FXML private TextField filterEndTextField; 
            
    @FXML private TextField searchTextBox;
    @FXML private CheckComboBox<TimingLocation> searchScopeCheckComboBox;
    @FXML private Label listSizeLabel;
    @FXML private Label filteredSizeLabel;
    @FXML private TableView<CookedTimeData> timeTableView;
    @FXML private TableColumn bibColumn;
    @FXML private TableColumn<CookedTimeData,String> chipColumn;
    @FXML private TableColumn timeColumn;
    @FXML private TableColumn<CookedTimeData,String> nameColumn;
    @FXML private TableColumn<CookedTimeData,String> locationColumn;
    @FXML private TableColumn<CookedTimeData,String> inputColumn;
    @FXML private TableColumn<CookedTimeData,Boolean> backupColumn;
    @FXML private TableColumn<CookedTimeData,Boolean> ignoreColumn;
    @FXML private CheckBox customChipBibCheckBox;
    
    @FXML private ListView<TimeOverride> overrideListView;
//    @FXML private TableView<TimeOverride> overrideTableView;
//    @FXML private TableColumn<TimeOverride,String> overrideBibColumn;
//    @FXML private TableColumn<TimeOverride,String> overrideTimeColumn;
//    @FXML private TableColumn<TimeOverride,String> overrideSplitColumn;
//    @FXML private TableColumn<TimeOverride,Boolean> overrideRelativeColumn;
    
    @FXML private Button overrideEditButton;
    @FXML private Button overrideRemoveButton;
    
    @FXML private ToggleSwitch announcerToggleSwitch;
    @FXML private ToggleSwitch assignToRaceToggleSwitch;
    //@FXML private ChoiceBox<Race> assignToRaceChoiceBox;
    @FXML private ComboBox<Race> assignToRaceComboBox;
    
    private ObservableList<CookedTimeData> cookedTimeList;
    FilteredList<CookedTimeData> filteredTimesList;
    private ObservableList<TimingLocation> timingLocationList;
    private TimingLocation selectedTimingLocation;
    private RaceDAO raceDAO;
    private TimingDAO timingDAO; 
    private ParticipantDAO participantDAO;
    //private FXMLTimingLocationInputController timingLocationDetailsController;
    private FXMLLoader timingLocationDetailsLoader ;
    private Bib2ChipMap bib2ChipMap;
    
    @FXML private VBox timingDetailsVBox;
    /**
     * Initializes the controller class.
     */
    @FXML
    public void initialize() {
        
        selectedTimingLocation = null;
        
        
        raceDAO=RaceDAO.getInstance();
        timingDAO=TimingDAO.getInstance();
        participantDAO=ParticipantDAO.getInstance();
        timingLocationList=timingDAO.listTimingLocations(); 
        
        timingLocListView.setItems(timingLocationList);
        timingLocListView.setEditable(true);
        
        //timingLocListView.setCellFactory(TextFieldListCell.forListView(null));
        timingLocListView.setCellFactory(TextFieldListCell.forListView(new StringConverter(){
            @Override
            public TimingLocation fromString(String s) {
                TimingLocation t = new TimingLocation();
                t.setLocationName(s);
                return t; 
            }
            @Override
            public String toString(Object t) {
                if (t != null) {
                return ((TimingLocation)t).toString(); 
                } else {
                    System.out.println("Timing StringConverter toString null object detected.");
                    return "";
                }
            }
        }        
        ));		

        timingLocListView.setOnEditCommit((ListView.EditEvent<TimingLocation> t) -> {
            System.out.println("setOnEditCommit " + t.getIndex());
            if(t.getIndex() < t.getSource().getItems().size()) {
                TimingLocation tl = t.getSource().getItems().get(t.getIndex()); 
                if (t.getNewValue().toString().isEmpty()) {
                    //timingLocationDAO.removeTimingLocation(tl);
                    //tl.setLocationName("New Timing Location");
                    timingDAO.removeTimingLocation(tl);
                } else {
                    tl.setLocationName(t.getNewValue().toString());
                    timingDAO.updateTimingLocation(tl);
                }
            } else {
                System.out.println("Timing setOnEditCommit event out of index: " + t.getIndex());
            }
            timingLocAddButton.requestFocus();
           // timingLocAddButton.setDefaultButton(true);
        });

        timingLocListView.setOnEditCancel((ListView.EditEvent<TimingLocation> t) ->{
            System.out.println("setOnEditCancel " + t.getIndex());
            if (t.getIndex() >= 0 && t.getIndex() < t.getSource().getItems().size()) {
                TimingLocation tl = t.getSource().getItems().get(t.getIndex());
                if (tl.getLocationName().isEmpty()) {
                    //tl.setLocationName("New Timing Location");
                    timingDAO.removeTimingLocation(tl);
                }
            } else {
                System.out.println("Timing setOnEditCancel event out of index: " + t.getIndex());
            }
            timingLocAddButton.requestFocus();
            //timingLocAddButton.setDefaultButton(true);
        });
        
        
        timingLocRemoveButton.disableProperty().bind(timingLocListView.getSelectionModel().selectedItemProperty().isNull());

        

        
        
        // Deal with the filtering and such. 
        // TODO Only filter on the visible colums 
        // 1. Wrap the ObservableList in a FilteredList (initially display all data).
        cookedTimeList = timingDAO.getCookedTimes();
        filteredTimesList = new FilteredList<>(cookedTimeList, p -> true);

        // 2. Set the filter Predicate whenever the filter changes.
        searchTextBox.textProperty().addListener((observable, oldValue, newValue) -> {
            updateFilterPredicate();
        });
        searchScopeCheckComboBox.getCheckModel().getCheckedItems().addListener((ListChangeListener.Change<? extends TimingLocation> c) -> {
            
            System.out.println("TimingPartController::searchScopeCheckComboBox(changeListener) fired...");
            updateFilterPredicate();
            //System.out.println(waveComboBox.getCheckModel().getCheckedItems());
        });
        
        searchScopeCheckComboBox.getItems().setAll(timingLocationList);

        // 3. Wrap the FilteredList in a SortedList. 
        SortedList<CookedTimeData> sortedTimeList = new SortedList<>(filteredTimesList);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedTimeList.comparatorProperty().bind(timeTableView.comparatorProperty());
        
        // 5. Set the cell factories and stort routines... 
        bibColumn.setCellValueFactory(new PropertyValueFactory<>("bib"));
        bibColumn.setComparator(new AlphanumericComparator());
        bibColumn.setStyle( "-fx-alignment: CENTER-RIGHT;");
        
        chipColumn.setCellValueFactory(c -> c.getValue().rawChipIDProperty());
        chipColumn.setComparator(new AlphanumericComparator());
        chipColumn.setStyle( "-fx-alignment: CENTER-RIGHT;");
        
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timestampString"));
        timeColumn.setComparator(new AlphanumericComparator());
        timeColumn.setStyle( "-fx-alignment: CENTER-RIGHT;");
        
        nameColumn.setCellValueFactory(cellData -> {
            String bib = cellData.getValue().getBib();
            Participant p = participantDAO.getParticipantByBib(bib);
            if (p == null) { 
                if (bib.startsWith("Unmapped")) return new SimpleStringProperty("Unknown Chip");
                if (cellData.getValue().getRawChipID().equals("0")) return new SimpleStringProperty("START TRIGGER");
                return new SimpleStringProperty("Unregistered bib: " + bib);
            } else {
                return p.fullNameProperty();
            }
        });
        locationColumn.setCellValueFactory(cellData -> {
            TimingLocation t =  timingDAO.getTimingLocationByID(cellData.getValue().getTimingLocationId());
            if (t == null) { return new SimpleStringProperty("Unknown");
            } else {
                return t.LocationNameProperty();
            }
        });
        inputColumn.setCellValueFactory(cellData -> {
            TimingLocation t =  timingDAO.getTimingLocationByID(cellData.getValue().getTimingLocationId());
            
            if (t == null) return new SimpleStringProperty("Unknown");
            else {
                TimingLocationInput tl = t.getInputByID(cellData.getValue().getTimingLocationInputId());
                if (tl == null) return new SimpleStringProperty("Unknown");
                return tl.LocationNameProperty();
            }
        });
        
        backupColumn.setCellValueFactory(new PropertyValueFactory<>("backupTime"));
        //backupColumn.setCellFactory(tc -> new CheckBoxTableCell<>());
        backupColumn.setCellFactory(column -> new BackupFlagTableCell());
        backupColumn.setEditable(false);
        backupColumn.setStyle( "-fx-alignment: CENTER;");
        
        ignoreColumn.setCellValueFactory(new PropertyValueFactory<>("ignoreTime"));
        ignoreColumn.setCellFactory(tc -> new CheckBoxTableCell<>());
        
        
        ///
        // FIX THIS if we permit multiple select and a right mouse click to set the ignored state
        // for multiple times at once.... (use c.getFrom -> c.getTo)
        // until then this is simpler
        ///

        sortedTimeList.addListener((ListChangeListener.Change<? extends CookedTimeData> c) -> {
            while (c.next()) {
                if (c.wasUpdated()) {
                    timingDAO.saveCookedTime(sortedTimeList.get(c.getFrom()));
                    System.out.println("CookedTimeData "+sortedTimeList.get(c.getFrom()).getBib()+" changed value to " +sortedTimeList.get(c.getFrom()).getIgnoreTime());
                }
            }
        });
//        ///It is not this easy: CheckBoxTableCell does ot fire an onEditCommit action
//        //Need a change listener for the ignoreColumn. Find the cooked read, flag it as an ignore, reprocess the bib
//        ignoreColumn.setOnEditCommit((CellEditEvent<CookedTimeData, Boolean> t) -> {
//            CookedTimeData ct = t.getTableView().getItems().get(t.getTablePosition().getRow());
//            System.out.println("Ignore flag for CookedTime for bib " + ct.getBib() + " at " + ct.getTimestamp().toString() + " is now " + ct.ignoreTimeProperty().toString());
//            timingDAO.saveCookedTime(ct);
//        });

        
        
        
        
        // 6. Add sorted (and filtered) data to the table.
        timeTableView.setItems(sortedTimeList);
        timeTableView.setPlaceholder(new Label("No times have been entered yet"));
        
        // set the default sort order to the finish time
        timeColumn.setSortType(TableColumn.SortType.DESCENDING);
        timeTableView.getSortOrder().clear();
        timeTableView.getSortOrder().add(timeColumn);
        
        // Set the bib number to be an alphanumeric sort
        bibColumn.setComparator(new AlphanumericComparator());
        
        
        listSizeLabel.textProperty().bind(Bindings.size(cookedTimeList).asString());
        filteredSizeLabel.textProperty().bind(Bindings.size(sortedTimeList).asString());
        
        
        // load up the TimingLocationDetailsPane
                    
        //if there are no timing locations selected in the view then disable the entire right hand side
        timingDetailsVBox.visibleProperty().bind(timingLocListView.getSelectionModel().selectedItemProperty().isNull().not());
         
        
        assignToRaceComboBox.disableProperty().bind(assignToRaceToggleSwitch.selectedProperty().not());
        assignToRaceComboBox.visibleProperty().bind(Bindings.size(RaceDAO.getInstance().listWaves()).greaterThan(1));
        assignToRaceComboBox.managedProperty().bind(Bindings.size(RaceDAO.getInstance().listWaves()).greaterThan(1));
        assignToRaceToggleSwitch.visibleProperty().bind(Bindings.size(RaceDAO.getInstance().listWaves()).greaterThan(1));
        assignToRaceToggleSwitch.managedProperty().bind(Bindings.size(RaceDAO.getInstance().listWaves()).greaterThan(1));
        
        timingLocListView.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Cha‌​nge<? extends TimingLocation> c) -> { 
            System.out.println("timingLocListView changed...");
            //timingLocListView.getSelectionModel().getSelectedItems().forEach(System.out::println); 
            ObservableList<TimingLocation> selectedTimingLocations = timingLocListView.getSelectionModel().getSelectedItems();

            timingDetailsVBox.getChildren().clear();

            if ( selectedTimingLocations.isEmpty() ) {
               System.out.println("Nothing Selected");
               //timingLocationDetailsController.selectTimingLocation(null);
               if (selectedTimingLocation != null) {
                   timingLocationNameTextField.textProperty().unbindBidirectional(selectedTimingLocation.LocationNameProperty());
                   selectedTimingLocation=null; 
                   timingLocVBox.disableProperty().setValue(true);
               }
            } else {
                System.out.println("We just selected " + selectedTimingLocations.get(0).getLocationName());
                //timingLocationNameTextField.textProperty().setValue(selectedTimingLocations.get(0).LocationNameProperty().getValue());
                timingLocVBox.disableProperty().setValue(false);
                if (selectedTimingLocation != null) {
                    System.out.println("Unbinding timingLocationNameTextField");
                    timingLocationNameTextField.textProperty().unbindBidirectional(selectedTimingLocation.LocationNameProperty());
                }
                selectedTimingLocation=selectedTimingLocations.get(0); 
                timingLocationNameTextField.textProperty().bindBidirectional(selectedTimingLocation.LocationNameProperty());
                
                if (selectedTimingLocation.getAutoAssignRaceID() < 0) {
                    assignToRaceToggleSwitch.setSelected(false);
                    assignToRaceComboBox.getSelectionModel().clearSelection();
                } else {
                    assignToRaceToggleSwitch.setSelected(true);
                    assignToRaceComboBox.getSelectionModel().select(selectedTimingLocation.autoAssignRaceProperty().getValue());
                }
                
                if (selectedTimingLocation.getIsAnnouncer()) announcerToggleSwitch.setSelected(true);
                else announcerToggleSwitch.setSelected(false);
                
                // Show the filter start/end values
                filterEndTextField.textProperty().setValue(DurationFormatter.durationToString(selectedTimingLocation.getFilterEndDuration(), 3, Boolean.TRUE).replace(".000", ""));
                filterStartTextField.textProperty().setValue(DurationFormatter.durationToString(selectedTimingLocation.getFilterStartDuration(), 3, Boolean.TRUE).replace(".000", ""));
                
                System.out.println("Selected timing location is now " + selectedTimingLocation.getLocationName());
                //timingLocationDetailsController.setTimingLocationInput(null); // .selectTimingLocation(selectedTimingLocations.get(0));
                if (selectedTimingLocation.inputsProperty().isEmpty() ) { // no inputs yet
                    addTimingInput(null);
                } else { // display all of the inputs
                    System.out.println("Starting the display of inputs for a timing location");
                    selectedTimingLocation.getInputs().forEach(i -> {
                        System.out.println("showing input for a timing location ");
                        showTimingInput(i);
                    });
                    System.out.println("Done showing inputs for a timing location");
                }
            }
        });
         
       
        
        timingLocationNameTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
            System.out.println("timingLocationNameTextField out focus");

                if ( timingLocationNameTextField.getText().isEmpty() ) {
                    timingLocationNameTextField.textProperty().setValue("Unnamed");
                    timingDAO.updateTimingLocation(timingLocListView.getSelectionModel().getSelectedItems().get(0));
                }
                
                timingDAO.updateTimingLocation(timingLocListView.getSelectionModel().getSelectedItems().get(0));
                
            }
        });
        
        // Use this if you whant keystroke by keystroke monitoring.... Reject any non digit attempts
//        filterStartTextField.textProperty().addListener((observable, oldValue, newValue) -> {
//            if ( newValue.isEmpty() || newValue.matches("([0-9]*|[0-9]*:[0-5]?)") ) {
//                System.out.println("Possiblely good filter Time (newValue: " + newValue + ")");
//            } else if(newValue.matches("[0-9]*:[0-5][0-9]") ) { // Looks like a HH:MM time, lets check
//                System.out.println("Looks like a valid start filter Time (newValue: " + newValue + ")");
//            } else {
//                filterStartTextField.setText(oldValue);
//                System.out.println("Bad filter Start Time (newValue: " + newValue + ")");
//            }
//                
//        });
        // but only update when the textfield focus changes. 
        filterStartTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("filterStartTextField out focus");
                Duration newTime;
                if (DurationParser.parsable(filterStartTextField.getText())){
                    newTime = DurationParser.parse(filterStartTextField.getText());
                } else {
                    filterStartTextField.textProperty().setValue(DurationFormatter.durationToString(selectedTimingLocation.getFilterStartDuration(), 3, Boolean.TRUE).replace(".000", ""));
                    return;
                }
                
                if (selectedTimingLocation.getFilterStartDuration().equals(newTime)) {
                    filterStartTextField.textProperty().setValue(DurationFormatter.durationToString(selectedTimingLocation.getFilterStartDuration(), 3, Boolean.TRUE).replace(".000", ""));
                    return; 
                } else if ( filterStartTextField.getText().isEmpty() ) {
                    // set duration to zero
                    selectedTimingLocation.setFilterStart(0L);
                } else {
                    // duration is not zero... parse it
                    selectedTimingLocation.setFilterStart(newTime.toNanos());
                }
                timingDAO.updateTimingLocation(timingLocListView.getSelectionModel().getSelectedItems().get(0));
                filterStartTextField.textProperty().setValue(DurationFormatter.durationToString(selectedTimingLocation.getFilterStartDuration(), 3, Boolean.TRUE).replace(".000", ""));
                selectedTimingLocation.reprocessReads();
            }
        });
        filterEndTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("filterEndTextField out focus");
                                        
                Duration newTime;
                if (DurationParser.parsable(filterEndTextField.getText())){
                    newTime = DurationParser.parse(filterEndTextField.getText());
                } else {
                    filterEndTextField.textProperty().setValue(DurationFormatter.durationToString(selectedTimingLocation.getFilterEndDuration(), 3, Boolean.TRUE).replace(".000", ""));
                    return;
                }
                
                if (selectedTimingLocation.getFilterEndDuration().equals(newTime)) {
                    filterEndTextField.textProperty().setValue(DurationFormatter.durationToString(selectedTimingLocation.getFilterEndDuration(), 3, Boolean.TRUE).replace(".000", ""));
                    return; 
                } else if ( filterEndTextField.getText().isEmpty() ) {
                    // set duration to zero
                    selectedTimingLocation.setFilterEnd(0L);
                } else {
                    // duration is not zero... parse it
                    selectedTimingLocation.setFilterEnd(newTime.toNanos());
                }
                timingDAO.updateTimingLocation(timingLocListView.getSelectionModel().getSelectedItems().get(0));
                filterEndTextField.textProperty().setValue(DurationFormatter.durationToString(selectedTimingLocation.getFilterEndDuration(), 3, Boolean.TRUE).replace(".000", ""));
                selectedTimingLocation.reprocessReads();
            }
        });
        
        filterStartTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
            if ( newValue.isEmpty() || newValue.matches("^[0-9]+(:([0-5]?([0-9]?(:([0-5]?([0-9]?(\\.\\d*)?)?)?)?)?)?)?") ){
                System.out.println("Possiblely good Time (newValue: " + newValue + ")");
            } else {
                Platform.runLater(() -> {
                    int c = filterStartTextField.getCaretPosition();
                    if (oldValue.length() > newValue.length()) c++;
                    else c--;
                    filterStartTextField.setText(oldValue);
                    filterStartTextField.positionCaret(c);
                });
                System.out.println("Bad End Filter Time (newValue: " + newValue + ")");
            }
                
        });
        
        filterEndTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
            if ( newValue.isEmpty() || newValue.matches("^[0-9]+(:([0-5]?([0-9]?(:([0-5]?([0-9]?(\\.\\d*)?)?)?)?)?)?)?") ){
                System.out.println("Possiblely good Time (newValue: " + newValue + ")");
            } else {
                Platform.runLater(() -> {
                    int c = filterEndTextField.getCaretPosition();
                    if (oldValue.length() > newValue.length()) c++;
                    else c--;
                    filterEndTextField.setText(oldValue);
                    filterEndTextField.positionCaret(c);
                });
                System.out.println("Bad End Filter Time (newValue: " + newValue + ")");
            }
                
        });
        announcerToggleSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            TimingLocation tl = timingLocListView.getSelectionModel().getSelectedItems().get(0);
            if (tl != null && !Objects.equals(newPropertyValue, tl.getIsAnnouncer())) {
                tl.setIsAnnouncer(newPropertyValue);
                timingDAO.updateTimingLocation(tl);
            }
        });
        
        assignToRaceToggleSwitch.selectedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) { // Don't auto-assign
                TimingLocation tl = timingLocListView.getSelectionModel().getSelectedItems().get(0);
                if (tl != null) {
                    if (!tl.getAutoAssignRaceID().equals(-1)) {
                        System.out.println("Everybody at " + tl.getLocationName() + " is no longer auto-assigned");
                        tl.setAutoAssignRaceID(-1);
                        timingDAO.updateTimingLocation(tl);
                        assignToRaceComboBox.getSelectionModel().clearSelection();
                        //selectedTimingLocation.reprocessReads();
                    }
                }
            } else {
                // Do nothing. If they select a race to assign the runners to then we will do something.
            }
        });
        assignToRaceComboBox.setItems(RaceDAO.getInstance().listRaces());
        assignToRaceComboBox.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                TimingLocation tl = timingLocListView.getSelectionModel().getSelectedItems().get(0);
                Race race = assignToRaceComboBox.getSelectionModel().getSelectedItem();
                if (tl != null && race != null) {
                    if (!tl.getAutoAssignRaceID().equals(race.getID())) {
                        System.out.println("Everybody at " + tl.getLocationName() + " now assigned to race " + race.getRaceName());
                        tl.setAutoAssignRaceID(race.getID());
                        timingDAO.updateTimingLocation(tl);
                        selectedTimingLocation.reprocessReads();
                    }
                }
            }
        });

        
        // bib2Chip mappings
        bib2ChipMap = timingDAO.getBib2ChipMap();
        customChipBibCheckBox.setSelected(bib2ChipMap.getUseCustomMap()); 
        //customChipBibCheckBox.selectedProperty().bind(bib2ChipMap.useCustomMapProperty());
        
        customChipBibCheckBox.setOnAction(a -> {
            System.out.println("bib2ChipMap custom checkbox clicked!");
            if (bib2ChipMap.useCustomMapProperty().get()) {
                bib2ChipMap.setUseCustomMap(false);
                timingDAO.saveBib2ChipMap(bib2ChipMap);
                timingDAO.reprocessAllRawTimes();
            } else if (!bib2ChipMap.useCustomMapProperty().get() && !bib2ChipMap.getChip2BibMap().isEmpty() ) {
                bib2ChipMap.setUseCustomMap(true);
                timingDAO.saveBib2ChipMap(bib2ChipMap);
                timingDAO.reprocessAllRawTimes();
            } else {
                setupCustomChipMap(null);
            }
        });    
//        customChipBibCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
//            if (!old_val.equals(new_val)) {
//                System.out.println("Setting bib2ChipMap custom to " + new_val.toString());
//                if (new_val) setupCustomChipMap(null);
//                //bib2ChipMap.setUseCustomMap(new_val);
//                //timingDAO.saveBib2ChipMap(bib2ChipMap);
//            }
//        });
        
        
        // Override table
        overrideListView.setPlaceholder(new Label("No overrides have been entered yet"));
        overrideListView.setItems(timingDAO.getOverrides());
        overrideListView.setCellFactory(param -> new ListCell<TimeOverride>() {
            Label locationLabel = new Label("Location: ");
            Label bonusLabel = new Label("Bonus: ");
            Label penaltyLabel = new Label("Penalty: ");
            Label bib = new Label("");
            Label fullName = new Label("");
            Label split = new Label("");
            Label time = new Label("");
            Label note = new Label("");
            VBox toVBox = new VBox();
            HBox nameHBox = new HBox();
            HBox timeHBox = new HBox();
            
            @Override
            protected void updateItem(TimeOverride to, boolean empty) {
                super.updateItem(to, empty);
                
                if (empty || to == null || to.getBib() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    
                    bib.setText("Bib: " + to.getBib());
                    fullName.textProperty().bind(ParticipantDAO.getInstance().getParticipantByBib(to.getBib()).fullNameProperty());
                    nameHBox.setSpacing(5);
                    nameHBox.getChildren().setAll(bib,fullName);
                    nameHBox.setStyle("-fx-font-size: 14px;");
                    
                    if (null == to.getOverrideType()) {
                        
                    } else switch (to.getOverrideType()) {
                        case OVERRIDE:
                            split.textProperty().bind(raceDAO.getSplitByID(to.getSplitId()).splitNameProperty());
                            time.setText(DurationFormatter.durationToString(to.getTimestamp()));
                            timeHBox.getChildren().setAll(locationLabel,split,time);
                            break;
                        case PENALTY:
                            time.setText(DurationFormatter.durationToString(to.getTimestamp()));
                            timeHBox.getChildren().setAll(penaltyLabel,time);
                            break;
                        case BONUS:
                            time.setText(DurationFormatter.durationToString(to.getTimestamp()));
                            timeHBox.getChildren().setAll(bonusLabel,time);
                        default:
                            break;
                    }
                    timeHBox.setSpacing(5);
                    
                    if (to.getNote().isEmpty()) toVBox.getChildren().setAll(nameHBox,timeHBox);
                    else {
                        note.setText(to.getNote());
                        toVBox.getChildren().setAll(nameHBox,timeHBox,note);
                    }
                    
                    setText(null);
                    setGraphic(toVBox);
                    
                    setOnMouseClicked(ev -> {
                        if (ev.getClickCount() == 2 )
                            editOverride(getItem());
                    });
                }
            }
        });
        
        
//        overrideTableView.setPlaceholder(new Label("No overrides have been entered yet"));
//        overrideTableView.setItems(timingDAO.getOverrides());
//        overrideBibColumn.setCellValueFactory(cellData -> {
//            return cellData.getValue().bibProperty();
//        });
//        overrideBibColumn.setComparator(new AlphanumericComparator());
//                
//        overrideSplitColumn.setCellValueFactory(cellData -> {
//            Split s = raceDAO.getSplitByID(cellData.getValue().getSplitId());
//            if (s== null) { return new SimpleStringProperty("Unknown: " + cellData.getValue().getSplitId());
//            } else {
//                return s.splitNameProperty();
//            }
//        });
//        
//        
//        overrideTimeColumn.setCellValueFactory(cellData -> {
//            return cellData.getValue().timestampStringProperty();
//            //return new SimpleStringProperty(DurationFormatter.durationToString(cellData.getValue().getTimestamp(), 3));
//        });
//        
//        overrideRelativeColumn.setCellValueFactory(cellData -> {
//            return cellData.getValue().relativeProperty();
//        });
//        overrideRelativeColumn.setCellFactory(tc -> new CheckBoxTableCell());
//        
//        overrideTableView.setRowFactory((TableView<TimeOverride> tableView1) -> {
//            final TableRow<TimeOverride> row = new TableRow<>();
//
//            row.setOnMouseClicked(event -> {
//            if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
//                editOverride(overrideTableView.getSelectionModel().getSelectedItem());
//            }
//            });
//        
//        
//            return row;
//        
//        });
        
        overrideEditButton.disableProperty().bind(overrideListView.getSelectionModel().selectedItemProperty().isNull());
        overrideRemoveButton.disableProperty().bind(overrideListView.getSelectionModel().selectedItemProperty().isNull());
        
         timingLocListView.getSelectionModel().clearAndSelect(0);
         
         timingLocationList.addListener((Change<? extends TimingLocation> change) -> {
            //waveComboBox.getItems().clear();
            //Platform.runLater(() -> {
            System.out.println("TimingController::timingLocationList(changeListener) fired...");
                
            // TODO
            //rework the popup menu for the add/delete
            searchScopeCheckComboBox.getCheckModel().clearChecks();
//            while (change.next() ) 
//                change.getRemoved().forEach(removed -> {
//                    searchScopeCheckComboBox.getCheckModel().clearCheck(removed);
//                });
            searchScopeCheckComboBox.getItems().setAll(timingLocationList);
            

            //});
        });
    }    
    
    
        public void resetTimingLocations(ActionEvent fxevent){
        // prompt 
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Resetting all Timing Locations");
        alert.setHeaderText("This action cannot be undone.");
        alert.setContentText("This will reset the timing locations to default values.\nAll splits will be reassigned to one of the default locations.");
        //Label alertContent = new Label("This will reset the timing locations to default values.\nAll splits will be reassigned to one of the default locations.");
        //alertContent.setWrapText(true); 
        //alert.getDialogPane().setContent(alertContent);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK){
            timingDAO.createDefaultTimingLocations();
        } else {
            // ... user chose CANCEL or closed the dialog
        }
        
        timingLocAddButton.requestFocus();
        //timingLocAddButton.setDefaultButton(true);
    }
        
    public void addTimingLocation(ActionEvent fxevent){
        // prompt 
        TimingLocation t = new TimingLocation();
        t.setLocationName("New Timing Location");

        
        timingDAO.addTimingLocation(t);

        System.out.println("Setting the timingLocListView.edit to " + timingLocationList.size() + " " + timingLocationList.indexOf(t));
        timingLocListView.getSelectionModel().select(timingLocationList.indexOf(t));
        
        //timingLocListView.edit(timingLocationList.indexOf(t));
        timingLocationNameTextField.requestFocus();
        timingLocationNameTextField.selectAll();
        //Because we call the timingLocListView.edit, we don't want to pull back focus
        //timingLocAddButton.requestFocus();

    }
    public void removeTimingLocation(ActionEvent fxevent){
        final TimingLocation tl = timingLocListView.getSelectionModel().getSelectedItem();
        
        // If the location is referenced by a split, 
        // toss up a warning and leave it alone
        final StringProperty splitsUsing = new SimpleStringProperty();
        raceDAO.listRaces().forEach(r -> {
            r.getSplits().forEach(s -> {
                if (s.getTimingLocation().equals(tl)) splitsUsing.set(splitsUsing.getValueSafe() + r.getRaceName() + " " + s.getSplitName() + "\n");
            });
        });
        
        if (splitsUsing.isEmpty().get()) {
            timingDAO.removeTimingLocation(tl);;
            timingLocAddButton.requestFocus();
            //timingLocAddButton.setDefaultButton(true);
        } else {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Unable to Remove Timing Location");
            alert.setHeaderText("Unable to remove the " + tl.getLocationName() + " timing location.");
            alert.setContentText("The timing location is in use by the following splits:\n" + splitsUsing.getValueSafe());
            alert.showAndWait();
        }
    }
    
    public void addTimingInput(ActionEvent fxevent){
        TimingLocationInput tli = new TimingLocationInput();
        tli.setTimingLocation(selectedTimingLocation);
        tli.setLocationName("New " + selectedTimingLocation.getLocationName() + " Input " + (selectedTimingLocation.inputsProperty().size()+1));
        timingDAO.addTimingLocationInput(tli);
        showTimingInput(tli);
        //timingLocationDetailsController.selectTimingLocation(selectedTimingLocation);
    }
    
    private void showTimingInput(TimingLocationInput i) {
        System.out.println("showTimingInput called... ");
        FXMLLoader tlLoader = new FXMLLoader(getClass().getResource("/com/pikatimer/timing/FXMLTimingLocationInput.fxml"));
        try {
            timingDetailsVBox.getChildren().add(tlLoader.load());
            
        } catch (IOException ex) {
            Logger.getLogger(FXMLTimingController.class.getName()).log(Level.SEVERE, null, ex);
        }
        ((FXMLTimingLocationInputController)tlLoader.getController()).setTimingLocationInput(i); 
        //timingLocationDetailsController.selectTimingLocation(selectedTimingLocation);
    }
    
    public void clearAllTimes(ActionEvent fxevent){
        //TODO: Prompt and then remove all times associated with that timing location 
        // _or_ all timing locations
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Clear Timing Data...");
        alert.setHeaderText("Clear Timing Data:");
        alert.setContentText("Do you want to clear the times for all locations or just the " + selectedTimingLocation.getLocationName()+ " location?");

        ButtonType allButtonType = new ButtonType("All Times");
        
        ButtonType currentButtonType = new ButtonType(selectedTimingLocation.getLocationName() + " Times",ButtonData.YES);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(cancelButtonType, allButtonType,  currentButtonType );

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == allButtonType){
            // ... user chose All
            timingDAO.clearAllTimes();
            // itterate over all of the timing locations
            timingLocationList.stream().forEach(tl -> {tl.getInputs().stream().forEach(tli -> {tli.clearLocalReads();});});
        } else if (result.get() == currentButtonType) {
            // ... user chose "Two"
            selectedTimingLocation.getInputs().stream().forEach(tli -> {tli.clearReads();});
        } else {
            // ... user chose CANCEL or closed the dialog
        }
 
    }

    public void setupCustomChipMap(ActionEvent fxevent){
        
        
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FXMLSetupBibMap.fxml"));
        Parent chipMapRoot;
        try {
            
            chipMapRoot = (Parent) fxmlLoader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Chip to Bib Map Setup");
            stage.setScene(new Scene(chipMapRoot));  
            stage.showAndWait();
        } catch (IOException ex) {
            Logger.getLogger(FXMLTimingController.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        customChipBibCheckBox.setSelected(bib2ChipMap.getUseCustomMap()); 
    }
    
    public void addOverride(ActionEvent fxevent){
        editOverride(new TimeOverride());
    }
    
    public void editOverride(ActionEvent fxevent){
        editOverride(overrideListView.getSelectionModel().getSelectedItem());
    }
    
    public void editOverride(TimeOverride o){
        // Open a Dialog box and ask for four things:
        // 1) Bib
        // 2) Type (override, penalty, bonus)
        // 3) split (based on the bib)
        // 4) Time
        // 5) if that time is based on the time of day or a duration from the start
        // 
        // The dialog modifies a timeOverride object on the fly....
        if (o == null) return;
        
        
        BooleanProperty bibOK = new SimpleBooleanProperty(false);
        BooleanProperty timeOK = new SimpleBooleanProperty(false);
        BooleanProperty splitOK = new SimpleBooleanProperty(false);
        BooleanProperty allOK = new SimpleBooleanProperty(false);
       
        allOK.bind(Bindings.and(bibOK, timeOK));
        // Create the custom dialog.
        Dialog<TimeOverride> dialog = new Dialog();
        if (o.getID() == null) {
            dialog.setTitle("Add Override");
            dialog.setHeaderText("Add Override");
        } else {
            dialog.setTitle("Edit Override");
            dialog.setHeaderText("Edit Override");
        }
        // Set the button types.
        ButtonType createButtonType;
        
        if (o.getID() == null) {
            createButtonType = new ButtonType("Create", ButtonData.OK_DONE);
        } else {
            createButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        }
        
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Create the various labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));
        ColumnConstraints column = new ColumnConstraints(USE_COMPUTED_SIZE);
        grid.getColumnConstraints().add(column);

        TextField bibTextField = new TextField();
        bibTextField.setPromptText("Bib");
        
        bibTextField.setPrefWidth(50.0);
        Label participantLabel = new Label();
        PrefixSelectionComboBox<TimeOverrideType> typeComboBox = new PrefixSelectionComboBox();
        PrefixSelectionComboBox<Split> splitComboBox = new PrefixSelectionComboBox();
        
        Label splitLabel = new Label("Split:");
        
        typeComboBox.setItems(FXCollections.observableArrayList(TimeOverrideType.values()));
        
        
        splitComboBox.setVisibleRowCount(5);
        bibTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            Participant p = participantDAO.getParticipantByBib(newValue);
            if (p != null) {
                participantLabel.setText(p.fullNameProperty().getValueSafe());
                ObservableList<Split> splitList = FXCollections.observableArrayList();
                p.wavesObservableList().forEach(w -> {
                    splitList.addAll(w.getRace().getSplits());
                });
                
                if (p.wavesObservableList().size() > 1) splitComboBox.setConverter(
                    new StringConverter<Split>() {
                        @Override
                        public String toString(Split s) {
                            if (s == null) {
                                return "";
                            } else {
                                return s.getRace().getRaceName() + ": " + s.getSplitName();
                            }
                        }

                        @Override
                        public Split fromString(String s) {
                            return null;
                        }
                    }
                );
                else splitComboBox.setConverter(null);
                
                splitComboBox.setItems(splitList);
                splitComboBox.getSelectionModel().selectFirst();
                bibOK.set(true);
            } else {
                participantLabel.setText("Unknown");
                splitComboBox.setItems(null);
                bibOK.set(false);
            }
        });
        
        
        TextField noteTextField = new TextField(o.getNote());
        
        
        CheckBox relativeCheckBox = new CheckBox("Relative to start time");
        relativeCheckBox.selectedProperty().setValue(Boolean.FALSE);
        relativeCheckBox.disableProperty().setValue(Boolean.TRUE);
        
        TextField timeTextField = new TextField();
        timeTextField.setPromptText("HH:MM:SS.sss");
        timeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                    //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
            timeOK.setValue(false);
            
            if (TimeOverrideType.OVERRIDE.equals(typeComboBox.getValue())) {
                if (relativeCheckBox.isSelected() && DurationParser.parsable(newValue)) timeOK.setValue(Boolean.TRUE);
                else if (!relativeCheckBox.isSelected() && DurationParser.parsable(newValue, false)) timeOK.setValue(Boolean.TRUE);
                else timeOK.setValue(false);
            } else {
                if (DurationParser.parsable(newValue)) timeOK.setValue(Boolean.TRUE);
                else timeOK.setValue(false);
            }
            if ( newValue.isEmpty() || newValue.matches("^[0-9]*(:?([0-5]?([0-9]?(:([0-5]?([0-9]?(\\.\\d*)?)?)?)?)?)?)?") ){
                System.out.println("Possiblely good Time (newValue: " + newValue + ")");
            } else {
                Platform.runLater(() -> {
                    int c = timeTextField.getCaretPosition();
                    if (oldValue.length() > newValue.length()) c++;
                    else c--;
                    timeTextField.setText(oldValue);
                    timeTextField.positionCaret(c);
                });
                System.out.println("Bad End Filter Time (newValue: " + newValue + ")");
            }
                
        });
        
        
        
        

        splitComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (splitComboBox.getSelectionModel().getSelectedItem() != null && splitComboBox.getSelectionModel().getSelectedItem().getPosition() == 1) {
                relativeCheckBox.selectedProperty().setValue(Boolean.FALSE);
                relativeCheckBox.disableProperty().setValue(Boolean.TRUE);
            } else {
                relativeCheckBox.disableProperty().setValue(Boolean.FALSE);
            }
        });
        
        typeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (TimeOverrideType.OVERRIDE.equals(newValue)){
                splitComboBox.setVisible(true);
                splitComboBox.setManaged(true);
                splitLabel.setVisible(true);
                splitLabel.setManaged(true);
                relativeCheckBox.setVisible(true);
                relativeCheckBox.setManaged(true);
            } else {
                splitComboBox.setVisible(false);
                splitComboBox.setManaged(false);
                splitLabel.setVisible(false);
                splitLabel.setManaged(false);
                relativeCheckBox.setVisible(false);
                relativeCheckBox.setManaged(false);
            }
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
        });
        typeComboBox.getSelectionModel().select(o.getOverrideType());

                
        HBox bibLabelHBox = new HBox();
        bibLabelHBox.setSpacing(5.0);
        bibLabelHBox.getChildren().addAll(bibTextField,participantLabel);
        bibLabelHBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(new Label("Bib:"), 0, 0);
        grid.add(bibLabelHBox, 1, 0);
        
        grid.add(new Label("Type"), 0, 1);
        grid.add(typeComboBox,1,1);
        
        grid.add(splitLabel, 0, 2);
        grid.add(splitComboBox, 1, 2);
        
        grid.add(new Label("Time:"),0,3);
        grid.add(timeTextField,1,3);
        //grid.add(new Label("Time Since:"),0,3);
        
        grid.add(relativeCheckBox,1,4);
        
        grid.add(new Label("Note:"),0,5);
        grid.add(noteTextField,1,5);

        // Disable create button unless everything is ok
        Node createButton = dialog.getDialogPane().lookupButton(createButtonType);
        createButton.disableProperty().bind(allOK.not());

        //grid.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        dialog.getDialogPane().setContent(grid);
        
        if (o.getID() != null) {
            Platform.runLater(() -> { 
                bibTextField.setText(o.getBib());
                bibTextField.setEditable(false); 
                bibLabelHBox.getChildren().setAll(new Label(o.getBib()), participantLabel);
                timeTextField.setText(DurationFormatter.durationToString(o.getTimestamp(), 3, Boolean.TRUE).replaceFirst("^(\\d:)", "0$1"));
                try{
                    ObservableList<Split> splitList = FXCollections.observableArrayList();
                    splitList.addAll(splitComboBox.getItems().stream().filter(e -> e.getID().equals(o.getSplitId())).findFirst().get());
                    splitComboBox.setItems(splitList);
                    splitComboBox.getSelectionModel().selectFirst();
                } catch (Exception e) {}
                
                relativeCheckBox.setSelected(o.getRelative());
                
                bibTextField.setEditable(false);
                splitComboBox.setEditable(false);
                
            });
        }

        // Convert the result to a TimeOverride
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                o.setBib( bibTextField.textProperty().getValueSafe());
                
                if (TimeOverrideType.OVERRIDE.equals(typeComboBox.getValue())) {
                    o.setOverrideType(typeComboBox.getValue());
                    o.setSplitId(splitComboBox.getValue().getID());

                    if (relativeCheckBox.isSelected()) o.setTimestamp(DurationParser.parse(timeTextField.getText(), false));
                    else o.setTimestamp(DurationParser.parse(timeTextField.getText(), true));
                    o.setRelative(relativeCheckBox.isSelected());
                    o.setNote(noteTextField.getText());
                    
                } else {
                    o.setOverrideType(typeComboBox.getValue());
                    o.setSplitId(-1);
                    o.setTimestamp(DurationParser.parse(timeTextField.getText(), false));
                    o.setRelative(true);
                    o.setNote(noteTextField.getText());
                    
                }
                return o;
            }
            return null;
        });
        // Request focus on the username field by default.
        Platform.runLater(() -> bibTextField.requestFocus());

        // Show the dialog
        Optional<TimeOverride> result = dialog.showAndWait();
        
        // If we got a time, let's save it.
        if (result.isPresent()) timingDAO.saveOverride(result.get());
        
        
    }
    
    public void removeOverride(ActionEvent fxevent){
        timingDAO.deleteOverride(overrideListView.getSelectionModel().getSelectedItem());
    }
    
    
    public void clearAllOverrides(ActionEvent fxevent){
        //TODO: Prompt and then remove all times associated with that timing location 
        // _or_ all timing locations
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Clear Overrides...");
        alert.setHeaderText("Delete Overrides:");
        alert.setContentText("Do you want to delete all overrides?");

        //ButtonType allButtonType = new ButtonType("All");
        
        ButtonType deleteButtonType = new ButtonType("Delete",ButtonData.YES);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(cancelButtonType, deleteButtonType );

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == deleteButtonType) {
            // delete all
            timingDAO.clearAllOverrides(); 
        }
 
    }
    
    public void startTriggerLookup(ActionEvent fxevent){
        
        // Check to see if we have any START_TIMEs to map
        // Wrap it in a new list to avoid the list being modified while we stream/filter it. 
        List<CookedTimeData> cTimes = new ArrayList(timingDAO.getCookedTimes()); 
        List<CookedTimeData> cookedTimes = cTimes.stream().filter(c -> !c.ignoreTimeProperty().getValue()).collect(Collectors.toList());
        
        // For each timing location look for Zero chips at that split
        Map<Integer,ObservableList<Duration>> startTimesByLocation = new HashMap();
        timingDAO.listTimingLocations().forEach(tl -> {
            Integer tlID = tl.getID();
            List<Duration> startTimes = cookedTimes.stream().filter(p -> p.getRawChipID().equals("0") && p.getTimingLocationId().equals(tlID)).map(c -> c.getTimestamp()).collect(Collectors.toList());
            startTimes.sort((p1, p2) -> p1.compareTo(p2));
            if (!startTimes.isEmpty()) startTimesByLocation.put(tlID, FXCollections.observableArrayList(startTimes));
        });
        
        // Debug output
        startTimesByLocation.keySet().forEach(k -> {
            startTimesByLocation.get(k).forEach(d -> {
                System.out.println("Found Start: TL#" + k + " -> " + DurationFormatter.durationToString(d));
            });
        });
        
        if (startTimesByLocation.isEmpty()) {
            // No start times, complain and bail
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("No Start Times...");
            alert.setHeaderText("No Start Times Found");
            alert.setContentText("No Start times (typically a \"0\" chip) were\nfound at any of the start timing locations!\nTo manually enter start times, update\\nthe race or wave start value on the 'Event Details' tab.");

            alert.showAndWait();
            return;
        }
        
        // for each race/wave, see if we have any start times
        Map<Integer,List<Wave>> wavesByLocation = new HashMap();
        RaceDAO.getInstance().listRaces().forEach(race -> {
            if (race.getSplits() == null || race.getSplits().isEmpty()) {
                System.out.println(" RACE HAS NO SPLITS!!! " + race.getRaceName());
                return;
            }
            Integer tlID = race.getSplits().get(0).getTimingLocationID();
            if (wavesByLocation.containsKey(tlID)) wavesByLocation.get(tlID).addAll(race.getWaves());
            else wavesByLocation.put(tlID, new ArrayList(race.getWaves()));
             
        });
        
        

        // Create the base dialog
        Dialog<List<WaveStartTime>> dialog = new Dialog();
        dialog.resizableProperty().set(true);
        dialog.getDialogPane().setMaxHeight(Screen.getPrimary().getVisualBounds().getHeight()-150);
        dialog.setTitle("Lookup Start Times");
        dialog.setHeaderText("Lookup Start Times");
        ButtonType okButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
        
        // Create a scrollPane to put the tables and such in
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-font-size: 16px;"); // Make the scroll bar a bit larger
        VBox scrollVBox = new VBox();
        scrollVBox.setStyle("-fx-font-size: 12px;"); // Make everything normal again
        scrollVBox.fillWidthProperty().set(true);
        scrollVBox.maxHeight(Double.MAX_VALUE);
      
        scrollPane.setContent(scrollVBox);
        scrollPane.fitToWidthProperty().set(true);
        scrollPane.fitToHeightProperty().set(true);
        // For each start location, create a table and a list of possible start times
        
        List<WaveStartTime> waveStartMasterList = new ArrayList();
        
        wavesByLocation.keySet().forEach(tlID -> {
                VBox locationVBox = new VBox();
                locationVBox.fillWidthProperty().set(true);
                locationVBox.maxHeight(Double.MAX_VALUE);
                VBox.setVgrow(locationVBox, Priority.SOMETIMES);
                scrollVBox.getChildren().add(locationVBox);
                if (wavesByLocation.keySet().size()>1) {
                    Label locLabel = new Label(timingDAO.getTimingLocationByID(tlID).getLocationName() + " Timing Location");
                    locLabel.setFont(new Font(16));
                    locationVBox.getChildren().add(locLabel);
                }
                if(!startTimesByLocation.containsKey(tlID)) { // no start times
                    Label noStarts = new Label("No start times found at this location");
                    Label noStarts2 = new Label("To manually enter start times, update\nthe race or wave start value on the 'Event Details' tab.");
                    locationVBox.getChildren().add(noStarts);
                    locationVBox.getChildren().add(noStarts2);
                } else {
                    TableView<WaveStartTime> waveTable = new TableView();
                    VBox.setVgrow(waveTable, Priority.SOMETIMES);
                    waveTable.setFixedCellSize(30);
                    int size = wavesByLocation.get(tlID).size();
                    if (size >= 6) {
                        waveTable.prefHeightProperty().setValue(waveTable.getFixedCellSize()*5 + waveTable.getFixedCellSize()/2);
                        waveTable.minHeightProperty().setValue(waveTable.getFixedCellSize()*5 + waveTable.getFixedCellSize()/2);
                        //waveTable.maxHeightProperty().setValue(waveTable.getFixedCellSize()*5 + waveTable.getFixedCellSize()/2);
                        waveTable.maxHeight(Double.MAX_VALUE);
                    } else {
                        waveTable.prefHeightProperty().setValue(1+(size + 1 )* waveTable.getFixedCellSize());
                        waveTable.minHeightProperty().setValue(1+(size + 1 )* waveTable.getFixedCellSize());
                        //waveTable.maxHeightProperty().setValue(1+(size + 1 )* waveTable.getFixedCellSize());
                        waveTable.maxHeight(Double.MAX_VALUE);
                    }
                    
                    waveTable.maxWidthProperty().setValue(Double.MAX_VALUE);
                    waveTable.setEditable(true);
                    waveTable.columnResizePolicyProperty().setValue(CONSTRAINED_RESIZE_POLICY);
                    
                    TableColumn<WaveStartTime,String> waveColumn = new TableColumn("Race/Wave");
                    waveColumn.setCellValueFactory(c -> c.getValue().wName);
                    waveColumn.setEditable(false);
                    
                    TableColumn<WaveStartTime,String> oldTimeColumn = new TableColumn("Current");
                    oldTimeColumn.setPrefWidth(110);
                    oldTimeColumn.setMinWidth(oldTimeColumn.getPrefWidth());
                    oldTimeColumn.setMaxWidth(oldTimeColumn.getPrefWidth());
                    oldTimeColumn.setCellValueFactory(c -> c.getValue().wStart);
                    oldTimeColumn.setEditable(false);
                    
                    TableColumn<WaveStartTime,Duration> newTimeColumn = new TableColumn("New");
                    newTimeColumn.setPrefWidth(110);
                    newTimeColumn.setMinWidth(newTimeColumn.getPrefWidth());
                    newTimeColumn.setMaxWidth(newTimeColumn.getPrefWidth());
                    newTimeColumn.setCellValueFactory(c -> c.getValue().newStartTime);
                    newTimeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(new DurationStringConverter(), startTimesByLocation.get(tlID)));
                    newTimeColumn.setOnEditCommit((TableColumn.CellEditEvent<WaveStartTime, Duration> t) -> {
                        WaveStartTime s = t.getTableView().getItems().get(t.getTablePosition().getRow());
                        s.setNewDuration(t.getNewValue());
                    });

                    TableColumn<WaveStartTime,Boolean> updateColumn = new TableColumn("Update");
                    updateColumn.setCellValueFactory(c -> c.getValue().update);
                    updateColumn.setCellFactory(tc -> new CheckBoxTableCell<>());
                    updateColumn.setEditable(true);
                    waveTable.getColumns().addAll(waveColumn,oldTimeColumn,newTimeColumn,updateColumn);

                    // create a list of waves for this location
                    ObservableList<WaveStartTime> waveStarts = FXCollections.observableArrayList();
                    wavesByLocation.get(tlID).forEach(w -> {
                        waveStarts.add(new WaveStartTime(w));
                    });
                    waveStartMasterList.addAll(waveStarts);
                    
                    // match up the waves with the best possible start time
                    // we start looking 1 minute before the scheduled start and go from there
                    // and match to the closes to the scheduled start 
                    waveStarts.sort((w1, w2) -> w1.orgStartTime.compareTo(w2.orgStartTime));
                    
                    int maxWaveIndex = waveStarts.size();
                    List<Duration> startTimes = startTimesByLocation.get(tlID); // already sorted
                    int startIndex = 0;
                    int maxStartIndex = startTimes.size();
                    
                    // Loop through the waves
                    for (int waveIndex = 0; waveIndex < maxWaveIndex; waveIndex++){
                        WaveStartTime w = waveStarts.get(waveIndex);
                        Duration d = w.orgStartTime;
                        Long millis = null; 
                        
                        System.out.println("Looking for a start trigger for " + w.wName.get() + " at " + DurationFormatter.durationToString(w.orgStartTime,3));
                        // loop through the unprocessed start times
                        for (int i = startIndex; i < maxStartIndex; i++) {
                            
                            // If it is exactly equal to the existing, stop
                            if (startTimes.get(i).equals(w.orgStartTime)) {
                                w.newStartTime.set(startTimes.get(i));
                                w.update.set(false); // unset the update flag
                                startIndex = i+1;
                                System.out.println("  Exact Match at " + DurationFormatter.durationToString(w.newStartTime.get(),3));
                                break;
                            }
                            // otherwise, if we have not assigned a time yet 
                            // or the new time is closer than the old one... 
                            if (millis == null || startTimes.get(i).minus(d).abs().toMillis() < millis) { 
                                millis = startTimes.get(i).minus(d).abs().toMillis();
                                System.out.println("  Possible Match of " + DurationFormatter.durationToString(startTimes.get(i),3) + " within " + millis + " millis away");
                                // if the new time is closer to the next wave bail
                                if (waveIndex+1 < maxWaveIndex && 
                                        millis > waveStarts.get(waveIndex+1).orgStartTime.minus(startTimes.get(i)).abs().toMillis()) {
                                    System.out.println("  But it is closer to the next time of " + DurationFormatter.durationToString(waveStarts.get(waveIndex+1).orgStartTime,3));
                                    System.out.println("  which is only " + waveStarts.get(waveIndex+1).orgStartTime.minus(startTimes.get(i)).abs().toMillis() + " millis away");
                                    break;
                                } 
                                w.newStartTime.set(startTimes.get(i));
                                w.update.set(true);
                                startIndex = i+1;
                            } else {
                                System.out.println("  Possible Match of " + DurationFormatter.durationToString(startTimes.get(i),3) + " is " + startTimes.get(i).minus(d).abs().toMillis() + " millis away");
                                break;
                            }
                        }
                    }
                    


                    waveTable.setItems(waveStarts);
                    locationVBox.getChildren().add(waveTable);
                }
            
        });
        
        Label manualAddLabel = new Label("To manuall adjust the start times,\ngo to the Event Details tab.");
         manualAddLabel.setFont(new Font(16));
        scrollVBox.getChildren().add(manualAddLabel);
    
        


        
        dialog.getDialogPane().setContent(scrollPane);
        
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return waveStartMasterList;
            }
            return null;
        });
        
        Platform.runLater(() -> { dialog.setY((Screen.getPrimary().getVisualBounds().getHeight()-dialog.getHeight())/2); });
        Optional<List<WaveStartTime>> result = dialog.showAndWait();
        if (result.isPresent()) {
            // Walk the map
            List<WaveStartTime> results = result.get();
            results.forEach(k -> {
                if (k.update.get()) {
                    k.w.setWaveStart((LocalTime.MIDNIGHT.plus(k.newStartTime.get())).format(DateTimeFormatter.ISO_LOCAL_TIME));
                    ResultsDAO.getInstance().reprocessWaveResults(k.w);
                    RaceDAO.getInstance().updateWave(k.w);
                }
            });
        }
    }
    
    private void updateFilterPredicate(){
        filteredTimesList.setPredicate(cookedRead -> {
            // If filter text is empty, display all persons.
           // System.out.println("filteredParticpantsList.predicateProperty changing...");
            //System.out.println("...filterField="+filterField.textProperty().getValue());
            //System.out.println("...searchWaveComboBox=" + searchWaveComboBox.getCheckModel().getCheckedItems().size());
            if (searchTextBox.textProperty().getValueSafe().isEmpty() && searchScopeCheckComboBox.getCheckModel().getCheckedItems().isEmpty()) {
                //System.out.println("...both are empty: true");
                return true;
            }

            BooleanProperty waveFilterMatch = new SimpleBooleanProperty(false);

            if (!searchScopeCheckComboBox.getCheckModel().getCheckedItems().isEmpty()) {
                searchScopeCheckComboBox.getCheckModel().getCheckedItems().forEach(tl -> {
                        if (tl != null && tl.getID().equals(cookedRead.getTimingLocationId())) waveFilterMatch.set(true);
                    }); 
            } else {
                //System.out.println("...searchWaveComboBox is empty: true");

                waveFilterMatch.set(true);
            }

            if (searchTextBox.textProperty().getValueSafe().isEmpty() && waveFilterMatch.getValue()) {
                //System.out.println("...filterField is empty and wave matches");
                return true;
            } else if (!waveFilterMatch.getValue()) {
                //System.out.println("...filterField is empty and wave does not match");
                return false;
            } 

            // Compare first name and last name of every person with filter text.
            String lowerCaseFilter = "(.*)(" + searchTextBox.textProperty().getValueSafe() + ")(.*)";
            try {
                Pattern pattern =  Pattern.compile(lowerCaseFilter, Pattern.CASE_INSENSITIVE);

                String name; 
                Participant p = participantDAO.getParticipantByBib(cookedRead.getBib());
                if (p == null) { 
                    if (cookedRead.getBib().startsWith("Unmapped")) name="Unknown Chip";
                    else if (cookedRead.getRawChipID().equals("0")) name="START TRIGGER";
                    else name="Unregistered bib: " + cookedRead.getBib();
                } else {
                    name =  StringUtils.stripAccents(p.fullNameProperty().get());
                }
                

                if (    pattern.matcher(name).matches() ||
                        pattern.matcher(cookedRead.getRawChipID()).matches() ||
                        pattern.matcher(cookedRead.getBib()).matches()) {
                    return true; // Filter matches first/last/bib.
                } 

            } catch (PatternSyntaxException e) {
                
                return true;
            }
            return false; // Does not match.
        });
    }

        private class BackupFlagTableCell extends TableCell<CookedTimeData, Boolean> {
        @Override
        protected void updateItem(Boolean d, boolean empty) {
            super.updateItem(d, empty);
            if (d == null || empty) {
                setText(null);
            } else {
                // Format duration.
                CookedTimeData t = (CookedTimeData)getTableRow().getItem();
                if (t == null) {
                    setText("");
                    return;
                }
                
                if (! t.getBackupTime()) setText("-");
                else setText("X");
            }
        }

    };
        
    private static class WaveStartTime {
        public Wave w;
        public StringProperty wName = new SimpleStringProperty();
        public StringProperty wStart = new SimpleStringProperty();
        public ObjectProperty<Duration> newStartTime = new SimpleObjectProperty(Duration.ZERO);
        public BooleanProperty update = new SimpleBooleanProperty(false);
        public Duration orgStartTime;
                
        public WaveStartTime() {
        }
        public WaveStartTime(Wave w){
            this.setWave(w);
        }
        
        public void setWave(Wave wave){
            w=wave;
            wName.setValue(WaveStringConverter.getString(w));
            wStart.bind(w.waveStartStringProperty());
            newStartTime.setValue(Duration.between(LocalTime.MIDNIGHT, w.waveStartProperty()));
            orgStartTime=newStartTime.get();
        }
        
        public void setNewDuration(Duration d){
            newStartTime.set(d);
            if (orgStartTime.equals(d)) update.set(false);
            else update.set(true);
        }
        
    }
    
}
