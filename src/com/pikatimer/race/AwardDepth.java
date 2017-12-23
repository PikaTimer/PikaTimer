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
 * @author John Garner <segfaultcoredump@gmail.com>
 */
@Embeddable
public class AwardDepth {

    private final IntegerProperty startCountProperty = new SimpleIntegerProperty();
    private final StringProperty endCountProperty = new SimpleStringProperty(); 
    private final IntegerProperty depthProperty = new SimpleIntegerProperty();
    
    public AwardDepth() {
        
    } 
    
    @Column(name="start")
    public Integer getStartCount() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return startCountProperty.getValue(); 
    }
    public void setStartCount(Integer i) {
        startCountProperty.set(i);
    }
    public IntegerProperty startCountProperty() {
        return startCountProperty;
    }
    
    public StringProperty endCountProperty() {
        return endCountProperty;
    }
    
    @Column(name="depth")
    public Integer getDepth() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return depthProperty.getValue();
    }
    public void setDepth(Integer i) {
        System.out.println("AwardDepth.setAGIncrement() with " + i);
        depthProperty.setValue(i);
    }
    public IntegerProperty depthProperty() {
        return depthProperty;
    }
    
    public static Callback<AwardDepth, Observable[]> extractor() {
        return (AwardDepth i) -> new Observable[]{i.startCountProperty,i.endCountProperty,i.depthProperty};
    }

}    

