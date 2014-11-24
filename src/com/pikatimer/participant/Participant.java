/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */

package com.pikatimer.participant;

import javafx.beans.property.SimpleStringProperty;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * @author jcgarner
 */

@Entity
@Table(name="PARTICIPANT")
public class Participant {
    
   @Id
   @GeneratedValue
   @Column(name="PARTICIPANT_ID")
   private Integer ID; 
   
   @Column(name="FIRST_NAME")
   private String firstName;
   
   @Column(name="LAST_NAME")
   private String lastName;
   
   @Column(name="EMAIL")
   private String email;


    public Participant() {
        this("", "", "");
    }
 
    public Participant(String firstName, String lastName, String email) {
        setFirstName(firstName);
        setLastName(lastName);
        setEmail(email);
    }

    public int getID() {
        return ID; 
    }
    public String getFirstName() {
        return firstName;
    }
 
    public void setFirstName(String fName) {
        firstName=fName;
    }
        
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String fName) {
        lastName=fName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String fName) {
        email=fName;
    }
}