/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.util;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jcgarner
 */
public enum Pace {
    MPM, // Minutes per Mile
    MPK, // Minutes per Kilometer
    MPH, // Miles Per Hour
    KPH, // Kilometers Per Hour
    MPS, // Meters per Second
    YPS, // Yards per Second
    FPS, // Feet per Second
    MP100M, // Minutes per 100M
    MP100Y; // Minutes per 100Y
    
    private static final Map<Pace, String> PACE_MAP = createMap();

    private static Map<Pace, String> createMap() {
        Map<Pace, String> result = new HashMap<>();
        result.put(MPM, "Minutes per Mile (MM:SS/mile)");
        result.put(MPK, "Minutes per Kilometer (MM:SS/Kilo");
        result.put(MPH, "Miles Per Hour (XX mph)");
        result.put(KPH, "Kilometers per Hour (XX kph)");
        result.put(MPS, "Meters per Second (XX m/s)");
        result.put(YPS, "Yards per Second (XX y/s)");
        result.put(FPS, "Feet per Second (XX f/s)");
        result.put(MP100M, "Minutes per 100 Meters (MM:SS/100m)");
        result.put(MP100Y, "Minutes per 100 Yards (MM:SS/100y)");
        return Collections.unmodifiableMap(result);
    }
    
//    private String unit;
    
//    private Unit(String s){
//        unit=s;
//    }
    
    @Override 
    public String toString(){
        return PACE_MAP.get(this);
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
    public static final String getPace(Float d, Unit u, Duration t, Pace p) {
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
        
        return dist.setScale(3, BigDecimal.ROUND_HALF_UP).toString();

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
    

