/*
 * Copyright (C) 2017 John Garner <segfaultcoredump@gmail.com>
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
import com.pikatimer.results.ProcessedResult;
import com.pikatimer.util.AlphanumericComparator;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class AwardFilter {
    protected String attribute;
    protected String comparisonType;
    protected String value;
    static private AlphanumericComparator ac = new AlphanumericComparator();
    
    public AwardFilter(){
        
    }
    public AwardFilter(String a, String c, String v){
        attribute = a;
        comparisonType = c;
        value = v;
    }
    public Boolean filter(ProcessedResult pr, Race race){
        return filter(pr.getParticipant(), race);
    }
    public Boolean filter (Participant p, Race race) {
        String pvalue = "";
        if (attribute.equals("AG")) {
            pvalue = race.getAgeGroups().ageToAGString(p.getAge());
        }
        else if (attribute.matches("^\\d+$")) value = p.getCustomAttribute(Integer.parseInt(attribute)).getValueSafe();
        else  pvalue = p.getNamedAttribute(attribute);
        
        //System.out.println("filter() " + attribute + " " + comparisonType + " " + value + " " + pvalue);

        // We are going to abuse the heck out of the Alphanumeic Cpmarator for now.
        // This should work for numbers, letters, and most other things.
        // It could produce odd results on Dates, Times, boolean,
        // or otther arbitrary string based
        // attributes (like Sex, Age Group, or City). 
        if (comparisonType.equalsIgnoreCase("=") ) {
            return ac.compare(pvalue, value) == 0;
        } else if (comparisonType.equalsIgnoreCase(">") ) {
            return ac.compare(pvalue, value) > 0;
        } else if (comparisonType.equalsIgnoreCase("<") ) {
            return ac.compare(pvalue, value) < 0;
        } else if (comparisonType.equalsIgnoreCase("<=") ) {
            return ac.compare(pvalue, value) <= 0;
        } else if (comparisonType.equalsIgnoreCase(">=") ) {
            return ac.compare(pvalue, value) >= 0;
        } else if (comparisonType.equalsIgnoreCase("!=") ) {
            return ac.compare(pvalue, value) != 0;
        } 
        
        return true;
    }
    
}
