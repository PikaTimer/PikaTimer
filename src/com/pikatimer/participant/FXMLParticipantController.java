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
package com.pikatimer.participant;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.race.Wave;
import com.pikatimer.race.WaveAssignment;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.WaveStringConverter;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.container.DefaultFlowContainer;
import static java.lang.Boolean.FALSE;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.controlsfx.control.CheckComboBox;

/**
 *
 * @author jcgarner
 */
public class FXMLParticipantController  {

    @FXML private TableView<Participant> tableView;
    @FXML private TableColumn bibNumberColumn;
    @FXML private VBox formVBox; 
    @FXML private TextField bibTextField;
    @FXML private CheckComboBox<Wave> waveComboBox; 
    @FXML private TextField firstNameField;
    @FXML private TextField middleNameTextField;
    @FXML private TextField lastNameField;
    @FXML private TextField ageTextField;
    @FXML private TextField sexTextField;
    @FXML private TextField cityTextField; 
    @FXML private TextField stateTextField;
    @FXML private TextField emailField;
    @FXML private TextField filterField; 
    @FXML private Button formAddButton; 
    @FXML private Button formUpdateButton;
    @FXML private Button formResetButton;
    @FXML private Label filteredSizeLabel;
    @FXML private Label listSizeLabel; 
    @FXML private CheckBox dnfCheckBox;
    @FXML private TextField dnfTextField;
    @FXML private CheckBox dqCheckBox;
    @FXML private TextField dqTextField;
    
    private ObservableList<Participant> participantsList;
    private ParticipantDAO participantDAO;
    private Participant editedParticipant; 
    
    
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
        
        
        // Setup the context menu and actions
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        tableView.setRowFactory((TableView<Participant> tableView1) -> {
            final TableRow<Participant> row = new TableRow<>();
            final ContextMenu rowMenu = new ContextMenu();
            
            row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
                editParticipant(tableView.getSelectionModel().getSelectedItem());
            }
            });
            
            // Context menu
            MenuItem editItem = new MenuItem("Edit");
            editItem.setOnAction((ActionEvent event) -> {
                editParticipant(tableView.getSelectionModel().getSelectedItem());
            });
            
            MenuItem removeItem = new MenuItem("Delete");
            removeItem.setOnAction((ActionEvent event) -> {
                
                removeParticipants(FXCollections.observableArrayList(tableView.getSelectionModel().getSelectedItems()));
//                ObservableList deleteMe = FXCollections.observableArrayList(tableView.getSelectionModel().getSelectedItems());
//                
//                Iterator<Participant> deleteMeIterator = deleteMe.iterator();
//		while (deleteMeIterator.hasNext()) {
//                    Participant p = deleteMeIterator.next();
//                    removeParticipant(p); 
//		}
                
            });
            
            MenuItem swapBibs = new MenuItem("Swap Bibs");
            swapBibs.setOnAction((ActionEvent event) -> {
                ObservableList<Participant> swapMe = FXCollections.observableArrayList(tableView.getSelectionModel().getSelectedItems());
                
                if (swapMe.size() == 2) {
                    String tmp = swapMe.get(1).getBib();
                    swapMe.get(1).setBib(swapMe.get(0).getBib());
                    swapMe.get(0).setBib(tmp);
                    
                    participantDAO.updateParticipant(swapMe.get(2));
                    participantDAO.updateParticipant(swapMe.get(1));
                    
                }
		
            });
            
            Menu assignWave = new Menu("Assign");
            RaceDAO.getInstance().listWaves().sorted((Wave u1, Wave u2) -> u1.toString().compareTo(u2.toString())).stream().forEach(w -> {
                MenuItem m = new MenuItem(w.toString());
                m.setOnAction(e -> {
                    tableView.getSelectionModel().getSelectedItems().stream().forEach(p -> {
                        p.setWaves(w);
                        participantDAO.updateParticipant(p);

                    });
                });
                assignWave.getItems().add(m);
            });
            
            RaceDAO.getInstance().listWaves().addListener((Change<? extends Wave> change) -> {
                assignWave.getItems().clear();
                RaceDAO.getInstance().listWaves().sorted((Wave u1, Wave u2) -> u1.toString().compareTo(u2.toString())).stream().forEach(w -> {
                    MenuItem m = new MenuItem(w.toString());
                    m.setOnAction(e -> {
                        tableView.getSelectionModel().getSelectedItems().stream().forEach(p -> {
                            p.setWaves(w);
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
            tableView.getSelectionModel().getSelectedIndices().addListener((Change<? extends Integer> change) -> {
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
        // TODO Only filter on the visible colums 
        // 1. Wrap the ObservableList in a FilteredList (initially display all data).
        FilteredList<Participant> filteredParticipantsList = new FilteredList<>(participantsList, p -> true);

        // 2. Set the filter Predicate whenever the filter changes.
        filterField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredParticipantsList.setPredicate(participant -> {
                // If filter text is empty, display all persons.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                // Compare first name and last name of every person with filter text.
                String lowerCaseFilter = "(.*)(" + newValue.toLowerCase() + ")(.*)";

                try {    
                    if ((participant.getFirstName() + " " + participant.getLastName() + " " + participant.getEmail() + " " + participant.getBib()).toLowerCase().matches(lowerCaseFilter)) {
                        return true; // Filter matches first/last/email/bib.
                    } 

                } catch (PatternSyntaxException e) {
                    return true;
                }
                return false; // Does not match.
            });
        });
        // 3. Wrap the FilteredList in a SortedList. 
        SortedList<Participant> sortedParticipantsList = new SortedList(filteredParticipantsList);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedParticipantsList.comparatorProperty().bind(tableView.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.
        tableView.setItems(sortedParticipantsList);
        
        // Set the bib number to be an alphanumeric sort
        bibNumberColumn.setComparator(new AlphanumericComparator());
        
        
        listSizeLabel.textProperty().bind(Bindings.size(participantsList).asString());
        filteredSizeLabel.textProperty().bind(Bindings.size(sortedParticipantsList).asString());
        
        // Only show note if the DNF/DQ checkbox is checked
        dqTextField.visibleProperty().bind(dqCheckBox.selectedProperty());
        dqTextField.managedProperty().bind(dqCheckBox.selectedProperty());
        
        dnfTextField.visibleProperty().bind(dnfCheckBox.selectedProperty());
        dnfTextField.managedProperty().bind(dnfCheckBox.selectedProperty());
        
        dnfCheckBox.disableProperty().bind(dqCheckBox.selectedProperty());
        dqCheckBox.disableProperty().bind(dnfCheckBox.selectedProperty());

        
        // if there is only one race, hide the option to pick a race... 
        waveComboBox.visibleProperty().bind(Bindings.size(RaceDAO.getInstance().listWaves()).greaterThan(1));
        waveComboBox.managedProperty().bind(Bindings.size(RaceDAO.getInstance().listWaves()).greaterThan(1));
        waveComboBox.getItems().addAll(RaceDAO.getInstance().listWaves());
        RaceDAO.getInstance().listWaves().addListener((Change<? extends Wave> change) -> {
            //waveComboBox.getItems().clear();
            //Platform.runLater(() -> {
               System.out.println("PartController::raceWaves(changeListener) fired...");
                
            // TODO
            //rework the popup menu for the add/delete
            
            waveComboBox.getItems().setAll(RaceDAO.getInstance().listWaves().sorted((Wave u1, Wave u2) -> u1.toString().compareTo(u2.toString())));
            //});
        });
        
        waveComboBox.setConverter(new WaveStringConverter());
        // DOES NOT WORK :-( 
        waveComboBox.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            System.out.println("PartController::waveComboBox(focusedListener) fired...");
            if (!newPropertyValue) {
                System.out.println("waveComboBox out focus");
            } else {
                System.out.println("waveComboBox in focus");
            }
        });
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
        
        
      
        bibTextField.focusedProperty().addListener((ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) -> {
            if (!newPropertyValue) {
                System.out.println("bibTextField out focus");
                if ( editedParticipant == null || !bibTextField.getText().equals(editedParticipant.getBib())) { 
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
            } else {
                //System.out.println("bibTextField in focus");
            }
        });
    }
    
    @FXML
    protected void addPerson(ActionEvent fxevent) {
        // Make sure they actually entered something first
        if (!firstNameField.getText().isEmpty() && !lastNameField.getText().isEmpty()) {
            Participant p = new Participant(firstNameField.getText(),
                lastNameField.getText()
            );
            
            p.setMiddleName(middleNameTextField.getText());
            p.setEmail(emailField.getText());
            p.setBib(bibTextField.getText());
            p.setAge(Integer.parseUnsignedInt(ageTextField.getText()));
            p.setSex(sexTextField.getText());
            p.setState(stateTextField.getText());
            p.setCity(cityTextField.getText());
            p.setWaves(waveComboBox.getCheckModel().getCheckedItems());
            
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
        long starttime = System.currentTimeMillis();
        
        participantDAO.removeParticipants(p);
        
        long endtime = System.currentTimeMillis();
        //System.out.println("Delete Time: " + (endtime-starttime));
    }
    
    public void editParticipant(Participant p) {
        
        editedParticipant=p;
        waveComboBox.getItems().setAll(RaceDAO.getInstance().listWaves());
        sexTextField.setText(p.getSex());
        ageTextField.setText(p.getAge().toString());
        bibTextField.setText(p.getBib());
        firstNameField.setText(p.getFirstName());
        middleNameTextField.setText(p.getMiddleName());
        lastNameField.setText(p.getLastName());
        emailField.setText(p.getEmail());   
        cityTextField.setText(p.getCity()); 
        stateTextField.setText(p.getState());
        
        // just in case something goes sideways
        if (p.getDNF() && p.getDQ()) p.setDNF(FALSE);
        
        dnfCheckBox.selectedProperty().setValue(p.getDNF());
        dqCheckBox.selectedProperty().setValue(p.getDQ());
        dnfTextField.setText(p.getDNFNote());
        dqTextField.setText(p.getDQNote());
        
        waveComboBox.getCheckModel().clearChecks();
        
        
        p.wavesProperty().stream().forEach(w -> {
            waveComboBox.getCheckModel().check(w);
            System.out.println("Checking " + w.getID() + " " + w.toString());
        });
        waveComboBox.getCheckModel().getCheckedItems().forEach(w -> {
            System.out.println("Checked " + w.getID() + " " + w.toString());
        });
        //waveComboBox.getCheckModel().check(null);
        
        
        // Make the update button visible and hide the add button
        formUpdateButton.setVisible(true);
        formUpdateButton.setManaged(true);
        formUpdateButton.setDefaultButton(true);
        formAddButton.setVisible(false);
        formAddButton.setManaged(false);
        bibTextField.requestFocus();
    }
    
    public void updateParticipant(ActionEvent fxevent){
        
        if (!firstNameField.getText().isEmpty() && !lastNameField.getText().isEmpty()) {
            
            // pull the list of checked waves
            
            editedParticipant.setFirstName(firstNameField.getText());
            editedParticipant.setMiddleName(middleNameTextField.getText());
            editedParticipant.setLastName(lastNameField.getText());
            editedParticipant.setEmail(emailField.getText());
            editedParticipant.setBib(bibTextField.getText());
            editedParticipant.setAge(Integer.parseUnsignedInt(ageTextField.getText()));
            editedParticipant.setSex(sexTextField.getText());
            editedParticipant.setCity(cityTextField.getText());
            editedParticipant.setState(stateTextField.getText());
            editedParticipant.setWaves(waveComboBox.getCheckModel().getCheckedItems());
            
            editedParticipant.setDNF(dnfCheckBox.selectedProperty().getValue());
            editedParticipant.setDQ(dqCheckBox.selectedProperty().getValue());
            editedParticipant.setDNFNote(dnfTextField.getText());
            editedParticipant.setDQNote(dqTextField.getText());
            
            // reset the fields
            resetForm();   
            
            // perform the actual update
            participantDAO.updateParticipant(editedParticipant);
            
            //participantsList.remove(editedParticipant);
            //participantsList.add(editedParticipant); 
            
            editedParticipant=null; 
            
        }
        bibTextField.requestFocus();
    }
    
    public void resetForm() {
        
        
        
        // reset the fields
        
        //waveComboBox.getItems().setAll(RaceDAO.getInstance().listWaves());
        waveComboBox.getItems().setAll(RaceDAO.getInstance().listWaves().sorted((Wave u1, Wave u2) -> u1.toString().compareTo(u2.toString())));
        waveComboBox.getCheckModel().clearChecks();


        sexTextField.setText("");
        ageTextField.setText("");
        bibTextField.setText("");
        firstNameField.setText("");
        middleNameTextField.setText("");
        lastNameField.setText("");
        emailField.setText("");  
        cityTextField.setText("");
        stateTextField.setText("");
        
        dqCheckBox.selectedProperty().setValue(FALSE);
        dnfCheckBox.selectedProperty().setValue(FALSE);
        dnfTextField.setText("");
        dqTextField.setText("");
        
        // set the Update buton to invisible
        formUpdateButton.setVisible(false);
        formUpdateButton.setManaged(false);
        formUpdateButton.setDefaultButton(false);
                
        // make the add button visible
        formAddButton.setVisible(true);
        formAddButton.setManaged(true);
        formAddButton.setDefaultButton(true);
        bibTextField.requestFocus();
    }
    public void resetForm(ActionEvent fxevent){
        resetForm();
    }
    
    public void importParticipants(ActionEvent fxevent) throws FlowException{
        // todo
        Stage importStage = new Stage();
                
        Flow flow  = new Flow(ImportWizardController.class);
        
        FlowHandler flowHandler = flow.createHandler();

        StackPane pane = flowHandler.start(new DefaultFlowContainer());
        importStage.setScene(new Scene(pane));
        importStage.initModality(Modality.APPLICATION_MODAL);
        importStage.show(); 
       // Open a dialog to get the file name
       // radio button for clearing the existing data or merging the data
        
       // look for headers
        
       // map the headers to fields
        
       // import the csv file by calling 
       //participantDAO.importFromCSV(file, property2columMap );
       
    }
    
    public void exportParticipants(ActionEvent fxevent){
        // todo
        // Open a dialog box to select the fields to export
        // Then the name of the file
        // finally do the dump using the H2 -> csv libraries. 
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
}