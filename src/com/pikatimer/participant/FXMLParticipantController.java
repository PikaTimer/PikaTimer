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
package com.pikatimer.participant;
import com.pikatimer.event.Event;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import com.pikatimer.race.WaveAssignment;
import com.pikatimer.results.ResultsDAO;
import com.pikatimer.timing.TimingDAO;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.WaveStringConverter;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.container.DefaultFlowContainer;
import java.io.File;
import java.io.IOException;
import static java.lang.Boolean.FALSE;
import static java.lang.Double.MAX_VALUE;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import static javafx.scene.layout.Region.USE_PREF_SIZE;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.ListSelectionView;
import org.controlsfx.control.PrefixSelectionChoiceBox;
import org.controlsfx.control.ToggleSwitch;
/**
 *
 * @author jcgarner
 */
public class FXMLParticipantController  {

    @FXML private TableView<Participant> participantTableView;
    @FXML private TableColumn bibNumberColumn;
    //@FXML private TableColumn<Participant,ObservableList<Wave>> raceColumn;
    @FXML private TableColumn<Participant,Number> raceColumn;
    @FXML private TableColumn<Participant,Status> statusColumn;
    @FXML private VBox formVBox; 
    @FXML private TextField bibTextField;
    @FXML private Label raceLabel;
    @FXML private CheckComboBox<Wave> waveComboBox; 
    @FXML private HBox raceHBox;
    @FXML private TextField firstNameField;
    @FXML private TextField middleNameTextField;
    @FXML private TextField lastNameField;
    @FXML private TextField ageTextField;
    //@FXML private PrefixSelectionChoiceBox<String> sexPrefixSelectionChoiceBox;
    @FXML private TextField sexTextField;
    @FXML private TextField cityTextField; 
    @FXML private TextField stateTextField;
    @FXML private TextField zipTextField;
    @FXML private TextField countryTextField;
    @FXML private DatePicker birthdayDatePicker;    

    @FXML private TextField filterField; 
    @FXML private Button formAddButton; 
    @FXML private Button formUpdateButton;
    @FXML private Button formResetButton;
    @FXML private Label filteredSizeLabel;
    @FXML private Label listSizeLabel; 
    
    @FXML private HBox zipHBox;
    @FXML private HBox countryHBox;
    @FXML private CheckComboBox<Wave> searchWaveComboBox; 
    @FXML private Label filterLabel;

    @FXML private PrefixSelectionChoiceBox<Status> statusPrefixSelectionChoiceBox;
    @FXML private TextField noteTextField;
    
    @FXML private Button deleteParticipantsButton;
    
    @FXML private Button customAtrtributesButton;
    @FXML private VBox customAttributesVBox;
    
    @FXML private Button bulkBibAssignmentButton;
    
    private final List<TableColumn> customAttributesColumns = new ArrayList();
    private final Map<Integer,TextField> customAttributesTextFields = new HashMap();
    private final Map<Integer,PrefixSelectionChoiceBox<String>> customAttributesChoiceBoxes = new HashMap();
    private final Map<Integer,DatePicker> customAttributesDatePickers = new HashMap();
    private final Map<Integer,CheckBox> customAttributesCheckBox = new HashMap();
    
    private ObservableList<Participant> participantsList;
    private ParticipantDAO participantDAO;
    private Participant editedParticipant; 
    FilteredList<Participant> filteredParticipantsList ;
    
    
    
    @FXML
    protected void initialize() {
        // setup the ObservableList of people
        
        System.out.println("Initializing ParticipantController");
        System.out.println("Creating ParticipantDAO");
        participantDAO=ParticipantDAO.getInstance();
        //participantsList=FXCollections.observableArrayList();
        System.out.println("Retrieving Participants");
        //participantsList.addAll(participantDAO.listParticipants());
        participantsList=participantDAO.listParticipants(); 
        
        System.out.println("Binding tableView to the participantsList");
        
        filterField.requestFocus(); // set the focus to the filter menu first
        
        participantTableView.setPlaceholder(new Label("No participants have been entered yet"));
        
        // Setup the context menu and actions
        participantTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        participantTableView.setRowFactory((TableView<Participant> tableView1) -> {
            final TableRow<Participant> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();
            
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                    editParticipant(participantTableView.getSelectionModel().getSelectedItem());
                }
            });
            
            // Context menu
            MenuItem editItem = new MenuItem("Edit");
            editItem.setOnAction((ActionEvent event) -> {
                editParticipant(participantTableView.getSelectionModel().getSelectedItem());
            });
            
            MenuItem removeItem = new MenuItem("Delete");
            removeItem.setOnAction((ActionEvent event) -> {
                
                removeParticipants(FXCollections.observableArrayList(participantTableView.getSelectionModel().getSelectedItems()));
//                ObservableList deleteMe = FXCollections.observableArrayList(participantTableView.getSelectionModel().getSelectedItems());
//                
//                Iterator<Participant> deleteMeIterator = deleteMe.iterator();
//		while (deleteMeIterator.hasNext()) {
//                    Participant p = deleteMeIterator.next();
//                    removeParticipant(p); 
//		}
                
            });
            
            MenuItem swapBibs = new MenuItem("Swap Bibs");
            swapBibs.setOnAction((ActionEvent event) -> {
                ObservableList<Participant> swapMe = FXCollections.observableArrayList(participantTableView.getSelectionModel().getSelectedItems());
                
                if (swapMe.size() == 2) {
                    String tmp = swapMe.get(1).getBib();
                    swapMe.get(1).setBib(swapMe.get(0).getBib());
                    swapMe.get(0).setBib(tmp);
                    
                    participantDAO.updateParticipant(swapMe.get(1));
                    participantDAO.updateParticipant(swapMe.get(0));
                    
                }
		
            });
            
            Menu assignWave = new Menu("Assign");
            //RaceDAO.getInstance().listWaves().sorted((Wave u1, Wave u2) -> u1.toString().compareTo(u2.toString())).stream().forEach(w -> {
            RaceDAO.getInstance().listWaves().sorted(new AlphanumericComparator()).stream().forEach(w -> {
                MenuItem m = new MenuItem(w.toString());
                m.setOnAction(e -> {
                    participantTableView.getSelectionModel().getSelectedItems().stream().forEach(p -> {
                        p.setWaves((Wave)w);
                        participantDAO.updateParticipant(p);

                    });
                });
                assignWave.getItems().add(m);
            });
            
            RaceDAO.getInstance().listWaves().addListener((Change<? extends Wave> change) -> {
                assignWave.getItems().clear();
                //RaceDAO.getInstance().listWaves().sorted((Wave u1, Wave u2) -> u1.toString().compareTo(u2.toString())).stream().forEach(w -> {
                RaceDAO.getInstance().listWaves().sorted(new AlphanumericComparator()).stream().forEach(w -> {
                    MenuItem m = new MenuItem(w.toString());
                    m.setOnAction(e -> {
                        participantTableView.getSelectionModel().getSelectedItems().stream().forEach(p -> {
                            p.setWaves((Wave)w);
                            participantDAO.updateParticipant(p);

                        });
                    });
                    assignWave.getItems().add(m);
                });
                
            });
            
            // context menu to assign/unassign runners to a given wave
            rowMenu.getItems().addAll(editItem, removeItem, swapBibs, assignWave);
            
            // only display context menu for non-null items:
            row.contextMenuProperty().bind(
                    Bindings.when(Bindings.isNotNull(row.itemProperty()))
                            .then(rowMenu)
                            .otherwise((ContextMenu)null));
            
            // Hide the edit option if more than one item is selected and only show
            // the swap option if exactly two items are selected. 
            participantTableView.getSelectionModel().getSelectedIndices().addListener((Change<? extends Integer> change) -> {
                if (change.getList().size() == 2) {
                    swapBibs.setDisable(false);
                } else {
                    swapBibs.setDisable(true);
                }
                if (change.getList().size() == 1) {
                    editItem.setDisable(false);
                } else {
                    editItem.setDisable(true);
                }
            });
            
            return row;
        });
        
        
        // Deal with the filtering and such. 
        // 1. Wrap the ObservableList in a FilteredList (initially display all data).
        //FilteredList<Participant> filteredParticipantsList = new FilteredList<>(participantsList, p -> true);
        filteredParticipantsList = new FilteredList<>(participantsList, p -> true);

        // 2. Set the filter Predicate whenever the filters changes.
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateFilterPredicate();
        });
        searchWaveComboBox.getCheckModel().getCheckedItems().addListener((ListChangeListener.Change<? extends Wave> c) -> {
            
            System.out.println("PartController::searchWaveComboBox(changeListener) fired...");
            updateFilterPredicate();
            //System.out.println(waveComboBox.getCheckModel().getCheckedItems());
        });
        
        
        // 3. Wrap the FilteredList in a SortedList. 
        SortedList<Participant> sortedParticipantsList = new SortedList(filteredParticipantsList);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedParticipantsList.comparatorProperty().bind(participantTableView.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.
        participantTableView.setItems(sortedParticipantsList);
        
        // Set the bib number to be an alphanumeric sort
        bibNumberColumn.setComparator(new AlphanumericComparator());
        bibNumberColumn.setStyle( "-fx-alignment: CENTER-RIGHT;");
        
        listSizeLabel.textProperty().bind(Bindings.size(participantsList).asString());
        filteredSizeLabel.textProperty().bind(Bindings.size(sortedParticipantsList).asString());
        
        // if there is only one race, hide the option to pick a race... 
//        raceLabel.visibleProperty().bind(waveComboBox.visibleProperty());
//        waveComboBox.visibleProperty().bind(Bindings.size(RaceDAO.getInstance().listWaves()).greaterThan(1));
//        waveComboBox.managedProperty().bind(Bindings.size(RaceDAO.getInstance().listWaves()).greaterThan(1));
        
        raceHBox.managedProperty().bind(Bindings.size(RaceDAO.getInstance().listWaves()).greaterThan(1));
        raceHBox.visibleProperty().bind(Bindings.size(RaceDAO.getInstance().listWaves()).greaterThan(1));
        if (RaceDAO.getInstance().listRaces().size() > 1) raceLabel.setText("Race");
        else raceLabel.setText("Wave");
        
        searchWaveComboBox.visibleProperty().bind(Bindings.size(RaceDAO.getInstance().listWaves()).greaterThan(1));
        filterLabel.visibleProperty().bind(Bindings.size(RaceDAO.getInstance().listWaves()).greaterThan(1));
        waveComboBox.getItems().addAll(RaceDAO.getInstance().listWaves());
        waveComboBox.getCheckModel().check(0);
        searchWaveComboBox.getItems().addAll(RaceDAO.getInstance().listWaves());
        
        waveComboBox.setConverter(new WaveStringConverter());
        searchWaveComboBox.setConverter(new WaveStringConverter());
        
        RaceDAO.getInstance().listWaves().addListener((Change<? extends Wave> change) -> {
            //waveComboBox.getItems().clear();
            //Platform.runLater(() -> {
               System.out.println("PartController::raceWaves(changeListener) fired...");
                
            // TODO
            //rework the popup menu for the add/delete
            while (change.next() ) 
                change.getRemoved().forEach(removed -> {
                    searchWaveComboBox.getCheckModel().clearCheck(removed);
                });
            waveComboBox.getItems().setAll(RaceDAO.getInstance().listWaves().sorted((Wave u1, Wave u2) -> u1.toString().compareTo(u2.toString())));
            waveComboBox.getCheckModel().check(0);
            searchWaveComboBox.getItems().setAll(RaceDAO.getInstance().listWaves().sorted((Wave u1, Wave u2) -> u1.toString().compareTo(u2.toString())));
            
            if (change.getList().size() == 1 ) {
                raceColumn.visibleProperty().set(false);
                searchWaveComboBox.getCheckModel().clearChecks();
            }
            else raceColumn.visibleProperty().set(true);
            
            if (RaceDAO.getInstance().listRaces().size() > 1) raceLabel.setText("Race");
            else raceLabel.setText("Wave");
            //});
        });
        
        
        
        // DOES NOT WORK :-( 
//        waveComboBox.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
//            System.out.println("PartController::waveComboBox(focusedListener) fired...");
//            if (!newPropertyValue) {
//                System.out.println("waveComboBox out focus");
//            } else {
//                System.out.println("waveComboBox in focus");
//            }
//        });
        // Does Work
        waveComboBox.getCheckModel().getCheckedItems().addListener((ListChangeListener.Change<? extends Wave> c) -> {
            System.out.println("PartController::waveComboBox(changeListener) fired...");

            while (c.next()) {
                if (c.wasAdded()) {
                    //ObservableList<Wave>  added = FXCollections.observableArrayList(c.getAddedSubList());
                    FXCollections.observableArrayList(c.getAddedSubList()).forEach( w -> {
                    //added.forEach(w -> { 
                    //c.getAddedSubList().forEach(w -> {
                        //System.out.println("waveComboBox new selection: " + w.toString());
                        // uncheck all other waves associated with that race... 
                        waveComboBox.getItems().forEach(i -> {
                            if (i.getRace() == w.getRace() && !i.equals(w) )
                                waveComboBox.getCheckModel().clearCheck(i);
                        }); 
                    });
                }
            }
            //System.out.println(waveComboBox.getCheckModel().getCheckedItems());
        });
        System.out.println("Done Initializing ParticipantController");
        
        ageTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                    //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
                try {
                    if (!newValue.isEmpty()) {
                        Integer.parseUnsignedInt(ageTextField.getText());
                        if (newValue.matches("^0*([0-9]+)")) {
                            Platform.runLater(() -> { 
                                int c = ageTextField.getCaretPosition();
                                ageTextField.setText(newValue.replaceFirst("^0*([0-9]+)", "$1"));
                                ageTextField.positionCaret(c);
                                
                            });
                        }
                    }
                } catch (Exception e) {
                    Platform.runLater(() -> { 
                        int c = ageTextField.getCaretPosition();
                        ageTextField.setText(oldValue);
                        ageTextField.positionCaret(c);

                    }); 
                    
                }
                
        });
        
        // Strip off leading spaces/zeroes and trailing spaces
        bibTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            bibTextField.setText(newValue.replaceFirst("^[ 0]*", ""));
        });
      
        bibTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("bibTextField out focus");
                
                
                //Find out if the bib changed, and if so, if it conflicts with anybody else
                
                // If it is blank, just let it be and return
                if (bibTextField.getText().isEmpty()) {
                    return;
                }
                bibTextField.setText(bibTextField.getText().replaceFirst("^[ 0]*", "").replaceFirst(" *$", "")); // string trailing spaces
                
                // If it is not blank (previous check...
                // And nobody is being edited...
                // And the bib belogs to somebody, then edit them
                if (editedParticipant == null && !bibTextField.getText().isEmpty() && participantDAO.getParticipantByBib(bibTextField.getText()) != null) { 
                    
                    //Hang on, they entered something into the first or lalst name fields
                    if (!firstNameField.getText().isEmpty() || !lastNameField.getText().isEmpty()) {
                        Alert alert = new Alert(AlertType.INFORMATION);
                        alert.setTitle("Bib Assignemtn Error");
                        alert.setHeaderText("Bib " + bibTextField.getText() + " is in use!");
                        alert.setContentText("This bib has already been assigned to " + participantDAO.getParticipantByBib(bibTextField.getText()).fullNameProperty().getValueSafe());
                        alert.showAndWait();
                        
                        bibTextField.setText("");
                        bibTextField.requestFocus();
                        bibTextField.selectAll();
                    } else { //If the name fields are blank, then edit
                        editParticipant(participantDAO.getParticipantByBib(bibTextField.getText()));
                        //focus on the name since editParticipant sets the focus to the bibTextField
                        firstNameField.requestFocus();
                        return;
                    }
                }
                
                
                if ( editedParticipant == null || !bibTextField.getText().equals(editedParticipant.getBib())) { 
                    
                    if (participantDAO.getParticipantByBib(bibTextField.getText()) != null) {
                        //Error Alert
                        Alert alert = new Alert(AlertType.INFORMATION);
                        alert.setTitle("Bib Assignemtn Error");
                        alert.setHeaderText("Bib " + bibTextField.getText() + " is in use!");
                        alert.setContentText("This bib has already been assigned to " + participantDAO.getParticipantByBib(bibTextField.getText()).fullNameProperty().getValueSafe());
                        alert.showAndWait();
                        
                        //Revert, focus, select
                        bibTextField.setText(editedParticipant.getBib());
                        bibTextField.requestFocus();
                        bibTextField.selectAll();
                        
                    } else {
                        
                        if (RaceDAO.getInstance().listWaves().size()== 1){
                            waveComboBox.getCheckModel().checkAll(); // Only one to check anyway
                        } else {
                            // Figure out what wave they belong to based on the bib entered...
                    
                            AlphanumericComparator comp = new AlphanumericComparator(); 
                            waveComboBox.getCheckModel().clearChecks();
                            Map raceMap = new HashMap(); 
                            waveComboBox.getItems().forEach(i -> {
                                if (i.getWaveAssignmentMethod() == WaveAssignment.BIB) {
                                    String start = i.getWaveAssignmentStart(); 
                                    String end = i.getWaveAssignmentEnd(); 
                                    if (!(start.isEmpty() && end.isEmpty()) && (comp.compare(start, bibTextField.getText()) <= 0 || start.isEmpty()) && (comp.compare(end, bibTextField.getText()) >= 0 || end.isEmpty())) {
                                        if(raceMap.containsKey(i.getRace())) {
                                            //System.out.println("Already in race " + i.getRace().getRaceName()); 
                                        } else {
                                            waveComboBox.getCheckModel().check(i);
                                            //System.out.println("Bib " + bibTextField.getText() + " matched wave " + i.getWaveName() + " results: "+ comp.compare(start, bibTextField.getText()) + " and " + comp.compare(end, bibTextField.getText()) );
                                            raceMap.put(i.getRace(), true); 
                                        }
                                } else {
                                        //System.out.println("Bib " + bibTextField.getText() + " did not match wave " + i.getWaveName() + " results: "+ comp.compare(start, bibTextField.getText()) + " and " + comp.compare(end, bibTextField.getText()) );
                                    }
                                }
                            }); 
                        }
                    }
                }
            } else {
                //System.out.println("bibTextField in focus");
            }
        });
        
        //sexPrefixSelectionChoiceBox.setItems(FXCollections.observableArrayList("M","F") );
        sexTextField.setTextFormatter(new TextFormatter<>((change) -> {
            change.setText(change.getText().toUpperCase());
            return change;
        }));
        
        // TODO: Get the CellFactory to work
        raceColumn.setCellValueFactory(person -> person.getValue().wavesChangedCounterProperty());
        
        raceColumn.setCellFactory(column -> {
            return new TableCell<Participant, Number>() {
                @Override
                protected void updateItem(Number n, boolean empty) {
                    super.updateItem(n, empty);
                    Participant p = (Participant)getTableRow().getItem();
                    if (p == null) {
                        setText(null);
                        return;
                    }
                    ObservableList<Wave> waves = p.wavesObservableList();
                    if (waves == null || empty) {
                        setText(null);
                    } else {
                        WaveStringConverter wsc = new WaveStringConverter();
                        setText(waves.stream().map (w -> wsc.toString(w)).collect(Collectors.joining(",")));
                    }
                }
            };
        });

//        raceColumn.setCellValueFactory((CellDataFeatures<Participant, String> p) -> {
//            StringProperty wavesString = new SimpleStringProperty();
//            WaveStringConverter wString=new WaveStringConverter();
//            if (p.getValue().wavesObservableList().isEmpty()) return new SimpleStringProperty();
//            
//            p.getValue().wavesObservableList().stream().forEach(w -> {
//                wavesString.setValue(wavesString.getValueSafe() + wString.toString(w) + ", " );
//                //System.out.println("Checking " + w.getID() + " " + w.toString());
//            });
//            // remove the trailing ", "
//            wavesString.set(wavesString.getValueSafe().substring(0, wavesString.getValueSafe().length()-2));
//            
//            
//            return wavesString;
//        });
        
        //Sorting the raceColumn triggers a StackOverflow
        raceColumn.sortableProperty().setValue(Boolean.FALSE);
        
        if (RaceDAO.getInstance().listWaves().size() == 1 ) raceColumn.visibleProperty().set(false);
            else raceColumn.visibleProperty().set(true);
        
        ObservableList<Status> statusypeList = FXCollections.observableArrayList(Arrays.asList(Status.values()));
        statusPrefixSelectionChoiceBox.setItems(statusypeList);
        statusPrefixSelectionChoiceBox.getSelectionModel().select(Status.GOOD);
        
        statusColumn.setCellValueFactory(person -> person.getValue().statusProperty());
        
        birthdayDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null ) {
                ageTextField.requestFocus();
                ageTextField.selectAll();
                return;
            }
            if (newValue.equals(oldValue)) return;
            
            //sexPrefixSelectionChoiceBox.requestFocus();
            sexTextField.requestFocus();
            ageTextField.setText(Integer.toString(Period.between(newValue, Event.getInstance().getLocalEventDate()).getYears()));
        
        });
        
        birthdayDatePicker.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("birthdayDatePicker out focus");
                
                //If it is now null, just bail
                if (birthdayDatePicker.getValue() == null) return; 
                
                //else sexPrefixSelectionChoiceBox.requestFocus();
                else sexTextField.requestFocus();
            }
        });
        
        formAddButton.defaultButtonProperty().bind(formAddButton.focusedProperty());
        formUpdateButton.defaultButtonProperty().bind(formUpdateButton.focusedProperty());
        deleteParticipantsButton.disableProperty().bind(participantTableView.getSelectionModel().selectedItemProperty().isNull());
        
        // Add button only enabled if both the first and last names are empty
        formAddButton.disableProperty().bind(Bindings.and(firstNameField.textProperty().isEmpty(), lastNameField.textProperty().isEmpty()));
        
        
        // Custom attribute setup
        displayCustomAttributes();

    }
    
    @FXML
    protected void addPerson(ActionEvent fxevent) {
        // Make sure they actually entered something first
        System.out.println("addPerson fired");
        if (!(firstNameField.getText().isEmpty() && lastNameField.getText().isEmpty())) {
            Participant p = new Participant(firstNameField.getText(),
                lastNameField.getText()
            );
            
            p.setMiddleName(middleNameTextField.getText());
            //p.setEmail(emailField.getText());
            p.setBib(bibTextField.getText());
            p.setBirthday(birthdayDatePicker.getValue());
            try {
                p.setAge(Integer.parseUnsignedInt(ageTextField.getText()));
            } catch(NumberFormatException e) {
                p.setAge(0);
            }
            //p.setSex(sexTextField.getText());
            //p.setSex(sexPrefixSelectionChoiceBox.getSelectionModel().getSelectedItem());
            p.setSex(sexTextField.getText());
            p.setState(stateTextField.getText());
            p.setCity(cityTextField.getText());
            p.setZip(zipTextField.getText());
            p.setCountry(countryTextField.getText());
            
            // If there is only one wave, assign it
            if (RaceDAO.getInstance().listWaves().size()== 1){
                p.setWaves(RaceDAO.getInstance().listWaves());
            } else  {
                p.setWaves(waveComboBox.getCheckModel().getCheckedItems());
            }
            
            p.setNote(noteTextField.getText());
            p.setStatus(statusPrefixSelectionChoiceBox.getSelectionModel().getSelectedItem());
            
            //custom attributes
            participantDAO.getCustomAttributes().forEach(a -> {
                Integer aID = a.getID();
                switch (a.getAttributeType()) {
                    case LIST:
                        {
                            p.setCustomAttribute(aID, customAttributesChoiceBoxes.get(aID).getValue());
                            break;
                        }
                    case DATE:
                        {
                            try {
                            p.setCustomAttribute(aID, customAttributesDatePickers.get(aID).getValue().format(DateTimeFormatter.ISO_DATE));
                            } catch (Exception e){
                                p.setCustomAttribute(aID, "");
                            }
                            break;
                        }
                    case BOOLEAN:
                        {
                            p.setCustomAttribute(aID, customAttributesCheckBox.get(aID).selectedProperty().getValue().toString());
                            break;
                        }
                    default:
                        {
                            p.setCustomAttribute(aID, customAttributesTextFields.get(aID).getText());                        
                            break;
                        }
                }
            });
            
            
            //participantsList.add(p);
            participantDAO.addParticipant(p);
            
            resetForm();
            
            bibTextField.requestFocus();
        }
    }

    public ObservableList<Participant> getParticipantList(){
        
        return participantDAO.listParticipants(); 
    }
    
    public void removeParticipant(Participant p)     {
        participantDAO.removeParticipant(p);
    }
    
    public void removeParticipants(ObservableList p) {
        //long starttime = System.currentTimeMillis();
        
        participantDAO.removeParticipants(p);
        
        //long endtime = System.currentTimeMillis();
        //System.out.println("Delete Time: " + (endtime-starttime));
    }
    
    public void editParticipant(Participant p) {
        
        editedParticipant=p;
        waveComboBox.getItems().setAll(RaceDAO.getInstance().listWaves());
        //sexTextField.setText(p.getSex());
        //sexPrefixSelectionChoiceBox.getSelectionModel().select(p.getSex());
        sexTextField.setText(p.getSex());
        ageTextField.setText(p.getAge().toString());
        bibTextField.setText(p.getBib());
        firstNameField.setText(p.getFirstName());
        middleNameTextField.setText(p.getMiddleName());
        lastNameField.setText(p.getLastName());
        //emailField.setText(p.getEmail());   
        cityTextField.setText(p.getCity()); 
        stateTextField.setText(p.getState());
        zipTextField.setText(p.getZip());
        countryTextField.setText(p.getCountry());
        birthdayDatePicker.setValue(p.birthdayProperty().getValue());
        
        statusPrefixSelectionChoiceBox.getSelectionModel().select(p.statusProperty().getValue());
        noteTextField.setText(p.getNote());
             
        waveComboBox.getCheckModel().clearChecks();
        
        
        p.wavesObservableList().stream().forEach(w -> {
            waveComboBox.getCheckModel().check(w);
            //System.out.println("Checking " + w.getID() + " " + w.toString());
        });
        waveComboBox.getCheckModel().getCheckedItems().forEach(w -> {
            //System.out.println("Checked " + w.getID() + " " + w.toString());
        });
        //waveComboBox.getCheckModel().check(null);
        
        //custom attributes
        participantDAO.getCustomAttributes().forEach(a -> {
            Integer aID = a.getID();
            switch (a.getAttributeType()) {
                case LIST:
                    {
                        if(! editedParticipant.getCustomAttribute(aID).getValueSafe().isEmpty())
                            customAttributesChoiceBoxes.get(aID).getSelectionModel().select(editedParticipant.getCustomAttribute(aID).getValueSafe());
                        else customAttributesChoiceBoxes.get(aID).getSelectionModel().selectFirst();
                        break;
                    }
                case DATE:
                    {
                        if(! editedParticipant.getCustomAttribute(aID).getValueSafe().isEmpty())
                            customAttributesDatePickers.get(aID).setValue(LocalDate.parse(editedParticipant.getCustomAttribute(aID).getValueSafe(), DateTimeFormatter.ISO_DATE));
                        else customAttributesDatePickers.get(aID).setValue(null);
                        break;
                    }
                case BOOLEAN:
                    {
                        if(! editedParticipant.getCustomAttribute(aID).getValueSafe().isEmpty())
                            customAttributesCheckBox.get(aID).setSelected(Boolean.parseBoolean(editedParticipant.getCustomAttribute(aID).getValueSafe()));
                        else customAttributesCheckBox.get(aID).setSelected(false);
                        break;
                    }
                default:
                    {
                        customAttributesTextFields.get(aID).setText(editedParticipant.getCustomAttribute(aID).getValueSafe());
                        break;
                    }
            }
        });

        
        // Make the update button visible and hide the add button
        formUpdateButton.setVisible(true);
        formUpdateButton.setManaged(true);
        //formUpdateButton.setDefaultButton(true);
        formAddButton.setVisible(false);
        formAddButton.setManaged(false);
        bibTextField.requestFocus();
    }
    
    public void updateParticipant(ActionEvent fxevent){
        
        if (!(firstNameField.getText().isEmpty() && lastNameField.getText().isEmpty())) {
            
            // pull the list of checked waves
            editedParticipant.setBib(bibTextField.getText());
            
            editedParticipant.setFirstName(firstNameField.getText());
            editedParticipant.setMiddleName(middleNameTextField.getText());
            editedParticipant.setLastName(lastNameField.getText());
            
            
            editedParticipant.setBirthday(birthdayDatePicker.getValue());
            
            try {
                editedParticipant.setAge(Integer.parseUnsignedInt(ageTextField.getText()));
            } catch(NumberFormatException e) {
                editedParticipant.setAge(0);
            }
            
            //editedParticipant.setSex(sexPrefixSelectionChoiceBox.getSelectionModel().getSelectedItem());
            editedParticipant.setSex(sexTextField.getText());
            editedParticipant.setCity(cityTextField.getText());
            editedParticipant.setState(stateTextField.getText());
            editedParticipant.setZip(zipTextField.getText());
            editedParticipant.setCountry(countryTextField.getText());
            
            editedParticipant.setStatus(statusPrefixSelectionChoiceBox.getSelectionModel().getSelectedItem());
            editedParticipant.setNote(noteTextField.getText());
            
            // If there is only one wave, assign it
            if (RaceDAO.getInstance().listWaves().size()== 1){
                editedParticipant.setWaves(RaceDAO.getInstance().listWaves());
            } else  {
                editedParticipant.setWaves(waveComboBox.getCheckModel().getCheckedItems());
            }
            
            System.out.println("Upating....");
            //custom attributes
            participantDAO.getCustomAttributes().forEach(a -> {
                System.out.println("Upating " + a.getName());
                Integer aID = a.getID();
                switch (a.getAttributeType()) {
                    case LIST:
                        {
                            editedParticipant.setCustomAttribute(aID, customAttributesChoiceBoxes.get(aID).getValue());
                            break;
                        }
                    case DATE:
                        {
                            try {
                            editedParticipant.setCustomAttribute(aID, customAttributesDatePickers.get(aID).getValue().format(DateTimeFormatter.ISO_DATE));
                            } catch (Exception e){
                                editedParticipant.setCustomAttribute(aID, "");
                            }
                            break;
                        }
                    case BOOLEAN:
                        {
                            editedParticipant.setCustomAttribute(aID, customAttributesCheckBox.get(aID).selectedProperty().getValue().toString());
                            break;
                        }
                    default:
                        {
                            editedParticipant.setCustomAttribute(aID, customAttributesTextFields.get(aID).getText());                        
                            break;
                        }
                }
            });
            
            
            
            // perform the actual update
            participantDAO.updateParticipant(editedParticipant);
            TimingDAO.getInstance().reprocessBib(editedParticipant.getBib());
            ResultsDAO.getInstance().reprocessAllCRs();
            
            // reset the fields
            resetForm();   
            
        }
        bibTextField.requestFocus();
    }
    
    public void resetForm() {
        
        // reset the fields
        editedParticipant=null; 
        //waveComboBox.getItems().setAll(RaceDAO.getInstance().listWaves());
        waveComboBox.getItems().setAll(RaceDAO.getInstance().listWaves().sorted((Wave u1, Wave u2) -> u1.toString().compareTo(u2.toString())));
        waveComboBox.getCheckModel().clearChecks();
        waveComboBox.getCheckModel().check(0);


        //sexPrefixSelectionChoiceBox.getSelectionModel().clearSelection();
        sexTextField.setText("");
        ageTextField.setText("");
        bibTextField.setText("");
        firstNameField.setText("");
        middleNameTextField.setText("");
        lastNameField.setText("");
        
        birthdayDatePicker.setValue(null);
        
        cityTextField.setText("");
        stateTextField.setText("");
        zipTextField.setText("");
        countryTextField.setText("");
        
        noteTextField.setText("");
        statusPrefixSelectionChoiceBox.getSelectionModel().select(Status.GOOD);
        
   
        
        // set the Update buton to invisible
        formUpdateButton.setVisible(false);
        formUpdateButton.setManaged(false);
        //formUpdateButton.setDefaultButton(false);
        
        //custom attributes
        participantDAO.getCustomAttributes().forEach(a -> {
            Integer aID = a.getID();
            switch (a.getAttributeType()) {
                case LIST:
                    {
                        customAttributesChoiceBoxes.get(aID).getSelectionModel().selectFirst();
                        break;
                    }
                case DATE:
                    {
                        customAttributesDatePickers.get(aID).setValue(null);
                        break;
                    }
                case BOOLEAN:
                    {
                        customAttributesCheckBox.get(aID).setSelected(false);
                        break;
                    }
                default:
                    {
                        customAttributesTextFields.get(aID).setText("");
                        break;
                    }
            }
        });
                
        // make the add button visible
        formAddButton.setVisible(true);
        formAddButton.setManaged(true);
        //formAddButton.setDefaultButton(true);
        bibTextField.requestFocus();
    }
    public void resetForm(ActionEvent fxevent){
        resetForm();
    }
    
    public void deleteParticipants(ActionEvent fxevent){
        //TODO: if over X number, prompt
        
        if (editedParticipant!= null && participantTableView.getSelectionModel().getSelectedItems().contains(editedParticipant)) resetForm();
        removeParticipants(FXCollections.observableArrayList(participantTableView.getSelectionModel().getSelectedItems()));
        participantTableView.getSelectionModel().clearSelection();
    }
    
    public void importParticipants(ActionEvent fxevent) throws FlowException{
        // todo
        Stage importStage = new Stage();
                
        Flow flow  = new Flow(ImportWizardController.class);
        
        FlowHandler flowHandler = flow.createHandler();

        StackPane pane = flowHandler.start(new DefaultFlowContainer());
        
        importStage.setScene(new Scene(pane));
        importStage.initModality(Modality.APPLICATION_MODAL);
        importStage.setTitle("Import Participants...");
        importStage.show(); 
       
    }
    
    public void exportParticipants(ActionEvent fxevent){
        // todo
        // Open a dialog box to select the fields to export
        // Then the name of the file
        // finally do the dump as a csv file. 
        
        // Figure out how to deal with nultiple race/waves. We don't let folks 
        // import them directly right now. Maybe we just hide that field for now?
        
        // An innter class to map the field names to their internal codes
        class AttributeMap {
            public SimpleStringProperty key = new SimpleStringProperty();
            public SimpleStringProperty value= new SimpleStringProperty();

            private AttributeMap(String k, String v) {
                key.setValue(k);
                value.setValue(v);            
            }
            
            @Override
            public String toString(){
                return value.getValueSafe();
            }
            
        }
        
        // Create the two lists. One of possible fields to export, the other
        // of selected fields to export
        
        ObservableList<AttributeMap> availableAttributes = FXCollections.observableArrayList();
        ObservableList<AttributeMap> sortAttributes = FXCollections.observableArrayList();

        Participant.getAvailableAttributes().entrySet().stream().forEach((entry) -> {
            availableAttributes.add(new AttributeMap(entry.getKey(),entry.getValue()));
            sortAttributes.add(new AttributeMap(entry.getKey(),entry.getValue()));
        });
         
        participantDAO.getCustomAttributes().forEach(ca -> {
            availableAttributes.add(new AttributeMap(ca.getID().toString(),ca.getName()));
            sortAttributes.add(new AttributeMap(ca.getID().toString(),ca.getName()));
        });
        
        //Add Race/Wave attributes IF there are multiple races/waves to deal with
        
        if (RaceDAO.getInstance().listRaces().size() > 1) availableAttributes.add(new AttributeMap("RACE","Race"));
        if (RaceDAO.getInstance().listWaves().size() > RaceDAO.getInstance().listRaces().size()) availableAttributes.add(new AttributeMap("WAVE","Wave"));
        
        // Setup a ListSelectionView
        ListSelectionView<AttributeMap> listSelectionView = new ListSelectionView<>();
        listSelectionView.setSourceItems(availableAttributes);
        
        
        // If more than one race/wave, setup a filter by race/wave
        
        // Create a dialog
        Dialog<ObservableList<AttributeMap>> dialog = new Dialog();
        dialog.setTitle("Export Participants");
        dialog.setHeaderText("Select fields to export");

        // Set the button types.
        ButtonType exportButtonType = new ButtonType("Export...", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(exportButtonType, ButtonType.CANCEL);
        
        // setup a grid 
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));
        ColumnConstraints column = new ColumnConstraints(USE_COMPUTED_SIZE);
        grid.getColumnConstraints().add(column);
        
        // Add the race/wave filter if there is something to filter on
        
        CheckBox waveFilterCheckBox = new CheckBox("Filter by Race");
        HBox filterHBox = new HBox();
        filterHBox.setPadding(new Insets(0,0,0, 10));
        CheckComboBox<Race> waveFilterComboBox = new CheckComboBox();
        
        if (RaceDAO.getInstance().listRaces().size() > 1 || RaceDAO.getInstance().listWaves().size() > 1){
            
            waveFilterComboBox.getItems().addAll(RaceDAO.getInstance().listRaces());
            
            filterHBox.getChildren().addAll(waveFilterCheckBox,waveFilterComboBox);

            waveFilterComboBox.prefWidth(USE_COMPUTED_SIZE);
            waveFilterComboBox.maxWidth(USE_PREF_SIZE);
            filterHBox.setAlignment(Pos.CENTER_LEFT);

            filterHBox.setSpacing(5);
            waveFilterComboBox.disableProperty().bind(waveFilterCheckBox.selectedProperty().not());
            grid.add(filterHBox, 0, 0);
        }
        
        // Add the sort by comboBox
        ComboBox<AttributeMap> sortComboBox = new ComboBox();
        sortComboBox.setItems(sortAttributes);
        sortComboBox.getSelectionModel().selectFirst();
        
        HBox sortHBox = new HBox();
        sortHBox.setSpacing(5);
        sortHBox.setPadding(new Insets(0,0,0, 10));
        sortHBox.getChildren().addAll(new Label("Sort by:"), sortComboBox);
                
        // Add the listSelectionView
        grid.add(listSelectionView, 0,1);
        
        // TODO: Optional 'move up' 'move down' buttons for the listSelectionView
        grid.add(sortHBox,0,2);
        
        // wire in the ok button to be enabled when there is something in the list
       
        dialog.getDialogPane().lookupButton(exportButtonType).disableProperty().bind(Bindings.size(listSelectionView.targetItemsProperty().getValue()).greaterThan(0).not());
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == exportButtonType) {
                return listSelectionView.targetItemsProperty().get();
            }
            return null;
        });
        
        dialog.getDialogPane().setContent(grid);
        
        Optional<ObservableList<AttributeMap>> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            
//            result.get().forEach(a -> {
//                System.out.println("Export: Selected " + a.key.getValueSafe());
//            });
            
            // if OK, prompt for a target file
            final FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Participants...");
            fileChooser.setInitialDirectory(
                new File(System.getProperty("user.home"))
            ); 
            //fileChooser.getExtensionFilters().add(
            //        new FileChooser.ExtensionFilter("PikaTimer Events", "*.db") 
            //    );
            fileChooser.setInitialFileName("participants.csv");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV", "*.csv"),
                new FileChooser.ExtensionFilter("Text","*.txt"),
                new FileChooser.ExtensionFilter("All files", "*")
            );
            
            
            File file = fileChooser.showSaveDialog(participantTableView.getScene().getWindow());
            
            // make sure we have a file and that they didn't cancel on us
            if (file != null) {
                // Make suer we can open it for writting
                if (file.exists() && ! file.canWrite()) {
                    // oops, we can't write to the file
                    // toss up an error dialog and bail
                    Alert alert = new Alert(AlertType.INFORMATION);
                    alert.setTitle("Unable to export");
                    alert.setHeaderText("Unable to export.");
                    alert.setContentText("We are unable to export the data to the selected file. \n You do not appear to have permissions to write to it.");
                    alert.showAndWait();
                    return;
                } else if (!file.exists()){
                    try {
                        if (!file.createNewFile()){
                            // oops, we can't create the target file
                            // toss up an error dialog and bail
                            Alert alert = new Alert(AlertType.INFORMATION);
                            alert.setTitle("Unable to export");
                            alert.setHeaderText("Unable to export.");
                            alert.setContentText("We are unable to export the data to the selected file. \n You do not appear to have permissions to create the file.");
                            alert.showAndWait();

                            return;
                        }
                    } catch (IOException ex) {
                        // oops, we can't create the target file
                        // toss up an error dialog and bail
                        Alert alert = new Alert(AlertType.INFORMATION);
                            alert.setTitle("Unable to export");
                            alert.setHeaderText("Unable to export.");
                            alert.setContentText("We are unable to export the data to the selected file. \n You do not appear to have permissions to create the file.\n Error: " + ex.getLocalizedMessage());
                            alert.showAndWait();

                        return;
                    } 
                } 
                
                // At this point, we should have a file we can actually write to
                
                List<String> exportData = new ArrayList();
                
                //First the header
                String header = "";
                int fieldCount=result.get().size();
                for(int i = 0; i< fieldCount;i++) {
                    header+="\"";
                    header+=result.get().get(i).toString().replace("\"", "\"\"");
                    header+="\"";
                    if(i != fieldCount -1) header+=",";
                }
                
                exportData.add(header);
                
                // now itterate through the participants and create a quoted csv file
                // escape any double quotes in the values by using double double quotes
                
                // ParticipantsList -> Filter -> Sort -> itterate on attributes
                AlphanumericComparator acComparator = new AlphanumericComparator();
                String sortAttribute = sortComboBox.getSelectionModel().getSelectedItem().key.getValueSafe();
                participantDAO.listParticipants().stream().sorted((Participant o1, Participant o2) -> {
                    if(sortAttribute.matches("^\\d+$")) 
                        return acComparator.compare(o1.getCustomAttribute(Integer.parseInt(sortAttribute)).getValueSafe(), o2.getCustomAttribute(Integer.parseInt(sortAttribute)).getValueSafe());
                    else
                        return acComparator.compare(o1.getNamedAttribute(sortAttribute), o2.getNamedAttribute(sortAttribute));
                }).forEach(p -> {
                    BooleanProperty filtered = new SimpleBooleanProperty(true);
                    if (waveFilterCheckBox.selectedProperty().get()){
                        waveFilterComboBox.getCheckModel().getCheckedItems().forEach(r -> {
                            p.wavesObservableList().forEach(w -> {
                                if (w.getRace().equals(r)) filtered.setValue(false);
                            });
                        });
                    } else filtered.setValue(false); 
                    
                    if (filtered.getValue()) return;
                    
                    System.out.println("Exporting Particpant " + p.fullNameProperty().getValueSafe());
                    String part = "";
                    String fieldName;
                    
                    
                    for(int i = 0; i< fieldCount;i++) {
                        fieldName=result.get().get(i).key.getValueSafe();
                        part+="\"";
                        
                        //We need to handle the race/wave exports carefully
                        if (fieldName.equals("RACE") ){ 
                            part += p.wavesObservableList().stream().map (w -> w.getRace().getRaceName()).distinct().collect (Collectors.joining(","));
                        } else if (fieldName.equals("WAVE") ) {
                            part += p.wavesObservableList().stream().map (w -> w.getWaveName()).collect (Collectors.joining(","));
                        } else if (fieldName.matches("^\\d+$")) {
                            //System.out.println(" " + fieldName + " -> " + p.getCustomAttribute(Integer.parseInt(fieldName)).getValueSafe());
                            part+=p.getCustomAttribute(Integer.parseInt(fieldName)).getValueSafe().replace("\"", "\"\"");
                        }
                        else {
                            //System.out.println(" -> " + p.getNamedAttribute(fieldName));
                            part+=p.getNamedAttribute(fieldName).replace("\"", "\"\"");
                        }
                        
                        part+="\"";
                        
                        if(i != fieldCount -1) part+=",";
                    }
                    exportData.add(part);
                });

                try {
                    
                    Files.write(file.toPath(),exportData,StandardCharsets.UTF_8,StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    
                } catch (IOException e) {
                    // oops, we can't create the target file
                            // toss up an error dialog and bail
                            Alert alert = new Alert(AlertType.INFORMATION);
                            alert.setTitle("Unable to export");
                            alert.setHeaderText("An error occurred during the export.");
                            alert.setContentText("We are unable to export all of the data to the selected file. \n Error: " + e.getLocalizedMessage());

                            alert.showAndWait();

                }
            }
        }
    }
    
    public void clearParticipants(ActionEvent fxevent){
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Confirm Participant Removal");
        alert.setHeaderText("This will remove all Participants from the event.");
        alert.setContentText("This action cannot be undone. Are you sure you want to do this?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK){
            participantDAO.clearAll();
            resetForm(); 
        } else {
            // ... user chose CANCEL or closed the dialog
        }
        
    }
    
    public void bulkBibAssignment(ActionEvent fxevent){
        RaceDAO raceDAO = RaceDAO.getInstance();
        
        PrefixSelectionChoiceBox<Race> raceComboBox = new PrefixSelectionChoiceBox();
        raceComboBox.setItems(raceDAO.listRaces());
        raceComboBox.setPrefWidth(90);
        
        //TextField startTextArea = new TextField();
        //startTextArea.setPrefWidth(90);
        
        TextField startTextField = new TextField("1");
        startTextField.setPrefWidth(90);
        startTextField.textProperty().addListener((obs, prevVal, newVal) -> {
            int c = startTextField.getCaretPosition();
            if (newVal != null && !newVal.isEmpty() ){
                try {
                    Integer.parseUnsignedInt(newVal);
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        startTextField.textProperty().set(prevVal);
                        startTextField.positionCaret(c);
                    });
                }
            }
        });
        
        TextField endTextField = new TextField();
        endTextField.setPrefWidth(90);
        endTextField.textProperty().addListener((obs, prevVal, newVal) -> {
            int c = endTextField.getCaretPosition();
            if (newVal != null && !newVal.isEmpty() ){
                try {
                    Integer.parseUnsignedInt(newVal);
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        endTextField.textProperty().set(prevVal);
                        endTextField.positionCaret(c);
                    });
                }
            }
        });
        
        ToggleSwitch clearExistingToggleSwitch = new ToggleSwitch();
        clearExistingToggleSwitch.setPrefWidth(90);
        
        ObservableList<Attribute> attributeList = FXCollections.observableArrayList();
        attributeList.add(new Attribute("<None>","SKIP"));
        Participant.getAvailableAttributes().keySet().stream().sorted().forEach(k -> {
            attributeList.add(new Attribute(Participant.getAvailableAttributes().get(k),k));
        });
        ParticipantDAO.getInstance().getCustomAttributes().forEach(ca -> {
            attributeList.add(new Attribute(ca.getName(),ca.getID()));
        });
                
        
        PrefixSelectionChoiceBox<Attribute> sort1ComboBox = new PrefixSelectionChoiceBox();
        sort1ComboBox.setPrefWidth(90);
        PrefixSelectionChoiceBox<String> sort1TypeComboBox = new PrefixSelectionChoiceBox();
        PrefixSelectionChoiceBox<Attribute> sort2ComboBox = new PrefixSelectionChoiceBox();
        sort2ComboBox.setPrefWidth(90);
        PrefixSelectionChoiceBox<String> sort2TypeComboBox = new PrefixSelectionChoiceBox();
        
        sort1ComboBox.setItems(attributeList);
        sort1TypeComboBox.setItems(FXCollections.observableArrayList("Asc","Dec"));

        sort2TypeComboBox.setItems(FXCollections.observableArrayList("Asc","Dec"));
        sort2ComboBox.setItems(attributeList);
        
        TextArea skipBibs = new TextArea();
        skipBibs.setPrefWidth(90);
        skipBibs.setPrefHeight(75);
        skipBibs.textProperty().addListener((obs, prevVal, newVal) -> {
            System.out.println("SkipBibs updated: " + newVal);
            int c = skipBibs.getCaretPosition();
            for (String line: newVal.split("\\R")) {
                System.out.println("skipBibs: " + line);
                if (line != null && !line.isEmpty() ){
                    try {
                        Integer.parseUnsignedInt(line);
                    } catch (Exception e) {
                        System.out.println("Abort! reset to " + prevVal);
                        Platform.runLater(() -> {
                            skipBibs.textProperty().set(prevVal);
                            skipBibs.positionCaret(c);
                        });
                        break;
                    }
                }
            }
        });
        
        Dialog<Boolean> dialog = new Dialog();
        dialog.resizableProperty().set(true);
        dialog.getDialogPane().setMaxHeight(Screen.getPrimary().getVisualBounds().getHeight()-150);
        dialog.setTitle("Bib Assigment");
        dialog.setHeaderText("Bulk Bib Assigment");
        ButtonType okButtonType = new ButtonType("Assign", ButtonData.OK_DONE);
        
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);
       
        VBox contentVBox = new VBox();
        contentVBox.setSpacing(5);
        contentVBox.setMaxHeight(MAX_VALUE);
        contentVBox.setMaxWidth(MAX_VALUE);
        
        HBox raceSelection = new HBox();
        raceSelection.setSpacing(5);
        Label raceLabel = new Label("Race");
        raceLabel.setPrefWidth(100);
        raceSelection.getChildren().addAll(raceLabel, raceComboBox);
        raceComboBox.getSelectionModel().selectFirst();
        if (raceDAO.listRaces().size() > 1) contentVBox.getChildren().add(raceSelection);
        
        HBox start = new HBox();
        start.setSpacing(5);
        Label startLabel = new Label("Start Bib");
        startLabel.setPrefWidth(100);
        start.getChildren().addAll(startLabel,startTextField);
        contentVBox.getChildren().add(start);
                
        HBox end = new HBox();
        end.setSpacing(5);
        Label endLabel = new Label("End Bib");
        endLabel.setPrefWidth(100);
        end.getChildren().addAll(endLabel,endTextField);
        contentVBox.getChildren().add(end);
        
        HBox clear = new HBox();
        clear.setSpacing(5);
        Label clearLabel = new Label("Clear Existing Bibs");
        clearLabel.setPrefWidth(100);
        clearExistingToggleSwitch.selectedProperty().set(false);
        clear.getChildren().addAll(clearLabel,clearExistingToggleSwitch);
        contentVBox.getChildren().add(clear);
        
        HBox sort1 = new HBox();
        sort1.setSpacing(5);
        Label sort1Label = new Label("Sort By ");
        sort1Label.setPrefWidth(100);
        sort1TypeComboBox.getSelectionModel().selectFirst();
        sort1ComboBox.getSelectionModel().selectFirst();
        sort1.getChildren().addAll(sort1Label,sort1ComboBox,sort1TypeComboBox);
        contentVBox.getChildren().add(sort1);
        
        HBox sort2 = new HBox();
        sort2.setSpacing(5);
        Label sort2Label = new Label("Sort By ");
        sort2Label.setPrefWidth(100);
        sort2TypeComboBox.getSelectionModel().selectFirst();
        sort2ComboBox.getSelectionModel().selectFirst();
        sort2.getChildren().addAll(sort2Label,sort2ComboBox,sort2TypeComboBox);
        contentVBox.getChildren().add(sort2);
        
        HBox skip = new HBox();
        skip.setSpacing(5);
        Label skipLabel = new Label("Skip bibs ending:");
        skipLabel.setPrefWidth(100);
        skip.getChildren().addAll(skipLabel,skipBibs);
        contentVBox.getChildren().add(skip);
        
        dialog.getDialogPane().lookupButton(okButtonType).disableProperty().bind(startTextField.textProperty().isEmpty());

        dialog.getDialogPane().setContent(contentVBox);
        
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return Boolean.TRUE;
            }
            return null;
        });
        
        Platform.runLater(() -> { dialog.setY((Screen.getPrimary().getVisualBounds().getHeight()-dialog.getHeight())/2); });
        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent()) {
            AlphanumericComparator ac = new AlphanumericComparator();
            
            Integer currentBib = Integer.parseInt(startTextField.getText());
            Integer lastBib = Integer.MAX_VALUE;
            if (!endTextField.getText().isEmpty()) {
                try{
                    lastBib = Integer.parseInt(endTextField.getText());
                } catch (Exception ex){
                    lastBib = Integer.MAX_VALUE;
                }
            }
            
            Set<String> skipList = new HashSet(Arrays.asList(skipBibs.getText().split("\\R")));
            for(String t: skipList) {
                if (t.isEmpty()) skipList.remove(t);
                System.out.println("Skipping bibs ending with \"" + t + "\"");
            }
            
            Attribute s1 = sort1ComboBox.getSelectionModel().getSelectedItem();
            Attribute s2 = sort2ComboBox.getSelectionModel().getSelectedItem();
            String s1Type = sort1TypeComboBox.getSelectionModel().getSelectedItem();   
            String s2Type = sort2TypeComboBox.getSelectionModel().getSelectedItem();   
            List<Participant> assignees = participantDAO.listParticipants().stream()
                    .filter(p -> {  // race filter
                        System.out.println("F1: Evaling " + p.fullNameProperty().getValueSafe());
                        if (raceDAO.listRaces().size()== 1) return true; // only one race
                        if (p.wavesObservableList().isEmpty()) return true; // unassigned
                        if (p.wavesObservableList().stream().anyMatch((w) -> (w.getRace().equals(raceComboBox.getSelectionModel().getSelectedItem())))) {
                            return true;
                        }
                        System.out.println("F1: Rejected");
                        return false;
                    }) 
                    .filter(p -> {          // existing assignment filter
                        System.out.println("F2: Evaling " + p.fullNameProperty().getValueSafe());
                        if (clearExistingToggleSwitch.selectedProperty().get()) return true;
                        else if (p.getBib().isEmpty()) return true;
                        System.out.println("F2: Rejected");

                        return false;
                    }) 
                    .filter(p -> { // start bib filter
                        System.out.println("F3: Evaling " + p.fullNameProperty().getValueSafe());
                        if (p.getBib().isEmpty()) return true;
                        if (startTextField.getText().isEmpty()) return true;
                        System.out.println("F3: " + ac.compare(startTextField.getText(), p.getBib()));
                        return ac.compare(startTextField.getText(), p.getBib()) <= 0;
                    }) 
                    .filter(p -> {// end bib filter
                        System.out.println("F4: Evaling " + p.fullNameProperty().getValueSafe());
                        if (p.getBib().isEmpty()) return true;
                        if (endTextField.getText().isEmpty()) return true;
                        System.out.println("F4: " + ac.compare(endTextField.getText(), p.getBib()));

                        return ac.compare(endTextField.getText(), p.getBib()) >= 0;
                    }) 
                    .sorted((p1,p2) -> {
                        
                        if (s1.cKey >=0){
                            Integer r1 = ac.compare(p1.getCustomAttribute(s1.cKey).getValueSafe(), p2.getCustomAttribute(s1.cKey).getValueSafe());
                            System.out.println("Sort C1: " + p1.getCustomAttribute(s1.cKey).getValueSafe() + " vs " + p2.getCustomAttribute(s1.cKey).getValueSafe() + " -> " + r1);

                            if (r1  != 0) {
                                if (s1Type.equals("Asc"))return r1;
                                else return -r1;
                            }
                        } else if (! s1.key.equals("SKIP")) {
                            Integer r1 = ac.compare(p1.getNamedAttribute(s1.key), p2.getNamedAttribute(s1.key));
                            System.out.println("Sort A1: " + p1.getNamedAttribute(s1.key) + " vs " + p2.getNamedAttribute(s1.key) + " -> " + r1);
                            if (r1  != 0) {
                                if (s1Type.equals("Asc"))return r1;
                                else return -r1;
                            }
                        }
                        if (s2.cKey >=0){
                            Integer r2 = ac.compare(p1.getCustomAttribute(s2.cKey).getValueSafe(), p2.getCustomAttribute(s2.cKey).getValueSafe());
                            System.out.println("Sort C2: " + r2);
                            if (r2  != 0) {
                                if (s2Type.equals("Asc"))return r2;
                                else return -r2;
                            }
                        } else if (! s2.key.equals("SKIP")) {
                            Integer r2 = ac.compare(p1.getNamedAttribute(s2.key), p2.getNamedAttribute(s2.key));
                            System.out.println("Sort A2: " + p1.getNamedAttribute(s2.key) + " vs " + p2.getNamedAttribute(s2.key) + " -> " + r2);
                            if (r2  != 0) {
                                if (s2Type.equals("Asc"))return r2;
                                else return -r2;
                            }
                        }
                        System.out.println("Sort returning 0");
                        return 0;
                    
                    })  // Sort them
                    .collect(Collectors.toList());
            
            
            Participant existing = null;
            for(Participant p: assignees){
                if (currentBib > lastBib) break;
                System.out.println("Assigning bib for " + p.fullNameProperty().getValueSafe());
                System.out.println("  currentBib is now " + currentBib.toString());
                if (!clearExistingToggleSwitch.selectedProperty().get()) {
                    while(currentBib <= lastBib && participantDAO.getParticipantByBib(currentBib.toString()) != null){
                        System.out.println("  currentBib is now " + currentBib.toString());
                        currentBib++;
                    }
                }
                if (!skipList.isEmpty()) {
                    System.out.println("skiList is NOT empty");
                    Boolean good = true;
                    do {
                        good = true;
                        for(String t: skipList){
                            if (currentBib.toString().endsWith(t)) good=false;
                        }
                        if (good==false) currentBib++;
                        if (currentBib > lastBib) break;
                    } while (!good);
                } else {
                    System.out.println("skiList was empty");
                }
                if (currentBib > lastBib) break;
                
                // This should be null if we are not clearing existing
                existing = participantDAO.getParticipantByBib(currentBib.toString());
                
                if (existing != null && !existing.equals(p)) {
                    existing.setBib("OLD: " + currentBib.toString());
                    participantDAO.updateParticipant(existing);
                }
                System.out.println("Assigning " + currentBib.toString() + "...");
                p.setBib(currentBib.toString());
                p.setWaves(participantDAO.getWaveByBib(currentBib.toString()));
                participantDAO.updateParticipant(p);
                System.out.println("  " + p.fullNameProperty().getValueSafe() + " now has bib " + currentBib);
                currentBib++;
            }
        }
    }
    
    
    public void setupCustomAttributes(ActionEvent fxevent){
        // Do something.... 
        List<CustomAttribute> customAttributes = new ArrayList(participantDAO.getCustomAttributes());
        List<CustomAttribute> deletedCustomAttributes = new ArrayList();
        
        // Create the base dialog
        Dialog<Boolean> dialog = new Dialog();
        dialog.resizableProperty().set(true);
        dialog.getDialogPane().setMaxHeight(Screen.getPrimary().getVisualBounds().getHeight()-150);
        dialog.setTitle("Custom Attributes");
        dialog.setHeaderText("Custom Attributes");
        ButtonType okButtonType = new ButtonType("Close", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType);
        
        VBox contentVBox = new VBox();
        contentVBox.setSpacing(5);
        contentVBox.setMaxHeight(MAX_VALUE);
        contentVBox.setMaxWidth(MAX_VALUE);

        
        Button addCustomButton = new Button("Add New...");
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setMaxWidth(MAX_VALUE);
        scrollPane.setMaxHeight(MAX_VALUE);
        scrollPane.fitToWidthProperty().set(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        VBox casVBox = new VBox();
        casVBox.setPrefWidth(400);
        casVBox.setPrefHeight(200);
        casVBox.setSpacing(5);
        casVBox.setMaxWidth(MAX_VALUE);
        casVBox.setMaxHeight(MAX_VALUE);
        
        scrollPane.setContent(casVBox);
        contentVBox.getChildren().addAll(addCustomButton,scrollPane);
        addCustomButton.setOnAction(a -> {
            CustomAttribute ca = new CustomAttribute();
            customAttributes.add(ca);
            VBox caVBox = new VBox();
            caVBox.setPadding(new Insets(5,5,5,5));
            caVBox.setSpacing(5);
            HBox caHBox = new HBox();
            caHBox.setMaxWidth(MAX_VALUE);
            caHBox.setSpacing(5);
            caHBox.setAlignment(Pos.CENTER_LEFT);
            Label nameLabel = new Label("Name: ");
            Label typeLabel = new Label("Type: ");
            TextField name = new TextField();
            name.setPrefWidth(150);
            name.textProperty().addListener((observable, oldValue, newValue) -> {
                System.out.println("TextField Text Changed (newValue: " + newValue + ")");
                ca.nameProperty().set(newValue);
            });
            
            VBox caListVBox = new VBox();
            caListVBox.setSpacing(5);
            ListView<String> list = new ListView<String>(ca.allowableValuesProperty());
            list.setPrefHeight(100);
            list.setMinHeight(100);
            list.setCellFactory(TextFieldListCell.forListView());
            list.setEditable(true);
            caListVBox.getChildren().add(list);
            HBox caListHBox = new HBox();
            Button add = new Button("Add");
            add.setOnAction(aa -> {
                list.getItems().add("Item");
                list.scrollTo(list.getItems().size() - 1);
                list.layout();
                list.edit(list.getItems().size() - 1);
            });
            
            
            Button remove = new Button("Remove");
            remove.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
            remove.setOnAction(ra -> {
                list.getItems().remove(list.getSelectionModel().getSelectedItem());
            });
            
            list.setOnEditCommit((ListView.EditEvent<String> t) -> {
                System.out.println("setOnEditCommit " + t.getIndex());
                if (t.getIndex() >= 0 && t.getIndex() < t.getSource().getItems().size()) {
                    if (t.getNewValue().isEmpty()) {
                        list.getItems().remove(t.getIndex());
                    } else {
                        list.getItems().set(t.getIndex(), t.getNewValue());
                    }
                } else {
                    System.out.println("Timing setOnEditCancel event out of index: " + t.getIndex());
                }
                add.requestFocus();
                add.setDefaultButton(true);
            });
            list.setOnEditCancel((ListView.EditEvent<String> t) -> {
                System.out.println("setOnEditCancel " + t.getIndex());
                if (t.getIndex() >= 0 && t.getIndex() < t.getSource().getItems().size()) {
                   
                    if (t.getNewValue() == null || t.getNewValue().isEmpty()) {
                        list.getItems().remove(t.getIndex());
                    } 
                } else {
                    System.out.println("Timing setOnEditCancel event out of index: " + t.getIndex());
                }
                add.requestFocus();
                add.setDefaultButton(true);
            });
            caListHBox.getChildren().addAll(add,remove);
            caListVBox.getChildren().add(caListHBox);
            
            PrefixSelectionChoiceBox type = new PrefixSelectionChoiceBox();
            ObservableList<CustomAttributeType> typeList = FXCollections.observableArrayList(Arrays.asList(CustomAttributeType.values()));
            type.setItems(typeList);
            type.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<CustomAttributeType>() {
                @Override
                public void changed(ObservableValue<? extends CustomAttributeType> observableValue, CustomAttributeType o, CustomAttributeType n) {
                    if (n.equals(CustomAttributeType.LIST)) {
                        caListVBox.setManaged(true);
                        caListVBox.setVisible(true);
                    } else {
                        caListVBox.setManaged(false);
                        caListVBox.setVisible(false);
                    }
                    ca.setAttributeType(n);
                }
            });
            type.getSelectionModel().select(CustomAttributeType.STRING);
            
            Pane spring = new Pane();
            spring.setMaxWidth(MAX_VALUE);
            HBox.setHgrow(spring, Priority.ALWAYS);
            
            Button deleteButton = new Button("Delete");
            Separator sep = new Separator();
            
            caHBox.getChildren().addAll(nameLabel,name,typeLabel,type,spring,deleteButton);
            caVBox.getChildren().addAll(caHBox,caListVBox,sep);
            
            casVBox.getChildren().addAll(caVBox);
            
            deleteButton.setOnAction(da -> {
                casVBox.getChildren().remove(caVBox);
                customAttributes.remove(ca);
            });
        });
        
        // existing attributes
        customAttributes.forEach(attrib -> {
            
            VBox caVBox = new VBox();
            caVBox.setSpacing(5);
            caVBox.setPadding(new Insets(5,5,5,5));
            HBox caHBox = new HBox();
            caHBox.setMaxWidth(MAX_VALUE);
            caHBox.setSpacing(5);
            caHBox.setAlignment(Pos.CENTER_LEFT);
            Label nameLabel = new Label("Name: ");
            Label typeLabel = new Label("Type: ");
            TextField name = new TextField(attrib.getName());
            name.setPrefWidth(150);
            
            name.textProperty().addListener((observable, oldValue, newValue) -> {
                System.out.println("TextField Text Changed (newValue: " + newValue + ")");
                attrib.nameProperty().set(newValue);
            });
            
            VBox caListVBox = new VBox();
            if (attrib.getAttributeType() == null) attrib.setAttributeType(CustomAttributeType.STRING);
            if (attrib.getAttributeType().equals(CustomAttributeType.LIST)) {

                caListVBox.setSpacing(5);
                ListView<String> list = new ListView<String>(attrib.allowableValuesProperty());
                list.setPrefHeight(100);
                list.setMinHeight(100);
                list.setCellFactory(TextFieldListCell.forListView());
                list.setEditable(true);
                caListVBox.getChildren().add(list);
                HBox caListHBox = new HBox();
                Button add = new Button("Add");
                add.setOnAction(aa -> {
                    list.getItems().add("Item");
                    list.scrollTo(list.getItems().size() - 1);
                    list.layout();
                    list.edit(list.getItems().size() - 1);
                });


                Button remove = new Button("Remove");
                remove.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
                remove.setOnAction(ra -> {
                    list.getItems().remove(list.getSelectionModel().getSelectedItem());
                });

                list.setOnEditCommit((ListView.EditEvent<String> t) -> {
                    System.out.println("setOnEditCommit " + t.getIndex());
                    if (t.getIndex() >= 0 && t.getIndex() < t.getSource().getItems().size()) {
                        if (t.getNewValue().isEmpty()) {
                            list.getItems().remove(t.getIndex());
                        } else {
                            list.getItems().set(t.getIndex(), t.getNewValue());
                        }
                    } else {
                        System.out.println("Timing setOnEditCancel event out of index: " + t.getIndex());
                    }
                    add.requestFocus();
                    add.setDefaultButton(true);
                });
                list.setOnEditCancel((ListView.EditEvent<String> t) -> {
                    System.out.println("setOnEditCancel " + t.getIndex());
                    if (t.getIndex() >= 0 && t.getIndex() < t.getSource().getItems().size()) {

                        if (t.getNewValue() == null || t.getNewValue().isEmpty()) {
                            list.getItems().remove(t.getIndex());
                        } 
                    } else {
                        System.out.println("Timing setOnEditCancel event out of index: " + t.getIndex());
                    }
                    add.requestFocus();
                    add.setDefaultButton(true);
                });
                caListHBox.getChildren().addAll(add,remove);
                caListVBox.getChildren().add(caListHBox);
            }
            
            Label type = new Label(attrib.getAttributeType().toString());
            
            Pane spring = new Pane();
            spring.setMaxWidth(MAX_VALUE);
            HBox.setHgrow(spring, Priority.ALWAYS);
            
            Button deleteButton = new Button("Delete");
            Separator sep = new Separator();
            
            caHBox.getChildren().addAll(nameLabel,name,typeLabel,type,spring,deleteButton);
            if (attrib.getAttributeType().equals(CustomAttributeType.LIST)) caVBox.getChildren().addAll(caHBox,caListVBox,sep);
            else caVBox.getChildren().addAll(caHBox,sep);
                
            casVBox.getChildren().addAll(caVBox);
            
            deleteButton.setOnAction(da -> {
                casVBox.getChildren().remove(caVBox);
                customAttributes.remove(attrib);
                deletedCustomAttributes.add(attrib);
            });
        });
        
        
        dialog.getDialogPane().setContent(contentVBox);
        
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return Boolean.TRUE;
            }
            return null;
        });
        
        Platform.runLater(() -> { dialog.setY((Screen.getPrimary().getVisualBounds().getHeight()-dialog.getHeight())/2); });
        Optional<Boolean> result = dialog.showAndWait();
        if (result.isPresent()) {
            deletedCustomAttributes.forEach(ca -> {
                System.out.println("Deleting Custom Attribute " + ca.getName());
                participantDAO.deleteCustomAttribute(ca);
            });
            customAttributes.forEach(ca -> {
                System.out.println("Saving Custom Attribute " + ca.getName());
                // remove duplicates from the allowable values list
                Set<String> s = new LinkedHashSet<>(ca.allowableValuesProperty());
                ca.setAllowableValues(new ArrayList(s)); 
                
                // Save it
                participantDAO.saveCustomAttribute(ca);
            });
        }
        
        // Rework the table columns and form input fields
        displayCustomAttributes();
        
    }
    
    private void displayCustomAttributes() {
        
        // Clear the form
        customAttributesVBox.getChildren().clear();
        customAttributesTextFields.clear();
        customAttributesChoiceBoxes.clear();
        
        // clear the table columns
        customAttributesColumns.forEach(tc -> {
            participantTableView.getColumns().remove(tc);
        });
        customAttributesColumns.clear();
        
        // recreate the form and columns
        participantDAO.getCustomAttributes().forEach(a -> {
            Integer aID = a.getID();
            String name = a.getName();
            
            // table colums
                TableColumn<Participant,String> aColumn = new TableColumn(name);
                aColumn.setPrefWidth(75.0);
                participantTableView.getColumns().add(aColumn);
                aColumn.setCellValueFactory(cellData -> {
                    Participant p = cellData.getValue();
                    if (p == null) { return new SimpleStringProperty("");
                    } else {
                        return p.getCustomAttribute(aID);
                    }
                });
            customAttributesColumns.add(aColumn);
            
            //create the form
            HBox aHBox = new HBox();
            aHBox.setSpacing(2);
            HBox.setHgrow(aHBox, Priority.ALWAYS);
            Label aLabel = new Label(name);
            aLabel.setPrefWidth(50);
            aHBox.getChildren().add(aLabel);
            switch (a.getAttributeType()) {
                case LIST:
                    {
                        PrefixSelectionChoiceBox<String> aInput = new PrefixSelectionChoiceBox();
                        aInput.setItems(a.allowableValuesProperty());
                        aInput.getSelectionModel().selectFirst();
                        customAttributesChoiceBoxes.put(aID, aInput);
                        HBox.setHgrow(aInput, Priority.ALWAYS);
                        aHBox.getChildren().add(aInput);
                        break;
                    }
                case DATE:
                    {
                        DatePicker aInput = new DatePicker();
                        customAttributesDatePickers.put(aID, aInput);
                        HBox.setHgrow(aInput, Priority.ALWAYS);
                        aHBox.getChildren().add(aInput);
                        break;
                    }
                case BOOLEAN:
                    {
                        CheckBox aInput = new CheckBox();
                        customAttributesCheckBox.put(aID, aInput);
                        HBox.setHgrow(aInput, Priority.ALWAYS);
                        aHBox.getChildren().add(aInput);
                        break;
                    }
                default:
                    {
                        TextField aInput = new TextField();
                        HBox.setHgrow(aInput, Priority.ALWAYS);
                        customAttributesTextFields.put(aID,aInput);
                        
                        // Based on the type (time or number), limit what the 
                        // user can type into the field. 
                        if (a.getAttributeType().equals(CustomAttributeType.NUMBER)) {
                            aInput.textProperty().addListener((observable, oldValue, newValue) -> {
                                System.out.println("TextField Text Changed (newValue: " + newValue + ")");
                                if (!newValue.isEmpty()) {
                                    if (newValue.matches("^-?\\.\\d+$")) {
                                        Platform.runLater(() -> {
                                            int caret = aInput.getCaretPosition();
                                            aInput.setText(newValue.replaceFirst("\\.","0."));
                                            aInput.positionCaret(caret+1);
                                        });
                                    } else if (newValue.matches("^-?0([0-9]+\\.?[0-9]*)$")) {
                                        Platform.runLater(() -> { 
                                            int c = aInput.getCaretPosition();
                                            aInput.setText(newValue.replaceFirst("^(-?)0*([0-9]+\\.?[0-9]*)$", "$1$2"));
                                            aInput.positionCaret(c-1);
                                        });
                                    } else if (!newValue.matches("^-?([0-9]+\\.?[0-9]*)?$")) {
                                        Platform.runLater(() -> { 
                                            int c = aInput.getCaretPosition();
                                            aInput.setText(oldValue);
                                            aInput.positionCaret(c-1);
                                        }); 
                                    } 
                                }
                            });
                            
                        } else if (a.getAttributeType().equals(CustomAttributeType.TIME)) {
                            aInput.textProperty().addListener((observable, oldValue, newValue) -> {
                                        //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
                                if (newValue.isEmpty()) return; 
                                if (newValue.matches("^-?\\.\\d+$")) {
                                    Platform.runLater(() -> {
                                        int caret = aInput.getCaretPosition();
                                        aInput.setText(newValue.replaceFirst("\\.","0."));
                                        aInput.positionCaret(caret+1);
                                    });
                                } else if (newValue.matches("^-?(\\d*:)?(\\d*:)?\\d*\\.?\\d*$")){
                                    System.out.println("Good time: " + newValue);
                                } else {
                                    Platform.runLater(() -> {
                                        int caret = aInput.getCaretPosition();
                                        aInput.setText(oldValue);
                                        aInput.positionCaret(caret-1);
                                    });
                                }

                            });
                        }
                        aHBox.getChildren().add(aInput);
                        break;
                    }
            }
            customAttributesVBox.getChildren().add(aHBox);
        });
        
        
    }
    
    private void updateFilterPredicate(){
        filteredParticipantsList.setPredicate(participant -> {
            // If filter text is empty, display all persons.
           // System.out.println("filteredParticpantsList.predicateProperty changing...");
            //System.out.println("...filterField="+filterField.textProperty().getValue());
            //System.out.println("...searchWaveComboBox=" + searchWaveComboBox.getCheckModel().getCheckedItems().size());
            if (filterField.textProperty().getValueSafe().isEmpty() && searchWaveComboBox.getCheckModel().getCheckedItems().isEmpty()) {
                //System.out.println("...both are empty: true");
                return true;
            }

            BooleanProperty waveFilterMatch = new SimpleBooleanProperty(false);

            if (!searchWaveComboBox.getCheckModel().getCheckedItems().isEmpty()) {
                participant.getWaveIDs().forEach(w -> {
                    searchWaveComboBox.getCheckModel().getCheckedItems().forEach(sw -> {
                        if (sw != null && sw.getID().equals(w)) waveFilterMatch.set(true);
                    }); 
                });
            } else {
                //System.out.println("...searchWaveComboBox is empty: true");

                waveFilterMatch.set(true);
            }

            if (filterField.textProperty().getValueSafe().isEmpty() && waveFilterMatch.getValue()) {
                //System.out.println("...filterField is empty and wave matches");
                return true;
            } else if (!waveFilterMatch.getValue()) {
                //System.out.println("...filterField is empty and wave does not match");
                return false;
            } 

            // Compare first name and last name of every person with filter text.
            String lowerCaseFilter = "(.*)(" + filterField.textProperty().getValueSafe() + ")(.*)";
            try {
            Pattern pattern =  Pattern.compile(lowerCaseFilter, Pattern.CASE_INSENSITIVE);


                if (    pattern.matcher(participant.getFirstName()).matches() ||
                        pattern.matcher(participant.getLastName()).matches() ||
                        pattern.matcher(participant.getFirstName() + " " + participant.getLastName()).matches() ||
                        pattern.matcher(StringUtils.stripAccents(participant.fullNameProperty().getValueSafe())).matches() ||
                        pattern.matcher(participant.getBib()).matches()) {
                    return true; // Filter matches first/last/bib.
                } 

            } catch (PatternSyntaxException e) {
                
                return true;
            }
            return false; // Does not match.
        });
    }

    private static class Attribute {
        String name = "";
        String key = "";
        Integer cKey = -1;

        public Attribute() {
        }
        public Attribute(String n, String k){
            name = n;
            key = k;
        }
        public Attribute(String n, Integer k){
            name = n;
            cKey = k;
        }
        
        @Override
        public String toString(){
            return name;
        }
    }
        

}