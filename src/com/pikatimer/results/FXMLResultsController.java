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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pikatimer.results;

import com.pikatimer.PikaPreferences;
import com.pikatimer.event.Event;
import com.pikatimer.event.EventDAO;
import com.pikatimer.event.EventOptions;
import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.participant.Status;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.FileTransferTypes;
import com.pikatimer.util.Pace;
import java.io.File;
import java.io.IOException;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Double.MAX_VALUE;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventType;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.ToggleSwitch;


/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLResultsController  {
    
    //@FXML ChoiceBox<Race> raceChoiceBox;
    @FXML ComboBox<Race> raceComboBox; 
    @FXML Label selectedRaceLabel;
    
    @FXML TextField resultsSearchTextField;
    @FXML GridPane resultsGridPane;
    
    @FXML VBox outputDetailsVBox;
    
    @FXML Button updateNowButton;
    
    @FXML ToggleSwitch autoUpdateToggleSwitch;
    @FXML ChoiceBox<String> updateTimeDelayChoiceBox;
    @FXML ProgressBar autoUpdateProgressBar;
    
    @FXML ChoiceBox<String> timeRoundingChoiceBox;
    @FXML ChoiceBox<String>  timeFormatChoiceBox;
    @FXML ChoiceBox<Pace> paceFormatChoiceBox;
    
    @FXML ListView<ReportDestination> outputDestinationsListView;
    @FXML Button addOutputDestinationsButton;
    @FXML Button editOutputDestinationsButton;
    @FXML Button removeOutputDestinationsButton;
    
    @FXML Label startedCountLabel;
    @FXML Label finishedCountLabel;
    @FXML Label pendingCountLabel;
    @FXML Label withdrawnLabel;
    @FXML Label withdrawnCountLabel;
    
    @FXML CheckBox useCustomHeaderCheckBox;
    
    
    final Map<Race,TableView> raceTableViewMap = new ConcurrentHashMap();
    final Map<Race,VBox> raceReportsUIMap = new ConcurrentHashMap();
    final RaceDAO raceDAO = RaceDAO.getInstance();
    final ResultsDAO resultsDAO = ResultsDAO.getInstance();
    final ParticipantDAO participantDAO = ParticipantDAO.getInstance();
    
    private Race activeRace;
    
    private final Event event = Event.getInstance();
    private final EventDAO eventDAO = EventDAO.getInstance();
    private final EventOptions eventOptions = eventDAO.getEventOptions();;
    //private final BooleanProperty populateAwardSettingsInProgress = new SimpleBooleanProperty(FALSE);

    /**
     * Initializes the controller class.
     */
    public void initialize() {
        
        raceComboBox.setItems(raceDAO.listRaces());
        raceComboBox.visibleProperty().bind(Bindings.size(raceDAO.listRaces()).greaterThan(1));
        selectedRaceLabel.visibleProperty().bind(Bindings.size(raceDAO.listRaces()).greaterThan(1));
        
        

        outputDetailsVBox.setFillWidth(true);
        
        
        // Event wide stuff
        //eventOptions 
        
        timeRoundingChoiceBox.setItems(FXCollections.observableArrayList("Down", "Up", "Half"));
        timeFormatChoiceBox.setItems(FXCollections.observableArrayList("HH:MM:ss","[HH:]MM:ss", "[HH:]MM:ss.S", "[HH:]MM:ss.SS", "[HH:]MM:ss.SSS"));
        paceFormatChoiceBox.setItems(FXCollections.observableArrayList(Pace.values()));
        
        
        initializeAutoUpdate();


        initializeOutputDestinations();
        
        updateNowButton.setOnAction((ActionEvent e) -> {
            resultsDAO.processAllReports();
        });
        
        raceComboBox.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> observableValue, Number number, Number number2) -> {
            // flip the table
            //System.out.println("raceChoiceBox listener fired: now with number2 set to " + number2.intValue());
            
            if (number2.intValue() == -1 )  {
                Platform.runLater(() -> {raceComboBox.getSelectionModel().clearAndSelect(0);});
                return;
            } 
            
            
            if (activeRace != null) { // The currently active race
                resultsGridPane.getChildren().remove(raceTableViewMap.get(activeRace));
            }

            
            activeRace = raceComboBox.getItems().get(number2.intValue());
            
            
            // Populate the Output Settings
            populateOutputDetailsVBox(activeRace);
            
            
            
            // Populate the results TableView
            if( ! raceTableViewMap.containsKey(activeRace)) {
                rebuildResultsTableView(activeRace);
            
                activeRace.splitsProperty().addListener( new ListChangeListener() {

                    @Override
                    public void onChanged(ListChangeListener.Change change) {
                        System.out.println("The list of splits has changed...");
                        TableView oldTableView = raceTableViewMap.get(activeRace);
                        rebuildResultsTableView(activeRace);
                        if (raceComboBox.getSelectionModel().getSelectedItem().equals(activeRace)) {
                            resultsGridPane.getChildren().remove(oldTableView);
                            resultsGridPane.add(raceTableViewMap.get(activeRace), 0, 1);
                        }
                    }
                });
                
                
                
                
            }
            
            resultsGridPane.getChildren().remove(raceTableViewMap.get(activeRace));
            resultsGridPane.add(raceTableViewMap.get(activeRace), 0, 1);
            

            
            
            String rm = activeRace.getStringAttribute("TimeRoundingMode");
            System.out.println("TimeRoundingMode: " + rm);
            if (rm == null) {
                rm = "Down";
                activeRace.setStringAttribute("TimeRoundingMode", rm);
                raceDAO.updateRace(activeRace);
            }
            timeRoundingChoiceBox.getSelectionModel().select(rm);
             
            String paceName = activeRace.getStringAttribute("PaceDisplayFormat");
            Pace pace; 
            if (paceName == null) {
                pace = Pace.MPM;
                activeRace.setStringAttribute("PaceDisplayFormat", pace.name());
                raceDAO.updateRace(activeRace);
            } else {
                try {
                    pace = Pace.valueOf(paceName);
                } catch (Exception e) {
                    pace = Pace.MPM;
                    activeRace.setStringAttribute("PaceDisplayFormat", pace.name());
                    raceDAO.updateRace(activeRace);
                }
            }
            paceFormatChoiceBox.getSelectionModel().select(pace);
            
            String dispFormat = activeRace.getStringAttribute("TimeDisplayFormat");
            System.out.println("TimeDisplayFormat: " + dispFormat);
            if (dispFormat == null) {
                dispFormat =  timeFormatChoiceBox.getItems().get(0);
                activeRace.setStringAttribute("TimeDisplayFormat", dispFormat);
                raceDAO.updateRace(activeRace);
            }
            timeFormatChoiceBox.getSelectionModel().select(dispFormat);
            
            if(activeRace.getBooleanAttribute("useCustomHeaders") == null) {
                activeRace.setBooleanAttribute("useCustomHeaders", false);
                raceDAO.updateRace(activeRace);
            }
            useCustomHeaderCheckBox.selectedProperty().set(activeRace.getBooleanAttribute("useCustomHeaders"));
            useCustomHeaderCheckBox.selectedProperty().addListener((ob, oldP, newP) -> {
                if (activeRace.getBooleanAttribute("useCustomHeaders") == newP) return;
                
                if (newP) setupHeaders(null);
                activeRace.setBooleanAttribute("useCustomHeaders", newP);
                raceDAO.updateRace(activeRace);
            
            });
            // Setup the started/finished/pending counters
//            resultsDAO.getResults(activeRace.getID()).addListener((ListChangeListener.Change<? extends Result> c) -> {
//                System.out.println("Race Result List Changed...");
//            
//            });
            
            FilteredList<Result> finishedFilteredParticipantsList = new FilteredList<>(resultsDAO.getResults(activeRace.getID()), res -> {
                if (res.getFinishDuration().equals(Duration.ZERO)) return false;
                if (Status.GOOD.equals(participantDAO.getParticipantByBib(res.getBib()).statusProperty().get())) return true;
                return false;
            });
            FilteredList<Result> dnfFilteredParticipantsList = new FilteredList<>(resultsDAO.getResults(activeRace.getID()), res -> {
                //System.out.println("DQ/DNF Check: " + res.getBib() + " " + participantDAO.getParticipantByBib(res.getBib()).dnfProperty().get());
                if (Status.GOOD.equals(participantDAO.getParticipantByBib(res.getBib()).getStatus())) return false;
                return true;
            });
            startedCountLabel.textProperty().bind(Bindings.size(resultsDAO.getResults(activeRace.getID())).asString());
            withdrawnCountLabel.textProperty().bind(Bindings.size(dnfFilteredParticipantsList).asString());
            finishedCountLabel.textProperty().bind(Bindings.size(finishedFilteredParticipantsList).asString());
            pendingCountLabel.textProperty().bind(Bindings.subtract(Bindings.size(resultsDAO.getResults(activeRace.getID())), Bindings.add(Bindings.size(finishedFilteredParticipantsList), Bindings.size(dnfFilteredParticipantsList))).asString());
            withdrawnCountLabel.visibleProperty().bind(Bindings.size(dnfFilteredParticipantsList).isNotEqualTo(0));
            withdrawnCountLabel.managedProperty().bind(Bindings.size(dnfFilteredParticipantsList).isNotEqualTo(0));
            withdrawnLabel.visibleProperty().bind(Bindings.size(dnfFilteredParticipantsList).isNotEqualTo(0));
            withdrawnLabel.managedProperty().bind(Bindings.size(dnfFilteredParticipantsList).isNotEqualTo(0));
        });
        
        timeRoundingChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue,  newValue) -> {
            Race r = raceComboBox.getValue();
            if (newValue != null && !newValue.equals(r.getStringAttribute("TimeRoundingMode"))) {
                System.out.println("EventOptions: TimeRoundingMode changed from " + oldValue + " to " + newValue);
                r.setStringAttribute("TimeRoundingMode", newValue);
                raceDAO.updateRace(r);
            }
         });
        timeFormatChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue,  newValue) -> {
            Race r = raceComboBox.getValue();
            if (newValue != null && !newValue.equals(r.getStringAttribute("TimeDisplayFormat"))) {
                System.out.println("Race: TimeDisplayFormat changed from " + oldValue + " to " + newValue);
                r.setStringAttribute("TimeDisplayFormat", newValue);
                raceDAO.updateRace(r);
            }
         });
        paceFormatChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue,  newValue) -> {
            Race r = raceComboBox.getValue();
            if (newValue != null && !newValue.name().equals(r.getStringAttribute("PaceDisplayFormat"))) {
                System.out.println("Race: PaceDisplayFormat changed from " + oldValue + " to " + newValue);
                r.setStringAttribute("PaceDisplayFormat", newValue.name());
                raceDAO.updateRace(r);
            }
         });
        
        
        raceComboBox.getSelectionModel().clearAndSelect(0);
        

    }    
    
    private void rebuildResultsTableView(Race r) {
        // create a table
                TableView<Result> table = new TableView();
                
                table.setPlaceholder(new Label("No results for the " + r.getRaceName() + " have been entered yet"));
                
                //table.setPadding(new Insets(5));
                table.setTableMenuButtonVisible(true);            
                
                // create the columns for the table with 
                // cellValueFactories convert from the Result data to something we can display
                // bib
                TableColumn<Result,String> bibColumn = new TableColumn("Bib");
                table.getColumns().add(bibColumn);
                bibColumn.setCellValueFactory(new PropertyValueFactory<>("bib"));
                bibColumn.setComparator(new AlphanumericComparator());
                
                // name
                TableColumn<Result,String> nameColumn = new TableColumn("Name");
                nameColumn.setPrefWidth(150.0);
                table.getColumns().add(nameColumn);
                nameColumn.setCellValueFactory(cellData -> {
                    Participant p = participantDAO.getParticipantByBib(cellData.getValue().getBib());
                    if (p == null) { return new SimpleStringProperty("Unknown: " + cellData.getValue().getBib());
                    } else {
                        return p.fullNameProperty();
                    }
                });
                // Sex
                TableColumn<Result,String> sexColumn = new TableColumn("Sex");
                sexColumn.setPrefWidth(30.0);
                table.getColumns().add(sexColumn);
                sexColumn.setCellValueFactory(cellData -> {
                    Participant p = participantDAO.getParticipantByBib(cellData.getValue().getBib());
                    if (p == null) { return new SimpleStringProperty("?");
                    } else {
                        return p.sexProperty();
                    }
                });
                // AgeGroup
                TableColumn<Result,Number> agColumn = new TableColumn("AG");
                agColumn.setPrefWidth(40.0);
                table.getColumns().add(agColumn);
                agColumn.setCellFactory(column -> new AgeGroupTableCell());
                agColumn.setCellValueFactory(cellData -> {
                    Participant p = participantDAO.getParticipantByBib(cellData.getValue().getBib());
                    if (p == null) { return new SimpleIntegerProperty();
                    } else {
                        return p.ageProperty();
                    }
                });
                
                // status
                TableColumn<Result,Status> statusColumn = new TableColumn("Status");
                table.getColumns().add(statusColumn);
                statusColumn.setCellValueFactory(cellData -> {
                    Participant p = participantDAO.getParticipantByBib(cellData.getValue().getBib());
                    
                    if (p == null) { return new SimpleObjectProperty();
                    } else {
                        return p.statusProperty();
                    }
                });
                
                // start
                TableColumn<Result,Duration> startColumn = new TableColumn("Start (TOD)");
                
                startColumn.setCellValueFactory(cellData -> {
                    return cellData.getValue().startTimeProperty();
                });
                startColumn.setCellFactory(column -> {
                    return new DurationTableCell();
                });
                startColumn.setStyle( "-fx-alignment: CENTER-RIGHT;");
                table.getColumns().add(startColumn);
                
                // Wave start
                TableColumn<Result,Duration> waveStartColumn = new TableColumn("Wave (TOD)");
                
                waveStartColumn.setCellValueFactory(cellData -> {
                    return cellData.getValue().waveStartTimeProperty();
                });
                waveStartColumn.setCellFactory(column -> {
                    return new DurationTableCell();
                });
                waveStartColumn.setStyle( "-fx-alignment: CENTER-RIGHT;");
                table.getColumns().add(waveStartColumn);
                
                // Start Last Seen
                TableColumn<Result,Duration> waveStartLastSeenColumn = new TableColumn("Start (Last Seen)");
                
                waveStartLastSeenColumn.setCellValueFactory(cellData -> {
                    return cellData.getValue().splitTimeByIDProperty(0);
                });
                waveStartLastSeenColumn.setCellFactory(column -> {
                    return new DurationTableCell();
                });
                waveStartLastSeenColumn.setStyle( "-fx-alignment: CENTER-RIGHT;");
                table.getColumns().add(waveStartLastSeenColumn);
                
                // Seen to Wave delta start
                TableColumn<Result,Duration> startDeltaColumn = new TableColumn("Start Offset");
                
                startDeltaColumn.setCellValueFactory(cellData -> {
                    return cellData.getValue().startOffsetProperty();
                });
                startDeltaColumn.setCellFactory(column -> {
                    return new DurationTableCell().showZeros();
                });
                startDeltaColumn.setStyle( "-fx-alignment: CENTER-RIGHT;");
                table.getColumns().add(startDeltaColumn);
                
                // for each split from 2 -> n-2
                //TableColumn<Result,String> splitColumn;
                for (int i = 2; i <  r.getSplits().size() ; i++) {
                    //TableColumn<Result,String> splitColumn = new TableColumn();
                    TableColumn<Result,Duration> splitColumn = new TableColumn();
                    table.getColumns().add(splitColumn);
                    int splitID  = i; 
                    //splitColumn.setCellFactory(null);
                    
                    splitColumn.textProperty().bind(r.splitsProperty().get(splitID-1).splitNameProperty());
                    
                    splitColumn.setCellValueFactory(cellData -> {return cellData.getValue().splitTimeByIDProperty(splitID);});
                    splitColumn.setCellFactory(column -> {
                        return new DurationTableCell();
                    });
                    splitColumn.setStyle( "-fx-alignment: CENTER-RIGHT;");

//                    splitColumn.setCellValueFactory(cellData -> {
//                        Duration split = cellData.getValue().getSplitTime(splitID);
//                        if (split.isZero()) return new SimpleStringProperty("");
//                        return new SimpleStringProperty(DurationFormatter.durationToString(split.minus(cellData.getValue().getStartDuration()), 3, Boolean.TRUE));
//                    });
//                    splitColumn.setComparator(new AlphanumericComparator());
                }
                
                // finish
                TableColumn<Result,Duration> finishColumn = new TableColumn("Finish");
                finishColumn.setCellValueFactory(cellData -> {
                    return cellData.getValue().finishTimeProperty(); 
                });
                finishColumn.setCellFactory(column -> {
                    return new DurationTableCell();
                });
                //finishColumn.setComparator(new DurationComparator());
                finishColumn.setStyle( "-fx-alignment: CENTER-RIGHT;");
                table.getColumns().add(finishColumn);
                
                // gun
                TableColumn<Result,Duration> gunColumn = new TableColumn("Gun");
                gunColumn.setCellValueFactory(cellData -> {
                    return cellData.getValue().finishGunTimeProperty(); 
                });
                gunColumn.setCellFactory(column -> {
                    return new DurationTableCell();
                });
                gunColumn.setStyle( "-fx-alignment: CENTER-RIGHT;");
                table.getColumns().add(gunColumn);
                //gunColumn.setComparator(new AlphanumericComparator());     
                
                // finish TOD
                
                TableColumn<Result,Duration> finishTODColumn = new TableColumn("Finish (TOD)");
                finishTODColumn.setCellValueFactory(cellData -> {
                    return cellData.getValue().finishTODProperty(); 
                });
                finishTODColumn.setCellFactory(column -> {
                    return new DurationTableCell();
                });
                //finishColumn.setComparator(new DurationComparator());
                finishTODColumn.setStyle( "-fx-alignment: CENTER-RIGHT;");
                table.getColumns().add(finishTODColumn);
                                
                // set the default sort order to the finish time
		finishColumn.setSortType(SortType.ASCENDING);
		table.getSortOrder().clear();
                table.getSortOrder().add(finishColumn);
                
                // Deal with the filtering and such. 
                
                // TODO Only filter on the visible colums 
                // 1. Wrap the ObservableList in a FilteredList (initially display all data).
                FilteredList<Result> filteredParticipantsList = new FilteredList(resultsDAO.getResults(r.getID()), p -> true);

                // 2. Set the filter Predicate whenever the filter changes.
                resultsSearchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
                    //System.out.println("resultsSearchTextField change: " + newValue);
                    filteredParticipantsList.setPredicate(result -> {
                        // If filter text is empty, display all persons.
                        if (newValue == null || newValue.isEmpty()) {
                            return true;
                        }
                        
                        Participant participant = participantDAO.getParticipantByBib(result.getBib());
                        if (participant == null) { 
                            System.out.println(" Null participant, bailing...");
                            return false;
                        }
                        String searchString = resultsSearchTextField.textProperty().getValueSafe().toLowerCase().replaceAll("\\s","");
                        if (searchString.contains(":")) {
                            String[] keyValue = searchString.split(":", 2);
                            if (keyValue.length < 2) return true;
                            switch (keyValue[0]) {
                                case "bib":  
                                        if (participant.getBib().equalsIgnoreCase(keyValue[1])) return true;
                                        else return false;
                                case "sex": ;
                                        if (participant.getSex().equalsIgnoreCase(keyValue[1])) return true;
                                        else return false;
                                case "ag": 
                                        String ag = r.getAgeGroups().ageToAGString(participant.getAge());
                                        if (ag.equalsIgnoreCase(keyValue[1])) return true;
                                        else if (ag.toLowerCase().startsWith(keyValue[1].toLowerCase(), 0)) return true;
                                        else if ((participant.getSex()+ag).equalsIgnoreCase(keyValue[1])) return true;
                                        else if ((participant.getSex()+ag).toLowerCase().startsWith(keyValue[1].toLowerCase())) return true;
                                        else return false;
                            }
                        } else {
                            String lowerCaseFilter = "(.*)(" + resultsSearchTextField.textProperty().getValueSafe() + ")(.*)";
                            try {
                                Pattern pattern =  Pattern.compile(lowerCaseFilter, Pattern.CASE_INSENSITIVE);

                                if (    pattern.matcher(participant.getFirstName()).matches() ||
                                        pattern.matcher(participant.getLastName()).matches() ||
                                        pattern.matcher(participant.getFirstName() + " " + participant.getLastName()).matches() ||
                                        pattern.matcher(StringUtils.stripAccents(participant.fullNameProperty().getValueSafe())).matches() ||
                                        pattern.matcher(participant.getSex()+r.getAgeGroups().ageToAGString(participant.getAge())).matches() ||
                                        pattern.matcher(participant.getBib()).matches()) {
                                    return true; // Filter matches first/last/bib.
                                } 

                            } catch (PatternSyntaxException e) {
                                return true;
                            }
                        }
                        return false; // Does not match.
                    });
                });
                // 3. Wrap the FilteredList in a SortedList. 
                SortedList<Result> sortedResultsList = new SortedList(filteredParticipantsList);

                // 4. Bind the SortedList comparator to the TableView comparator.
                sortedResultsList.comparatorProperty().bind(table.comparatorProperty());

                
                // 5. Add sorted (and filtered) data to the table.
                table.setItems(sortedResultsList);
                
                // 6. Add a context menu
                
                // Setup the context menu and actions
                table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

                table.setRowFactory((TableView<Result> tableView1) -> {
                    final TableRow<Result> row = new TableRow<>();
                    final ContextMenu rowMenu = new ContextMenu();

                    // For future 
//                    row.setOnMouseClicked(event -> {
//                        if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
//                            editParticipant(participantTableView.getSelectionModel().getSelectedItem());
//                        }
//                    });

                    // Context menu
                    
                    // Context menu
                    MenuItem clearMenuItem = new MenuItem("Clear DQ/DNF/DNS Flag");
                    clearMenuItem.setOnAction((ActionEvent event) -> {
                        // Dialog
                        List<Participant> perps = new ArrayList();
                        table.getSelectionModel().getSelectedItems().forEach(res -> {
                            if (res != null && ParticipantDAO.getInstance().getParticipantByBib(res.getBib()) != null)
                                perps.add(ParticipantDAO.getInstance().getParticipantByBib(res.getBib()));
                        });
                        
                        perps.forEach(perp -> {
                            perp.setStatus(Status.GOOD);
                            participantDAO.updateParticipant(perp);
                        });
                    });
                    
                    
                    
                    MenuItem dqMenuItem = new MenuItem("DQ: Disqulify");
                    dqMenuItem.setOnAction((ActionEvent event) -> {
                        // Dialog
                        List<Participant> perps = new ArrayList();
                        table.getSelectionModel().getSelectedItems().forEach(res -> {
                            if (res != null && ParticipantDAO.getInstance().getParticipantByBib(res.getBib()) != null)
                                perps.add(ParticipantDAO.getInstance().getParticipantByBib(res.getBib()));
                        });
                        
                        // Open a dialog
                        TextInputDialog dialog = new TextInputDialog();
                        dialog.setTitle("DQ Participants");
                        dialog.setHeaderText("You are about to Disqualify " + perps.size() + " participant(s)");
                        dialog.setContentText("Please enter a reason:");

                        // Traditional way to get the response value.
                        Optional<String> result = dialog.showAndWait();
                                                
                        result.ifPresent(reason -> {
                            // if yes, dq with note
                            perps.forEach(perp -> {
                                perp.setStatus(Status.DQ);
                                perp.setNote(reason);
                                participantDAO.updateParticipant(perp);
                            });
                        });
                    });

                    MenuItem dnfMenuItem = new MenuItem("DNF: Did Not Finish");
                    dnfMenuItem.setOnAction((ActionEvent event) -> {
                        // Dialog
                        List<Participant> perps = new ArrayList();
                        table.getSelectionModel().getSelectedItems().forEach(res -> {
                            if (res != null && ParticipantDAO.getInstance().getParticipantByBib(res.getBib()) != null)
                                perps.add(ParticipantDAO.getInstance().getParticipantByBib(res.getBib()));
                        });
                        
                        // Open a dialog
                        TextInputDialog dialog = new TextInputDialog();
                        dialog.setTitle("Set DNF Flag");
                        dialog.setHeaderText("You are about to flag " + perps.size() + " participant(s)\nas not having finished");
                        dialog.setContentText("Please enter a note (optional):");

                        // Traditional way to get the response value.
                        Optional<String> result = dialog.showAndWait();
                                                
                        result.ifPresent(reason -> {
                            // if yes, dq with note
                            perps.forEach(perp -> {
                                perp.setStatus(Status.DNF);
                                perp.setNote(reason);
                                participantDAO.updateParticipant(perp);
                            });
                        });
                    });

                    MenuItem dnsMenuItem = new MenuItem("DNS: Did Not Start");
                    dnsMenuItem.setOnAction((ActionEvent event) -> {
                        // Dialog
                        List<Participant> perps = new ArrayList();
                        table.getSelectionModel().getSelectedItems().forEach(res -> {
                            if (res != null && ParticipantDAO.getInstance().getParticipantByBib(res.getBib()) != null)
                                perps.add(ParticipantDAO.getInstance().getParticipantByBib(res.getBib()));
                        });
                        
                        // Open a dialog
                        TextInputDialog dialog = new TextInputDialog("Did not start");
                        dialog.setTitle("Set DNS Flag");
                        dialog.setHeaderText("You are about to flag " + perps.size() + " participant(s)\nas not having started");
                        dialog.setContentText("Please enter a optional note (optional):");

                        // Traditional way to get the response value.
                        Optional<String> result = dialog.showAndWait();
                                                
                        result.ifPresent(reason -> {
                            // if yes, dq with note
                            perps.forEach(perp -> {
                                perp.setStatus(Status.DNS);
                                perp.setNote(reason);
                                participantDAO.updateParticipant(perp);
                            });
                        });
                    });

                    Menu assignWaveMenu = new Menu("Re-Assign");
                    //RaceDAO.getInstance().listWaves().sorted((Wave u1, Wave u2) -> u1.toString().compareTo(u2.toString())).stream().forEach(w -> {
                    RaceDAO.getInstance().listWaves().sorted(new AlphanumericComparator()).stream().forEach(w -> {
                        MenuItem m = new MenuItem(w.toString());
                        m.setOnAction(e -> {
                            table.getSelectionModel().getSelectedItems().stream().forEach(res -> {
                                if (res != null ){
                                    Participant part = ParticipantDAO.getInstance().getParticipantByBib(res.getBib());
                                    part.setWaves((Wave)w);
                                    participantDAO.updateParticipant(part);
                                }
                            });
                        });
                        assignWaveMenu.getItems().add(m);
                    });

                    RaceDAO.getInstance().listWaves().addListener((Change<? extends Wave> change) -> {
                        assignWaveMenu.getItems().clear();
                        //RaceDAO.getInstance().listWaves().sorted((Wave u1, Wave u2) -> u1.toString().compareTo(u2.toString())).stream().forEach(w -> {
                        RaceDAO.getInstance().listWaves().sorted(new AlphanumericComparator()).stream().forEach(w -> {
                            MenuItem m = new MenuItem(w.toString());
                            m.setOnAction(e -> {
                                table.getSelectionModel().getSelectedItems().stream().forEach(res -> {
                                    if (res != null ){
                                        Participant part = ParticipantDAO.getInstance().getParticipantByBib(res.getBib());
                                        part.setWaves((Wave)w);
                                        participantDAO.updateParticipant(part);
                                    }
                                });
                            });
                            assignWaveMenu.getItems().add(m);
                        });
                    });
                    
                    Menu statusMenu = new Menu("Status");
                    statusMenu.getItems().addAll(dqMenuItem, dnfMenuItem, dnsMenuItem, new SeparatorMenuItem(), clearMenuItem);

                    // context menu to assign/unassign runners to a given wave
                    rowMenu.getItems().addAll(statusMenu, assignWaveMenu);

                    // only display context menu for non-null items:
                    row.contextMenuProperty().bind(
                            Bindings.when(Bindings.isNotNull(row.itemProperty()))
                                    .then(rowMenu)
                                    .otherwise((ContextMenu)null));

                    // Hide the edit option if more than one item is selected and only show
                    // the swap option if exactly two items are selected. 
//                    table.getSelectionModel().getSelectedIndices().addListener((Change<? extends Integer> change) -> {
//                        if (change.getList().size() == 2) {
//                            swapBibs.setDisable(false);
//                        } else {
//                            swapBibs.setDisable(true);
//                        }
//                        if (change.getList().size() == 1) {
//                            editItem.setDisable(false);
//                        } else {
//                            editItem.setDisable(true);
//                        }
//                    });

                    return row;
                });
                
 
                
                
                // 7. save it
                raceTableViewMap.put(r, table);
    }

    
    
    private void populateOutputDetailsVBox(Race r) {
        //@FXML VBox outputDetailsVBox;
        
        // Did we already build this?
        if (! raceReportsUIMap.containsKey(r)) {
            // No? then let's build this 
            VBox reportDetails = new VBox();
            raceReportsUIMap.put(r, reportDetails);
            
            if (r.raceReportsProperty().isEmpty()) {
                // create the default overall and award reports
                
                System.out.println("Adding default Ooverall and Award race reports");
                RaceReport overall = new RaceReport();
                overall.setReportType(ReportTypes.OVERALL);
                r.addRaceReport(overall);
                resultsDAO.saveRaceReport(overall);

                
                RaceReport award = new RaceReport();
                award.setReportType(ReportTypes.AWARD);
                r.addRaceReport(award);
                resultsDAO.saveRaceReport(award);
                
                raceDAO.updateRace(r);
                
            }
            
            r.raceReportsProperty().forEach(rr -> {
                FXMLLoader tlLoader = new FXMLLoader(getClass().getResource("/com/pikatimer/results/FXMLResultOutput.fxml"));
                try {
                    reportDetails.getChildren().add(tlLoader.load());
                    System.out.println("Showing RaceReport of type " + rr.getReportType().toString());
                } catch (IOException ex) {
                    System.out.println("Loader Exception for race reports!");
                    ex.printStackTrace();
                    Logger.getLogger(FXMLResultOutputController.class.getName()).log(Level.SEVERE, null, ex);
                }
                ((FXMLResultOutputController)tlLoader.getController()).setRaceReport(rr);
            
            });

        }
        // Ok, now lets clear the existing outputDetails
        // the setAll below should take care of this... 
        // outputDetailsVBox.getChildren().clear(); 
        
        // And set it to the new one
        outputDetailsVBox.getChildren().setAll(raceReportsUIMap.get(r));
    }
    
    private void initializeAutoUpdate(){
        autoUpdateToggleSwitch.selectedProperty().set(false);
        updateTimeDelayChoiceBox.setItems(FXCollections.observableArrayList("30s", "1m", "2m", "5m"));
        updateTimeDelayChoiceBox.disableProperty().bind(autoUpdateToggleSwitch.selectedProperty().not());
        autoUpdateProgressBar.disableProperty().bind(autoUpdateToggleSwitch.selectedProperty().not());
        updateTimeDelayChoiceBox.getSelectionModel().selectLast();
        
        Task autoUpdateTask = new Task<Void>() {

            @Override 
            public Void call() {
                String delayString = "30s";
                int delay = 30;
                int counter = 0;
                while(true) {
                    try {
                        if (autoUpdateToggleSwitch.selectedProperty().not().get()) counter = 0;
                        
                        delayString=updateTimeDelayChoiceBox.getSelectionModel().getSelectedItem();
                        delay = Integer.parseUnsignedInt(delayString.replaceAll("\\D+", ""));
                        if (delayString.contains("m")) delay = delay * 60;
                        //System.out.println("Auto-Update timer " + (delay-counter) + "s (" + counter + "/" + delay + ")");
                        
                        if (counter >= delay) {
                            counter = 0;
                            resultsDAO.processAllReports();
                            updateMessage("Processing...");
                        } else counter++;
                        
                        updateProgress(counter, delay);
                        Thread.sleep(1000);
                        updateMessage((delay-counter) + "s");
                    } catch (Exception ex) {
                        System.out.println("AutoUpdateReportsThread Exception: " + ex.getMessage());
                    }

                }
            }
        };
        Thread processNewResultThread = new Thread(autoUpdateTask);
        processNewResultThread.setName("Thread-AutoUpdateReportsThread");
        processNewResultThread.setDaemon(true);
        processNewResultThread.start();
        autoUpdateProgressBar.progressProperty().bind(autoUpdateTask.progressProperty());
    }
    
    private void initializeOutputDestinations(){
        
        

        removeOutputDestinationsButton.disableProperty().bind(outputDestinationsListView.getSelectionModel().selectedItemProperty().isNull());
        editOutputDestinationsButton.disableProperty().bind(outputDestinationsListView.getSelectionModel().selectedItemProperty().isNull());
        outputDestinationsListView.setItems(resultsDAO.listReportDestinations());
        outputDestinationsListView.setEditable(false);
        outputDestinationsListView.setCellFactory((ListView<ReportDestination> listView) -> new OutputPortalListCell());
        // If empty, create a default local file output
        if(resultsDAO.listReportDestinations().isEmpty()) {
            ReportDestination op = new ReportDestination();
            op.setName(System.getProperty("user.home"));
            op.setBasePath(System.getProperty("user.home"));
            op.setOutputProtocol(FileTransferTypes.LOCAL);
            
            resultsDAO.saveReportDestination(op);
        }
        
        outputDestinationsListView.setOnMouseClicked((MouseEvent click) -> {
            if (click.getClickCount() == 2) {
                ReportDestination sp = outputDestinationsListView.getSelectionModel().selectedItemProperty().getValue();
                editOutputDestination(sp);
            }
        });
        
    }
    public void addOutputDestination(ActionEvent fxevent){
        editOutputDestination(new ReportDestination());
    }
    
    public void editOutputDestination(ActionEvent fxevent){
        ReportDestination sp = outputDestinationsListView.getSelectionModel().selectedItemProperty().getValue();
        editOutputDestination(sp);
        
    }
    
    private void editOutputDestination(ReportDestination sp){
        
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Report Destination");
        dialog.setHeaderText("Edit Report Destination");

        // Set the button types.
        ButtonType saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the grid for the labels and fields.
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setPadding(new Insets(5, 5, 5, 5));

        // Output Type 
        ChoiceBox<FileTransferTypes> typeChoiceBox = new ChoiceBox();
        typeChoiceBox.getItems().setAll(FileTransferTypes.values());
        grid.add(new Label("Type"), 0, 0);
        grid.add(typeChoiceBox, 1, 0);
        grid.setStyle("-fx-font-size: 14px;");
        
        // Now we create two Grids, one for local files, one for remote
        // We do this so we can easily show one and hide the other as the 
        // typeChoiceBox changes
        GridPane localGrid = new GridPane();
            localGrid.setHgap(5);
            localGrid.setVgap(5);
        GridPane remoteGrid = new GridPane();
            remoteGrid.setHgap(5);
            remoteGrid.setVgap(5);
        
        grid.add(localGrid,0,1,2,1); // col 0, row 1, colspan 2, rowspan 1
        grid.add(remoteGrid,0,1,2,1);
        


        // create and populate the localGrid
        TextField filePath = new TextField("");
        
        localGrid.add(new Label("Directory"),0,0);
        localGrid.add(filePath,1,0);
        
        
        
        
        Button chooseDirectoryButton = new Button("Select...");
        localGrid.add(chooseDirectoryButton,2,0);
        chooseDirectoryButton.setOnAction((event) -> {
            File current = PikaPreferences.getInstance().getCWD();
            if (filePath.getText() != null && ! filePath.getText().isEmpty()) current = new File(filePath.getText());
            if (! current.isDirectory() || !current.canWrite()) current = PikaPreferences.getInstance().getCWD();
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setInitialDirectory(current);
            dirChooser.setTitle("Report Output Directory");
            current = dirChooser.showDialog(chooseDirectoryButton.getParent().getScene().getWindow());
            if (current != null) filePath.setText(current.getPath());
        });
        
        
        Label statusLabel = new Label("Please enter a target directory.");
        localGrid.add(statusLabel, 1, 1, 2, 1);
        
        filePath.textProperty().addListener((observable, oldValue, newValue) -> {
            //System.out.println("TextField Text Changed (newValue: " + newValue + ")");
            if (newValue == null || newValue.isEmpty()){
                statusLabel.setText("Please enter a target directory.");
            }
            else {
                File newFile = new File(newValue);
                
                if (! newFile.isDirectory()) statusLabel.setText("No Such Directory: " + newValue);
                else statusLabel.setText("");
            }
        });
        filePath.setText(sp.getBasePath());
        
        
        
        // create and populate the remoteGrid
        TextField remoteServer = new TextField();
        remoteServer.setText(sp.getServer());
        remoteGrid.add(new Label("Server"),0,0);
        remoteGrid.add(remoteServer,1,0);       
        
        TextField remoteDir = new TextField();
        remoteDir.setText(sp.getBasePath());
        remoteGrid.add(new Label("Path"),0,1);
        remoteGrid.add(remoteDir,1,1);
        
        TextField remoteUsername = new TextField();
        remoteUsername.setText(sp.getUsername());
        remoteGrid.add(new Label("Username"),0,2);
        remoteGrid.add(remoteUsername,1,2);
        
        TextField remotePassword = new TextField();
        remotePassword.setText(sp.getPassword());
        remoteGrid.add(new Label("Password"),0,3);
        remoteGrid.add(remotePassword,1,3);
        
        CheckBox stripAccents = new CheckBox("Strip Accents");
        stripAccents.setSelected(sp.getStripAccents());
        stripAccents.tooltipProperty().set(new Tooltip("Remove accent marks from files. e.g. é -> e"));
        grid.add(stripAccents,0,2,2,1);
        
        VBox testVBox= new VBox();
        Label resultLabel = new Label("Test Results:");
        TextArea resultOutputTextArea = new TextArea("");
        resultOutputTextArea.setPrefWidth(300);

        testVBox.setManaged(false);
        testVBox.setVisible(false);
        testVBox.getChildren().addAll(resultLabel,resultOutputTextArea);
        grid.add(testVBox,2,1,1,2);
        
        Button testButton = new Button("Test Connection...");
        remoteGrid.add(testButton,1,4);
        GridPane.setHalignment(testButton, HPos.RIGHT);
        testButton.setOnAction((event) -> {
            ReportDestination newRD = new ReportDestination();
            StringProperty result = new SimpleStringProperty();
            
            resultOutputTextArea.textProperty().bind(result);
            
            if (FileTransferTypes.LOCAL.equals(typeChoiceBox.getSelectionModel().getSelectedItem())) {
                newRD.setBasePath(filePath.getText());
            } else {
                newRD.setBasePath(remoteDir.getText());
            }

            newRD.setServer(remoteServer.getText());
            newRD.setUsername(remoteUsername.getText());
            newRD.setPassword(remotePassword.getText());
            newRD.setStripAccents(stripAccents.selectedProperty().get());
            
            // show the result screen
            
            testVBox.setManaged(true);
            testVBox.setVisible(true);
           
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
            typeChoiceBox.getSelectionModel().getSelectedItem().getNewTransport().test(newRD, result);
            
        });
        
        
        
        typeChoiceBox.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> observableValue, Number number, Number number2) -> {
            FileTransferTypes ftt = typeChoiceBox.getItems().get((Integer) number2);
            if (number2.intValue() < 0) {
                remoteGrid.managedProperty().setValue(FALSE);
                remoteGrid.visibleProperty().setValue(FALSE);
                localGrid.managedProperty().setValue(FALSE);
                localGrid.visibleProperty().setValue(FALSE);
            } else if(ftt.equals(FileTransferTypes.LOCAL)) {
                localGrid.managedProperty().setValue(TRUE);
                localGrid.visibleProperty().setValue(TRUE);
                
                remoteGrid.managedProperty().setValue(FALSE);
                remoteGrid.visibleProperty().setValue(FALSE);
                
                dialog.getDialogPane().lookupButton(saveButtonType).disableProperty().bind(filePath.textProperty().isEmpty());
            } else {
                remoteGrid.managedProperty().setValue(TRUE);
                remoteGrid.visibleProperty().setValue(TRUE);
                
                localGrid.managedProperty().setValue(FALSE);
                localGrid.visibleProperty().setValue(FALSE);
                
                BooleanProperty oneEmpty = new SimpleBooleanProperty(false);
                
                oneEmpty.bind(Bindings.or(remoteServer.textProperty().isEmpty(), 
                        remoteDir.textProperty().isEmpty()));
                
                dialog.getDialogPane().lookupButton(saveButtonType).disableProperty().bind(oneEmpty);
                testButton.disableProperty().bind(oneEmpty);
            }
            
            dialog.getDialogPane().getScene().getWindow().sizeToScene();
        });
        
        typeChoiceBox.getSelectionModel().select(sp.getOutputProtocol());
        
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton.equals(saveButtonType)) {
                sp.setOutputProtocol(typeChoiceBox.getSelectionModel().getSelectedItem());
                
                if (FileTransferTypes.LOCAL.equals(sp.getOutputProtocol())) {
                    sp.setBasePath(filePath.getText());
                } else {
                    sp.setBasePath(remoteDir.getText());
                }
                
                sp.setServer(remoteServer.getText());
                sp.setUsername(remoteUsername.getText());
                sp.setPassword(remotePassword.getText());
                
                sp.setStripAccents(stripAccents.selectedProperty().get());
                        
                return Boolean.TRUE;
            }
            return null;
        });

        Optional<Boolean> result = dialog.showAndWait();
        
        result.ifPresent(dialogOK -> {
            if (dialogOK) {
                resultsDAO.saveReportDestination(sp);
            }
        });
    }
    public void removeOutputDestination(ActionEvent fxevent){
        BooleanProperty inUse = new SimpleBooleanProperty(FALSE);
        StringBuilder inUseBy = new StringBuilder("In use by the following reports:\n");
        ReportDestination rd = outputDestinationsListView.getSelectionModel().getSelectedItem();
        if (rd == null) return;
        // Make sure it is  not in use anywhere, then remove it.
        raceDAO.listRaces().forEach(r -> {
            System.out.println("  Race: " + r.getRaceName());
            r.getRaceReports().forEach(rr -> {
                System.out.println("  Report: " + rr.getReportType().toString());
                rr.getRaceOutputTargets().forEach(rot -> {
                    System.out.println("  Target: " + rot.getUUID() );
                    if (rd.getID().equals(rot.getOutputDestination()) ) {
                        // the ReportDestination is in use
                        inUse.set(true);
                        inUseBy.append(r.getRaceName() + ": " + rr.getReportType().toString() +"\n" );
                    }
                });
            });
        });
        
        if (inUse.getValue()) {
            // Alert dialog box time
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Unable to Remove ");
            alert.setHeaderText("Unable to Remove the selected Output Destination");
            alert.setContentText(inUseBy.toString());

            alert.showAndWait();
        } else resultsDAO.removeReportDestination(outputDestinationsListView.getSelectionModel().getSelectedItem());
    }
    
    public void addNewReport(ActionEvent fxevent){
        System.out.println("addNewReport called");
        
        Race r = activeRace; 
        
        if (! raceReportsUIMap.containsKey(r)) {
        } else {
            System.out.println("Adding a new report for " + r.getRaceName());
            RaceReport newRR = new RaceReport();
            newRR.setReportType(ReportTypes.OVERALL);
            r.addRaceReport(newRR);

            resultsDAO.saveRaceReport(newRR);

            FXMLLoader tlLoader = new FXMLLoader(getClass().getResource("/com/pikatimer/results/FXMLResultOutput.fxml"));
            try {
                raceReportsUIMap.get(r).getChildren().add(tlLoader.load());
                System.out.println("Added new RaceReport of type " + newRR.getReportType().toString());


            } catch (IOException ex) {
                System.out.println("Loader Exception for race reports!");
                ex.printStackTrace();

                Logger.getLogger(FXMLResultOutputController.class.getName()).log(Level.SEVERE, null, ex);
            }
            ((FXMLResultOutputController)tlLoader.getController()).setRaceReport(newRR);
        }
    }
    
    public void setupHeaders(ActionEvent fxevent){
                
        useCustomHeaderCheckBox.selectedProperty().set(true);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("FXMLSetupHeaders.fxml"));
        Parent setupHeadersRoot;
        try {
            setupHeadersRoot = (Parent) fxmlLoader.load();
            FXMLSetupHeadersController ctrl = (FXMLSetupHeadersController)fxmlLoader.getController();
            ctrl.setRace(activeRace);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Header/Footer Setup");
            stage.setScene(new Scene(setupHeadersRoot));  
            stage.showAndWait();
        } catch (IOException ex) {
            System.out.println("Loader Error in FXMLSetupHeaders.fxml");
        }
    }
    private class DurationTableCell extends TableCell<Result, Duration> {
        Boolean showZero = false;
        @Override
        protected void updateItem(Duration d, boolean empty) {
            super.updateItem(d, empty);
            if (d == null || empty) {
                setText(null);
            } else if (d.isZero() || d.equals(Duration.ofNanos(Long.MAX_VALUE))){
                if (showZero && d.isZero()) setText("0:00:00.000");
                else setText("");
            } else {
                // Format duration.
                setText(DurationFormatter.durationToString(d, 3, Boolean.TRUE));
            }
        }
        public DurationTableCell showZeros(){
            showZero = true;
            return this;
        }
    };
    
    
    private class AgeGroupTableCell extends TableCell<Result, Number> {
        @Override
        protected void updateItem(Number d, boolean empty) {
            super.updateItem(d, empty);
            if (d == null || empty) {
                setText(null);
            } else {
                // Format duration.
                Result r = (Result)getTableRow().getItem();
                if (r == null) {
                    setText("");
                    return;
                }
                Race race = raceDAO.getRaceByID(r.getRaceID());
                if (race == null) setText("");
                else setText(race.getAgeGroups().ageToAGString(d.intValue()));
            }
        }

    };
    
    private class OutputPortalListCell extends ListCell<ReportDestination> {
        ReportDestination boundRD = null;
        Label protocolLabel = new Label();
        Label serverLabel = new Label();
        Label pathLabel = new Label();
        Label transferStatusLabel = new Label();
        Label spring = new Label();
        
        CheckBox enabledCheckBox = new CheckBox("Enabled");
        VBox container = new VBox();
        HBox topLine = new HBox();
        HBox middleLine = new HBox();
        HBox bottomLine = new HBox();
        
        OutputPortalListCell(){
            topLine.setSpacing(5);
            topLine.setStyle("-fx-font-size: 14px;");
            enabledCheckBox.setStyle("-fx-font-size: 12px;");
            
            spring.setMinWidth(1);
            spring.setPrefWidth(1);
            spring.setMaxWidth(MAX_VALUE);
            protocolLabel.setMinWidth(USE_COMPUTED_SIZE);
            
            //transferStatusLabel.setMinWidth(1);
            //transferStatusLabel.setPrefWidth(2);
            
            HBox.setHgrow(protocolLabel, Priority.NEVER);
            HBox.setHgrow(spring, Priority.ALWAYS);
            topLine.getChildren().addAll(protocolLabel, spring, enabledCheckBox);

            
            serverLabel.setMinWidth(USE_COMPUTED_SIZE);
            serverLabel.setPrefWidth(USE_COMPUTED_SIZE);
            pathLabel.setMinWidth(1);
            //pathLabel.setPrefWidth(1);
            pathLabel.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
            
            //HBox.setHgrow(serverLabel, Priority.NEVER);
            HBox.setHgrow(pathLabel, Priority.ALWAYS);
            
            
            middleLine.setSpacing(2);
            middleLine.getChildren().addAll(pathLabel);
            
            bottomLine.getChildren().addAll(transferStatusLabel);
            container.getChildren().addAll(topLine, middleLine,bottomLine);
            
            
            // TODO: Find the right way to constrain a cell to the 
            // Note that the serverLabel will shrink despite the minWidth setting
            // The entire thing is beyond frustrating
            container.setMaxWidth(230);
            
        }
        
        @Override
        public void updateItem(ReportDestination op, boolean empty) {
            super.updateItem(op, empty);
            if (empty || op == null) {
                setText(null);
                setGraphic(null);
                if (boundRD != null) {
                    enabledCheckBox.selectedProperty().unbindBidirectional(boundRD.enabledProperty());
                    boundRD = null;
                }
            } else {
                setText(null);
                protocolLabel.setText(op.protocolProperty().getValueSafe());
                //serverLabel.setText(op.serverProperty().getValueSafe());
                if (op.getOutputProtocol().equals(FileTransferTypes.LOCAL)) pathLabel.setText(op.basePathProperty().getValueSafe());
                else pathLabel.setText(op.serverProperty().getValueSafe() + ":" + op.basePathProperty().getValueSafe());
                if (boundRD != null) enabledCheckBox.selectedProperty().unbindBidirectional(boundRD.enabledProperty());
                enabledCheckBox.selectedProperty().bindBidirectional(op.enabledProperty());
                boundRD = op;
                transferStatusLabel.setText("Status: " + op.transferStatusProperty().getValueSafe());
                setGraphic(container);
                System.out.println("updateItem called: " + op.transferStatusProperty().getValueSafe());
            }
        }
    }
}
