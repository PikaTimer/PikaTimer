/*
 * Copyright (C) 2017 John Garner <segfaultcoredump@gmail.com>
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

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;

/**
 * FXML Controller class
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class FXMLAboutController{

    @FXML Label pikaVersionLabel;
    
    /**
     * Initializes the controller class.
     */
    public void initialize() {
        // TODO
        pikaVersionLabel.setText("Version " + Pikatimer.VERSION);
    }    
    
    @FXML
    protected void openLink(ActionEvent fxevent) {
        Hyperlink hyperlink = (Hyperlink)fxevent.getSource();
        String link = hyperlink.getText();
        if (link.contains("(")) {
            link = link.replaceFirst("^.+\\(", "").replaceFirst("\\).*$", "");
        }
        System.out.println("Hyperlink pressed: " + link);
        Pikatimer.getInstance().getHostServices().showDocument(link);
    }
    
}
