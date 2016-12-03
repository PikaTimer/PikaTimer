/* 
 * Copyright (C) 2016 John Garner
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
import com.pikatimer.timing.reader.PikaRFIDFileReader;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.DurationParser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.PatternSyntaxException;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.controlsfx.control.PrefixSelectionChoiceBox;
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
    @FXML private TextField timingLocationNameTextField; 
    
    @FXML private TextField filterStartTextField;
    @FXML private TextField filterEndTextField; 
            
    @FXML private TextField searchTextBox;
    @FXML private Label listSizeLabel;
    @FXML private Label filteredSizeLabel;
    @FXML private TableView<CookedTimeData> timeTableView;
    @FXML private TableColumn bibColumn;
    @FXML private TableColumn<CookedTimeData,String> chipColumn;
    @FXML private TableColumn timeColumn;
    @FXML private TableColumn<CookedTimeData,String> nameColumn;
    @FXML private TableColumn<CookedTimeData,String> locationColumn;
    @FXML private TableColumn<CookedTimeData,String> inputColumn;
    @FXML private TableColumn backupColumn;
    @FXML private TableColumn ignoreColumn;
    @FXML private CheckBox customChipBibCheckBox;
    
    @FXML private TableView<TimeOverride> overrideTableView;
    @FXML private TableColumn<TimeOverride,String> overrideBibColumn;
    @FXML private TableColumn<TimeOverride,String> overrideTimeColumn;
    @FXML private TableColumn<TimeOverride,String> overrideSplitColumn;
    @FXML private TableColumn<TimeOverride,Boolean> overrideRelativeColumn;
    
    @FXML private Button overrideEditButton;
    @FXML private Button overrideRemoveButton;
    
    @FXML private ToggleSwitch assignToRaceToggleSwitch;
    @FXML private PrefixSelectionChoiceBox<Race> assignToRacePrefixSelectionChoiceBox;
    
    private ObservableList<CookedTimeData> cookedTimeList;
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
        FilteredList<CookedTimeData> filteredTimesList = new FilteredList<>(cookedTimeList, p -> true);

        // 2. Set the filter Predicate whenever the filter changes.
        searchTextBox.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredTimesList.setPredicate(time -> {
                // If filter text is empty, display all persons.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                // Compare first name and last name of every person with filter text.
                String lowerCaseFilter = "(.*)(" + newValue.toLowerCase() + ")(.*)";
                try {    
                    String name; 
                    Participant p = participantDAO.getParticipantByBib(time.getBib());
                    if (p == null) { 
                        if (time.getBib().startsWith("Unmapped")) name="Unknown Chip";
                        else name="Unregistered bib: " + time.getBib();
                    } else {
                        name = p.fullNameProperty().get();
                    }
                    if ((time.getBib() + " " + name).toLowerCase().matches(lowerCaseFilter)) {
                        return true; // Filter matches first/last/email/bib.
                    }

                } catch (PatternSyntaxException e) {
                    return true;
                }
                return false; // Does not match.
            });
        });
        // 3. Wrap the FilteredList in a SortedList. 
        SortedList<CookedTimeData> sortedTimeList = new SortedList<>(filteredTimesList);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedTimeList.comparatorProperty().bind(timeTableView.comparatorProperty());
        
        // 5. Set the cell factories and stort routines... 
        bibColumn.setCellValueFactory(new PropertyValueFactory<>("bib"));
        bibColumn.setComparator(new AlphanumericComparator());
        
        chipColumn.setCellValueFactory(c -> c.getValue().rawChipIDProperty());
        chipColumn.setComparator(new AlphanumericComparator());
        
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timestampString"));
        timeColumn.setComparator(new AlphanumericComparator());
        
        nameColumn.setCellValueFactory(cellData -> {
            String bib = cellData.getValue().getBib();
            Participant p = participantDAO.getParticipantByBib(bib);
            if (p == null) { 
                if (bib.startsWith("Unmapped")) return new SimpleStringProperty("Unknown Chip");
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
        backupColumn.setCellFactory(tc -> new CheckBoxTableCell<>());
        ignoreColumn.setCellValueFactory(new PropertyValueFactory<>("ignoreTime"));
        ignoreColumn.setCellFactory(tc -> new CheckBoxTableCell<>());

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
         
         
         timingLocListView.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Cha‌​nge<? extends TimingLocation> c) -> { 
             System.out.println("timingLocListView changed...");
             //timingLocListView.getSelectionModel().getSelectedItems().forEach(System.out::println); 
             ObservableList<TimingLocation> selectedTimingLocations = timingLocListView.getSelectionModel().getSelectedItems();
             
             timingDetailsVBox.getChildren().clear();
             
             if ( selectedTimingLocations.size() == 0 ) {
                System.out.println("Nothing Selected");
                //timingLocationDetailsController.selectTimingLocation(null);
                if (selectedTimingLocation != null) {
                    timingLocationNameTextField.textProperty().unbindBidirectional(selectedTimingLocation.LocationNameProperty());
                    selectedTimingLocation=null; 
                }
             } else {
                System.out.println("We just selected " + selectedTimingLocations.get(0).getLocationName());
                //timingLocationNameTextField.textProperty().setValue(selectedTimingLocations.get(0).LocationNameProperty().getValue());
                
                if (selectedTimingLocation != null) {
                    System.out.println("Unbinding timingLocationNameTextField");
                    timingLocationNameTextField.textProperty().unbindBidirectional(selectedTimingLocation.LocationNameProperty());
                }
                selectedTimingLocation=selectedTimingLocations.get(0); 
                timingLocationNameTextField.textProperty().bindBidirectional(selectedTimingLocation.LocationNameProperty());
                
                // Show the filter start/end values
                filterEndTextField.textProperty().setValue(DurationFormatter.durationToString(selectedTimingLocation.getFilterEndDuration(), 3, Boolean.TRUE).replace(".000", ""));
                filterStartTextField.textProperty().setValue(DurationFormatter.durationToString(selectedTimingLocation.getFilterStartDuration(), 3, Boolean.TRUE).replace(".000", ""));
                
                System.out.println("Selected timing location is now " + selectedTimingLocation.getLocationName());
                //timingLocationDetailsController.setTimingLocationInput(null); // .selectTimingLocation(selectedTimingLocations.get(0));
                if (selectedTimingLocation.getInputs().isEmpty() ) { // no inputs yet
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
        
        
        // bib2Chip mappings
        bib2ChipMap = timingDAO.getBib2ChipMap();
        customChipBibCheckBox.setSelected(bib2ChipMap.getUseCustomMap()); 
            
        customChipBibCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (!old_val.equals(new_val)) {
                System.out.println("Setting bib2ChipMap custom to " + new_val.toString());
                bib2ChipMap.setUseCustomMap(new_val);
                timingDAO.saveBib2ChipMap(bib2ChipMap);
            }
        });
        
        
        // Override table
        overrideTableView.setPlaceholder(new Label("No overrides have been entered yet"));
        overrideTableView.setItems(timingDAO.getOverrides());
        overrideBibColumn.setCellValueFactory(cellData -> {
            return cellData.getValue().bibProperty();
        });
        overrideBibColumn.setComparator(new AlphanumericComparator());
                
        overrideSplitColumn.setCellValueFactory(cellData -> {
            Split s = raceDAO.getSplitByID(cellData.getValue().getSplitId());
            if (s== null) { return new SimpleStringProperty("Unknown: " + cellData.getValue().getSplitId());
            } else {
                return s.splitNameProperty();
            }
        });
        
        
        overrideTimeColumn.setCellValueFactory(cellData -> {
            return cellData.getValue().timestampStringProperty();
            //return new SimpleStringProperty(DurationFormatter.durationToString(cellData.getValue().getTimestamp(), 3));
        });
        
        overrideRelativeColumn.setCellValueFactory(cellData -> {
            return cellData.getValue().relativeProperty();
        });
        overrideRelativeColumn.setCellFactory(tc -> new CheckBoxTableCell());
        
        overrideTableView.setRowFactory((TableView<TimeOverride> tableView1) -> {
            final TableRow<TimeOverride> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                editOverride(overrideTableView.getSelectionModel().getSelectedItem());
            }
            });
        
        
            return row;
        
        });
        
        overrideEditButton.disableProperty().bind(overrideTableView.getSelectionModel().selectedItemProperty().isNull());
        overrideRemoveButton.disableProperty().bind(overrideTableView.getSelectionModel().selectedItemProperty().isNull());
        
         timingLocListView.getSelectionModel().clearAndSelect(0);
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
        tli.setLocationName(selectedTimingLocation.getLocationName() + " Input " + selectedTimingLocation.getInputs().size()+1);
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
        FileChooser fileChooser = new FileChooser();
        File sourceFile;
        Map<String,String> bibMap = new ConcurrentHashMap();
        final BooleanProperty chipFirst = new SimpleBooleanProperty(false);
        
        fileChooser.setTitle("Select Bib -> Chip File");
        
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home"))); 
        
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt","*.csv"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"), 
                new FileChooser.ExtensionFilter("All files", "*")
            );
        
        sourceFile = fileChooser.showOpenDialog(customChipBibCheckBox.getScene().getWindow());
        if (sourceFile != null) {
            try {            
                    Optional<String> fs = Files.lines(sourceFile.toPath()).findFirst();
                    String[] t = fs.get().split(",", -1);
                    if (t.length != 2) return; 
                    
                    if(t[0].toLowerCase().contains("chip")) {
                        chipFirst.set(true);
                        System.out.println("Found a chip -> bib file");
                    } else if (t[0].toLowerCase().contains("bib")) {
                        chipFirst.set(false);
                        System.out.println("Found a bib -> chip file");
                    } else {
                        bibMap.put(t[0], t[1]);
                        System.out.println("No header in file");
                        System.out.println("Mapped chip " + t[0] + " to " + t[1]);
                    }
                    Files.lines(sourceFile.toPath())
                        .map(s -> s.trim())
                        .filter(s -> !s.isEmpty())
                        .skip(1)
                        .forEach(s -> {
                            //System.out.println("readOnce read " + s); 
                            String[] tokens = s.split(",", -1);
                            if(chipFirst.get()) {
                                bibMap.put(tokens[0], tokens[1]);
                                System.out.println("Mapped chip " + tokens[0] + " to " + tokens[1]);
                            } else {
                                bibMap.put(tokens[1], tokens[0]);
                                System.out.println("Mapped chip " + tokens[1] + " to " + tokens[0]);
                            }
                        });
                    System.out.println("Found a total of " + bibMap.size() + " mappings");
                    bib2ChipMap.setChip2BibMap(bibMap);
                    timingDAO.updateBib2ChipMap(bib2ChipMap);
                    timingDAO.reprocessAllRawTimes();
                } catch (IOException ex) {
                    Logger.getLogger(PikaRFIDFileReader.class.getName()).log(Level.SEVERE, null, ex);
                    // We had an issue reading the file.... 
                }
            
            
        }
    }
    
    public void addOverride(ActionEvent fxevent){
        editOverride(new TimeOverride());
    }
    
    public void editOverride(ActionEvent fxevent){
        editOverride(overrideTableView.getSelectionModel().getSelectedItem());
    }
    
    public void editOverride(TimeOverride o){
        // Open a Dialog box and ask for four things:
        // 1) Bib
        // 2) split (based on the bib)
        // 3) Time
        // 4) if that time is based on the time of day or a duration from the start
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
        PrefixSelectionComboBox<Split> splitComboBox = new PrefixSelectionComboBox();
        splitComboBox.setVisibleRowCount(5);
        bibTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            Participant p = participantDAO.getParticipantByBib(newValue);
            if (p != null) {
                participantLabel.setText(p.fullNameProperty().getValueSafe());
                ObservableList<Split> splitList = FXCollections.observableArrayList();
                p.wavesObservableList().forEach(w -> {
                    splitList.addAll(w.getRace().getSplits());
                });
                splitComboBox.setItems(splitList);
                splitComboBox.getSelectionModel().selectFirst();
                bibOK.set(true);
            } else {
                participantLabel.setText("Unknown");
                splitComboBox.setItems(null);
                bibOK.set(false);
            }
        });
        
        
        
        CheckBox typeCheckBox = new CheckBox("Relative to start time");
        typeCheckBox.selectedProperty().setValue(Boolean.FALSE);
        typeCheckBox.disableProperty().setValue(Boolean.TRUE);
        
        TextField timeTextField = new TextField();
        timeTextField.setPromptText("HH:MM:SS.sss");
        timeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                    //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
            timeOK.setValue(false);
            
            if (typeCheckBox.isSelected() && DurationParser.parsable(newValue)) timeOK.setValue(Boolean.TRUE);
            if (!typeCheckBox.isSelected() && DurationParser.parsable(newValue, false)) timeOK.setValue(Boolean.TRUE);

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
                typeCheckBox.selectedProperty().setValue(Boolean.FALSE);
                typeCheckBox.disableProperty().setValue(Boolean.TRUE);
            } else {
                typeCheckBox.disableProperty().setValue(Boolean.FALSE);
            }
        });
        
        HBox bibLabelHBox = new HBox();
        bibLabelHBox.setSpacing(5.0);
        bibLabelHBox.getChildren().addAll(bibTextField,participantLabel);
        grid.add(new Label("Bib:"), 0, 0);
        grid.add(bibLabelHBox, 1, 0);
        grid.add(new Label("Split:"), 0, 1);
        grid.add(splitComboBox, 1, 1);
        grid.add(new Label("Time:"),0,2);
        grid.add(timeTextField,1,2);
        //grid.add(new Label("Time Since:"),0,3);
        grid.add(typeCheckBox,1,3);

        // Disable create button unless everything is ok
        Node createButton = dialog.getDialogPane().lookupButton(createButtonType);
        createButton.disableProperty().bind(allOK.not());

        //grid.setMinSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        dialog.getDialogPane().setContent(grid);
        
        if (o.getID() != null) {
            Platform.runLater(() -> { 
                bibTextField.setText(o.getBib());
                
                
                timeTextField.setText(DurationFormatter.durationToString(o.getTimestamp(), 3, Boolean.TRUE).replaceFirst("^(\\d:)", "0$1"));
                try{
                    ObservableList<Split> splitList = FXCollections.observableArrayList();
                    splitList.addAll(splitComboBox.getItems().stream().filter(e -> e.getID().equals(o.getSplitId())).findFirst().get());
                    splitComboBox.setItems(splitList);
                    splitComboBox.getSelectionModel().selectFirst();
                } catch (Exception e) {}
                
                typeCheckBox.setSelected(o.getRelative());
                
                bibTextField.setEditable(false);
                splitComboBox.setEditable(false);
                
            });
        }

        // Convert the result to a TimeOverride
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                o.setBib( bibTextField.textProperty().getValueSafe());
                o.setSplitId(splitComboBox.getValue().getID());
                if (typeCheckBox.isSelected()) o.setTimestamp(DurationParser.parse(timeTextField.getText(), false));
                else o.setTimestamp(DurationParser.parse(timeTextField.getText(), true));
                o.setRelative(typeCheckBox.isSelected());
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
        timingDAO.deleteOverride(overrideTableView.getSelectionModel().getSelectedItem());
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
        
    }
    
}
