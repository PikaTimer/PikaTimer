/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jcgarner
 */
public enum Unit {
    KILOMETERS,
    MILES,
    YARDS,
    METERS,
    FEET;
    
    private static final Map<Unit, String> DISTANCE_MAP = createMap();

    private static Map<Unit, String> createMap() {
        Map<Unit, String> result = new HashMap<>();
        result.put(KILOMETERS, "Kilometers");
        result.put(MILES, "Miles");
        result.put(YARDS, "Yards");
        result.put(METERS, "Meters");
        result.put(FEET, "Feet");
        return Collections.unmodifiableMap(result);
    }
    
//    private String unit;
    
//    private Unit(String s){
//        unit=s;
//    }
    
    @Override 
    public String toString(){
        return DISTANCE_MAP.get(this);
    }
//    
//    public String getValue() {
//        return unit;
//    }
//    
//    public void setValue(String u) {
//        unit = u;
//    
    
    // TODO: Put in converters so that we can take miles to km, etc
    
    // 1ft = 0.3048m
    public static final Float toMiles(Float d, Unit u) {
        BigDecimal dist = new BigDecimal(d);
        
        switch(u){
            case KILOMETERS:
                dist = dist.multiply(BigDecimal.valueOf(0.621371)); 
                break;
            case MILES:
                // do nothing
                break; 
            case YARDS:
                dist = dist.divide(new BigDecimal(1760));
                break;
            case METERS:
                dist = dist.multiply(BigDecimal.valueOf(0.000621371)); 
                break;
            case FEET:
                dist = dist.divide(new BigDecimal(5280));
                break;
        }
        
        return dist.setScale(3, BigDecimal.ROUND_HALF_UP).floatValue();

    }
    
    public Float toKilometers(Float d){
            
        return d;
    }
    
    public Float toMeters(Float d) {
        switch(this) {
            
        }
                
        return d;

    }
    
    public Float toYards(Float d){
        
        return d;
    }
}
    

