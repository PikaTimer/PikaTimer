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
package com.pikatimer.results;

import com.pikatimer.participant.Participant;
import com.pikatimer.race.CourseRecord;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class ProcessedResult implements Comparable<ProcessedResult>{
    Map<Integer,Duration> splitTimes = new HashMap();
    Map<Integer,Duration> splitTOD = new HashMap();
    Map<Integer,Duration> segmentTimes = new HashMap();
    Map<Integer,Map<String,Integer>> segmentPlacement = new HashMap();
    
    List<CourseRecord> courseRecords = new ArrayList();
    
    Integer overallPlace;
    Integer genderPlace;
    Integer agPlace;

    
    Participant participant;
    // attributes we need a lot of
    Integer age = 0; // Dummy default to avoid nulls
    String sex="X";  // Dummy default to avoid nulls
    String agCode;
    
    String lastSeen;
       
    Duration chipFinishTime;
    Duration gunFinishTime;
    Duration chipStartTime;
    Duration waveStartTime;
    Duration finishTOD;
    
    Boolean bonus = false;
    Duration bonusTime = Duration.ZERO;
    
    Boolean penalty = false;
    Duration penaltyTime= Duration.ZERO;
    
    String bonusPenaltyNote ="";
    
    Boolean oco = false;
    Duration ocoTime= Duration.ZERO;
    Duration ocoCutoffTime= Duration.ZERO;
    Integer ocoSplit =0;
    
    Duration rawChipFinishTime;
    Duration rawGunFinishTime;
            
    
    private Integer latestSplitID = 0;
    
    public void setCourseRecords(List<CourseRecord> crs){
        courseRecords.addAll(crs);
    }
    public List<CourseRecord> getCourseRecords(){
        return courseRecords;
    }
    
    public void setChipFinish(Duration t){
        chipFinishTime = t;
    }
    public Duration getChipFinish(){
        return chipFinishTime;
    }
    
    public void setGunFinish(Duration t){
        gunFinishTime = t;
    }
    public Duration getGunFinish(){
        return gunFinishTime;
    }
    
    public void setChipStartTime(Duration t){
        chipStartTime = t;
    }
    public Duration getChipStartTime(){
        return chipStartTime;
    }
    
    public void setWaveStartTime(Duration t){
        waveStartTime = t;
    }
    public Duration getWaveStartTime(){
        return waveStartTime;
    }
    
    public void setFinishTOD(Duration t){
        finishTOD = t;
    }
    public Duration getFinishTOD(){
        return finishTOD;
    }
           
    public void setAge(Integer p){
        age = p;
    }
    public Integer getAge(){
        return age;
    }
    
    public void setSex(String x){
        sex=x;
    }
    public String getSex(){
        return sex;
    }
    
    public void setAGCode(String x){
        agCode=x;
    }
    public String getAGCode(){
        return agCode;
    }
    
    public void setOverall(Integer p){
        overallPlace = p;
    }
    public Integer getOverall(){
        return overallPlace;
    }
    
    public void setSexPlace(Integer p){
        genderPlace = p;
    }
    public Integer getSexPlace(){
        return genderPlace;
        
    }
    
    public void setAGPlace(Integer p){
        agPlace = p;
    }
    public Integer getAGPlace(){
        return agPlace;
    }
    
    public void setParticipant(Participant p){
        participant = p;    
        age=p.getAge();
        sex=p.getSex();
    }
    public Participant getParticipant(){
        return participant;
    }
    
    public Duration getSplit(Integer id){
        return splitTimes.get(id);
    }
    public void setSplit(Integer id, Duration time){
        splitTimes.put(id, time);
        if (id > latestSplitID) latestSplitID = id; 
    }
    
    public Duration getSplitTOD(Integer id){
        return splitTOD.get(id);
    }
    public void setSplitTOD(Integer id, Duration time){
        splitTOD.put(id, time);
    }
    
    public String getLastSeen(){
        return lastSeen;
    }
    public void setLastSeen(String l){
        lastSeen = l;
    }
    public Duration getSegmentTime(Integer id) {
        return segmentTimes.get(id);
    }
    public void setSegmentTime(Integer id, Duration time){
        segmentTimes.put(id,time);
    }
    public Integer getSegmentOverallPlace(Integer id){
        if (segmentPlacement.containsKey(id)) return segmentPlacement.get(id).get("Overall");
        return null;
    }
    public void setSegmentOverallPlace(Integer id, Integer place){
        segmentPlacement.putIfAbsent(id, new HashMap<>());
        segmentPlacement.get(id).put("Overall", place);
    }
    public Integer getSegmentSexPlace(Integer id){
        if (segmentPlacement.containsKey(id)) return segmentPlacement.get(id).get("Sex");
        return null;
    }
    public void setSegmentSexPlace(Integer id, Integer place){
        segmentPlacement.putIfAbsent(id, new HashMap<>());
        segmentPlacement.get(id).put("Sex", place);
    }
    public Integer getSegmentAGPlace(Integer id){
        if (segmentPlacement.containsKey(id)) return segmentPlacement.get(id).get("AG");
        return null;
    }
    public void setSegmentAGPlace(Integer id, Integer place){
        segmentPlacement.putIfAbsent(id, new HashMap<>());
        segmentPlacement.get(id).put("AG", place);
    }
    public Boolean getBonus(){
        return bonus;
    }
    public Duration getBonusTime(){
        return bonusTime;
    }
    
    public Boolean getPenalty(){
        return penalty;
    }
    public Duration getPenaltyTime(){
        return penaltyTime;
    }
    public String getBonusPenaltyNote(){
        return bonusPenaltyNote;
    }
    public Boolean getSplitOCO(){
        return oco;
    }
    public Duration getOCOTime(){
        return ocoTime;
    }
    public Duration getOCOCutoffTime(){
        return ocoCutoffTime;
    }
    public Integer getOCOSplit(){
        return ocoSplit;
    }
    public Duration getRawChipFinishTime(){
        if (rawChipFinishTime != null) return rawChipFinishTime;
        else return chipFinishTime;
    }
    public Duration getRawGunFinishTime(){
        if (rawGunFinishTime != null) return rawGunFinishTime;
        else return rawGunFinishTime;
    }
    
    @Override
    public int compareTo(ProcessedResult other) {
        
        // Compare based on the following:
        // dQ/DNF -> chipFinishTime -> split results -> start time -> full name (why not)
//        System.out.println("ProcessedResult.compareTo() starting " + this.participant.getBib() + " vs " + other.participant.getBib());
        // is somebody a DQ or DNF?
        if (participant.getDQ() && ! other.getParticipant().getDQ()) return 1;
        if (! participant.getDQ() && other.getParticipant().getDQ()) return -1;
        if (participant.getDNF() && ! other.getParticipant().getDNF()) return 1;
        if (! participant.getDNF() && other.getParticipant().getDNF()) return -1;
        
        // At this point, either both are DNF/DQ's or they both are not. 
        // Sort as if they are not. 

//        System.out.println("ProcessedResult.compareTo dnf/dq check done: " + this.participant.getBib() + " vs " + other.participant.getBib());
        
        // Step 1: check the finish time
        if (chipFinishTime != null && other.chipFinishTime != null ) { 
            // both have finish times and they are not equal
            return chipFinishTime.compareTo(other.chipFinishTime);
        } else if (chipFinishTime == null && other.chipFinishTime != null) { // we dont have a finish yet, but they do
//            System.out.println(participant.getBib() + " is null");
//            System.out.println(other.participant.getBib() + " is " + other.chipFinishTime.toString());
//            System.out.println("null vs <time>");
            return 1;
        } else if (other.chipFinishTime == null && chipFinishTime != null) {
//            System.out.println(other.participant.getBib() + " is null");
//            System.out.println(participant.getBib() + " is " + chipFinishTime.toString());
//            System.out.println("<time> vs null");
            return -1; 
        }
        
        //System.out.println("ProcessedResult.compareTo starting splits check " + this.participant.getBib() + " vs " + other.participant.getBib());
        // check the splits in reverse order
        if(!splitTimes.keySet().isEmpty() && !other.splitTimes.keySet().isEmpty()) {
            // we both have splits... 
            
            // Check #1: most recent split
            if (latestSplitID > other.latestSplitID) return -1;
            if (other.latestSplitID > latestSplitID) return 1;
            
            // Check #2: fastest split
            // splits start at 1 for the start (which we ignore) 
            // and MAY BE NULL! (so check for that). 
            for (int i = latestSplitID; i > 1; i--) {
                if (splitTimes.get(i) != null && other.splitTimes.get(i) != null && ! splitTimes.get(i).equals(other.splitTimes.get(i))) return splitTimes.get(i).compareTo(other.splitTimes.get(i));
                if (splitTimes.get(i) != null && other.splitTimes.get(i) == null) return -1;
                if (splitTimes.get(i) == null && other.splitTimes.get(i) != null) return 1;
            }
            
            
        } else if (splitTimes.keySet().isEmpty() && !other.splitTimes.keySet().isEmpty()) {
            //We don't, they do... 
            return 1;
        } else if (! splitTimes.keySet().isEmpty() && other.splitTimes.keySet().isEmpty()) {
            // we do, they don't 
            return -1;
        }
        
        //System.out.println("ProcessedResult.compareTo splits " + this.participant.getBib() + " vs " + other.participant.getBib());

        
        // check the start time
        if (! chipStartTime.equals(other.chipStartTime)) {
            return chipStartTime.compareTo(other.chipStartTime);
        }
        
//      System.out.println("ProcessedResult.compareTo start" + this.participant.getBib() + " vs " + other.participant.getBib());

        // Ok, so they have identical finish, split, start times, tiebreak on 
        // the full name property of the participant. 
        
        return participant.fullNameProperty().getValue().compareTo(other.getParticipant().fullNameProperty().getValueSafe());
    }
}
