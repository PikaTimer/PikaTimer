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
package com.pikatimer.timing;

import com.pikatimer.timing.reader.PikaPCTimerFileReader;
import com.pikatimer.timing.reader.PikaRFIDDirectReader;
import com.pikatimer.timing.reader.PikaRaceTimerFileReader;
import com.pikatimer.timing.reader.PikaRFIDFileReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jcgarner
 */
public enum TimingInputTypes {


    RFIDFile,
    RFIDDirect,
    PCTimer, 
    RaceTimer; 

    
    private static final Map<TimingInputTypes, String> InputMap = createMap();

    private static Map<TimingInputTypes, String> createMap() {
        Map<TimingInputTypes, String> result = new HashMap<>();

        result.put(RFIDFile, "RFIDServer / Outreach File");
        result.put(RFIDDirect, "RFID Ultra (TCP)");
        result.put(PCTimer, "PC Timer (Race Director)");
        result.put(RaceTimer, "Race Timer");

        return Collections.unmodifiableMap(result);
    }

    
    @Override 
    public String toString(){
        return InputMap.get(this);
    }

    public final  TimingReader getNewReader() {
                
        switch(this){
            case PCTimer:
                return new PikaPCTimerFileReader();
            case RaceTimer:
                return new PikaRaceTimerFileReader();
            case RFIDFile:
                return new PikaRFIDFileReader();
            case RFIDDirect:
                return new PikaRFIDDirectReader();
        }
        
        return null;

    }
    

}
    



