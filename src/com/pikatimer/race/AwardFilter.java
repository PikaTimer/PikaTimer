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
import java.util.Objects;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
@Embeddable
public class AwardFilter {
    private final StringProperty attributeProperty = new SimpleStringProperty(); 
    private final StringProperty comparisonTypeProperty = new SimpleStringProperty(); 
    private final StringProperty referenceValueProperty = new SimpleStringProperty(); 
    
    private Boolean sexGroupFilter = false;


    static private AlphanumericComparator ac = new AlphanumericComparator();
    
    
    public AwardFilter(){
    }
    
    public AwardFilter(String a, String c, String v){
        attributeProperty.setValue(a);
        comparisonTypeProperty.setValue(c);
        referenceValueProperty.setValue(v);
    }
    
    @Column(name="attribute")
    public String getAttribute() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return attributeProperty.getValue(); 
    }
    public void setAttribute(String i) {
        attributeProperty.set(i);
    }
    public StringProperty attributeProperty() {
        return attributeProperty;
    }

    @Column(name="comparison_type")
    public String getComparisonType() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return comparisonTypeProperty.getValue(); 
    }
    public void setComparisonType(String i) {
        comparisonTypeProperty.set(i);
    }
    public StringProperty comparisonTypeProperty() {
        return comparisonTypeProperty;
    }
    
    @Column(name="reference_value")
    public String getReferenceValue() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return referenceValueProperty.getValue(); 
    }
    public void setReferenceValue(String i) {
        referenceValueProperty.set(i);
    }
    public StringProperty referenceValueProperty() {
        return referenceValueProperty;
    }
    
    public static Callback<AwardFilter, Observable[]> extractor() {
        return (AwardFilter i) -> new Observable[]{i.attributeProperty,i.comparisonTypeProperty,i.referenceValueProperty};
    }
    
    public Boolean filter(ProcessedResult pr, Race race){
        return filter(pr.getParticipant(), race);
    }
    public Boolean filter (Participant p, Race race) {
        
        if (sexGroupFilter) {
            return race.getSexGroups().eligibilityFilter(p);
        }
        
        String attribute = attributeProperty.getValue();
        String value = referenceValueProperty.getValue();
        String comparisonType = comparisonTypeProperty.getValue();
                
        
        // if we are missing data, just return true.
        if (attribute.isEmpty() || value.isEmpty()) return true; 
        
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
        } else if (comparisonType.equalsIgnoreCase("=~") ) {
            try {
                return pvalue.matches(value);
            } catch (Exception e) {
                // invalid regex, just drop through
            }
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

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.attributeProperty.getValue());
        hash = 97 * hash + Objects.hashCode(this.comparisonTypeProperty.getValue());
        hash = 97 * hash + Objects.hashCode(this.referenceValueProperty.getValue());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AwardFilter other = (AwardFilter) obj;
        if (!Objects.equals(this.attributeProperty.getValue(), other.attributeProperty.getValue())) {
            return false;
        }
        if (!Objects.equals(this.comparisonTypeProperty.getValue(), other.comparisonTypeProperty.getValue())) {
            return false;
        }
        if (!Objects.equals(this.referenceValueProperty.getValue(), other.referenceValueProperty.getValue())) {
            return false;
        }
        return true;
    }

    // Special instance for filtering based on the race sexGroup setting
    public static AwardFilter sexGroup() {
        AwardFilter af = new AwardFilter();
        af.sexGroupFilter = true;
        return af;
    }
    
}
