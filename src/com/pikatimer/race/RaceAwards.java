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

import java.util.HashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 *
 * @author John
 */

@Entity
@DynamicUpdate
@Table(name="race_awards")
public class RaceAwards {
    private Integer raceID;
    private Map<String,String> attributes = new HashMap();
    private Map<String,Integer> intAttributes = new HashMap();
    private Map<String,Boolean> boolAttributes = new HashMap();

    private Race race;

    public RaceAwards() {
    
    }
    
    @Id
    @GenericGenerator(name = "race_awards_generator", strategy = "foreign", 
	parameters = @Parameter(name = "property", value = "race"))
    @GeneratedValue(generator = "race_awards_generator")
    @Column(name = "race_id", unique = true, nullable = false)
    public Integer getRaceID() {
        return raceID; 
    }
    public void setRaceID(Integer r) {
        raceID = r;
    }
    
    @OneToOne(mappedBy = "awards")
    @MapsId
    @JoinColumn(name="race_id")  
    public Race getRace() {
        return race;
    }
    public void setRace(Race r) {
        race=r;
    }
    
    // The map of attributes -> values
    // easier than a really wide table of attributes since this thing will just 
    // grow once we add in custom stuff
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name="attribute", insertable=false,updatable=false)
    @Column(name="value")
    @CollectionTable(name="race_awards_attributes", joinColumns=@JoinColumn(name="race_id"))
    @OrderColumn(name = "index_id")
    private Map<String, String> getAttributes() {
        return attributes;
    }
    private void setAttributes(Map<String,String> m) {
        attributes = m;
    } 
    

    //Overall
    //male
    public Integer getIntegerAttribute(String key) {
        if (!intAttributes.containsKey(key)) {
            if (attributes.containsKey(key)) {
                intAttributes.put(key,Integer.parseUnsignedInt(attributes.get(key)));
            } else {
                System.out.println("RaceAwards.getIntegerAtrribute key of " + key + " is NULL!");
                return null;
            }
        }
        return intAttributes.get(key);
    }
    public void setIntegerAttribute(String key, Integer n) {
        intAttributes.put(key,n);
        attributes.put(key, n.toString());
    }
    
    
    //Pull, Gun, etc
     public Boolean getBooleanAttribute(String key) {
        if (!boolAttributes.containsKey(key)) {
            if (attributes.containsKey(key)) {
                boolAttributes.put(key,Boolean.parseBoolean(attributes.get(key)));
            } else {
                System.out.println("RaceAwards.getBooleanAtrribute key of " + key + " is NULL!");
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
            return null;
        }
        return attributes.get(key);
    }
    public void setStringAttribute(String key, String v) {
        attributes.put(key, v);
    }
  
    
}
