/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.pikatimer.participant;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 *
 * @author jcgarner
 */
public class FXMLParticipantController  {

    @FXML private TableView<Participant> tableView;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField filterField; 
    private ObservableList<Participant> participantsList;
    private ParticipantDAO participantDAO;
    
    
    @FXML
    protected void initialize() {
        // setup the ObservableList of people
        
        System.out.println("Initializing ParticipantController");
        System.out.println("Creating ParticipantDAO");
        participantDAO=new ParticipantDAO();
        participantsList=FXCollections.observableArrayList();
        System.out.println("Retrieving Participants");
        participantsList.addAll(participantDAO.listParticipants());
        System.out.println("Binding tableView to the participantsList");
        
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
                String lowerCaseFilter = newValue.toLowerCase();

                if (participant.getFirstName().toLowerCase().indexOf(lowerCaseFilter) != -1) {
                    return true; // Filter matches first name.
                } else if (participant.getLastName().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches last name.
                } else if ((participant.getFirstName() + " " + participant.getLastName()).toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches last name.
                } else if (participant.getEmail().toLowerCase().contains(lowerCaseFilter)) {
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
        
        //tableView.setItems(participantsList);
        System.out.println("Done Initializing ParticipantController");
    }
    
    @FXML
    protected void addPerson(ActionEvent fxevent) {
        Participant p = new Participant(firstNameField.getText(),
            lastNameField.getText(),
            emailField.getText()
        );
        participantsList.add(p);
        participantDAO.addParticipant(p);
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");   
    }
    
    
    
    
   
    
    public ObservableList<Participant> getParticipantList(){
        if(!participantsList.isEmpty())
            participantsList.clear();
        participantsList.addAll(participantDAO.listParticipants());
        return participantsList;
    }
    
    public void removeParticipant(Integer id)     {
        participantDAO.removeParticipant(id);
    }
    
    public void updateParticipant(Participant p){
        participantDAO.updateParticipant(p);
    }
    
    
}