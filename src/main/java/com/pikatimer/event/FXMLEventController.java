/* 
 * Copyright (C) 2024 John Garner
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
package com.pikatimer.event;

import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.AgeGroups;
import com.pikatimer.race.AwardCategory;
import com.pikatimer.race.CourseRecord;
import com.pikatimer.race.FXMLRaceDetailsController;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceAwards;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.SexCode;
import com.pikatimer.race.SexGroups;
import com.pikatimer.race.Wave;
import com.pikatimer.timing.Segment;
import com.pikatimer.timing.Split;
import com.pikatimer.timing.TimingLocation;
import com.pikatimer.timing.TimingDAO;
import com.pikatimer.util.Unit;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLEventController {
    private static final Logger logger = LoggerFactory.getLogger(FXMLEventController.class);
    
    private final Event event = Event.getInstance();
    private EventDAO eDAO;    
    private FXMLLoader raceDetailsLoader;
    
    @FXML    private TextField eventTitle;
    @FXML    private DatePicker eventDate;
    @FXML    private VBox racesVBox;    
    @FXML    private TableView<Race> raceTableView;
    @FXML    private Button addRaceButton;
    @FXML    private Button copyRaceButton;
    @FXML    private Button removeRaceButton;    
    private ObservableList<Race> raceList;    

    @FXML    private VBox timingVBox;
    @FXML    private ListView<TimingLocation> timingLocListView;
    @FXML    private Button timingLocAddButton;
    @FXML    private Button timingLocRemoveButton;    

    private ObservableList<TimingLocation> timingLocationList;
    private TimingDAO timingLocationDAO;    
    private FXMLRaceDetailsController raceDetailsController;
    private RaceDAO raceDAO;

    //@FXML private Pane raceDetailsPane;
    @FXML    private VBox raceDetailsVBox;

    /**
     * Initializes the controller class.
     */
    @FXML
    protected void initialize() {
        logger.debug("FXMLpikaController initialize called...");
        
        eventTitle.setText(event.getEventName());
        logger.trace("FXMLpikaController initialize set title");

        //Watch for text changes... Because setOnInputMethodTextChanged does not work :-( 
        eventTitle.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                logger.trace("Textfield out focus");
                if (!eventTitle.getText().equals(event.getEventName())) {
                    setEventTitle();
                }
            }
        });


        eventDate.setValue(event.getLocalEventDate());
        eventDate.setOnAction((e) -> setEventDate());

        logger.trace("FXMLpikaController initialize set date");


        
        timingLocationDAO = TimingDAO.getInstance();
        timingLocationList = timingLocationDAO.listTimingLocations();        
        if (timingLocationList.isEmpty()) {
            timingLocationDAO.createDefaultTimingLocations();
        }
        
        timingLocListView.setItems(timingLocationList);
        
        timingLocAddButton.setOnAction((e) -> addTimingLocation());
        timingLocRemoveButton.setOnAction((e) -> removeTimingLocation());

        
        timingLocListView.setEditable(true);

        //timingLocListView.setCellFactory(TextFieldListCell.forListView(null));
        timingLocListView.setCellFactory(TextFieldListCell.forListView(new StringConverter<TimingLocation>() {
            @Override
            public TimingLocation fromString(String s) {
                TimingLocation t = new TimingLocation();
                t.setLocationName(s);
                return t;                
            }

            @Override
            public String toString(TimingLocation t) {
                if (t != null) {
                    return t.getLocationName();                    
                } else {
                    logger.warn("Timing StringConverter toString null object detected.");
                    return "";
                }
            }
        }
        ));        
        
        timingLocListView.setOnEditCommit((ListView.EditEvent<TimingLocation> t) -> {
            logger.debug("setOnEditCommit " + t.getIndex());
            if (t.getIndex() < t.getSource().getItems().size()) {
                TimingLocation tl = t.getSource().getItems().get(t.getIndex());                
                if (t.getNewValue().toString().isEmpty()) {
                    //timingLocationDAO.removeTimingLocation(tl);
                    //tl.setLocationName("New Timing Location");
                    timingLocationDAO.removeTimingLocation(tl);
                } else {
                    tl.setLocationName(t.getNewValue().toString());
                    timingLocationDAO.updateTimingLocation(tl);
                }
            } else {
                logger.warn("Timing setOnEditCommit event out of index: " + t.getIndex());
            }
            timingLocAddButton.requestFocus();
            timingLocAddButton.setDefaultButton(true);
        });
        
        timingLocListView.setOnEditCancel((ListView.EditEvent<TimingLocation> t) -> {
            logger.debug("setOnEditCancel " + t.getIndex());
            if (t.getIndex() >= 0 && t.getIndex() < t.getSource().getItems().size()) {
                TimingLocation tl = t.getSource().getItems().get(t.getIndex());
                if (tl.getLocationName().isEmpty()) {
                    //tl.setLocationName("New Timing Location");
                    timingLocationDAO.removeTimingLocation(tl);
                }
            } else {
                logger.warn("Timing setOnEditCancel event out of index: " + t.getIndex());
            }
            timingLocAddButton.requestFocus();
            timingLocAddButton.setDefaultButton(true);
        });
        
        timingLocRemoveButton.disableProperty().bind(timingLocListView.getSelectionModel().selectedItemProperty().isNull());

        addRaceButton.setOnAction((e) -> addRace());
        removeRaceButton.setOnAction((e) -> removeRace());
        copyRaceButton.setOnAction((e) -> copyRace());
        
        removeRaceButton.disableProperty().bind(raceTableView.getSelectionModel().selectedItemProperty().isNull());
        copyRaceButton.disableProperty().bind(raceTableView.getSelectionModel().selectedItemProperty().isNull());
        
        raceDAO = RaceDAO.getInstance();
        raceList = raceDAO.listRaces();        
        Race selectedRace;
        if (raceList.isEmpty()) {
            logger.error("Race list is empty!!!  Creating default race...");
            selectedRace = new Race();            
            selectedRace.setRaceName(event.getEventName());
            selectedRace.setRaceDistance(new BigDecimal("5.0"));            
            selectedRace.setRaceDistanceUnits(Unit.KILOMETERS);
            
            AgeGroups ageGroups = new AgeGroups();
            selectedRace.setAgeGroups(ageGroups);
            RaceAwards ra = new RaceAwards();
            selectedRace.setAwards(ra);
            //raceDAO.updateRace(selectedRace);
            SexGroups sg = new SexGroups();
                    
            logger.debug("Empty SexCodeList. Adding defaults");
            sg.addSexCode(new SexCode("F","Female"));
            sg.addSexCode(new SexCode("M","Male"));
            sg.addSexCode(new SexCode("X","Non-Binary"));
        
            selectedRace.setSexGroups(sg);
            
            raceDAO.addRace(selectedRace);
        } else {
            selectedRace = raceList.get(0);            
        }
        
        raceTableView.setItems(raceList);
        
        removeRaceButton.visibleProperty().bind(Bindings.size(raceList).greaterThan(1));

        // load up the raceDetailsPane
        // Save the FXMLLoader so that we can send it notes when things change in the races box
        raceDetailsVBox.getChildren().clear();
        try {
            raceDetailsLoader = new FXMLLoader(getClass().getResource("/com/pikatimer/race/FXMLRaceDetails.fxml"));
            raceDetailsVBox.getChildren().add(raceDetailsLoader.load());
        } catch (IOException ex) {
            logger.error("Exception in raceDetails VBox initialization", ex);
        }
        
        raceDetailsController = (FXMLRaceDetailsController) raceDetailsLoader.getController();        
        raceDetailsController.selectRace(selectedRace);
        // bind the selected race to the
        //FXMLRaceDetailsController raceDetailsController = raceDetailsLoader.<FXMLRaceDetailsController>getController(); 
        //raceDetailsController.selectRace(r);

        //if there are no races selected in the race table then disable the entire right hand side
        raceDetailsVBox.disableProperty().bind(raceTableView.getSelectionModel().selectedItemProperty().isNull());
        raceTableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Cha‌​nge<? extends Race> c) -> {            
            raceTableView.getSelectionModel().getSelectedItems().forEach( sr -> logger.trace(sr.toString()));            
            ObservableList<Race> selectedRaces = raceTableView.getSelectionModel().getSelectedItems();
            if (selectedRaces.isEmpty()) {
                logger.trace("Nothing Selected");
                raceDetailsController.selectRace(null);
            } else {
                logger.trace(selectedRaces.get(0).getRaceName());
                raceDetailsController.selectRace(selectedRaces.get(0));
            }
        });
        
        raceTableView.getSelectionModel().clearAndSelect(0);
        
        logger.debug("FXMLpikaController initialized!");
        
    }


    
    protected void setEventTitle() {
        event.setEventName(eventTitle.getText());
        updateEvent();

    }
    
    protected void setEventDate() {
        
        event.setEventDate(eventDate.getValue());
        updateEvent();

        // TODO: Recalc all participant ages if they have a birthdate set
    }


    private void updateEvent() {
        if (eDAO == null) {
            eDAO = new EventDAO();            
        }
        eDAO.updateEvent();
    }

    
    public void addTimingLocation() {
        // prompt 
        TimingLocation t = new TimingLocation();
        t.setLocationName("New Timing Location");
        
        timingLocationDAO.addTimingLocation(t);
        logger.debug("Setting the timingLocListView.edit to " + timingLocationList.size() + " " + timingLocationList.indexOf(t));
        timingLocListView.getSelectionModel().select(timingLocationList.indexOf(t));
        timingLocListView.edit(timingLocationList.indexOf(t));

        //Because we call the timingLocListView.edit, we don't want to pull back focus
        //timingLocAddButton.requestFocus();
    }

    public void removeTimingLocation() {
        
        final TimingLocation tl = timingLocListView.getSelectionModel().getSelectedItem();

        // If the location is referenced by a split, 
        // toss up a warning and leave it alone
        final StringProperty splitsUsing = new SimpleStringProperty();
        raceDAO.listRaces().forEach(r -> {
            r.getSplits().forEach(s -> {
                if (s.getTimingLocation().equals(tl)) {
                    splitsUsing.set(splitsUsing.getValueSafe() + r.getRaceName() + " " + s.getSplitName() + "\n");
                }
            });
        });
        
        if (splitsUsing.isEmpty().get()) {
            timingLocationDAO.removeTimingLocation(tl);;
            timingLocAddButton.requestFocus();
            timingLocAddButton.setDefaultButton(true);
        } else {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Unable to Remove Timing Location");
            alert.setHeaderText("Unable to remove the " + tl.getLocationName() + " timing location.");
            alert.setContentText("The timing location is in use by the following splits:\n" + splitsUsing.getValueSafe());
            
            alert.showAndWait();
        }
    }
    
    public void resetRaces() {
        // prompt 
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Resetting All Races");
        alert.setContentText("This action cannot be undone.");
        alert.setHeaderText("This will delete all configured races!");
        //Label alertContent = new Label("This will reset the timing locations to default values.\nAll splits will be reassigned to one of the default locations.");
        //alertContent.setWrapText(true); 
        //alert.getDialogPane().setContent(alertContent);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            raceDAO.clearAll();
        } else {
            // ... user chose CANCEL or closed the dialog
        }
        
        addRaceButton.requestFocus();
        addRaceButton.setDefaultButton(true);
    }
    
    public void addRace() {
        // prompt 
        Race r = new Race();
        r.setRaceName("New Event");
        r.setRaceDistance(new BigDecimal("5.0"));        
        r.setRaceDistanceUnits(Unit.KILOMETERS);
        r.setAgeGroups(new AgeGroups());

        RaceAwards ra = new RaceAwards();
        r.setAwards(ra);
        SexGroups sg = new SexGroups();

        logger.debug("Empty SexCodeList. Adding defaults");
        sg.addSexCode(new SexCode("F","Female"));
        sg.addSexCode(new SexCode("M","Male"));
        sg.addSexCode(new SexCode("X","Non-Binary"));

        r.setSexGroups(sg);
            
 
            
        raceDAO.addRace(r);
        logger.debug("Adding a new race. New Size =" + raceList.size() + " New Race Index=" + raceList.indexOf(r));
        Platform.runLater(() -> {
            raceTableView.getSelectionModel().select(raceList.indexOf(r));
        });

        addRaceButton.setDefaultButton(false);
        
        raceDAO.updateRace(r);
        
    }

    public void copyRace(){
        // WARNING:  This is a huge anti-pattern. 
        //
        // In order to copy a race and the waves/splits/awards/results/etc, 
        // we need to know a lot about the inner functioning and 
        // structure of a race and every class that the race has in order to 
        // be able to do the deep copy/clone. 
        //
        // I'm sure there is a "proper" way to do this, but at this point 
        // I really don't think it is worth the brain damage. 
        
        final Race sourceRace = raceTableView.getSelectionModel().getSelectedItem();
        
        if (sourceRace == null) {
            return;
        }
        
        Race newRace = new Race();
        logger.debug("copyRace called for {} ",sourceRace.getRaceName());
        // copy basic details
        newRace.setRaceName(sourceRace.getRaceName() + " (copy)");
        newRace.setRaceDistance(sourceRace.getRaceDistance());
        newRace.setRaceDistanceUnits(sourceRace.getRaceDistanceUnits());
        newRace.setBibStart(sourceRace.getBibStart());
        newRace.setBibEnd(sourceRace.getBibEnd());
        newRace.setRaceCutoff(sourceRace.getRaceCutoff());
        newRace.setRaceMaxStart(sourceRace.getRaceMaxStart());
        
        // copy race attributes
        Map<String,String> a = new HashMap();
        a.putAll(sourceRace.getAttributes());
        newRace.setAttributes(a);

        // Save the race so we can add the waves, splits, and segments
        raceDAO.addRace(newRace);
        
        // copy waves
        sourceRace.getWaves().forEach(w -> {
            Wave newWave = new Wave(newRace);
            newWave.copy(w);
            raceDAO.addWave(newWave);
        });
        
        // copy sex groups
        SexGroups newSG = new SexGroups();
        newSG.clone(sourceRace.getSexGroups());
        newRace.setSexGroups(newSG);
        raceDAO.updateRace(newRace);
        
        
        // copy AG settings 
        AgeGroups newAG = new AgeGroups();
        newAG.clone(sourceRace.getAgeGroups());
        newRace.setAgeGroups(newAG);
        raceDAO.updateRace(newRace);
        
        
        // copy splits
        Map<Split,Split> splitMap = new HashMap(); // used by segments below
        sourceRace.getSplits().forEach(s -> {
            Split newSplit = new Split(newRace);
            newSplit.setSplitName(s.getSplitName());
            newSplit.setSplitDistanceUnits(newRace.getRaceDistanceUnits());
            newSplit.setSplitDistance(s.getSplitDistance());
            newSplit.setTimingLocation(s.getTimingLocation());
            newSplit.setPosition(s.getPosition()); 

            raceDAO.addSplit(newSplit);
            splitMap.put(s, newSplit);
        });
        
        
        // copy segments
        Map<Segment,Segment> segmentMap = new HashMap();
        sourceRace.getSegments().forEach(s -> {
            Segment newSegment = new Segment();
            newSegment.setRace(newRace);
            newSegment.setSegmentName(s.getSegmentName());
            newSegment.setStartSplit(splitMap.get(s.getStartSplit()));
            newSegment.setEndSplit(splitMap.get(s.getEndSplit()));
            raceDAO.updateSegment(s);
            newRace.addRaceSegment(newSegment);
        });
        
        // copy race awards
        logger.debug("Copying Race Awards...");
        RaceAwards newRA = new RaceAwards();
        newRA.getAttributes().putAll(sourceRace.getAwards().getAttributes());
        newRace.setAwards(newRA);
        raceDAO.updateRace(newRace);
        newRA.awardCategoriesProperty().clear();
        
        sourceRace.getAwards().getAwardCategories().forEach(oldAC -> {
            logger.debug("Copying AwardCategory {}",oldAC.getName());
            AwardCategory newAC = new AwardCategory();
            newAC.clone(oldAC);
            newAC.setRaceAward(newRA);
            newRA.addAwardCategory(newAC);
            raceDAO.updateAwardCategory(newAC);
        });
        logger.debug("New Race now has {} award categories.",newRace.getAwards().awardCategoriesProperty().size());
        
        // copy race reports
        sourceRace.getRaceReports().forEach(rr -> {
            //newRace.raceReportsProperty().add(rr.clone());
        });
             
        
        logger.debug("Adding a new race. New Size =" + raceList.size() + " New Race Index=" + raceList.indexOf(newRace));
        Platform.runLater(() -> {
            raceTableView.getSelectionModel().select(raceList.indexOf(newRace));
        });
        
    }
    
    public void removeRace() {
        
        final Race r = raceTableView.getSelectionModel().getSelectedItem();
        
        if (r == null) {
            return;
        }

        // Do we have any runner's assigned?
        BooleanProperty assignedRunners = new SimpleBooleanProperty(false);
        BooleanProperty assignedTimingLoc = new SimpleBooleanProperty(false);
        StringProperty assignedTimingLocations = new SimpleStringProperty();
        
        ParticipantDAO.getInstance().listParticipants().forEach(x -> {
            x.getWaveIDs().forEach(w -> {
                if (RaceDAO.getInstance().getWaveByID(w).getRace().equals(r)) {
                    assignedRunners.setValue(Boolean.TRUE);
                    //logger.debug("Race " + RaceDAO.getInstance().getWaveByID(w).getRace().getRaceName() + " is in use by " + x.fullNameProperty().getValueSafe());
                }
            });
        });
        
        TimingDAO.getInstance().listTimingLocations().forEach(x -> {
            if (x.getAutoAssignRaceID() >= 0) {
                assignedTimingLoc.setValue(Boolean.TRUE);
                if (assignedTimingLocations.isEmpty().get()) {
                    assignedTimingLocations.setValue(x.getLocationName());
                } else {
                    assignedTimingLocations.setValue(assignedTimingLocations.getValue() + "\n" + x.getLocationName());
                }
            }
        });
        
        if (assignedRunners.get() || assignedTimingLoc.get()) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Unable to Remove Race");
            alert.setHeaderText("Unable to remove the selected race.");
            if (assignedRunners.get()) {
                alert.setContentText("This race currently has assigned runners.\nPlease assign them to a different race before removing.");
            }
            if (assignedTimingLoc.get()) {
                alert.setContentText("The following timing locations are set to\nauto-assign runners to this race:\n\n" + assignedTimingLocations.getValueSafe());
            }
            
            alert.showAndWait();
        } else {
            // prompt 
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirm Race Removal");
            alert.setContentText("This action cannot be undone!");
            alert.setHeaderText("This will remove the " + raceTableView.getSelectionModel().getSelectedItem().getRaceName() + " event");
            //Label alertContent = new Label("This will reset the timing locations to default values.\nAll splits will be reassigned to one of the default locations.");
            //alertContent.setWrapText(true); 
            //alert.getDialogPane().setContent(alertContent);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.get() == ButtonType.OK) {
                raceDAO.removeRace(raceTableView.getSelectionModel().getSelectedItem());
                raceTableView.getSelectionModel().select(raceList.indexOf(0));
                addRaceButton.requestFocus();
                addRaceButton.setDefaultButton(false);
            }
        }
        
    }
}
