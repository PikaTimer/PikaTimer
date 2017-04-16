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
package com.pikatimer.event;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
@Entity
@DynamicUpdate
@Table(name="event_options")
public class EventOptions {

    private Integer eventID;
    private Map<String,String> attributes = new HashMap();
    private Map<String,Integer> intAttributes = new HashMap();
    private Map<String,Boolean> boolAttributes = new HashMap();

    

    public EventOptions() {
    
    }
    
    @Id
//    @GenericGenerator(name = "event_id_generator", strategy = "foreign", 
//	parameters = @Parameter(name = "property", value = "race"))
//    @GeneratedValue(generator = "revent_id_generator")
    @Column(name = "event_id", unique = true, nullable = false)
    public Integer getEventID() {
        return eventID; 
    }
    public void setEventID(Integer r) {
        eventID = r;
    }
    
    
    
    // The map of attributes -> values
    // easier than a really wide table of attributes since this thing will just 
    // grow once we add in custom stuff
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="attribute", insertable=false,updatable=false)
    @Column(name="value")
    @CollectionTable(name="event_options_attributes", joinColumns=@JoinColumn(name="event_id"))
    //@OrderColumn(name = "index_id")
    private Map<String, String> getAttributes() {
        System.out.println("EventOptions::getAttributes()");
        attributes.keySet().forEach(k -> {
        System.out.println("  " + k + " -> " + attributes.get(k));
        });
        return attributes;
    }
    private void setAttributes(Map<String,String> m) {
        attributes = m;
        System.out.println("EventOptions::setAttributes(Map)");
        m.keySet().forEach(k -> {
        System.out.println("  " + k + " -> " + m.get(k));
        });
    } 
    

    public Integer getIntegerAttribute(String key) {
        if (!intAttributes.containsKey(key)) {
            if (attributes.containsKey(key)) {
                intAttributes.put(key,Integer.parseUnsignedInt(attributes.get(key)));
            } else {
                System.out.println("EventOptions.getIntegerAtrribute value for " + key + " is NULL!");
                return null;
            }
        }
        return intAttributes.get(key);
    }
    public void setIntegerAttribute(String key, Integer n) {
        intAttributes.put(key,n);
        attributes.put(key, n.toString());
    }
    
    
    
     public Boolean getBooleanAttribute(String key) {
        if (!boolAttributes.containsKey(key)) {
            if (attributes.containsKey(key)) {
                boolAttributes.put(key,Boolean.parseBoolean(attributes.get(key)));
            } else {
                System.out.println("EventOptions.getBooleanAtrribute value for " + key + " is NULL!");
                return null;
            }
        }
        return boolAttributes.get(key);
    }
    public void setBooleanAttribute(String key, Boolean n) {
        boolAttributes.put(key,n);
        attributes.put(key, n.toString());
    }
    
    public String getStringAttribute(String key) {
        if (!attributes.containsKey(key)) {
            System.out.println("EventOptions.getStringAttribute value for " + key + " is NULL!");

            return null;
        }
        return attributes.get(key);
    }
    public void setStringAttribute(String key, String v) {
        attributes.put(key, v);
    }
}
