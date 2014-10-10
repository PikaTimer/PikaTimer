/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.pikatimer.participant;


import com.pikatimer.participant.Participant;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.collections.ObservableList;
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
    
    @FXML
    protected void addPerson(ActionEvent event) {
        ObservableList<Participant> data = tableView.getItems();
        data.add(new Participant(firstNameField.getText(),
            lastNameField.getText(),
            emailField.getText()
        ));
        
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");   
    }
    
}