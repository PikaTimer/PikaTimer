/*
 *  Copyright 2014 John Garner. All rights reserved. 

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
    
    public static final String durationToString(Duration d, Integer p, Boolean Hours, RoundingMode rm) {
        //System.out.println("durationToString start with " + d.toNanos() + " and " + p.toString() + " or " + d.toString());
        //String result = d.toString(); 
        //result = d.toString().replace("PT", "").replace("H",":").replace("M",":"); 
        Long s = d.getSeconds(); 
        Long H = s/3600; 
        Long M = (s%3600)/60;
        s = s -(M*60 + H * 3600); 
        Integer t = d.getNano(); 
        if (d.toNanos() == 0) return ""; 
        BigDecimal S = new BigDecimal("0." + t.toString()).setScale(p, rm);
        //System.out.println("H:" + H.toString() + " M:" + M.toString() + " s:" + s.toString() + " S:" + S.toPlainString());

        String r = "";
        if (H > 0 ) r = H.toString() + ":";
        if (H == 0 && Hours) r = "00:";
        if (M > 9) r += M.toString();
        if (M < 10 && M > 0) r+= "0" + M.toString();
        if (H > 0 && M == 0) r+= "00";
        if (s > 9 ) r+= ":" + s.toString();
        if (s < 10 && s > 0) r+= ":0" + s.toString();
        if (p > 0 && s > 0) r += S.toPlainString().replace("0.", ".");
        if (p > 0 && s==0) r+=":00" + S.toPlainString().replace("0.", ".");
        return r;
       
    }
    
    public static final String durationToString(Duration d, Integer p, Boolean hours) {
        return durationToString(d,p,hours,RoundingMode.HALF_UP);
    }
    public static final String durationToString(Duration d, Integer p) {
        return durationToString(d,p,true,RoundingMode.HALF_UP);
    }
    
    public static final String durationToString(Duration d) {
        return durationToString(d,0,true,RoundingMode.HALF_UP);
    }
}
