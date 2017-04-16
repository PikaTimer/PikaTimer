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
package com.pikatimer.race;

import com.pikatimer.participant.Participant;

/**
 *
 * @author jcgarner
 */
public enum WaveAssignment {
    BIB("By Bib", "Bib");
    //AG("By Age Group", "AG");
    
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
