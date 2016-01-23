/*
 * Copyright 2014 John Garner
 * All Rights Reserved 
 * 
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
    
    private Integer overallMale;
    private Integer overallFemale;
    private Boolean overallDoubleDip;
    private Race race;

    public RaceAwards() {
        //this.self = this; 
        
        
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
    public Map<String, String> getAttributes() {
        return attributes;
    }
    public void setAttributes(Map<String,String> award_attributes) {
        attributes = award_attributes;

    } 
    
    //Overall
    //male
    @Transient
    public Integer getOverallMale() {
        if (overallMale == null) {
            if (attributes.containsKey("OverallMale")) {
                overallMale = Integer.parseUnsignedInt(attributes.get("OverallMale"));
            } else {
                overallMale = 0;
            }
        }
        return overallMale;
    }
    public void setOverallMale(Integer n) {
        overallMale = n;
        attributes.put("OverallMale", n.toString());
    }
    
    //female
    @Transient
    public Integer getOverallFemale() {
        if (overallFemale == null) {
            if (attributes.containsKey("OverallFemale")) {
                overallFemale = Integer.parseUnsignedInt(attributes.get("OverallFemale"));
            } else {
                overallFemale = 0;
            }
        }
        return overallFemale;
    }
    public void setOverallFemale(Integer n) {
        overallFemale = n;
        attributes.put("OverallFemale", n.toString());
    }
    
    
    //Masters
    //double dip?
    //male
    //female
    
    //AG
    //double dip?
    //male
    //female
  
    
}
