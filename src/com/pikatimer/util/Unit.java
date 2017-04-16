/* 
 * Copyright (C) 2017 John Garner
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
import java.math.RoundingMode;

/**
 *
 * @author jcgarner
 */
public enum Unit {
    KILOMETERS("Kilometers", "km"),
    MILES("Miles", "mi"),
    YARDS("Yards", "yd"),
    METERS("Meters", "M"),
    FEET("Feet", "ft");
    
    private final  String longUnit;
    private final  String shortUnit; 
    
    
//    private static final Map<Unit, String> DISTANCE_MAP = createMap();
//
//    private static Map<Unit, String> createMap() {
//        Map<Unit, String> result = new HashMap<>();
//        result.put(KILOMETERS, "Kilometers");
//        result.put(MILES, "Miles");
//        result.put(YARDS, "Yards");
//        result.put(METERS, "Meters");
//        result.put(FEET, "Feet");
//        return Collections.unmodifiableMap(result);
//    }
    
//    private String unit;
    
    private Unit(String l, String s){
        longUnit = l;
        shortUnit = s;
    }
    
    @Override 
    public String toString(){
        return this.longUnit;
    }
    
    public String toShortString(){
        return this.shortUnit; 
    }

    
    // convert this to Unit u
    public Float convertTo(Float d, Unit u){
        // 1ft = 0.3048m
        // 1m = 3.28084ft
        BigDecimal dist = new BigDecimal(d);
        
        // Step 1, convert to ft
        switch(this){
            case KILOMETERS:
                dist = dist.multiply(BigDecimal.valueOf(3280.84)); 
                break;
            case MILES:
                dist = dist.multiply(new BigDecimal(5280));
                break; 
            case YARDS:
                dist = dist.multiply(new BigDecimal(3));
                break;
            case METERS:
                dist = dist.multiply(BigDecimal.valueOf(3.28084)); 
                break;
            case FEET:
                break;
        }
        
        // step 2, convert to the target unit;
        switch(u){
            case KILOMETERS:
                dist = dist.divide(BigDecimal.valueOf(3280.84), 8, RoundingMode.HALF_UP); 
                break;
            case MILES:
                dist = dist.divide(new BigDecimal(5280), 8, RoundingMode.HALF_UP);
                break; 
            case YARDS:
                dist = dist.divide(new BigDecimal(3), 8, RoundingMode.HALF_UP);
                break;
            case METERS:
                dist = dist.divide(BigDecimal.valueOf(3.28084), 8, RoundingMode.HALF_UP); 
                break;
            case FEET:
                break;
        }
        
        // step 3, return to sender
        return dist.floatValue();
    }
}
    

