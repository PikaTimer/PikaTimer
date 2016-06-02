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

import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.AgeGroups;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceAwards;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.DurationComparator;
import com.pikatimer.util.DurationFormatter;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.PatternSyntaxException;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
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
    
    @FXML ChoiceBox<Integer> agIncrementChoiceBox;
    @FXML TextField agStartTextField;
    @FXML TextField agMastersStartTextField;
    
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

    @FXML ToggleSwitch autoUpdateToggleSwitch;
    
    final Map<Race,TableView> raceTableViewMap = new ConcurrentHashMap();
    final RaceDAO raceDAO = RaceDAO.getInstance();
    final ResultsDAO resultsDAO = ResultsDAO.getInstance();
    final ParticipantDAO participantDAO = ParticipantDAO.getInstance();
    
    private Race activeRace;
    
    private final BooleanProperty populateAwardSettingsInProgress = new SimpleBooleanProperty(FALSE);

    /**
     * Initializes the controller class.
     */
    public void initialize() {
        
        raceComboBox.setItems(raceDAO.listRaces());
        raceComboBox.visibleProperty().bind(Bindings.size(raceDAO.listRaces()).greaterThan(1));
        selectedRaceLabel.visibleProperty().bind(Bindings.size(raceDAO.listRaces()).greaterThan(1));

        initializeAgeGroupSettings();
        initializeAwardSettings();
        initializeOutputSettings();
                
        raceComboBox.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> observableValue, Number number, Number number2) -> {
            // flip the table
            //System.out.println("raceChoiceBox listener fired: now with number2 set to " + number2.intValue());
            
            if (number2.intValue() == -1 )  {
                raceComboBox.getSelectionModel().clearAndSelect(0);
                return;
            } 
            
            
            activeRace = raceComboBox.getItems().get(number2.intValue());
            
            // Populate the AG settings
            populateAgeGroupSettings(activeRace);
            // Populate the Awards Settings
            populateAwardsSettings(activeRace);
            // Populate the Output Settings
            populateOutputSettings(activeRace);
            
            // Populate the results TableView
            if( ! raceTableViewMap.containsKey(activeRace)) {
                rebuildResultsTableView(activeRace);
            }
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
            if (number.intValue() > 0) {
                Race old = raceComboBox.getItems().get(number.intValue());
                resultsGridPane.getChildren().remove(raceTableViewMap.get(old));
            }
            resultsGridPane.getChildren().remove(raceTableViewMap.get(activeRace));
            resultsGridPane.add(raceTableViewMap.get(activeRace), 0, 1);
            
        });
        
        raceComboBox.getSelectionModel().clearAndSelect(0);
        

    }    
    
    private void rebuildResultsTableView(Race r) {
        // create a table
                TableView<Result> table = new TableView();
                
                //table.setPadding(new Insets(5));
                
                
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
                
                // start
                TableColumn<Result,String> startColumn = new TableColumn("Start");
                table.getColumns().add(startColumn);
                startColumn.setCellValueFactory(cellData -> {
                    return new SimpleStringProperty(DurationFormatter.durationToString(cellData.getValue().getStartDuration(), 3, Boolean.TRUE));
                });
                startColumn.setComparator(new AlphanumericComparator());
                
                
                // for each split from 2 -> n-2
                //TableColumn<Result,String> splitColumn;
                for (int i = 2; i <  r.getSplits().size() ; i++) {
                    TableColumn<Result,String> splitColumn = new TableColumn();
                    table.getColumns().add(splitColumn);
                    int splitID  = i; 
                    //splitColumn.setCellFactory(null);
                    
                    splitColumn.textProperty().bind(r.splitsProperty().get(splitID-1).splitNameProperty());

                    splitColumn.setCellValueFactory(cellData -> {
                        Duration split = cellData.getValue().getSplitTime(splitID);
                        
                        if (split.isZero()) return new SimpleStringProperty("");
                        return new SimpleStringProperty(DurationFormatter.durationToString(split.minus(cellData.getValue().getStartDuration()), 3, Boolean.TRUE));
                    });
                    splitColumn.setComparator(new AlphanumericComparator());
                }
                
                
                // finish
                TableColumn<Result,Duration> finishColumn = new TableColumn("Finish");
                table.getColumns().add(finishColumn);
                finishColumn.setCellValueFactory(cellData -> {
                    return cellData.getValue().finishTimeProperty(); 
                });
                finishColumn.setCellFactory(column -> {
			return new TableCell<Result, Duration>() {
				@Override
				protected void updateItem(Duration d, boolean empty) {
					super.updateItem(d, empty);
					if (d == null || empty ) {
						setText(null);
						setStyle("");
					} else {
						// Format date.
                                                setText(DurationFormatter.durationToString(d, 3, Boolean.FALSE));
					}
                                        
				}
			};
		});
                //finishColumn.setComparator(new DurationComparator());
                
                // gun
                TableColumn<Result,String> gunColumn = new TableColumn("Gun");
                table.getColumns().add(gunColumn);
                gunColumn.setComparator(new AlphanumericComparator());
                
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
                        //System.out.println("Filtered list eval for result " + result.getBib());
                        // Compare first name and last name of every person with filter text.
                        String lowerCaseFilter = "(.*)(" + newValue.toLowerCase() + ")(.*)";

                        try {    
                            Participant p = participantDAO.getParticipantByBib(result.getBib());
                            if (p == null) { 
                                System.out.println(" Null participant, bailing...");
                                return false;
                            }

                            if ((p.fullNameProperty().getValueSafe() + " " + result.getBib() + " ").toLowerCase().matches(lowerCaseFilter)) {
                                //System.out.println(" Match: " + lowerCaseFilter + " " + p.fullNameProperty().getValueSafe() + " " + result.getBib() );
                                return true; // Filter matches first/last/email/bib.
                            } 

                        } catch (Exception e) {
                            //e.printStackTrace();
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

    private void populateAgeGroupSettings(Race r) {
        System.out.println("populateRaceAGSettings() called...");
        
        AgeGroups ageGroups;
        if (r.getAgeGroups() == null) {
            ageGroups = new AgeGroups();
            r.setAgeGroups(ageGroups);
            raceDAO.updateRace(r);
        } else {
            ageGroups = r.getAgeGroups();
        }

        agStartTextField.setText(ageGroups.getAGStart().toString());
        agIncrementChoiceBox.getSelectionModel().select(ageGroups.getAGIncrement());
        agMastersStartTextField.setText(ageGroups.getMasters().toString());
    }

            
    private void initializeAgeGroupSettings() {
        //@FXML ChoiceBox agIncrementChoiceBox;
        //@FXML TextField agStartTextField;
        //@FXML TextField agMastersStartTextField;
        System.out.println("initizlizeRaceAGSettings() called...");

        TextFormatter<String> AGSformatter = new TextFormatter<>( change -> {
            change.setText(change.getText().replaceAll("[^0-9]", ""));
            return change; 
        });
        agStartTextField.setTooltip(new Tooltip("Sets the max age for the first age group. i.e. 1 -> X"));  
        agStartTextField.setTextFormatter(AGSformatter);
        
        
        agIncrementChoiceBox.setItems(FXCollections.observableArrayList(5, 10));
        
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
        
    }
    
    private void initializeAwardSettings() {
        //    @FXML ChoiceBox awardOverallPullChoiceBox;
        awardOverallPullChoiceBox.setItems(FXCollections.observableArrayList("Yes","No"));
        
        
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
        
        populateAwardSettingsInProgress.setValue(FALSE);
    }
    
    private void initializeOutputSettings() {
        
    }

    private void populateOutputSettings(Race r) {
        
    }
    
}
