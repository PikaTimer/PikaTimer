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
package com.pikatimer.participant;

import java.util.List;
import java.util.Objects;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Callback;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;


/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
@Entity
@DynamicUpdate
@Table(name="custom_participant_attributes")
public class CustomAttribute {
    private CustomAttributeType type;
    private final StringProperty name = new SimpleStringProperty();
    private List<String> allowable_values;
    private final ObservableList<String> allowableValuesList = FXCollections.observableArrayList();  
    private final StringProperty uuidProperty = new SimpleStringProperty(java.util.UUID.randomUUID().toString());

    private Integer id;

    @Override
    public String toString() {
        return name.getValueSafe();
    }
    
    @Id
    @GenericGenerator(name="custom_attribute_id" , strategy="increment")
    @GeneratedValue(generator="custom_attribute_id")
    @Column(name="id")
    public Integer getID() {
        return id;
    }
    public void setID(Integer id) {
        this.id = id;
    }
    
    //    uuid varchar,
    @Column(name="uuid")
    public String getUUID() {
       // System.out.println("RaceReport UUID is " + uuidProperty.get());
        return uuidProperty.getValue(); 
    }
    public void setUUID(String  uuid) {
        uuidProperty.setValue(uuid);
        //System.out.println("RaceReport UUID is now " + uuidProperty.get());
    }
    public StringProperty uuidProperty() {
        return uuidProperty; 
    }
    
    @Column(name="attribute_name")
    public String getName() {
        return name.getValueSafe();
    }
    public void setName(String b){
        name.setValue(b);
    }
    public StringProperty nameProperty(){
        return name;
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name="attribute_type")
    public CustomAttributeType getAttributeType() {
        return type;
    }
    public void setAttributeType(CustomAttributeType t) {
        type = t;
    }
    
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name="value", nullable=false)
    @CollectionTable(name="custom_participant_attributes_values", joinColumns=@JoinColumn(name="id"))
    public List<String> getAllowableValues() {
        return allowable_values;  
    }
    public void setAllowableValues(List<String> l) {
        allowable_values = l;
        if (l != null) allowableValuesList.setAll(l);
    }
    public ObservableList<String> allowableValuesProperty(){
        return allowableValuesList;
    }

    public static Callback<CustomAttribute, Observable[]> extractor() {
        return (CustomAttribute ac) -> new Observable[]{ac.name};
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.type);
        hash = 97 * hash + Objects.hashCode(this.id);
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
        final CustomAttribute other = (CustomAttribute) obj;
        if (this.type != other.type) {
            return false;
        }
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
    
}
