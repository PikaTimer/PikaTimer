/* 
 * Copyright (C) 2017 John Garner
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private EventOptions eventOptions;
 
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
        
        public SimpleStringProperty eventNameProperty() {
            return eventName;
         }
        
        public void setEventDate(LocalDate d) {
            eventDate = d;
            eventDateString.set(eventDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
        }
        
        public void setEventDate(String d) {
            eventDate = LocalDate.parse(d,DateTimeFormatter.ISO_LOCAL_DATE);
            eventDateString.set(eventDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
        }
               
        public LocalDate getLocalEventDate() {
            return eventDate;
        }
        
        public SimpleStringProperty eventDateStringProperty() {
            return eventDateString;
        }
        
        public EventOptions eventOptions(){
            if (eventOptions == null) eventOptions=EventDAO.getInstance().getEventOptions();
            return eventOptions;
        }
}
