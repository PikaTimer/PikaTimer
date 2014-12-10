/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.pikatimer.participant;
import com.pikatimer.util.AlphanumericComparator;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.container.DefaultFlowContainer;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
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

/**
 *
 * @author jcgarner
 */
public class FXMLParticipantController  {

    @FXML private TableView<Participant> tableView;
    @FXML private TableColumn bibNumberColumn;
    @FXML private VBox formVBox; 
    @FXML private TextField bibTextField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField ageTextField;
    @FXML private TextField sexTextField;
    @FXML private ComboBox cityComboBox; 
    @FXML private ComboBox stateComboBox;
    @FXML private TextField emailField;
    @FXML private TextField filterField; 
    @FXML private Button formAddButton; 
    @FXML private Button formUpdateButton;
    @FXML private Button formResetButton;
    @FXML private Label filteredSizeLabel;
    @FXML private Label listSizeLabel; 
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
                }
		
            });
            rowMenu.getItems().addAll(editItem, removeItem,swapBibs);
            
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
        SortedList<Participant> sortedParticipantsList = new SortedList<>(filteredParticipantsList);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedParticipantsList.comparatorProperty().bind(tableView.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.
        tableView.setItems(sortedParticipantsList);
        
        // Set the bib number to be an alphanumeric sort
        bibNumberColumn.setComparator(new AlphanumericComparator());
        
        
        listSizeLabel.textProperty().bind(Bindings.size(participantsList).asString());
        filteredSizeLabel.textProperty().bind(Bindings.size(sortedParticipantsList).asString());
        
        System.out.println("Done Initializing ParticipantController");
    }
    
    @FXML
    protected void addPerson(ActionEvent fxevent) {
        // Make sure they actually entered something first
        if (!firstNameField.getText().isEmpty() && !lastNameField.getText().isEmpty()) {
            Participant p = new Participant(firstNameField.getText(),
                lastNameField.getText()
            );
            
            p.setEmail(emailField.getText());
            p.setBib(bibTextField.getText());
            p.setAge(Integer.parseUnsignedInt(ageTextField.getText()));
            p.setSex(sexTextField.getText());
            p.setCity(cityComboBox.getValue().toString());
            p.setState(stateComboBox.getValue().toString());
            
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
        System.out.println("Delete Time: " + (endtime-starttime));
    }
    
    public void editParticipant(Participant p) {
        
        editedParticipant=p;

        sexTextField.setText(p.getSex());
        ageTextField.setText(p.getAge().toString());
        bibTextField.setText(p.getBib());
        firstNameField.setText(p.getFirstName());
        lastNameField.setText(p.getLastName());
        emailField.setText(p.getEmail());   
        cityComboBox.setValue(p.getCity()); 
        stateComboBox.setValue(p.getState());
        
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
            editedParticipant.setFirstName(firstNameField.getText());
            editedParticipant.setLastName(lastNameField.getText());
            editedParticipant.setEmail(emailField.getText());
            editedParticipant.setBib(bibTextField.getText());
            editedParticipant.setAge(Integer.parseUnsignedInt(ageTextField.getText()));
            editedParticipant.setSex(sexTextField.getText());
            editedParticipant.setCity(cityComboBox.getValue().toString());
            editedParticipant.setState(stateComboBox.getValue().toString());
            
            
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
        cityComboBox.setValue(null); 
        stateComboBox.setValue(null);

        sexTextField.setText("");
        ageTextField.setText("");
        bibTextField.setText("");
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");  
        
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
//        long starttime = System.currentTimeMillis();
//        
//        for ( int i=1; i<=10000; i++ ) {
//            Participant p = new Participant("Test" + Integer.toString(i), "Last");
//            p.setBib(Integer.toString(i));
//            participantDAO.addParticipant(p);
//        }
//        long endtime = System.currentTimeMillis();
//        System.out.println("Import Time: " + (endtime-starttime));
      
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