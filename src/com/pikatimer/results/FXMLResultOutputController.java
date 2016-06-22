/*
 * Copyright (C) 2016 jcgarner
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
package com.pikatimer.results;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/**
 * FXML Controller class
 *
 * @author jcgarner
 */
public class FXMLResultOutputController {

    @FXML ChoiceBox outputTypeChoiceBox;
            
    @FXML CheckBox inProgressCheckBox;
    @FXML CheckBox showDQCheckBox;
    @FXML CheckBox showDNFCheckBox;
    @FXML CheckBox showSplitsCheckBox;
    
    @FXML VBox outputTargetsVBox;
    @FXML FlowPane outputOptionsFlowPane;
            
    @FXML Button outputAddButton;
    
    /**
     * Initializes the controller class.
     */
    public void initialize() {
        // TODO
        
       
    }    
    
    public void setRaceReport(RaceReport r){
        
    }
    
}
