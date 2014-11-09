/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.event;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.SimpleStringProperty;


/**
 *
 * @author jcgarner
 */
public class Event {
    
    private final SimpleStringProperty eventName = new SimpleStringProperty("");
    private final SimpleStringProperty eventDateString = new SimpleStringProperty("");
    private  LocalDate eventDate = LocalDate.now(); 
    
    
 
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
        
        public void setEventName(String a) {
            eventName.set(a);
        } 
                               
        public SimpleStringProperty getEventName() {
            return eventName;
         }
        
        public void setEventDate(LocalDate d) {
            eventDate = d;
            eventDateString.set(eventDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
        }
        
        public SimpleStringProperty getEventDateString() {
            return eventDateString;
        }
}
