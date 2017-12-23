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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
    MP100Y, // Minutes per 100Y
    NONE;  // Blank
    
    private static final Map<Pace, String> PACE_MAP = createMap();

    private static Map<Pace, String> createMap() {
        Map<Pace, String> result = new HashMap<>();
        result.put(MPM, "MM:SS/mi (Minutes per Mile)");
        result.put(MPK, "MM:SS/km (Minutes per Kilometer)");
        result.put(MPH, "XX.X mph (Miles Per Hour)");
        result.put(KPH, "XX.X kph (Kilometers per Hour)");
        result.put(MPS, "XX m/s (Meters per Second)");
        result.put(YPS, "XX y/s (Yards per Second)");
        result.put(FPS, "XX f/s (Feet per Second)");
        result.put(MP100M, "MM:SS/100m (Minutes per 100 Meters)");
        result.put(MP100Y, "MM:SS/100y (Minutes per 100 Yards)");
        result.put(NONE, "None / Blank");
        return Collections.unmodifiableMap(result);
    }

    
    @Override 
    public String toString(){
        return PACE_MAP.get(this);
    }

    public String getPace(Float d, Unit u, Duration t){
        return getPace(d,u,t,this);
    }
    
    public static final String getPace(Float d, Unit u, Duration t, Pace p) {
        BigDecimal dist;
        
        switch(p){
            case MPM: // Minutes per Mile
                dist = new BigDecimal(u.convertTo(d, Unit.FEET));
                return DurationFormatter.durationToString(t.dividedBy(dist.longValue()).multipliedBy(5280L),0, FALSE, RoundingMode.HALF_UP).replaceFirst("^0", "") + "/mi";
            case MPK: // Minutes per Kilometer
                dist = new BigDecimal(u.convertTo(d, Unit.METERS));
                return DurationFormatter.durationToString(t.dividedBy(dist.longValue()).multipliedBy(1000L),0, FALSE, RoundingMode.HALF_UP).replaceFirst("^0", "") + "/km";
            case MPH: // Miles Per Hour
                dist = new BigDecimal(u.convertTo(d, Unit.MILES));
                return dist.divide(BigDecimal.valueOf(t.toMillis()), 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(3600000L)).setScale(1, RoundingMode.HALF_UP).toPlainString() + " mph";
            case KPH: // Kilometers Per Hour
                dist = new BigDecimal(u.convertTo(d, Unit.KILOMETERS));
                return dist.divide(BigDecimal.valueOf(t.toMillis()), 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(3600000L)).setScale(1, RoundingMode.HALF_UP).toPlainString() + " kph";
            case MPS: // Meters per Second
                dist = new BigDecimal(u.convertTo(d, Unit.METERS));
                return dist.divide(BigDecimal.valueOf(t.toMillis()), 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(1000L)).setScale(1, RoundingMode.HALF_UP).toPlainString() + " m/s";
            case YPS: // Yards per Second
                dist = new BigDecimal(u.convertTo(d, Unit.YARDS));
                return dist.divide(BigDecimal.valueOf(t.toMillis()), 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(1000L)).setScale(1, RoundingMode.HALF_UP).toPlainString() + " y/s";
            case FPS: // Feet per Second
                dist = new BigDecimal(u.convertTo(d, Unit.FEET));
                return dist.divide(BigDecimal.valueOf(t.toMillis()), 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(1000L)).setScale(1, RoundingMode.HALF_UP).toPlainString() + " f/s";
            case MP100M: // Minutes per 100M
                dist = new BigDecimal(u.convertTo(d, Unit.METERS));
                return DurationFormatter.durationToString(t.dividedBy(dist.longValue()).multipliedBy(100L),0, TRUE, RoundingMode.HALF_UP).replaceFirst("^0:", "") + "/100M";
            case MP100Y: // Minutes per 100Y
                dist = new BigDecimal(u.convertTo(d, Unit.FEET));
                return DurationFormatter.durationToString(t.dividedBy(dist.longValue()).multipliedBy(300L),0, TRUE, RoundingMode.HALF_UP).replaceFirst("^0:", "") + "/100Y";
            case NONE:
                return "";
        }
       return "???";
    }
    
    public Integer getFieldWidth(){
        switch(this){
            case MPM: // Minutes per Mile
            case MPK: // Minutes per Kilometer
                // XX:XX/yy
                return 8;
            case MPH: // Miles Per Hour
            case KPH: // Kilometers Per Hour
            case MPS: // Meters per Second
            case YPS: // Yards per Second
            case FPS: // Feet per Second
                // XX.X yyy
                return 8;     
            case MP100M: 
            case MP100Y:
                // XX:XX/100y
                return 10; 
            case NONE:
                return 0;
        }
        return 10;
    }

}
    

