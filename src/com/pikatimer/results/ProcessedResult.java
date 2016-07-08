/*
 * Copyright (C) 2016 John Garner <segfaultcoredump@gmail.com>
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
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class ProcessedResult implements Comparable<ProcessedResult>{
    Map<Integer,Duration> splitTimes = new HashMap();
    
    Integer overallPlace;
    Integer genderPlace;
    Integer agPlace;

    
    Participant participant;
    // attributes we need a lot of
    Integer age;
    String sex;
    String agCode;
    
       
    Duration chipFinishTime;
    Duration gunFinishTime;
    Duration chipStartTime;
    Duration waveStartTime;
    
    private Integer latestSplitID = 0;
    
    
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
    
    @Override
    public int compareTo(ProcessedResult other) {
        
        // Compare based on the following:
        // dQ/DNF -> chipFinishTime -> split results -> start time -> full name (why not)
        //System.out.println("ProcessedResult.compareTo " + this.participant.getBib() + " vs " + other.participant.getBib());
        // is somebody a DQ or DNF?
        if (participant.getDQ() && ! other.getParticipant().getDQ()) return 1;
        if (! participant.getDQ() && other.getParticipant().getDQ()) return -1;
        if (participant.getDNF() && ! other.getParticipant().getDNF()) return 1;
        if (! participant.getDNF() && other.getParticipant().getDNF()) return -1;
        
        // At this point, either both are DNF/DQ's or they both are not. 
        // Sort as if they are not. 

        
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
        
        // check the splits in reverse order
        if(!splitTimes.keySet().isEmpty() && !other.splitTimes.keySet().isEmpty()) {
            // we both have splits... 
            
            // Check #1: most recent split
            if (latestSplitID > other.latestSplitID) return -1;
            if (other.latestSplitID > latestSplitID) return 1;
            
            for (int i = latestSplitID; i == 1; i--) {
                if (! splitTimes.get(i).equals(other.splitTimes.get(i))) return splitTimes.get(i).compareTo(other.splitTimes.get(i));
            }
            
            
        } else if (splitTimes.keySet().isEmpty()) {
            //We don't, they do... 
            return 1;
        } else if (! splitTimes.keySet().isEmpty()) {
            // we do, they don't 
            return -1;
        }
        
//        System.out.println("ProcessedResult.compareTo " + this.participant.getBib() + " vs " + other.participant.getBib());

        
        // check the start time
        if (! chipStartTime.equals(other.chipStartTime)) {
            return chipStartTime.compareTo(other.chipStartTime);
        }
        
//        System.out.println("ProcessedResult.compareTo " + this.participant.getBib() + " vs " + other.participant.getBib());

        // Ok, so they have identical finish, split, start times, tiebreak on 
        // the full name property of the participant. 
        
        return participant.fullNameProperty().getValue().compareTo(other.getParticipant().fullNameProperty().getValueSafe());
    }
}
