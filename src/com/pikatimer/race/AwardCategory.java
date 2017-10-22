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

import com.pikatimer.results.ProcessedResult;
import com.pikatimer.participant.Participant;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.results.ResultsDAO;
import com.pikatimer.util.DurationFormatter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Callback;
import javafx.util.Pair;
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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
@Entity
@DynamicUpdate
@Table(name="race_award_categories")
public class AwardCategory {
    
    private final IntegerProperty IDProperty = new SimpleIntegerProperty();
    private final StringProperty uuidProperty = new SimpleStringProperty(java.util.UUID.randomUUID().toString());
    private final StringProperty nameProperty = new SimpleStringProperty("New Award");
    private final IntegerProperty priorityProperty = new SimpleIntegerProperty();
    private final ObjectProperty<AwardCategoryType> typeProperty = new SimpleObjectProperty(AwardCategoryType.OVERALL);
    private final ObjectProperty<AwardDepthType> depthTypeProperty = new SimpleObjectProperty(AwardDepthType.FIXED);
    private final BooleanProperty pullProperty = new SimpleBooleanProperty(true);
    private final BooleanProperty chipProperty = new SimpleBooleanProperty(true);
    private final IntegerProperty depthProperty = new SimpleIntegerProperty(3);
    private final IntegerProperty mastersAgeProperty = new SimpleIntegerProperty(40);
    private final ObservableList<AwardDepth> customDepthObservableList = FXCollections.observableArrayList(AwardDepth.extractor());
    
    private RaceAwards raceAward;
    
    private List<AwardFilter> filters;
    private List<String> splitBy;
    private List<AwardDepth> customDepthList;

    public AwardCategory() {
        
    }
    
    @Id
    @GenericGenerator(name="award_category_id" , strategy="increment")
    @GeneratedValue(generator="award_category_id")
    @Column(name="ID")
    public Integer getID() {
        return IDProperty.getValue(); 
    }
    public void setID(Integer id) {
        IDProperty.setValue(id);
    }
    public IntegerProperty idProperty() {
        return IDProperty; 
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RACE_ID",nullable=false)
    public RaceAwards getRaceAward() {
        return raceAward;
    }
    public void setRaceAward(RaceAwards r) {
        raceAward=r;
    }
    
    @Column(name="category_name")
    public String getName() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return nameProperty.getValueSafe(); 
    }
    public void setName(String i) {
        nameProperty.setValue(i);
    }
    public StringProperty nameProperty() {
        return nameProperty;
    }
    
    @Column(name="category_priority")
    public Integer getPriority() {
        //System.out.println("AgeGroups.getAGIncrement() returning " + agIncrement);
        return priorityProperty.getValue();
    }
    public void setPriority(Integer i) {
        priorityProperty.setValue(i);
    }
    public IntegerProperty priorityProperty() {
        return priorityProperty;
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name="award_type")
    public AwardCategoryType getType(){
        return typeProperty.getValue();
    }
    public void setType(AwardCategoryType t) {
        typeProperty.setValue(t); 
    }
    
    public ObjectProperty<AwardCategoryType> typeProperty(){
        return typeProperty;
    }
    
    @Column(name="pull")
    public Boolean getPull(){
        return pullProperty.getValue();
    }
    public void setPull(Boolean t) {
        pullProperty.setValue(t); 
    }
    
    public BooleanProperty pullProperty(){
        return pullProperty;
    }
    
    @Column(name="chip")
    public Boolean getChip(){
        return chipProperty.getValue();
    }
    public void setChip(Boolean t) {
        chipProperty.setValue(t); 
    }
    
    public BooleanProperty chipProperty(){
        return chipProperty;
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name="depth_type")
    public AwardDepthType getDepthType(){
        return depthTypeProperty.getValue();
    }
    public void setDepthType(AwardDepthType t) {
        depthTypeProperty.setValue(t); 
    }
    
    public ObjectProperty<AwardDepthType> depthTypeProperty(){
        return depthTypeProperty;
    }
    
    @Column(name="category_depth")
    public Integer getDepth() {
        System.out.println("AwardCategory.getDepth(): returning " + depthProperty.getValue());
        return depthProperty.getValue();
    }
    public void setDepth(Integer i) {
        System.out.println("AwardCategory.setDepth(): " + i);
        depthProperty.setValue(i);
    }
    public IntegerProperty depthProperty() {
        return depthProperty;
    }
    
    @Column(name="masters_age")
    public Integer getMastersAge() {
        System.out.println("AwardCategory.mastersAge(): returning " + mastersAgeProperty.getValue());
        return mastersAgeProperty.getValue();
    }
    public void setMastersAge(Integer i) {
        System.out.println("AwardCategory.setDepth(): " + i);
        mastersAgeProperty.setValue(i);
    }
    public IntegerProperty mastersAgeProperty() {
        return mastersAgeProperty;
    }
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
          name="race_award_category_depths",
          joinColumns=@JoinColumn(name="ac_id")
    )
    protected List<AwardDepth> getCustomDepthList(){
        return customDepthList;
    }
    protected void setCustomDepthList(List<AwardDepth> i){
        customDepthList = i;
    }
    
    public ObservableList<AwardDepth> customDepthProperty(){
        if (customDepthObservableList.isEmpty() && customDepthList != null && ! customDepthList.isEmpty() ) {
            customDepthObservableList.addAll(customDepthList);
            recalcCustomDepths();
        }
        return customDepthObservableList;
    }
    
    public void addCustomDepth(AwardDepth i){
        System.out.println("addCustomDepth called");
        customDepthObservableList.add(i);
        recalcCustomDepths();
        customDepthList = customDepthObservableList;
    }
    
    public void removeCustomDepth(AwardDepth i){
        customDepthObservableList.remove(i);
        recalcCustomDepths();
        customDepthList = customDepthObservableList;
    }
    
    public void recalcCustomDepths(){
        customDepthObservableList.sort((i1, i2) -> i1.getStartCount().compareTo(i2.getStartCount()));
        for (int i=0; i < customDepthObservableList.size(); i++) {
            if (i == customDepthObservableList.size() -1) customDepthObservableList.get(i).endCountProperty().setValue("∞");
            else customDepthObservableList.get(i).endCountProperty().setValue(Integer.toString(customDepthObservableList.get(i+1).getStartCount() - 1));
        }
    }
    
    public Pair<Map<String,List<AwardWinner>>,List<ProcessedResult>> process(List<ProcessedResult> pr){
        System.out.println("Processing " + typeProperty.toString() + " " + nameProperty.getValueSafe());
        // What is going on here...
        
        // The "pr" list is the contenders for the award
        // Let's make a copy for downstream contenders
        List<ProcessedResult> downstreamContenders =new ArrayList(pr);
        
        // We send back a Pair consisting for a map of the subCategory to the winners
        // and a list of results elliglble for downstream awards.
        // if we are not a Custom award type, setup some default
        // filters and splitBy arrays.
        switch (typeProperty.get()) {
            case OVERALL:
                // no Filter
                filters = new ArrayList();
                splitBy = Arrays.asList("sex");
                break;
            case MASTERS:
                filters= new ArrayList();
                filters.add(new AwardFilter("age",">=",mastersAgeProperty.getValue().toString()));
                splitBy = Arrays.asList("sex");
                break;
            case AGEGROUP:
                filters= new ArrayList();
                splitBy = Arrays.asList("sex","AG");
                break;
            default:
                System.out.println("UNKNOWN TYPE: " + typeProperty.getName());
                break;
        }
            
        // Step 1: filter
        // We assume that the list we have already filtered
        // all DNF's, DQ's, and folks with no finish times. 
        // Then filter by whatever the overall filter is (if any);
        // The result our own copy to screw with
        Race race = raceAward.getRace();
        Duration cutoffTime = Duration.ofNanos(race.getRaceCutoff());
        String dispFormat = race.getStringAttribute("TimeDisplayFormat");
        String roundMode = race.getStringAttribute("TimeRoundingMode");
        String cutoffTimeString = DurationFormatter.durationToString(cutoffTime, dispFormat, roundMode);

        List<ProcessedResult> contendersList = new ArrayList(
            pr.stream().filter(p -> {
                    for(int i=0; i< filters.size(); i++){
                        if (filters.get(i).filter(p,race) == false) return false;
                    }
                    return true;
                })
            .sorted((p1, p2) -> p1.getChipFinish().compareTo(p2.getChipFinish()))
            .collect(Collectors.toList())
        );
        // Step 2: sort by award time
        contendersList.sort((p1,p2) -> {
            if (chipProperty.get()) return p1.getChipFinish().compareTo(p2.getChipFinish());
            else return p1.getGunFinish().compareTo(p2.getGunFinish());
        });
        
        // Step 3: Split
        Map<String,List<ProcessedResult>> contendersMap = new HashMap();
        contendersList.forEach(r -> {
            String splitCat = "";
            
            // What is their split category string?
            for(int i=0; i<splitBy.size();i++){
                String attrib = splitBy.get(i);
                if (attrib.startsWith("sex")) {
                    if (r.getSex().startsWith("M")) splitCat += "Male ";
                    else splitCat += "Female ";
                } else if (attrib.equals("AG")) {
                    splitCat += r.getAGCode() + " ";
                } else if (attrib.matches("^\\d+$")) { // custom attribute
                    try {splitCat += r.getParticipant().getCustomAttribute(Integer.parseInt(attrib)) + " ";} catch (Exception e){}
                } else {
                    splitCat += r.getParticipant().getNamedAttribute(attrib) + " ";
                }
            }
            splitCat = splitCat.trim();
            if (!contendersMap.containsKey(splitCat)) contendersMap.put(splitCat,new ArrayList());
            contendersMap.get(splitCat).add(r);
        
        });
        
        // Step 4: calculate award depths
        Map<String,Integer> depthMap = new HashMap();
        if (AwardDepthType.FIXED.equals(depthTypeProperty.get()))
            contendersMap.keySet().forEach(k -> {depthMap.put(k, depthProperty.getValue());});
        else { // Oh god, 
            
            // first, figure oout how many folks are in play
            List<Participant> part;
            if (AwardDepthType.BYREG.equals(depthTypeProperty.get())) part = ParticipantDAO.getInstance().listParticipants();
            else part = ResultsDAO.getInstance().getResults(race.getID()).stream()
                .filter(p -> {
                    for(int i=0; i< filters.size(); i++){
                        if (filters.get(i).filter(ParticipantDAO.getInstance().getParticipantByBib(p.getBib()),race) == false) return false;
                    }
                    return true;
                })
                .map(p -> ParticipantDAO.getInstance().getParticipantByBib(p.getBib()))
                .collect(Collectors.toList());
            
            // now sort them into subdivisions.... 
            Map<String,Integer> subMap = new HashMap();
            part.forEach(r -> {
                String splitCat = "";
                // What is their split category string?
                for(int i=0; i<splitBy.size();i++){
                    String attrib = splitBy.get(i);
                    if (attrib.startsWith("sex")) {
                        if (r.getSex().startsWith("M")) splitCat += "Male ";
                        else splitCat += "Female ";
                    } else if (attrib.equals("AG")) {
                        splitCat += race.getAgeGroups().ageToAGString(r.getAge()) + " ";
                    } else if (attrib.matches("^\\d+$")) { // custom attribute
                        try {splitCat += r.getCustomAttribute(Integer.parseInt(attrib)) + " ";} catch (Exception e){}
                    } else {
                        splitCat += r.getNamedAttribute(attrib) + " ";
                    }
                }
                splitCat = splitCat.trim();
                if (!subMap.containsKey(splitCat)) subMap.put(splitCat,1);
                else subMap.put(splitCat, subMap.get(splitCat) + 1);
            });
            
            // now create the depthMap based on the registration numbers

            contendersMap.keySet().forEach(k -> {
                Integer count = 0;
                if (!subMap.containsKey(k)) {
                    System.out.println("Odd, we have a contender with a sub-type of k but no registered or starting participants");
                } else {
                    count = subMap.get(k);
                }
                subMap.get(k);
                Integer depth =0;
                for(AwardDepth d: customDepthList){
                    if(count >= d.getStartCount()) depth = d.getDepth();
                }
                System.out.println("AwardDepth: for " + k + " we have " + count + " and will go " + depth);

                depthMap.put(k, depth);
            });
        }
        
        // Step 5: divy up awards by subcategory
        Map<String,List<AwardWinner>> winners = new HashMap();
        Boolean ties = race.getBooleanAttribute("permitTies");
        
        contendersMap.keySet().forEach(cat -> {
            winners.put(cat, new ArrayList());
            if (! contendersMap.containsKey(cat) || contendersMap.get(cat).isEmpty()) return;
            String lastTime="";
            String currentTime="";
            Integer currentPlace = 1;
            AwardWinner prevAW = null;
            for(int i=0; i<depthMap.get(cat) && i <contendersMap.get(cat).size(); i++) {
                if (i==0) {
                    lastTime="";
                    currentPlace = 0;
                }
                AwardWinner a = new AwardWinner();
                
                if (chipProperty.get()) a.awardTime = contendersMap.get(cat).get(i).getChipFinish();
                else a.awardTime = contendersMap.get(cat).get(i).getGunFinish();
                
                currentTime = DurationFormatter.durationToString(a.awardTime, dispFormat, roundMode);
            
                //System.out.println("Award::printWinners: Comparing previous " + lastTime + " to " + currentTime);
                if (ties && !lastTime.equals(currentTime)) currentPlace = i+1;
                else if (lastTime.equals(currentTime)) { prevAW.tie = true; a.tie = true;}
                else if (!ties) currentPlace++;
                
                a.participant = contendersMap.get(cat).get(i).getParticipant();
                a.awardPlace = currentPlace;
                a.awardTitle = cat;
                a.processedResult = contendersMap.get(cat).get(i);
                winners.get(cat).add(a);
                lastTime = currentTime;
                
                prevAW = a;
                
                // If we are pulling then remove them from downstream awards
                if(pullProperty.get()) downstreamContenders.remove(contendersMap.get(cat).get(i));
                
                if (ties && i == depthMap.get(cat)-1 && i+1 <contendersMap.get(cat).size()) { // last one....
                    String nextTime ="";
                    if (chipProperty.get()) nextTime = DurationFormatter.durationToString(contendersMap.get(cat).get(i+1).getChipFinish(), dispFormat, roundMode);
                    else nextTime = DurationFormatter.durationToString(contendersMap.get(cat).get(i+1).getGunFinish(), dispFormat, roundMode);
                    if (currentTime.equals(nextTime)){
                        if (chipProperty.get()) a.awardTime = contendersMap.get(cat).get(i+1).getChipFinish();
                        else a.awardTime = contendersMap.get(cat).get(i+1).getGunFinish();
                        prevAW.tie = true;
                        a = new AwardWinner();
                        a.tie = true;
                        a.participant = contendersMap.get(cat).get(i+1).getParticipant();
                        a.awardPlace = currentPlace;
                        a.awardTitle = cat;
                        a.processedResult = contendersMap.get(cat).get(i+1);
                        winners.get(cat).add(a);
                        if(pullProperty.get()) downstreamContenders.remove(contendersMap.get(cat).get(i+1));
                    }
                }
            }
        });
                
        return new Pair(winners,downstreamContenders);
        
    }
    
    
    
    
    
    public static Callback<AwardCategory, Observable[]> extractor() {
        return (AwardCategory ac) -> new Observable[]{ac.priorityProperty};
    }

    
}
