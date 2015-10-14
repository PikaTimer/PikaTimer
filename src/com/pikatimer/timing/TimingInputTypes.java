/*
 *  Copyright 2015 John Garner. All rights reserved. 

 */
package com.pikatimer.timing;

import com.pikatimer.timing.reader.PikaOutreachDirectReader;
import com.pikatimer.timing.reader.PikaOutreachFileReader;
import com.pikatimer.timing.reader.PikaPCTimerFileReader;
import com.pikatimer.timing.reader.PikaRFIDDirectReader;
import com.pikatimer.timing.reader.PikaRFIDFileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jcgarner
 */
public enum TimingInputTypes {
    /*
 *  Copyright 2014 John Garner. All rights reserved. 

 */

    OutreachDirect, // Minutes per Mile
    OutreachFile, // Minutes per Kilometer
    PCTimer, // Miles Per Hour
    RFIDDirect, // Kilometers Per Hour
    RFIDFile; // Meters per Second

    
    private static final Map<TimingInputTypes, String> InputMap = createMap();

    private static Map<TimingInputTypes, String> createMap() {
        Map<TimingInputTypes, String> result = new HashMap<>();
        result.put(OutreachDirect, "Outreach (Direct)");
        result.put(OutreachFile, "Outreach (File)");
        result.put(PCTimer, "PC Timer");
        result.put(RFIDDirect, "RFID (Direct)");
        result.put(RFIDFile, "RFID (File)");

        return Collections.unmodifiableMap(result);
    }
    
//    private String unit;
    
//    private Unit(String s){
//        unit=s;
//    }
    
    @Override 
    public String toString(){
        return InputMap.get(this);
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
    public final  TimingReader getNewReader() {
                
        switch(this){
            case OutreachDirect:
                return new PikaOutreachDirectReader();
            case OutreachFile:
                return new PikaOutreachFileReader();
            case PCTimer:
                return new PikaPCTimerFileReader();
            case RFIDDirect:
                return new PikaRFIDDirectReader();
            case RFIDFile:
                return new PikaRFIDFileReader();
        }
        
        return null;

    }
    

}
    



