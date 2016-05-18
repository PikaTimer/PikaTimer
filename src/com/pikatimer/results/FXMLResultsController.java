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
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.DurationComparator;
import com.pikatimer.util.DurationFormatter;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.PatternSyntaxException;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;


/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLResultsController  {
    
    @FXML TextField resultsSearchTextField;
    @FXML GridPane resultsGridPane;
    @FXML ChoiceBox<Race> raceChoiceBox;
    
    final Map<Race,TableView> raceTableViewMap = new ConcurrentHashMap();
    final RaceDAO raceDAO = RaceDAO.getInstance();
    final ResultsDAO resultsDAO = ResultsDAO.getInstance();
    final ParticipantDAO participantDAO = ParticipantDAO.getInstance();

    /**
     * Initializes the controller class.
     */
    public void initialize() {
        // TODO
        raceChoiceBox.setItems(raceDAO.listRaces());
        
        
        raceChoiceBox.getSelectionModel().selectedIndexProperty().addListener((ObservableValue<? extends Number> observableValue, Number number, Number number2) -> {
            // flip the table
            System.out.println("raceChoiceBox listener fired: now with number2 set to " + number2.intValue());
            
            if (number2.intValue() == -1 )  {
                raceChoiceBox.getSelectionModel().clearAndSelect(0);
                return;
            } 
            
            
            Race r = raceChoiceBox.getItems().get(number2.intValue());
            if( ! raceTableViewMap.containsKey(r)) {
                rebuildResultsTableView(r);
            }
            r.splitsProperty().addListener( new ListChangeListener() {
 
                @Override
                public void onChanged(ListChangeListener.Change change) {
                    System.out.println("The list of splits has changed...");
                    TableView oldTableView = raceTableViewMap.get(r);
                    rebuildResultsTableView(r);
                    if (raceChoiceBox.getSelectionModel().getSelectedItem().equals(r)) {
                        resultsGridPane.getChildren().remove(oldTableView);
                        resultsGridPane.add(raceTableViewMap.get(r), 0, 1);
                    }
                }
            });
            if (number.intValue() > 0) {
                Race old = raceChoiceBox.getItems().get(number.intValue());
                resultsGridPane.getChildren().remove(raceTableViewMap.get(old));
            }
            resultsGridPane.getChildren().remove(raceTableViewMap.get(r));
            resultsGridPane.add(raceTableViewMap.get(r), 0, 1);
            
        });
        
        raceChoiceBox.getSelectionModel().clearAndSelect(0);

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
    
}
