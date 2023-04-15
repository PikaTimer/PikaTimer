/*
 * Copyright (C) 2023 John Garner <segfaultcoredump@gmail.com>
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
package com.pikatimer.race;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public enum SexHandling {
    
    OFI("O/F Inc","Open/Female (Inclusive)"),
    OFE("O/F Ex","Open/Female (Exclusive)"),
    ALL("ALL","Awards for each value"),
    MF("M/F","M/F Only"); 
    

    private final String shortCode;
    private final String longCode;
    
    private static final Map<SexHandling, String> InputMap = createMap();
    
    SexHandling(String s, String l){
        shortCode=s;
        longCode=l;
    }

    private static Map<SexHandling, String> createMap() {
        Map<SexHandling, String> result = new HashMap<>();
        result.put(OFI, "Open/Female (Inclusive)");
        result.put(OFE, "Open/Female (Exclusive)");
        result.put(ALL, "Awards for each value");
        result.put(MF, "M/F Only");

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
