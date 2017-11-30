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
package com.pikatimer.results.reports;

import com.pikatimer.event.Event;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.results.ProcessedResult;
import com.pikatimer.results.RaceReport;
import com.pikatimer.results.RaceReportType;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.Pace;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class OverallCSV implements RaceReportType{
   
    Race race;
    
        // Defaults
    Boolean showDQ = true;
    Boolean inProgress = false;
    Boolean showSplits = false;
    Boolean showSegments = false;
    Boolean showSegmentPace = false;
    Boolean showDNF = true;
    Boolean showPace = true;
    Boolean showGun = true;

    Map<String,Boolean> supportedOptions = new HashMap();
    
    public OverallCSV(){
        supportedOptions.put("showDQ", true);
        supportedOptions.put("inProgress", false);
        supportedOptions.put("showSplits", false);
        supportedOptions.put("showSegments", true);
        supportedOptions.put("showSegmentPace", false);
        supportedOptions.put("showDNF", false);
        supportedOptions.put("showPace", true);
        supportedOptions.put("showGun", true);
    }
    
    @Override
    public void init(Race r) {
        race = r;
    }

    @Override
    public Boolean optionSupport(String feature) {
        return supportedOptions.containsKey(feature);
    }
    
    @Override
    public String process(List<ProcessedResult> prList, RaceReport rr) {
        System.out.println("OverallCSV.process() Called... ");
        String report = new String();
        
        race = rr.getRace();
        
        Event event = Event.getInstance();  // fun with singletons... 
        String eventDate = event.getLocalEventDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
        String eventName = event.getEventName();
        String rn = "";
        if (RaceDAO.getInstance().listRaces().size() > 1) rn = race.getRaceName();
        String raceName = rn;
        supportedOptions.keySet().forEach(k -> supportedOptions.put(k, rr.getBooleanAttribute(k)));


        showDQ = supportedOptions.get("showDQ");
        inProgress = supportedOptions.get("inProgress");
        showSplits = supportedOptions.get("showSplits");
        showSegments = supportedOptions.get("showSegments");
        showSegmentPace = supportedOptions.get("showSegmentPace");
        showDNF = supportedOptions.get("showDNF");
        showPace = supportedOptions.get("showPace");
        showGun = supportedOptions.get("showGun");
        
        
        
        String dispFormat = race.getStringAttribute("TimeDisplayFormat").replace("[","").replace("}","");
        String roundMode = race.getStringAttribute("TimeRoundingMode");
        Pace pace = Pace.valueOf(race.getStringAttribute("PaceDisplayFormat"));
        
        
        
        
        Duration cutoffTime = Duration.ofNanos(race.getRaceCutoff());
        String cutoffTimeString = DurationFormatter.durationToString(cutoffTime, dispFormat, roundMode);
        
        
        final Boolean  penaltiesOrBonuses = prList.stream().anyMatch(s -> (s.getBonus() || s.getPenalty()));


        // print the headder
        report += "Event,";
        if (!raceName.isEmpty()) report += "Race,";
        report += "Date,";
        report += "OA#,"; // 4R chars 
        report += "SEX#,"; // 5R chars
        report += "AG#,"; // 5R chars
        report += "BIB,"; // 5R chars for the bib #
        report += "AGE,"; // 4R for the age
        report += "SEX,"; // 4R for the sex
        report += "AG,"; //6L for the AG Group
        report += "Full Name,"; // based on the longest name
        report += "First Name,"; // based on the longest name
        report += "Middle Name,"; // based on the longest name
        report += "Last Name,"; // based on the longest name
        report += "City,"; // 18L for the city
        report += "ST,"; // 4C for the state code
        report += "CO,"; // country
         
        // Insert split stuff here
        if (showSplits) {
            for (int i = 2; i < race.splitsProperty().size(); i++) {
                if (!race.splitsProperty().get(i-1).getIgnoreTime()) report += race.splitsProperty().get(i-1).getSplitName() + ",";
            }
        }
        
        if (showSegments) {
            final StringBuilder chars = new StringBuilder();
            race.raceSegmentsProperty().forEach(seg -> {
                if(seg.getHidden() ) return;
                chars.append(seg.getSegmentName()).append(",");
                if (showSegmentPace&& ! (seg.getUseCustomPace() && Pace.NONE.equals(seg.getCustomPace()))) chars.append(seg.getSegmentName()).append(" Pace,"); 
            });
            report += chars.toString();
        }
        
        if (penaltiesOrBonuses) report += "Adj,";
        // Chip time
        report += "Time";
       
        // gun time
        if (showGun) report += ",Gun";
        // pace
        if (showPace) report += ",Pace"; 
        report += System.lineSeparator();
        
        
        
        final StringBuilder chars = new StringBuilder();
        
        prList.forEach(pr -> {
            
            // if they are a DNF or DQ swap out the placement stats
            Boolean hideTime = false; 
            Boolean dnf = pr.getParticipant().getDNF();
            Boolean dq = pr.getParticipant().getDQ();
            Boolean oco  = false;
            if (dq) hideTime = true;
            
            if (pr.getChipFinish() == null) dnf = true;
                
            if (!showDNF && !inProgress && dnf) return;
            if (!showDQ && dq) return;
            
            if (!cutoffTime.isZero() && ! dnf && !(cutoffTime.compareTo(pr.getChipFinish()) >= 0 
                        || cutoffTimeString.equals(DurationFormatter.durationToString(pr.getChipFinish(), dispFormat, roundMode)))) {
                oco = true;
            }
            
            if (!showDNF && oco) return; 
            
            
            chars.append(eventName).append(",");
            if (!raceName.isEmpty()) chars.append(raceName).append(",");
            chars.append(eventDate).append(",");
            
            if (dq) chars.append("DQ,,,");
            else if (pr.getSplitOCO()){
                chars.append("OCO,,,");
            } else if (inProgress && pr.getChipFinish() == null) {
                chars.append(",,,");
                //hideTime = true;
            } else if (! dnf && ! dq) { 
                if (!oco) {
                    chars.append(pr.getOverall().toString()).append(",");
                    chars.append(pr.getSexPlace().toString()).append(",");
                    chars.append(pr.getAGPlace().toString()).append(","); 
                } else {
                    chars.append("OCO,,,");
                }
            } else {
                chars.append("DNF,,,");
            }
            
            chars.append(pr.getParticipant().getBib()).append(",");
            chars.append(pr.getAge().toString()).append(",");
            chars.append(pr.getSex()).append(",");
            chars.append(pr.getAGCode()).append(",");
            chars.append("\"").append(pr.getParticipant().fullNameProperty().getValueSafe()).append("\"").append(",");
            chars.append("\"").append(pr.getParticipant().getFirstName()).append("\"").append(",");
            chars.append("\"").append(pr.getParticipant().getMiddleName()).append("\"").append(",");
            chars.append("\"").append(pr.getParticipant().getLastName()).append("\"").append(",");
            chars.append("\"").append(pr.getParticipant().getCity()).append("\"").append(",");
            chars.append("\"").append(pr.getParticipant().getState()).append("\"").append(",");
            chars.append("\"").append(pr.getParticipant().getCountry()).append("\"").append(",");


            // Insert split stuff here 
            if (showSplits) {
            // do stuff
                for (int i = 2; i < race.splitsProperty().size(); i++) {
                    if (!race.splitsProperty().get(i-1).getIgnoreTime()) {
                        if (hideTime) chars.append(",");
                        else chars.append(DurationFormatter.durationToString(pr.getSplit(i), dispFormat, roundMode)).append(",");
                    }
                }
            }
            if (showSegments) {
                Boolean ht = hideTime;
                race.raceSegmentsProperty().forEach(seg -> {
                    if(seg.getHidden() ) return;
                    if (ht) chars.append(",");
                    else chars.append(DurationFormatter.durationToString(pr.getSegmentTime(seg.getID()), dispFormat, roundMode)).append(",");
                    if (showSegmentPace && ! (seg.getUseCustomPace() && Pace.NONE.equals(seg.getCustomPace()))) {
                        if (ht) chars.append(",");
                        else if (pr.getSegmentTime(seg.getID()) != null ) {
                            if (seg.getUseCustomPace()) chars.append(seg.getCustomPace().getPace(seg.getSegmentDistance(), race.getRaceDistanceUnits(), pr.getSegmentTime(seg.getID()))).append(",");
                            else chars.append(pace.getPace(seg.getSegmentDistance(), race.getRaceDistanceUnits(), pr.getSegmentTime(seg.getID()))).append(",");
                        }
                        else chars.append(",");
                    }
                });
            }
            if (dnf || dq) { 
                hideTime = true;
            }
            
            if (penaltiesOrBonuses){
                if (pr.getBonus() || pr.getPenalty()) {
                    if (pr.getBonus()) chars.append("-").append(DurationFormatter.durationToString(pr.getBonusTime(), dispFormat, roundMode));
                    else chars.append("+").append(DurationFormatter.durationToString(pr.getPenaltyTime(), dispFormat, roundMode));
                } 
                chars.append(",");
            }
            // chip time
            if (! hideTime) chars.append(DurationFormatter.durationToString(pr.getChipFinish(), dispFormat, roundMode));
            
            if (showGun) {
                if (! hideTime) chars.append(",").append(DurationFormatter.durationToString(pr.getGunFinish(), dispFormat, roundMode));
                else chars.append(",");
            }
            
            if (showPace){
                if (!hideTime) chars.append(",").append(pace.getPace(race.getRaceDistance().floatValue(), race.getRaceDistanceUnits(), pr.getChipFinish()));
                else chars.append(",");
            }
            chars.append(System.lineSeparator());
        
        
        });
            
        
        report += chars.toString();
        

        
        return report;
    }
    
    
    
}
