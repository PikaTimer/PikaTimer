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
package com.pikatimer.results;

import com.pikatimer.event.Event;
import com.pikatimer.event.EventDAO;
import com.pikatimer.event.EventOptions;
import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.FileTransferTypes;
import java.io.IOException;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Double.MAX_VALUE;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
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
    @FXML ProgressIndicator autoUpdateProgressIndicator;
    
    @FXML ChoiceBox<String> timeRoundingChoiceBox;
    @FXML ChoiceBox<String>  timeFormatChoiceBox;
    
    @FXML ListView<OutputPortal> outputDestinationsListView;
    @FXML Button addOutputDestinationsButton;
    @FXML Button editOutputDestinationsButton;
    @FXML Button removeOutputDestinationsButton;
    
    @FXML Label startedCountLabel;
    @FXML Label finishedCountLabel;
    @FXML Label pendingCountLabel;
    
    
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
        
        autoUpdateProgressIndicator.visibleProperty().bind(autoUpdateToggleSwitch.selectedProperty());

        outputDetailsVBox.setFillWidth(true);
        
        
        // Event wide stuff
        //eventOptions 
        
        timeRoundingChoiceBox.setItems(FXCollections.observableArrayList("Down", "Up", "Half"));
        timeFormatChoiceBox.setItems(FXCollections.observableArrayList("HH:MM:ss","[HH:]MM:ss", "[HH:]MM:ss.S", "[HH:]MM:ss.SS", "[HH:]MM:ss.SSS"));

        
        


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
            
            if (number.intValue() > 0) {
                Race old = raceComboBox.getItems().get(number.intValue());
                resultsGridPane.getChildren().remove(raceTableViewMap.get(old));
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
            
            String dispFormat = activeRace.getStringAttribute("TimeDisplayFormat");
            System.out.println("TimeDisplayFormat: " + dispFormat);
            if (dispFormat == null) {
                dispFormat =  timeFormatChoiceBox.getItems().get(0);
                activeRace.setStringAttribute("TimeDisplayFormat", dispFormat);
                raceDAO.updateRace(activeRace);
            }
            timeFormatChoiceBox.getSelectionModel().select(dispFormat);
            
            
            // Setup the started/finished/pending counters
            
            FilteredList<Result> filteredParticipantsList = new FilteredList<>(resultsDAO.getResults(activeRace.getID()), res -> {
                if (res.getFinishDuration().equals(Duration.ZERO)) return false;
                return true;
            });
            startedCountLabel.textProperty().bind(Bindings.size(resultsDAO.getResults(activeRace.getID())).asString());
            finishedCountLabel.textProperty().bind(Bindings.size(filteredParticipantsList).asString());
            pendingCountLabel.textProperty().bind(Bindings.subtract(Bindings.size(resultsDAO.getResults(activeRace.getID())), Bindings.size(filteredParticipantsList)).asString());
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
        
        raceComboBox.getSelectionModel().clearAndSelect(0);
        

    }    
    
    private void rebuildResultsTableView(Race r) {
        // create a table
                TableView<Result> table = new TableView();
                
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
                
                // start
                TableColumn<Result,Duration> startColumn = new TableColumn("Start");
                
                startColumn.setCellValueFactory(cellData -> {
                    //return new SimpleStringProperty(DurationFormatter.durationToString(cellData.getValue().getStartDuration(), 3, Boolean.TRUE));
                    return cellData.getValue().startTimeProperty();
                });
                startColumn.setCellFactory(column -> {
                    return new DurationTableCell();
                });
                //startColumn.setComparator(new AlphanumericComparator());
                startColumn.setStyle( "-fx-alignment: CENTER-RIGHT;");
                table.getColumns().add(startColumn);
                
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
                        String lowerCaseFilter = "(.*)(" + resultsSearchTextField.textProperty().getValueSafe() + ")(.*)";
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
                });
                // 3. Wrap the FilteredList in a SortedList. 
                SortedList<Result> sortedResultsList = new SortedList(filteredParticipantsList);

                // 4. Bind the SortedList comparator to the TableView comparator.
                sortedResultsList.comparatorProperty().bind(table.comparatorProperty());

                
                // 5. Add sorted (and filtered) data to the table.
                table.setItems(sortedResultsList);
                
                // save it
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
    
    private void initializeOutputDestinations(){
        
        

        removeOutputDestinationsButton.disableProperty().bind(outputDestinationsListView.getSelectionModel().selectedItemProperty().isNull());
        editOutputDestinationsButton.disableProperty().bind(outputDestinationsListView.getSelectionModel().selectedItemProperty().isNull());
        outputDestinationsListView.setItems(resultsDAO.listOutputPortals());
        outputDestinationsListView.setEditable(false);
        outputDestinationsListView.setCellFactory((ListView<OutputPortal> listView) -> new OutputPortalListCell());
        // If empty, create a default local file output
        if(resultsDAO.listOutputPortals().isEmpty()) {
            OutputPortal op = new OutputPortal();
            op.setName(System.getProperty("user.home"));
            op.setBasePath(System.getProperty("user.home"));
            op.setOutputProtocol(FileTransferTypes.LOCAL);
            
            resultsDAO.saveOutputPortal(op);
        }
        
        outputDestinationsListView.setOnMouseClicked((MouseEvent click) -> {
            if (click.getClickCount() == 2) {
                OutputPortal sp = outputDestinationsListView.getSelectionModel().selectedItemProperty().getValue();
                editOutputDestination(sp);
            }
        });
        
    }
    public void addOutputDestination(ActionEvent fxevent){
        editOutputDestination(new OutputPortal());
    }
    
    public void editOutputDestination(ActionEvent fxevent){
        OutputPortal sp = outputDestinationsListView.getSelectionModel().selectedItemProperty().getValue();
        editOutputDestination(sp);
        
    }
    
    private void editOutputDestination(OutputPortal sp){
        
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
        
        // Now we create two Grids, one for local files, one for remote
        // We do this so we can easily show one and hide the other as the 
        // typeChoiceBox changes
        GridPane localGrid = new GridPane();
        GridPane remoteGrid = new GridPane();
        
        grid.add(localGrid,0,1,2,1); // col 0, row 1, colspan 2, rowspan 1
        grid.add(remoteGrid,0,1,2,1);
        
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
            } else {
                remoteGrid.managedProperty().setValue(TRUE);
                remoteGrid.visibleProperty().setValue(TRUE);
                
                localGrid.managedProperty().setValue(FALSE);
                localGrid.visibleProperty().setValue(FALSE);
                
                dialog.getDialogPane().getScene().getWindow().sizeToScene();
            }
        });
        
        typeChoiceBox.getSelectionModel().select(sp.getOutputProtocol());

        // create and populate the localGrid
        TextField filePath = new TextField();
        filePath.setText(sp.getBasePath());
        localGrid.add(new Label("Directory"),0,0);
        localGrid.add(filePath,1,0);
        
        // create and populate the remoteGrid
        TextField remoteDir = new TextField();
        remoteDir.setText(sp.getBasePath());
        remoteGrid.add(new Label("Path"),0,0);
        remoteGrid.add(remoteDir,1,0);
        
        TextField remoteServer = new TextField();
        remoteServer.setText(sp.getServer());
        remoteGrid.add(new Label("Server"),0,1);
        remoteGrid.add(remoteServer,1,1);
        
        TextField remoteUsername = new TextField();
        remoteUsername.setText(sp.getUsername());
        remoteGrid.add(new Label("Username"),0,2);
        remoteGrid.add(remoteUsername,1,2);
        
        TextField remotePassword = new TextField();
        remotePassword.setText(sp.getPassword());
        remoteGrid.add(new Label("Password"),0,3);
        remoteGrid.add(remotePassword,1,3);
        
        
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
                
                
                        
                return Boolean.TRUE;
            }
            return null;
        });

        Optional<Boolean> result = dialog.showAndWait();
        
        result.ifPresent(dialogOK -> {
            if (dialogOK) {
                resultsDAO.saveOutputPortal(sp);
            }
        });
    }
    public void removeOutputDestination(ActionEvent fxevent){
        // Make sure it is  not in use anywhere, then remove it.
        
        resultsDAO.removeOutputPortal(outputDestinationsListView.getSelectionModel().getSelectedItem());
    }
    
    public void addNewReport(ActionEvent fxevent){
        System.out.println("addNewReport called");
        
        Race r = activeRace; 
        
        if (! raceReportsUIMap.containsKey(r)) {
        } else {
            System.out.println("Adding default Ooverall and Award race reports");
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
    
    private class DurationTableCell extends TableCell<Result, Duration> {
        @Override
        protected void updateItem(Duration d, boolean empty) {
            super.updateItem(d, empty);
            if (d == null || empty) {
                setText(null);
            } else if (d.isZero() || d.equals(Duration.ofNanos(Long.MAX_VALUE))){
                setText("");
            } else {
                // Format duration.
                setText(DurationFormatter.durationToString(d, 3, Boolean.FALSE));
            }
        }
    };
    
    private class OutputPortalListCell extends ListCell<OutputPortal> {
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
        public void updateItem(OutputPortal op, boolean empty) {
            super.updateItem(op, empty);
            if (empty || op == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(null);
                protocolLabel.setText(op.protocolProperty().getValueSafe());
                //serverLabel.setText(op.serverProperty().getValueSafe());
                if (op.getOutputProtocol().equals(FileTransferTypes.LOCAL)) pathLabel.setText(op.basePathProperty().getValueSafe());
                else pathLabel.setText(op.serverProperty().getValueSafe() + ":" + op.basePathProperty().getValueSafe());
                enabledCheckBox.selectedProperty().bindBidirectional(op.enabledProperty());
                transferStatusLabel.setText("Status: " + op.transferStatusProperty().getValueSafe());
                setGraphic(container);
                System.out.println("updateItem called: " + op.transferStatusProperty().getValueSafe());
            }
        }
    }
}
