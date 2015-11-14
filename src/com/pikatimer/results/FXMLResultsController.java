/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.results;

import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.DurationFormatter;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
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
            
            Race r = raceChoiceBox.getItems().get(number2.intValue());
            if( ! raceTableViewMap.containsKey(r)) {
                // create a table
                TableView<Result> table = new TableView();
                table.setItems(resultsDAO.getResults(r.getID()));
                
                // create the columns for the table with 
                // cellValueFactories convert from the Result data to something we can display
                // bib
                TableColumn<Result,String> bibColumn = new TableColumn("Bib");
                table.getColumns().add(bibColumn);
                bibColumn.setCellValueFactory(new PropertyValueFactory<>("bib"));
                bibColumn.setComparator(new AlphanumericComparator());
                
                // name
                TableColumn<Result,String> nameColumn = new TableColumn("Name");
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
                TableColumn<Result,String> splitColumn;
                for (int i = 1; i <  r.getSplits().size() - 1 ; i++) {
                    splitColumn = new TableColumn(r.getSplits().get(i).getSplitName());
                    table.getColumns().add(splitColumn);
                    int splitID  = i+1; 
                    splitColumn.setCellValueFactory(cellData -> {
                        Duration split = cellData.getValue().getSplitTime(splitID);
                        if (split.isZero()) return new SimpleStringProperty("");
                        return new SimpleStringProperty(DurationFormatter.durationToString(split.minus(cellData.getValue().getStartDuration()), 3, Boolean.TRUE));
                    });
                    splitColumn.setComparator(new AlphanumericComparator());
                }
                
                
                // finish
                TableColumn<Result,String> finishColumn = new TableColumn("Finish");
                table.getColumns().add(finishColumn);
                finishColumn.setCellValueFactory(cellData -> {
                    return new SimpleStringProperty(DurationFormatter.durationToString(cellData.getValue().getFinishDuration().minus(cellData.getValue().getStartDuration()), 3, Boolean.TRUE));
                });
                finishColumn.setComparator(new AlphanumericComparator());
                
                // gun
                TableColumn<Result,String> gunColumn = new TableColumn("Gun");
                table.getColumns().add(gunColumn);
                gunColumn.setComparator(new AlphanumericComparator());
                // set the sort order for the colums
                
                // wire up the search field
                
                table.setItems(resultsDAO.getResults(r.getID()));
                
                // save it
                raceTableViewMap.put(r, table);
                
               
                
            }
            
            if (number.intValue() > 0) {
                Race old = raceChoiceBox.getItems().get(number.intValue());
                resultsGridPane.getChildren().remove(raceTableViewMap.get(old));
            }
            resultsGridPane.getChildren().remove(raceTableViewMap.get(r));
            resultsGridPane.add(raceTableViewMap.get(r), 0, 1);
            
        });
        
        raceChoiceBox.getSelectionModel().clearAndSelect(0);

    }    
    
}
