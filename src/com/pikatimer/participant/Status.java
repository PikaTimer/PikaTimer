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

    GOOD("","Running"),
    DNS("DNS","Did Not Start"),
    DNF("DNF","Did Not Finish"), 
    DQ("DQ","Disqualified"); 

    private final String shortCode;
    private final String longCode;
    
    private static final Map<Status, String> InputMap = createMap();
    
    Status(String s, String l){
        shortCode=s;
        longCode=l;
    }

    private static Map<Status, String> createMap() {
        Map<Status, String> result = new HashMap<>();
        result.put(GOOD, "Running");
        result.put(DNS, "DNS: Did Not Start");
        result.put(DNF, "DNF: Did Not Finish");
        result.put(DQ, "DQ: Disqualified");

        return Collections.unmodifiableMap(result);
    }
    
    public String getLongCode(){ 
        return longCode;
    }
    public String getShortCode(){
        return shortCode;
    }

    
    @Override 
    public String toString(){
        return InputMap.get(this);
    }

    
    

}