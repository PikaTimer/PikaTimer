/*
 *  Copyright 2014 John Garner. All rights reserved. 

 */
package com.pikatimer.race;

import com.pikatimer.participant.Participant;

/**
 *
 * @author jcgarner
 */
public enum WaveAssignment {
    BIB("By Bib", "Bib"),
    AG("By Age Group", "AG");
    
    private final  String longDescription;
    private final  String shortDescription; 
    
    private WaveAssignment(String l, String s){
        longDescription = l;
        shortDescription = s;
    }
    
    @Override 
    public String toString(){
        return this.shortDescription;
    }
    
    public String toLongString(){
        return this.longDescription; 
    }
    
    public Boolean isElligable(Participant p, String criteria){
        return true; 
    }
}
