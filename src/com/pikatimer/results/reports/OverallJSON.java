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
import com.pikatimer.timing.CookedTimeData;
import com.pikatimer.timing.TimingDAO;
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
public class OverallJSON implements RaceReportType{
   
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
    
    public OverallJSON(){
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
        System.out.println("OverallJSON.process() Called... ");
        String report = new String();
        
        race = rr.getRace();
        
        supportedOptions.keySet().forEach(k -> supportedOptions.put(k, rr.getBooleanAttribute(k)));


        showDQ = supportedOptions.get("showDQ");
        inProgress = supportedOptions.get("inProgress");
        showSplits = supportedOptions.get("showSplits");
        showSegments = supportedOptions.get("showSegments");
        showSegmentPace = supportedOptions.get("showSegmentPace");
        showDNF = supportedOptions.get("showDNF");
        showPace = supportedOptions.get("showPace");
        showGun = supportedOptions.get("showGun");
        
        
        String dispFormat = race.getStringAttribute("TimeDisplayFormat");
        String dispTimestamp = race.getStringAttribute("TimeDisplayFormat").replace("[","").replace("}","");
        String roundMode = race.getStringAttribute("TimeRoundingMode");
        Pace pace = Pace.valueOf(race.getStringAttribute("PaceDisplayFormat"));
        
        
        
        
        Duration cutoffTime = Duration.ofNanos(race.getRaceCutoff());
        String cutoffTimeString = DurationFormatter.durationToString(cutoffTime, dispFormat, roundMode);
        
        
        

        
        
        
        report =    "[\n";
        

         
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
            
            
            chars.append("\t{\n");
            

            
            chars.append("\t\t\"bib\": ").append("\"").append(pr.getParticipant().getBib()).append("\",\n");
            chars.append("\t\t\"age\": ").append("\"").append(pr.getAge().toString()).append("\",\n");
            chars.append("\t\t\"sex\": ").append("\"").append(pr.getSex()).append("\",\n");
            chars.append("\t\t\"ag\": ").append("\"").append(pr.getAGCode()).append("\",\n");
            chars.append("\t\t\"full_name\": ").append("\"").append(pr.getParticipant().fullNameProperty().getValueSafe()).append("\"").append(",\n");
            chars.append("\t\t\"first_name\": ").append("\"").append(pr.getParticipant().getFirstName()).append("\"").append(",\n");
            chars.append("\t\t\"middle_name\": ").append("\"").append(pr.getParticipant().getMiddleName()).append("\"").append(",\n");
            chars.append("\t\t\"last_name\": ").append("\"").append(pr.getParticipant().getLastName()).append("\"").append(",\n");
            chars.append("\t\t\"city\": ").append("\"").append(pr.getParticipant().getCity()).append("\"").append(",\n");
            chars.append("\t\t\"state\": ").append("\"").append(pr.getParticipant().getState()).append("\"").append(",\n");
            chars.append("\t\t\"country\": ").append("\"").append(pr.getParticipant().getCountry()).append("\"").append(",\n");
            chars.append("\t\t\"note\": ").append("\"").append(pr.getParticipant().getNote()).append("\"").append(",\n");
            
            if (dq) {
                chars.append("\t\t\"oa_place\": ").append("\"").append("DQ").append("\",\n");
                chars.append("\t\t\"sex_place\": ").append("\"").append("~~").append("\",\n");
                chars.append("\t\t\"ag_place\": ").append("\"").append("~~").append("\",\n");
            }
            else if (inProgress && pr.getChipFinish() == null) {
                chars.append("\t\t\"oa_place\": ").append("\"").append("Started").append("\",\n");
                chars.append("\t\t\"sex_place\": ").append("\"").append("~~").append("\",\n");
                chars.append("\t\t\"ag_place\": ").append("\"").append("~~").append("\",\n");
                //hideTime = true;
            } else if (! dnf && ! dq) { 
                if (!oco) {
                    chars.append("\t\t\"oa_place\": ").append("\"").append(pr.getOverall().toString()).append("\",\n");
                    chars.append("\t\t\"sex_place\": ").append("\"").append(pr.getSexPlace().toString()).append("\",\n");
                    chars.append("\t\t\"ag_place\": ").append("\"").append(pr.getAGPlace().toString()).append("\",\n");
                } else {
                    chars.append("\t\t\"oa_place\": ").append("\"").append("OCO").append("\",\n");
                chars.append("\t\t\"sex_place\": ").append("\"").append("~~").append("\",\n");
                chars.append("\t\t\"ag_place\": ").append("\"").append("~~").append("\",\n");
                }
            } else {
                chars.append("\t\t\"oa_place\": ").append("\"").append("DNF").append("\",\n");
                chars.append("\t\t\"sex_place\": ").append("\"").append("~~").append("\",\n");
                chars.append("\t\t\"ag_place\": ").append("\"").append("~~").append("\",\n");
            }

            // Insert split stuff here 
            if (showSplits) {
            // do stuff
                chars.append("\t\t\"splits\": {\n");
                for (int i = 2; i < race.splitsProperty().size(); i++) {
                    chars.append("\t\t\t\"split_").append(race.splitsProperty().get(i-1).getSplitName()).append("\": {\n");
                    if (hideTime || pr.getSplit(i) == null) {
                        chars.append("\t\t\t\t\t\"display\": ").append("\"\"").append(",\n");
                        chars.append("\t\t\t\t\t\"sort\": ").append("\"~~\"").append("\n");
                    }
                    else {
                        chars.append("\t\t\t\t\t\"display\": ").append("\"").append(DurationFormatter.durationToString(pr.getSplit(i), dispFormat, roundMode)).append("\",\n");
                        chars.append("\t\t\t\t\t\"sort\": ").append("\"").append(DurationFormatter.durationToString(pr.getSplit(i), dispTimestamp, roundMode)).append("\"\n");

                    }
                    chars.append("\t\t\t},\n");
                }
                chars.deleteCharAt(chars.lastIndexOf(","));

                chars.append("\t\t},\n");
            }
            if (showSegments) {
                Boolean ht = hideTime;
                chars.append("\t\t\"segments\": {\n");
                race.getSegments().forEach(seg -> {
                    chars.append("\t\t\t\"segment_").append(seg.getSegmentName()).append("\": {\n");
                    if (ht || pr.getSegmentTime(seg.getID()) == null) {
                        chars.append("\t\t\t\t\t\"display\": ").append("\"\"").append(",\n");
                        chars.append("\t\t\t\t\t\"sort\": ").append("\"~~\"").append(",\n");
                        chars.append("\t\t\t\t\t\"oa_place\": ").append("\"~~\"").append(",\n");
                        chars.append("\t\t\t\t\t\"sex_place\": ").append("\"~~\"").append(",\n");
                        chars.append("\t\t\t\t\t\"ag_place\": ").append("\"~~\"").append("\n");;
                    }
                    else {
                        chars.append("\t\t\t\t\t\"display\": \"").append(DurationFormatter.durationToString(pr.getSegmentTime(seg.getID()), dispFormat, roundMode)).append("\",\n");
                        chars.append("\t\t\t\t\t\"sort\": \"").append(DurationFormatter.durationToString(pr.getSegmentTime(seg.getID()), dispTimestamp, roundMode)).append("\",\n");
                        chars.append("\t\t\t\t\t\"oa_place\": ").append("\"" + pr.getSegmentOverallPlace(seg.getID()) +"\"").append(",\n");
                        chars.append("\t\t\t\t\t\"sex_place\": ").append("\"" + pr.getSegmentSexPlace(seg.getID()) +"\"").append(",\n");
                        chars.append("\t\t\t\t\t\"ag_place\": ").append("\"" + pr.getSegmentAGPlace(seg.getID()) +"\"").append("\n");;
                    }
                    if (showSegmentPace) {
                        if (ht || pr.getSegmentTime(seg.getID()) == null ) chars.append(",\n\t\t\t\t\t\"pace\": ").append("\"~~\"").append("\n");
                        else chars.append(",\n\t\t\t\t\t\"pace\": \"").append(pace.getPace(seg.getSegmentDistance(), race.getRaceDistanceUnits(), pr.getSegmentTime(seg.getID()))).append("\"\n");
                    } else chars.append("\n");
                    chars.append("\t\t\t},\n");
                });
                chars.deleteCharAt(chars.lastIndexOf(","));
                chars.append("\t\t},\n");
            }
            if (inProgress) {
                TimingDAO td = TimingDAO.getInstance();
                List<CookedTimeData> times = td.getCookedTimesByBib(pr.getParticipant().getBib());
                if (times != null && !times.isEmpty()) {
                    times.sort((p1, p2) -> p1.getTimestamp().compareTo(p2.getTimestamp()));
                    CookedTimeData last_chip = times.get(times.size()-1);
                    chars.append("\t\t\"last_seen\": \"").append(td.getTimingLocationByID(last_chip.getTimingLocationId()).getLocationName() + " at " + DurationFormatter.durationToString(last_chip.getTimestamp(), dispTimestamp, roundMode)).append("\",\n");

                } else {
                    chars.append("\t\t\"last_seen\": \"Never\",\n");
                }
            }
            if (dnf || dq) { 
                hideTime = true;
            }
            // chip time
            if (hideTime) {
                chars.append("\t\t\"finish_display\": ").append("\"\"").append(",\n");
                chars.append("\t\t\"finish_sort\": ").append("\"~~\"").append("");
            }
            else {
                chars.append("\t\t\"finish_display\": \"").append(DurationFormatter.durationToString(pr.getChipFinish(), dispFormat, roundMode)).append("\",\n");
                chars.append("\t\t\"finish_sort\": \"").append(DurationFormatter.durationToString(pr.getChipFinish(), dispTimestamp, roundMode)).append("\"");
            }
            
            
            if (showGun) {

                if (hideTime) {
                    chars.append(",\n\t\t\"gun_display\": ").append("\"\"").append(",\n");
                    chars.append("\t\t\"gun_sort\": ").append("\"~~\"").append("");
                }
                else {
                    chars.append(",\n\t\t\"gun_display\": \"").append(DurationFormatter.durationToString(pr.getGunFinish(), dispFormat, roundMode)).append("\",\n");
                    chars.append("\t\t\"gun_sort\": \"").append(DurationFormatter.durationToString(pr.getGunFinish(), dispTimestamp, roundMode)).append("\"");
                }
            }
            
            if (showPace){
                if (hideTime) {
                    chars.append(",\n\t\t\"finish_pace\": ").append("\"~~\"").append("\n");
                }
                else {
                    chars.append(",\n\t\t\"finish_pace\": \"").append(pace.getPace(race.getRaceDistance().floatValue(), race.getRaceDistanceUnits(), pr.getChipFinish())).append("\"");
                }

            }
            chars.append("\n\t},\n");
            
        });
            
        chars.deleteCharAt(chars.lastIndexOf(","));
        report += chars.toString();
        report +=   "  ]\n";

        
        return report;
    }
    
    
    
}
