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
import com.pikatimer.timing.reader.PikaRFIDFileReader;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.DurationFormatter;
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
    @FXML private TableColumn timeColumn;
    @FXML private TableColumn<CookedTimeData,String> nameColumn;
    @FXML private TableColumn<CookedTimeData,String> inputColumn;
    @FXML private TableColumn backupColumn;
    @FXML private TableColumn ignoreColumn;
    @FXML private CheckBox customChipBibCheckBox;
    
    @FXML private TableView<TimeOverride> overrideTableView;
    @FXML private TableColumn<TimeOverride,String> overrideBibColumn;
    @FXML private TableColumn<TimeOverride,String> overrideTimeColumn;
    @FXML private TableColumn<TimeOverride,String> overrideSplitColumn;
    @FXML private TableColumn<TimeOverride,Boolean> overrideRelativeColumn;
    
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
            timingLocAddButton.setDefaultButton(true);
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
            timingLocAddButton.setDefaultButton(true);
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
                        name="unknown";
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
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timestampString"));
        timeColumn.setComparator(new AlphanumericComparator());
        
        nameColumn.setCellValueFactory(cellData -> {
            Participant p = participantDAO.getParticipantByBib(cellData.getValue().getBib());
            if (p == null) { return new SimpleStringProperty("Unknown: " + cellData.getValue().getBib());
            } else {
                return p.fullNameProperty();
            }
        });
        inputColumn.setCellValueFactory(cellData -> {
            TimingLocation t =  timingDAO.getTimingLocationByID(cellData.getValue().getTimingLocationId());
            if (t == null) { return new SimpleStringProperty("Unknown");
            } else {
                return t.LocationNameProperty();
            }
        });
        
        backupColumn.setCellValueFactory(new PropertyValueFactory<>("backupTime"));
        backupColumn.setCellFactory(tc -> new CheckBoxTableCell<>());
        ignoreColumn.setCellValueFactory(new PropertyValueFactory<>("ignoreTime"));
        ignoreColumn.setCellFactory(tc -> new CheckBoxTableCell<>());

        // 6. Add sorted (and filtered) data to the table.
        timeTableView.setItems(sortedTimeList);
        
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
                filterEndTextField.textProperty().setValue(DurationFormatter.durationToString(selectedTimingLocation.getFilterEndDuration(), 0, Boolean.TRUE));
                filterStartTextField.textProperty().setValue(DurationFormatter.durationToString(selectedTimingLocation.getFilterStartDuration(), 0, Boolean.TRUE));
                
                System.out.println("Selected timing location is now " + selectedTimingLocation.getLocationName());
                //timingLocationDetailsController.setTimingLocationInput(null); // .selectTimingLocation(selectedTimingLocations.get(0));
                if (selectedTimingLocation.getInputs().size() == 0 ) { // no inputs yet
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
         
        timingLocListView.getSelectionModel().clearAndSelect(0);
        
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
                if (filterStartTextField.getText().equals(DurationFormatter.durationToString(selectedTimingLocation.getFilterStartDuration(), 0, Boolean.TRUE))) {
                    return; 
                } else if ( filterStartTextField.getText().isEmpty() ) {
                    // set duration to zero
                    selectedTimingLocation.setFilterStart(0L);
                } else {
                    // duration is not zero... parse it
                    LocalTime time = LocalTime.parse(filterStartTextField.getText(), DateTimeFormatter.ISO_LOCAL_TIME);
                    selectedTimingLocation.setFilterStart(Duration.between(LocalTime.MIDNIGHT, time).toNanos());
                }
                selectedTimingLocation.reprocessReads();
                timingDAO.updateTimingLocation(timingLocListView.getSelectionModel().getSelectedItems().get(0));
                
            }
        });
        filterEndTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("filterEndTextField out focus");
                
                if (filterEndTextField.getText().equals(DurationFormatter.durationToString(selectedTimingLocation.getFilterEndDuration(), 0, Boolean.TRUE))) {
                    return; 
                } else if ( filterEndTextField.getText().isEmpty() ) {
                    // set duration to zero
                    selectedTimingLocation.setFilterEnd(0L);
                } else {
                    // duration is not zero... parse it
                    LocalTime time = LocalTime.parse(filterEndTextField.getText(), DateTimeFormatter.ISO_LOCAL_TIME);
                    selectedTimingLocation.setFilterEnd(Duration.between(LocalTime.MIDNIGHT, time).toNanos());
                }
                
                selectedTimingLocation.reprocessReads();
                timingDAO.updateTimingLocation(timingLocListView.getSelectionModel().getSelectedItems().get(0));
                
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
            return new SimpleStringProperty(DurationFormatter.durationToString(cellData.getValue().getTimestamp(), 3));
        });
        
        overrideRelativeColumn.setCellValueFactory(cellData -> {
            return cellData.getValue().relativeProperty();
        });
        overrideRelativeColumn.setCellFactory(tc -> new CheckBoxTableCell());
        
        
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
        timingLocAddButton.setDefaultButton(true);
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
        //TODO: If the location is referenced by a split, 
        //prompt to reassign the split to a new location or cancel the edit. 
        timingDAO.removeTimingLocation(timingLocListView.getSelectionModel().getSelectedItem());
        //timingLocAddButton.requestFocus();
        //timingLocAddButton.setDefaultButton(true);
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
        alert.setContentText("Do you want to clear the times for just this imput or all inputs?.");

        ButtonType allButtonType = new ButtonType("All");
        
        ButtonType currentButtonType = new ButtonType("Current",ButtonData.YES);
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
                } catch (IOException ex) {
                    Logger.getLogger(PikaRFIDFileReader.class.getName()).log(Level.SEVERE, null, ex);
                    // We had an issue reading the file.... 
                }
            
            
        }
    }
    
    public void addOverride(ActionEvent fxevent){
        // Open a Dialog box and ask for four things:
        // 1) Bib
        // 2) split (based on the bib)
        // 3) Time
        // 4) if that time is based on the time of day or a duration from the start
        // 
        // The dialog modifies a timeOverride object on the fly....
        
        
        
        BooleanProperty bibOK = new SimpleBooleanProperty(false);
        BooleanProperty timeOK = new SimpleBooleanProperty(false);
        BooleanProperty splitOK = new SimpleBooleanProperty(false);
        BooleanProperty allOK = new SimpleBooleanProperty(false);
       
        allOK.bind(Bindings.and(bibOK, timeOK));
        // Create the custom dialog.
        Dialog<TimeOverride> dialog = new Dialog();
        dialog.setTitle("Add Override");
        dialog.setHeaderText("Add Override");

        // Set the button types.
        ButtonType createButtonType = new ButtonType("Create", ButtonData.OK_DONE);
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
        ComboBox<Split> splitComboBox = new ComboBox();
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
        
        
        
        
        TextField timeTextField = new TextField();
        timeTextField.setPromptText("HH:MM:SS.sss");
        timeTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                    //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
            timeOK.setValue(false);

            if (newValue.matches("([3-9]|[012]:)")) {
                //Integer pos = raceStartTimeTextField.getCaretPosition();
                timeTextField.setText("0" + newValue);
                Platform.runLater(() -> {
                    timeTextField.positionCaret(newValue.length()+2);
                });
            } else if (    newValue.isEmpty() || 
                    newValue.matches("([012]|[01][0-9]|2[0-3])") || 
                    newValue.matches("([01][0-9]|2[0-3]):[0-5]?") || 
                    newValue.matches("([01][0-9]|2[0-3]):[0-5][0-9]:[0-5]?") ){
                System.out.println("Possiblely good Time (newValue: " + newValue + ")");
                timeOK.setValue(false);
            } else if(newValue.matches("([01][0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9](\\.[0-9]*)?)?") ) { // Looks like a time, lets check
                System.out.println("Testing Time (newValue: " + newValue + ")");
            
                try {
                    if (!newValue.isEmpty()) {
                        //LocalTime.parse(raceStartTimeTextField.getText(), DateTimeFormatter.ISO_LOCAL_TIME );
                        LocalTime.parse(timeTextField.getText(), DateTimeFormatter.ISO_LOCAL_TIME);
                        timeOK.setValue(true);
                    }
                } catch (Exception e) {
                    timeTextField.setText(oldValue);
                    System.out.println("Exception Bad Time (newValue: " + newValue + ")");
                    e.printStackTrace();
                    
                }
            } else {
                timeTextField.setText(oldValue);
                System.out.println("Bad Time (newValue: " + newValue + ")");
            }
                
        });
        
        
        CheckBox typeCheckBox = new CheckBox("Relative to start time");
        typeCheckBox.selectedProperty().setValue(Boolean.FALSE);
        typeCheckBox.disableProperty().setValue(Boolean.TRUE);

        splitComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (splitComboBox.getSelectionModel().getSelectedIndex() == 0) {
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

        // Convert the result to a TimeOverride
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                TimeOverride o = new TimeOverride();
                o.setBib( bibTextField.textProperty().getValueSafe());
                o.setSplitId(splitComboBox.getValue().getID());
                o.setTimestamp(Duration.between(LocalTime.MIN,LocalTime.parse(timeTextField.getText(), DateTimeFormatter.ISO_LOCAL_TIME)));
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
        alert.setContentText("Do you want to delete all overrides?.");

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
    
}
