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

import java.time.Duration;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class DurationParser {
    public static final Duration parse(String s, boolean hours) {
        if (s == null || s.isEmpty()) return Duration.ZERO;
        
        Boolean isNegative = false;
        Long h = 0L;
        Long m = 0L;
        Long sec = 0L;
        Long nano = 0L;
        if (s.startsWith("-")) {
            s= s.replaceFirst("-", "");
            isNegative = true;
        }
        
        String[] values = s.split(":");
        if (hours ) { // HH:MM[:SS.sss]
            
            h = Long.valueOf(values[0]);
            m = Long.valueOf(values[1]);
            if (values.length == 3 && values[2] != null) nano = (long)(Double.valueOf(values[2]) * 1000000000);
        } else { // [[HH:]MM]:SS[.sss] 
            
            if (values.length == 3 && values[2] != null) { // HH:MM:SS.sss
                h = Long.valueOf(values[0]);
                m = Long.valueOf(values[1]);
                if (values[2] != null) nano = (long)(Double.valueOf(values[2]) * 1000000000);
            } else if (values.length == 2 && values[1] != null) { // MM:SS.sss
                m = Long.valueOf(values[0]);
                if (values[1] != null) nano = (long)(Double.valueOf(values[1]) * 1000000000);
            } else { // SS.ssss
                if (values[0] != null) nano = (long)(Double.valueOf(values[0]) * 1000000000);
            }
        }
        
        if (isNegative) return Duration.ofHours(h).plusMinutes(m).plusNanos(nano).negated();
        else return Duration.ofHours(h).plusMinutes(m).plusNanos(nano);
    }
    
    public static final Duration parse(String s) {
        return parse(s,true);
    }
    public static final Boolean parsable(String s, boolean hours){
        if (s == null || s.isEmpty()) return Boolean.TRUE;
        else if (hours && s.matches("^-?\\d+:[0-5][0-9](:[0-5][0-9](\\.\\d*)?)?$")) return Boolean.TRUE;
        else if (s.matches("^-?((\\d+:)?[0-5]?[0-9]:)?[0-5][0-9](\\.\\d*)?$")) return Boolean.TRUE;
        else return Boolean.FALSE;
    }
    
    public static final Boolean parsable(String s) {
        return parsable(s,true);
    }
}
