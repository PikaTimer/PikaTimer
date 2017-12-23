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
package com.pikatimer.util;

import java.util.function.UnaryOperator;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class Formatters {
    
    public static TextFormatter<Integer> integerFormatter() {
        return integerFormatter(false);
    }
    public static TextFormatter<Integer> integerFormatter(Boolean negOK) {
        
        UnaryOperator<TextFormatter.Change> filter = c -> {
            String newText = c.getControlNewText() ;
            if (newText.isEmpty()) { // always allow deleting all characters
                return c ;
            } else if (negOK && ! newText.matches("-?\\d*")) { 
                return null;
            } else if (! newText.matches("\\d+")) {// otherwise, must have all digits:
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
                    return 0 ;
                }
            }
        };
        
        return new TextFormatter<>(sc, 0, filter) ;
    }
    
}
