/*
 * Copyright (C) 2017 John
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

import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Callback;
import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 *
 * @author John
 */    
@Embeddable 
public class AgeGroupIncrement {

    private final IntegerProperty startAgeProperty = new SimpleIntegerProperty();
    private final StringProperty endAgeProperty = new SimpleStringProperty(); 
    private final StringProperty nameProperty = new SimpleStringProperty();

    
    
    
    public AgeGroupIncrement() {
        
    } 
    
    @Column(name="increment_start")
    public Integer getStartAge() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return startAgeProperty.getValue(); 
    }
    public void setStartAge(Integer i) {
        startAgeProperty.set(i);
    }
    public IntegerProperty startAgeProperty() {
        return startAgeProperty;
    }
    
    public StringProperty endAgeProperty() {
        return endAgeProperty;
    }
    
    @Column(name="increment_name")
    public String getName() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return nameProperty.getValueSafe(); 
    }
    public void setName(String i) {
        System.out.println("AgeGroups.setAGIncrement() with " + i);
        nameProperty.setValue(i);
    }
    public StringProperty nameProperty() {
        return nameProperty;
    }
    
    public static Callback<AgeGroupIncrement, Observable[]> extractor() {
        return (AgeGroupIncrement i) -> new Observable[]{i.startAgeProperty,i.endAgeProperty};
    }

}
