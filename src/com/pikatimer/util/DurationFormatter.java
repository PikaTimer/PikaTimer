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
import java.time.Duration;

/**
 *
 * @author jcgarner
 */
public class DurationFormatter {
    
    public static final String durationToString(Duration d, Integer p, Boolean printHours, RoundingMode rm) {
        // d is the duration to print
        // p is the precision. 0 for no sub-seconds, 
        // hours for padding the time with a 00: if it is under 1 hour
        // rm is the rounding mode. see java.math.RoundingMode for options
        Boolean isNegative = false;
        
        //System.out.println("durationToString start with " + d.toNanos() + " and " + p.toString() + " or " + d.toString());
        //String result = d.toString(); 
        //result = d.toString().replace("PT", "").replace("H",":").replace("M",":"); 
        if (d == null || d.isZero() || d.equals(Duration.ofNanos(Long.MAX_VALUE))) return "";
        
        if (d.isNegative()) {
            isNegative = true;
            d=d.negated();
        }
        
        
        Long s = d.getSeconds(); 
        Long H = s/3600; 
        Long M = (s%3600)/60;
        s = s -(M*60 + H * 3600); 
        Integer t = d.getNano(); 
        //if (d.toNanos() == 0) return ""; 
        BigDecimal S = new BigDecimal(d.getNano()).divide(new BigDecimal(1000000000)).setScale(p, rm);
        //System.out.println("DurationFormatter::durationToString: H:" + H.toString() + " M:" + M.toString() + " s:" + s.toString() + " S:" + S.toPlainString());
        
        if (p == 0 && S.compareTo(new BigDecimal(1)) == 0) {
            s+= 1;
            if (s==60){
                s=0L;
                M++;
                if (M==60) {
                    M = 0L; 
                    H++ ; 
                }
            }
            
        } 
        
        String r = "";
        if (isNegative) r = "-"; 
        if (H > 0 ) r += H.toString() + ":";
        if (H == 0 && printHours) r += "0:";
        
        if (M > 9) r += M.toString() +":";
        if (M < 10 && M > 0) {
            if (H > 0 || printHours ) r+= "0" + M.toString()+":";
            else r+= M.toString()+":";
        }
        if ((H > 0 || printHours ) && M == 0) r+= "00:";
        
        if (s > 9 ) r+= s.toString();
        if (s < 10 && s > 0) r+= "0" + s.toString();
        if (p > 0 && s > 0) r += S.toPlainString().replace("0.", ".");
        if (p > 0 && s==0) r+="00" + S.toPlainString().replace("0.", ".");
        if (p == 0 && s == 0) r+="00";
        return r;
       
    }
    
    public static final String durationToString(Duration d, Integer p, Boolean printHours) {
        return durationToString(d,p,printHours,RoundingMode.HALF_EVEN);
    }
    public static final String durationToString(Duration d, Integer p) {
        return durationToString(d,p,true,RoundingMode.HALF_EVEN);
    }
    
    public static final String durationToString(Duration d) {
        return durationToString(d,0,true,RoundingMode.HALF_EVEN);
    }
    
    public static final String durationToString(Duration d, String format){
        return durationToString(d,format,"Down");
    }
    public static final String durationToString(Duration d, String format, String roundingMode){
        Integer precision = 0;
        String[] tokens = format.split("\\.", -1);
        if (tokens.length > 1) precision = tokens[1].length();
        
        Boolean hours = true;
        if (format.contains("[HH:]")) hours = false;
        
        RoundingMode rm = RoundingMode.HALF_EVEN;;
        if (roundingMode.equals("Down")) rm = RoundingMode.DOWN;
        if (roundingMode.equals("Half")) rm = RoundingMode.HALF_EVEN;
        if (roundingMode.equals("Up")) rm = RoundingMode.UP;
        
        return durationToString(d,precision,hours,rm);
    }
}
