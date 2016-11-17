/*
 * Copyright (C) 2016 John Garner <segfaultcoredump@gmail.com>
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
package com.pikatimer.participant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
    
public enum Status {

    GOOD,
    DNS,
    DNF, 
    DQ; 

    
    private static final Map<Status, String> InputMap = createMap();

    private static Map<Status, String> createMap() {
        Map<Status, String> result = new HashMap<>();
        result.put(GOOD, "");
        result.put(DNS, "Did Not Start");
        result.put(DNF, "Did Not Finish");
        result.put(DQ, "Disqualified");

        return Collections.unmodifiableMap(result);
    }
    

    
    @Override 
    public String toString(){
        return InputMap.get(this);
    }

    
    

}
