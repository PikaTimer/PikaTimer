/*
 * Copyright (C) 2024 John Garner <segfaultcoredump@gmail.com>
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
package com.pikatimer.util;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.UnaryOperator;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class TextFieldFormatters {
    private static final Logger logger = LoggerFactory.getLogger(TextFieldFormatters.class);
    
    
    public static TextFormatter<Integer> integerFormatter() {
        return integerFormatter(false);
    }
    public static TextFormatter<Integer> integerFormatter(Boolean negOK) {
        
        UnaryOperator<TextFormatter.Change> filter = c -> {
            String newText = c.getControlNewText() ;
            if (newText.isEmpty()) { // always allow deleting all characters
                return c ;
            } else if (negOK && ! newText.matches("-?([1-9]\\d*)*")) { 
                return null;
            } else if (! newText.matches("[1-9]\\d*")) {// otherwise, must have all digits:
                return null ;
            }
            return c;
        };
        
        // Custom string converter to deal with blanks and such
        StringConverter<Integer> sc = new StringConverter<Integer>() {

            @Override
            public String toString(Integer i) {
                if (i == null) return "";
                return i.toString() ;
            }

            @Override
            public Integer fromString(String s) {
                if (s != null && s.matches("-?\\d+")) {
                    return Integer.valueOf(s);
                } else {
                    // fall back to what the value used to be 
                    // (in case they blanked it out)
                    return null ;
                }
            }
        };
        
        return new TextFormatter<>(sc, 0, filter) ;
    }
    
    public static TextFormatter<BigDecimal> getBigDecimalFormatter(){
        UnaryOperator<Change> decimalFilter = change -> {
            // if the content does not change, just return
            if (!change.isContentChange()) return change;
            
            String newValue = change.getControlNewText();
            logger.trace("BigDecimalFormatter TextFormatter fired: Changed? {} Value: {}",change.isContentChange(),newValue);
            
            if (newValue.isEmpty() || newValue.matches("^-?([0-9]+)?(\\.[0-9]*)?$")) { 
                logger.trace("TextFieldFormatters::BigDecimal {} matches",newValue);
                return change;
            }
            logger.debug("TextFieldFormatters::BigDecimal {} does NOT match a decimal format",newValue);
            return null;
        };
        
        return new TextFormatter<>(decimalFilter);
    }
    
    public static TextFormatter<BigDecimal> getPositiveBigDecimalFormatter(){
        UnaryOperator<Change> decimalFilter = change -> {
            // if the content does not change, just return
            if (!change.isContentChange()) return change;
            
            String newValue = change.getControlNewText();
            logger.trace("PositiveBigDecimalFormatter TextFormatter fired: Changed? {} Value: {}",change.isContentChange(),newValue);
            
            if (newValue.isEmpty() || newValue.matches("^([0-9]+)?(\\.[0-9]*)?$")) { 
                logger.trace("TextFieldFormatters::BigDecimal pre-parser {} matches",newValue);
                return change;
            }
            logger.debug("TextFieldFormatters::PositiveBigDecimal {} does NOT match a decimal format",newValue);
            return null;
        };
        
        return new TextFormatter<>(decimalFilter);
    }
    
    
    public static TextFormatter<LocalTime> getLocalTimeFormatter(){
        UnaryOperator<Change> localTimeFilter = change -> {
            // if the content does not change, just return
            if (!change.isContentChange()) return change;
            
            String newValue = change.getControlNewText();
            logger.trace("LocalTimeFormatter TextFormatter fired: Changed? {} Value: {}",change.isContentChange(),newValue);
            
            if (newValue.isBlank()) return change;
            else if (newValue.matches("^([3-9])|([0-9]:)$")) {  // zero pad the hour              
                change.setText("0" + newValue);
                change.setRange(0,newValue.length()-1);
                change.setAnchor(change.getAnchor()+1);
                change.setCaretPosition(change.getCaretPosition()+1);
                return change;
            } else if ( newValue.matches("^([012]|[01][0-9]|2[0-3]):?$") || 
                        newValue.matches("^([01][0-9]|2[0-3]):[0-5]?$") || 
                        newValue.matches("^([01][0-9]|2[0-3]):[0-5][0-9]:[0-5]?$") ){
                logger.debug("Possiblely good LocalTime (newValue: " + newValue + ")");
                return change;
            } else if(newValue.matches("^([01][0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9](\\.[0-9]*)?)?$") ) { // Looks like a time, lets check
                logger.debug("Testing Race Start Time (newValue: " + newValue + ")");            
                try {
                    if (!newValue.isEmpty()) {
                        LocalTime.parse(newValue, DateTimeFormatter.ISO_LOCAL_TIME);
                        return change;
                    }
                } catch (Exception e) {
                    logger.debug("Exception Bad Race Start Time (newValue: " + newValue + ")");
                    return null;
                }
            } else {
                logger.debug("Bad Race Start Time (newValue: " + newValue + ")");
                return null;
            }
            
            return null;
        };
        
        
        return new TextFormatter<>(localTimeFilter);
    }
    
    public static TextFormatter<Duration> getDurationFormatter(){
        UnaryOperator<Change> durationFilter = change -> {
            // if the content does not change, just return
            if (!change.isContentChange()) return change;
            
            String newValue = change.getControlNewText();
            logger.trace("DurationFormatter TextFormatter fired: Changed? {} Value: {}",change.isContentChange(),newValue);

            if ( newValue.isEmpty() || newValue.matches("^-?[0-9]+(:(([0-5]([0-9](:([0-5]([0-9](\\.\\d*)?)?)?)?)?)?)?)?$") ){
                logger.debug("Possiblely good Time (newValue: " + newValue + ")");
                return change;
            }
            
            logger.debug("Invalid duration: {}",newValue);
            return null;
        };
        
        return new TextFormatter<>(durationFilter);
    }
    
    public static TextFormatter<Duration> getPositiveDurationFormatter(){
        UnaryOperator<Change> durationFilter = change -> {
            // if the content does not change, just return
            if (!change.isContentChange()) return change;
            
            String newValue = change.getControlNewText();
            
            logger.trace("PositiveDurationFormatter TextFormatter fired: Changed? {} Value: {}",change.isContentChange(),newValue);
            
            if ( newValue.isEmpty() || newValue.matches("^[0-9]+(:(([0-5]([0-9](:([0-5]([0-9](\\.\\d*)?)?)?)?)?)?)?)?$") ){
                logger.debug("getDurationFormatter()  Possiblely good duration: {}",newValue);
                return change;
            }
            
            logger.debug("Invalid duration: {}",newValue);
            return null;
        };
        
        return new TextFormatter<>(durationFilter);
    }
    
    
}
