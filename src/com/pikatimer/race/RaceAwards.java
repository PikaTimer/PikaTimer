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

import com.pikatimer.results.ProcessedResult;
import com.pikatimer.util.DurationFormatter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Pair;
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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
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
    private final Map<String,Integer> intAttributes = new HashMap();
    private final Map<String,Boolean> boolAttributes = new HashMap();

    
    private List<AwardCategory> awardCategories; // To make hibernate happy
    private final ObservableList<AwardCategory> awardCategoriesList = FXCollections.observableArrayList(AwardCategory.extractor());
    
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
    

    
    @ElementCollection(fetch = FetchType.EAGER)
    @OneToMany(mappedBy="raceAward",cascade={CascadeType.PERSIST, CascadeType.REMOVE},fetch = FetchType.EAGER)
    @Fetch(FetchMode.SELECT)
    @OrderColumn(name = "category_priority")
    public List<AwardCategory> getAwardCategories(){
        return awardCategories;
    }
    public void setAwardCategories(List<AwardCategory> a){
        System.out.println("RaceAwards::setAwardCategories called ");
        awardCategories = a;
        if (awardCategoriesList.isEmpty() && a != null) {
            awardCategoriesList.addAll(a);
        }
    }
    public ObservableList<AwardCategory> awardCategoriesProperty(){
        return awardCategoriesList;
    }
    public void addAwardCategory(AwardCategory a) {
        a.setPriority(awardCategoriesList.size());
        awardCategoriesList.add(a);
        recalcPriorities();
    }
    public void removeAwardCategory(AwardCategory a){
        awardCategoriesList.remove(a);
        recalcPriorities();
    }
    
    public void recalcPriorities(){
        for (int i = 0; i < awardCategoriesList.size(); i++) 
            awardCategoriesList.get(i).setPriority(i);
        awardCategoriesList.sort((ac1,ac2) -> Integer.compare(ac1.getPriority(), ac2.getPriority()));
        awardCategories = awardCategoriesList;
    }
            
    
    // Loop through the categories and produce a map of the categories -> winners
    public Map<AwardCategory,Map<String,List<AwardWinner>>> getAwardWinners(List<ProcessedResult> pr){
        Map<AwardCategory,Map<String,List<AwardWinner>>> awardWinners = new HashMap();
        Duration cutoffTime = Duration.ofNanos(race.getRaceCutoff());
        String dispFormat = race.getStringAttribute("TimeDisplayFormat");
        String roundMode = race.getStringAttribute("TimeRoundingMode");
        String cutoffTimeString = DurationFormatter.durationToString(cutoffTime, dispFormat, roundMode);

        // filter out inelligible participants
        List<ProcessedResult> contendersList = new ArrayList(
            pr.stream().filter(p -> p.getChipFinish() != null)
                .filter(p -> p.getParticipant().getDNF() != true)
                .filter(p -> p.getParticipant().getDQ() != true)
                .filter(p -> {
                    if (cutoffTime.isZero()) return true;
                    return cutoffTime.compareTo(p.getChipFinish()) >= 0 ||
                            cutoffTimeString.equals(DurationFormatter.durationToString(p.getChipFinish(), dispFormat, roundMode));
                })
                .sorted((p1, p2) -> p1.getChipFinish().compareTo(p2.getChipFinish()))
                .collect(Collectors.toList())
        );
        
        for(int i=0; i< awardCategoriesList.size();i++) {
            Pair<Map<String,List<AwardWinner>>,List<ProcessedResult>> results = awardCategoriesList.get(i).process(contendersList);
            awardWinners.put(awardCategoriesList.get(i), results.getKey());
            contendersList = results.getValue();
        }
        return awardWinners;
    }
     
    public void createDefaultCategories(){
        System.out.println("RaceAwards:createDefaultCategories() Called...");

        // Overall
        AwardCategory o = new AwardCategory();
        o.setName("Overall");
        o.setType(AwardCategoryType.OVERALL);
        o.setPriority(0);
        o.setRaceAward(this);
        addAwardCategory(o);

        // Masters
        AwardCategory m = new AwardCategory();
        m.setName("Masters");
        m.setType(AwardCategoryType.MASTERS);
        m.setPriority(1);
        m.setRaceAward(this);
        m.setMastersAge(race.getAgeGroups().getMasters());
        addAwardCategory(m);

        // Masters
        AwardCategory ag = new AwardCategory();
        ag.setName("Age Group");
        ag.setType(AwardCategoryType.AGEGROUP);
        ag.setPriority(2);
        ag.setRaceAward(this);
        addAwardCategory(ag);
            
        // Check to see if we have some old settings laying around...
        // We no longer support a Male/Female depth so we will pull from the
        // Male depth if it was previously configured. 
        // The user can create a custom award to filter out one gender or the 
        // other if they want to play that game. 
        if(attributes != null && !attributes.isEmpty()) {
            
            if (attributes.containsKey("OverallMaleDepth") ) o.setDepth(Integer.parseInt(attributes.get("OverallMaleDepth")));
            if (attributes.containsKey("OverallChip") ) o.setChip(Boolean.parseBoolean(attributes.get("OverallChip")));
            if (attributes.containsKey("OverallPull") ) o.setPull(Boolean.parseBoolean(attributes.get("OverallPull")));

            if (attributes.containsKey("MastersMaleDepth") ) o.setDepth(Integer.parseInt(attributes.get("MastersMaleDepth")));
            if (attributes.containsKey("MastersChip") ) m.setChip(Boolean.parseBoolean(attributes.get("MastersChip")));
            if (attributes.containsKey("MastersPull") ) m.setPull(Boolean.parseBoolean(attributes.get("MastersPull")));

            
            if (attributes.containsKey("AGMaleDepth") ) ag.setDepth(Integer.parseInt(attributes.get("AGMaleDepth")));
            if (attributes.containsKey("AGChp") ) ag.setChip(Boolean.parseBoolean(attributes.get("AGChp")));
            ag.setPull(Boolean.TRUE);
            
            //attributes.clear(); // remove the outdated values
        }
        
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
    
    
    // **********
    // * To be removed
    // **********
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
