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
package com.pikatimer;

import com.pikatimer.event.Event;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;


/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLpikaController   {
    
    private final Event event = Event.getInstance();
    @FXML private Label eventName;
    @FXML private Label eventDate;
    @FXML private TabPane mainTabPane; 
    /**
     * Initializes the controller class.
     */
    @FXML
    protected void initialize() {
        // TODO
        //System.out.println("FXMLpikaController initialized!");
        eventName.textProperty().bind(Bindings.concat("PikaTimer: ").concat(event.eventNameProperty()));
        eventDate.textProperty().bind(event.eventDateStringProperty());
        event.setMainTabPane(mainTabPane);
    }
    
    
       
}
