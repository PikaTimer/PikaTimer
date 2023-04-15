/*
 * Copyright (C) 2023 John Garner <segfaultcoredump@gmail.com>
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
public class SexCode {

    private final StringProperty codeProperty = new SimpleStringProperty(); 
    private final StringProperty labelProperty = new SimpleStringProperty();

    public SexCode() {
        
    } 
    
    public SexCode(String code, String display){
        codeProperty.set(code);
        labelProperty.set(display);
    }
    
    @Column(name="code")
    public String getCode() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return codeProperty.getValue(); 
    }
    public void setCode(String i) {
        codeProperty.set(i);
    }
    public StringProperty codeProperty() {
        return codeProperty;
    }
    

    
    @Column(name="display")
    public String getLabel() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return labelProperty.getValueSafe(); 
    }
    public void setLabel(String i) {
        labelProperty.setValue(i);
    }
    public StringProperty labelProperty() {
        return labelProperty;
    }
    
    public static Callback<SexCode, Observable[]> extractor() {
        return (SexCode i) -> new Observable[]{i.codeProperty,i.labelProperty};
    }

}
