/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.event;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TabPane;



/**
 * 
 * @author jcgarner
 */
public class Event {
    
    private final SimpleStringProperty eventName = new SimpleStringProperty("");
    private final SimpleStringProperty eventDateString = new SimpleStringProperty("");
    private LocalDate eventDate = LocalDate.now(); 
    private final long eventID = 1;
    private  TabPane mainTabPane;  
 
	/**
	* SingletonHolder is loaded on the first execution of Singleton.getInstance() 
	* or the first access to SingletonHolder.INSTANCE, not before.
	*/
	private static class SingletonHolder { 
		private static final Event INSTANCE = new Event();
	}
 
	public static Event getInstance() {
		return SingletonHolder.INSTANCE;
	}
        
        
        public Long getEventID(){
            return eventID;
        }
        
        public void setMainTabPane(TabPane m) {
            mainTabPane=m; 
        }
        public TabPane getMainTabPane() {
            return mainTabPane; 
        }
        
        public void setEventName(String a) {
            eventName.set(a);
        } 
        
        
        public String getEventName() {
            return eventName.getValue();
        }
        
        public SimpleStringProperty getObservableEventName() {
            return eventName;
         }
        
        public void setEventDate(LocalDate d) {
            eventDate = d;
            eventDateString.set(eventDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
        }
        
        
        public Date getEventDate() {
            return Date.from(eventDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        }
        
        public void setEventDate(Date d) {
            Instant instant = Instant.ofEpochMilli(d.getTime());
            setEventDate(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate());

        }
        
        public LocalDate getLocalEventDate() {
            return eventDate;
        }
        
        public SimpleStringProperty getObservableEventDateString() {
            return eventDateString;
        }
}
