/* 
 * Copyright (C) 2016 John Garner
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
import java.util.concurrent.ConcurrentHashMap;
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
@Table(name="race_age_groups")
public class AgeGroups {
    private Integer raceID;
    private Integer agStart = 9;
    private Integer agIncrement = 5;
    private Integer masters = 40; 
    
    private Map<Integer,String> agNameMap = new ConcurrentHashMap();
    private Map<Integer,Integer> agMap = new ConcurrentHashMap();
    
    private Race race;

    public AgeGroups() {
    
    }
    
    @Id
    @GenericGenerator(name = "ag_generator", strategy = "foreign", 
	parameters = @Parameter(name = "property", value = "race"))
    @GeneratedValue(generator = "ag_generator")
    @Column(name = "race_id", unique = true, nullable = false)
    public Integer getRaceID() {
        return raceID; 
    }
    public void setRaceID(Integer r) {
        raceID = r;
    }
    
    @OneToOne(mappedBy = "ageGroups")
    @MapsId
    @JoinColumn(name="race_id")  
    public Race getRace() {
        return race;
    }
    public void setRace(Race r) {
        race=r;
    }
    
    @Column(name="ag_increment")
    public Integer getAGIncrement() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return agIncrement; 
    }
    public void setAGIncrement(Integer i) {
        //System.out.println("AgeGroups.setAGIncrement() with " + i);

        agIncrement=i;
    }
    
    @Column(name="masters_start")
    public Integer getMasters() {
        return masters; 
    }
    public void setMasters(Integer i) {
        masters=i;
    }
    
    @Column(name="ag_start")
    public Integer getAGStart() {
        return agStart; 
    }
    public void setAGStart(Integer i) {
        agStart=i;
    }

    public String ageToAGString(Integer i){
        // Returns the string representation of the ag given an age
        // e.g., 42 -> 40-44
        // based on the increment and the agStart floor (1->9)
        if (agNameMap.containsKey(ageToAG(i))) return agNameMap.get(ageToAG(i));
        
        if(i < agStart) {
            agNameMap.put(ageToAG(i), "1-" + (agStart));
        } else {
            agNameMap.put(ageToAG(i), ageToAG(i) + "-" + (ageToAG(i)+agIncrement-1));
        }
        
        return agNameMap.get(ageToAG(i));
    }
    
    public Integer ageToAG(Integer i){
        // Returns the base Age for the age group given the participants age.
        // e.g., 42 -> 40
        // based on the increment and the agStart floor
        
        if (agMap.containsKey(i)) return agMap.get(i);
        
        if (i <= agStart) agMap.put(i,1);
        else agMap.put(i,((i/agIncrement)*agIncrement));
        
        return agMap.get(i);
        
    }
  
    
}
