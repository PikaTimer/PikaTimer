/* 
 * Copyright (C) 2023 John Garner
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
package com.pikatimer.timing.reader;

import com.pikatimer.event.Event;
import com.pikatimer.timing.RawTimeData;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.DurationParser;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;



/**
 *
 * @author jcgarner
 */
public class PikaGenericChipTimeFileReader extends TailingReader{
    private SimpleIntegerProperty chipIndex;
    private SimpleIntegerProperty timeIndex;
    
    
    public PikaGenericChipTimeFileReader(){
        chipIndex = new SimpleIntegerProperty();
        timeIndex = new SimpleIntegerProperty();
        
    }
    
    @Override
    public void showControls(Pane p) {
        if (displayVBox == null){
            super.showControls(p);

            Spinner chipIndexSpinner = new Spinner(1, 9, 1);
            Label chipIndexLabel = new Label("Chip Field Index:");
            HBox chipIndexHBox = new HBox();
            chipIndexHBox.setSpacing(3);
            chipIndexHBox.getChildren().addAll(chipIndexLabel,chipIndexSpinner);


            HBox timeIndexHBox = new HBox();
            Label timeIndexLabel = new Label("Time Field Index:");
            Spinner timeIndexSpinner = new Spinner(1, 9, 2);
            timeIndexHBox.setSpacing(3);
            timeIndexHBox.getChildren().addAll(timeIndexLabel,timeIndexSpinner);

            displayVBox.getChildren().addAll(chipIndexHBox,timeIndexHBox);
        } else {
            super.showControls(p);
        }
    }
    
    @Override
    public void process(String s) {
        String port="";
        String chip="";
        String dateAndTime="";
        
        if (s.contains(",")) {
            String[] tokens = s.split(",", -1);
            // we only care about the following fields:
            // 0 -- The port (1->4)
            // 1 -- chip
            // 2 -- bib
            // 3 -- time (as a string)
            // 4 & 5 -- the Reader {1 or 2} and Port (again)

            // Step 1: Make sure we have a time in the 4th field
            // Find out if we have a date + time or just a time
            port = tokens[0];
            chip = tokens[1];
            //String bib = tokens[2]; // We don't care what the bib is
            dateAndTime = tokens[3].replaceAll("\"", "");
            
            if (port.equals("0") && ! chip.equals("0")) { // invalid combo
                System.out.println("Non Start time: " + s);
                return;
            } else if (!port.matches("[1234]") && !chip.equals("0")){
                System.out.println("Invalid Port: " + s);
                return;
            }
            
        } else if (s.contains("\t")){
            String[] tokens = s.split("\t", -1);
            // we only care about the following fields:
            
            // 0 -- chip
            // 1 -- time (as a string)
            // 2 -- the Reader {1 or 2} 
            // 3 -- The port (1->4)
            

            // Step 1: Make sure we have a time in the 4th field
            // Find out if we have a date + time or just a time
            chip = tokens[0];
            //String bib = tokens[2]; // We don't care what the bib is
            dateAndTime = tokens[1].replaceAll("\"", "");
        }
                
                
        String date = null;
        String time = null;
        String[] dateTime = dateAndTime.split(" ", 2);
        if (dateTime.length > 1) {
            date = dateTime[0];
            time = dateTime[1];
        } else time = dateTime[0];

        //System.out.println("Chip: " + chip);
        //System.out.println("dateTime: " + dateTime);
        

        
        
        Duration timestamp = Duration.ZERO;
        if (date != null) {
            // parse the date
            try { 
                LocalDate d = LocalDate.parse(date,DateTimeFormatter.ISO_LOCAL_DATE); 
                
                // set the timestamp to the duration between the event start
                // and this time
                timestamp = Duration.ofDays(Event.getInstance().getLocalEventDate().until(d, ChronoUnit.DAYS));
                // if it is before the event date, just return
                if (timestamp.isNegative()) {
                    String status = "Date of " + date + " is in the past, ignoring";
                    System.out.println(status);
                    Platform.runLater(() -> {
                        statusLabel.textProperty().setValue(status);
                    });
                    return;
                } 
            } catch (Exception e) {
                String status = "Unable to parse the date in \"" + date +"\" : " + e.getMessage();
                System.out.println(status);
                Platform.runLater(() -> {
                    statusLabel.textProperty().setValue(status);
                });
                return;
            }
        }

        // First look for timestams without a date attached to them
        if(time.matches("^\\d{1,2}:\\d{2}:\\d{2}\\.\\d{3}$")) {
            if(time.matches("^\\d{1}:\\d{2}:\\d{2}\\.\\d{3}$")) {
                //ISO_LOCAL_TIME wants a two digit hour....
                time = "0" + time;
            }
            if (DurationParser.parsable(time)){ 
                timestamp = timestamp.plus(DurationParser.parse(time));
                //LocalTime timestamp = LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME );
                RawTimeData rawTime = new RawTimeData();
                rawTime.setChip(chip);
                rawTime.setTimestampLong(timestamp.toNanos());
                String status = "Added raw time: " + chip + " at " + DurationFormatter.durationToString(timestamp, 3);
                Platform.runLater(() -> {
                    statusLabel.textProperty().setValue(status);
                });
                timingListener.processRead(rawTime); // process it
            } else {
                String status = "Unable to parse the time in " + time;
                System.out.println(status);
                Platform.runLater(() -> {
                    statusLabel.textProperty().setValue(status);
                });
            }
        } else {
            String status="Unable to parse the time: " + s;
            System.out.println(status);
            Platform.runLater(() -> {
                statusLabel.textProperty().setValue(status);
            });
            
        }

    }
    
   @Override
    public Boolean chipIsBib() {
        return Boolean.FALSE; 
    }
    
}
