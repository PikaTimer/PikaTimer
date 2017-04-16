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
package com.pikatimer.timing.reader;

import com.pikatimer.timing.RawTimeData;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.DurationParser;
import java.time.Duration;
import javafx.application.Platform;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class PikaRaceTimerFileReader  extends NonTailingReader{
    @Override
    public void process(String s) {
        System.out.println("PikaRaceTimerFileReader::process: " + s);
        // the file is either pipe or comma delimited
        // and we don't care which one since the rest of it is just numbers
        // and colons 
        String[] tokens = s.split(",", -1); 
        // we only care about the following fields:
        // 0 -- The place
        // 1 -- bib
        // 2 -- time (as a string)
        // 3+ -- it should not be here
        if (tokens.length < 2 ) {
            System.out.println("  Unable to parse " + s);
            return;
        }

        // numbers only for the bib. The app sometimes makes the first entry 
        // "BIB####" if folks don't first hit backspace
        String bib = tokens[1].replaceAll("\\D+", ""); 
        
        // one way to get rid of the header line.... 
        if (bib == null || bib.isEmpty()) { // invalid bib
            System.out.println("  Empty bib: " + s);
            return;
        }
        if (bib.equals("0")) { // invalid bib
            System.out.println("  Zero bib: " + s);
            return;
        }
        
        // strip any extra quotes
        String time = tokens[2].replaceAll("\"", ""); 
        
        //System.out.println("Chip: " + chip);
        //System.out.println("dateTime: " + dateTime);
        
        
        
        
        Duration timestamp = offset; // We get this from the NonTailingReader class
        System.out.println("  Offset is: " + offset);


        // First look for timestams without a date attached to them
        if(time.matches("^\\d{1,2}:\\d{2}:\\d{2}(\\.\\d{3})?$")) {
            if (DurationParser.parsable(time)){ 
                timestamp = timestamp.plus(DurationParser.parse(time));
                //LocalTime timestamp = LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME );
                RawTimeData rawTime = new RawTimeData();
                rawTime.setChip(bib);
                
                rawTime.setTimestampLong(timestamp.toNanos());
                String status = "Added raw time: " + bib + " at " + DurationFormatter.durationToString(timestamp, 3);
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
            String status="Unable to parse the time: " + time;
            System.out.println(status);
            Platform.runLater(() -> {
                statusLabel.textProperty().setValue(status);
            });
            
        }

    }
    
   @Override
    public Boolean chipIsBib() {
        return Boolean.TRUE; 
    }
    
}