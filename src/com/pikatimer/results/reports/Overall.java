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
package com.pikatimer.results.reports;

import com.pikatimer.event.Event;
import com.pikatimer.participant.CustomAttribute;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.AwardCategory;
import com.pikatimer.race.AwardWinner;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.results.ProcessedResult;
import com.pikatimer.results.RaceReport;
import com.pikatimer.results.RaceReportType;
import static com.pikatimer.results.reports.OverallJSON.getOrdinal;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.Pace;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author jcgarner
 */
public class Overall implements RaceReportType{
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
    Boolean showAwards = true;
    
    Boolean showCustomAttributes = false;
    List<CustomAttribute> customAttributesList = new ArrayList();
    Map<Integer,Integer> customAttributeSizeMap = new HashMap();

    Map<String,Boolean> supportedOptions = new HashMap();
    
    public Overall(){
        supportedOptions.put("showDQ", true);
        supportedOptions.put("inProgress", false);
        supportedOptions.put("showSplits", false);
        supportedOptions.put("showSegments", true);
        supportedOptions.put("showSegmentPace", false);
        supportedOptions.put("showDNF", false);
        supportedOptions.put("showPace", true);
        supportedOptions.put("showGun", true);
        supportedOptions.put("hideCustomHeaders", false);
        supportedOptions.put("showAwards",true);
        supportedOptions.put("showCustomAttributes", false);

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
        System.out.println("Overall.process() Called... ");
        String report = new String();
        
        race = rr.getRace();
        
        Event event = Event.getInstance();  // fun with singletons... 
        
        //rr.getKnownAttributeNames().forEach(s -> {System.out.println("Known Attribute: " + s);});
        supportedOptions.keySet().forEach(k -> supportedOptions.put(k, rr.getBooleanAttribute(k)));


        showDQ = supportedOptions.get("showDQ");
        inProgress = supportedOptions.get("inProgress");
        showSplits = supportedOptions.get("showSplits");
        showSegments = supportedOptions.get("showSegments");
        showSegmentPace = supportedOptions.get("showSegmentPace");
        showDNF = supportedOptions.get("showDNF");
        showPace = supportedOptions.get("showPace");
        showGun = supportedOptions.get("showGun");
        showAwards = supportedOptions.get("showAwards");
        showCustomAttributes = supportedOptions.get("showCustomAttributes");

        Boolean showCO = false;
        Boolean showST = false;
        for (ProcessedResult x : prList){
            if (! x.getParticipant().getCountry().isEmpty()) showCO=true;
            if (! x.getParticipant().getState().isEmpty()) showST=true;
        }
        // Stupid lambda workarounds....
        final Boolean showCountry = showCO;
        final Boolean showState = showST;
        
        final Boolean  penaltiesOrBonuses = prList.stream().anyMatch(s -> (s.getBonus() || s.getPenalty()));
        
        Boolean customHeaders = race.getBooleanAttribute("useCustomHeaders");
        if (customHeaders && supportedOptions.get("hideCustomHeaders")) customHeaders = false;
        
        String dispFormat = race.getStringAttribute("TimeDisplayFormat");
        String roundMode = race.getStringAttribute("TimeRoundingMode");
        Pace pace = Pace.valueOf(race.getStringAttribute("PaceDisplayFormat"));
        
        if (showCustomAttributes) customAttributesList= ParticipantDAO.getInstance().getCustomAttributes().stream().filter(a -> { 
            if (rr.getBooleanAttribute(a.getUUID()) != null )
                return rr.getBooleanAttribute(a.getUUID());
            return false;
        }).collect(Collectors.toList());
        if (showCustomAttributes && customAttributesList.isEmpty()) showCustomAttributes = false;
        
        // how long are these things?
        if (showCustomAttributes) {
            customAttributesList.forEach((a) -> {
                int l = a.getName().length() + 1;
                customAttributeSizeMap.put(a.getID(),l);
                for (ProcessedResult p: prList){
                    if (l <= p.getParticipant().getCustomAttribute(a.getID()).getValueSafe().length()){
                        l = p.getParticipant().getCustomAttribute(a.getID()).getValueSafe().length() + 1;
                        customAttributeSizeMap.put(a.getID(),l);
                    }
                }
            });
        }
        
        Map<String,List<String>> awardWinnersByBibMap = new HashMap();
        if (showAwards){
            Map<AwardCategory,Map<String,List<AwardWinner>>>  awardWinnersMap = race.getAwards().getAwardWinners(prList);
            StringBuilder awardPrintout = new StringBuilder();
            race.getAwards().awardCategoriesProperty().forEach(ac -> {
                if (!ac.getVisibleOverall()) return;
                Map<String,List<AwardWinner>> resultsMap = awardWinnersMap.get(ac);
                List<String> categories = resultsMap.keySet().stream().sorted((k1,k2) -> k1.compareTo(k2)).collect(Collectors.toList());
                categories.forEach(cat -> {
                    String description = (ac.getName() + " -- " +  cat).trim();
                    resultsMap.get(cat).forEach(w -> {
                        if (!awardWinnersByBibMap.containsKey(w.participant.getBib())) awardWinnersByBibMap.put(w.participant.getBib(), new ArrayList());
                        awardWinnersByBibMap.get(w.participant.getBib()).add(w.awardPlace + getOrdinal(w.awardPlace) + " " + description + " (Time: " + DurationFormatter.durationToString(w.awardTime, dispFormat, roundMode) + ")");
                    });
                });
            });
        }
        
        
        Integer dispFormatLength;  // add a space
        if (dispFormat.contains("[HH:]")) dispFormatLength = dispFormat.length()-1; // get rid of the two brackets and add a space
        else dispFormatLength = dispFormat.length()+1;
        
        Duration cutoffTime = Duration.ofNanos(race.getRaceCutoff());
        String cutoffTimeString = DurationFormatter.durationToString(cutoffTime, dispFormat, roundMode);
        
        // what is the longest name?
        IntegerProperty fullNameLength = new SimpleIntegerProperty(10);
        prList.forEach (pr ->{
            if(pr.getParticipant().fullNameProperty().length().getValue() > fullNameLength.getValue()) 
                fullNameLength.setValue(pr.getParticipant().fullNameProperty().length().getValue());
        });
        fullNameLength.setValue(fullNameLength.getValue() + 1);
       
        if (customHeaders && race.getStringAttribute("textHeader") != null && !race.getStringAttribute("textHeader").isEmpty()) {
            report += race.getStringAttribute("textHeader");
            report += System.lineSeparator();
        }
        
        report += StringUtils.center(event.getEventName(),80) + System.lineSeparator();
        if (RaceDAO.getInstance().listRaces().size() > 1) 
            report += StringUtils.center(race.getRaceName(),80) + System.lineSeparator();
        report += StringUtils.center(event.getLocalEventDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),80) + System.lineSeparator();
        report += System.lineSeparator();
        
        if(inProgress) {
            report += StringUtils.center("*******In Progress*******",80) + System.lineSeparator();
            report += System.lineSeparator();
        }
        
        if (customHeaders && race.getStringAttribute("textMessage") != null && !race.getStringAttribute("textMessage").isEmpty()) {
            report += race.getStringAttribute("textMessage");
            report += System.lineSeparator();
        }
        
        // print the headder
        report += " OA#"; // 4R chars 
        report += " SEX#"; // 5R chars
        report += "  AG#"; // 5R chars
        report += "  BIB"; // 5R chars for the bib #
        report += " AGE"; // 4R for the age
        report += " SEX"; // 4R for the sex
        report += " AG   "; //6L for the AG Group
        report += StringUtils.rightPad(" Name",fullNameLength.get()); // based on the longest name
        report += " City              "; // 18L for the city
        if (showState) report += " ST "; // 4C for the state code
        if (showCountry) report += " CO "; // 4C for the country code 
        if (showCustomAttributes) {
            for( CustomAttribute a: customAttributesList){
                report += StringUtils.rightPad(a.getName(),customAttributeSizeMap.get(a.getID()));
            }
        }
        // Insert split stuff here
        if (showSplits) {
            for (int i = 2; i < race.splitsProperty().size(); i++) {
                if (!race.splitsProperty().get(i-1).getIgnoreTime()) report += StringUtils.leftPad(race.splitsProperty().get(i-1).getSplitName(),dispFormatLength);
            }
        }
        
        if (showSegments) {
            final StringBuilder chars = new StringBuilder();
            Integer dispLeg = dispFormatLength;
            race.raceSegmentsProperty().forEach(seg -> {
                if(seg.getHidden() ) return;
                chars.append(StringUtils.leftPad(seg.getSegmentName(),dispLeg));
                if (showSegmentPace) {
                    if (seg.getUseCustomPace() ) {
                        if (! (seg.getUseCustomPace() && Pace.NONE.equals(seg.getCustomPace())))
                             chars.append(StringUtils.leftPad("Pace",seg.getCustomPace().getFieldWidth()+1));
                    } else chars.append(StringUtils.leftPad("Pace",pace.getFieldWidth()+1));
                } // pace.getFieldWidth()+1
            });
            report += chars.toString();
        }
        
        if (penaltiesOrBonuses) report += StringUtils.leftPad("Adj", dispFormatLength);

        // Chip time
        report += StringUtils.leftPad("Finish", dispFormatLength);
       
        // gun time
        if (showGun) report += StringUtils.leftPad("Gun", dispFormatLength);
        // pace
        if (showPace) report += StringUtils.leftPad("Pace",pace.getFieldWidth()+1); 
        report += System.lineSeparator();
        
        
        
        final StringBuilder chars = new StringBuilder();
        
        prList.forEach(pr -> {
            
            // if they are a DNF or DQ swap out the placement stats
            Boolean hideTime = false; 
            Boolean dnf = pr.getParticipant().getDNF();
            Boolean dq = pr.getParticipant().getDQ();
            if (dq) hideTime = true;
            
            if (pr.getChipFinish() == null) dnf = true;
                
            if (!showDNF && !inProgress && dnf) return;
            if (!showDQ && dq) return;
            
            if (dq) chars.append(StringUtils.center("***DQ****",14));
            else if (inProgress && pr.getChipFinish() == null) {
                chars.append(StringUtils.center("**Started**",14)); 
                //hideTime = true;
            } else if (!showDNF && pr.getChipFinish() == null){
                return;
            } else if (! dnf && ! dq) { 
                if (cutoffTime.isZero() 
                        || cutoffTime.compareTo(pr.getChipFinish()) >= 0 
                        || cutoffTimeString.equals(DurationFormatter.durationToString(pr.getChipFinish(), dispFormat, roundMode))) {
                    chars.append(StringUtils.leftPad(pr.getOverall().toString(),4));
                    chars.append(StringUtils.leftPad(pr.getSexPlace().toString(),5));
                    chars.append(StringUtils.leftPad(pr.getAGPlace().toString(),5)); 
                } else {
                    if (!showDNF && !inProgress) return;
                    chars.append(StringUtils.center("***OCO***",14));
                }
            } else {
                chars.append(StringUtils.center("***DNF***",14));
            }
            
            chars.append(StringUtils.leftPad(pr.getParticipant().getBib(),5));
            chars.append(StringUtils.leftPad(pr.getAge().toString(),4));
            chars.append(StringUtils.center(pr.getSex(),5));
            chars.append(StringUtils.rightPad(pr.getAGCode(),6));
            chars.append(StringUtils.rightPad(pr.getParticipant().fullNameProperty().getValueSafe(),fullNameLength.get()));
            chars.append(StringUtils.rightPad(pr.getParticipant().getCity(),18));
            if (showState) chars.append(StringUtils.center(pr.getParticipant().getState(),4));
            if (showCountry) chars.append(StringUtils.rightPad(pr.getParticipant().getCountry(),4));
            if (dq) { 
                chars.append("    Reason: ").append(pr.getParticipant().getNote());
                chars.append(System.lineSeparator());
                return;
            }
            
            if (showCustomAttributes) {
                for( CustomAttribute a: customAttributesList){
                    chars.append(StringUtils.rightPad(pr.getParticipant().getCustomAttribute(a.getID()).getValueSafe(),customAttributeSizeMap.get(a.getID())));
                }
            }

            // Insert split stuff here 
            if (showSplits && ! hideTime) {
            // do stuff
                for (int i = 2; i < race.splitsProperty().size(); i++) {
                    if (!race.splitsProperty().get(i-1).getIgnoreTime()) chars.append(StringUtils.leftPad(DurationFormatter.durationToString(pr.getSplit(i), dispFormat, roundMode), dispFormatLength));
                }
            }
            if (showSegments) {
                Integer dispLen = dispFormatLength;
                race.raceSegmentsProperty().forEach(seg -> {
                    if (seg.getHidden()) return;
                    chars.append(StringUtils.leftPad(DurationFormatter.durationToString(pr.getSegmentTime(seg.getID()), dispFormat, roundMode),dispLen));
                    if (showSegmentPace) {
                        if (seg.getUseCustomPace()) {
                            if(!Pace.NONE.equals(seg.getCustomPace())) {
                                if (pr.getSegmentTime(seg.getID()) != null ) chars.append(StringUtils.leftPad(seg.getCustomPace().getPace(seg.getSegmentDistance(), race.getRaceDistanceUnits(), pr.getSegmentTime(seg.getID())),seg.getCustomPace().getFieldWidth()+1));
                                else chars.append(StringUtils.leftPad("",seg.getCustomPace().getFieldWidth()+1));
                            }
                        } else {
                            if (pr.getSegmentTime(seg.getID()) != null ) chars.append(StringUtils.leftPad(pace.getPace(seg.getSegmentDistance(), race.getRaceDistanceUnits(), pr.getSegmentTime(seg.getID())),pace.getFieldWidth()+1));
                            else chars.append(StringUtils.leftPad("",pace.getFieldWidth()+1));
                        }
                    }
                });
            }
            if (dnf) { 
                chars.append(System.lineSeparator());
                return;
            }
            if (penaltiesOrBonuses){
                if (pr.getBonus() || pr.getPenalty()) {
                    if (pr.getBonus()) chars.append(StringUtils.leftPad("-"+DurationFormatter.durationToString(pr.getBonusTime(), dispFormat, roundMode),dispFormatLength));
                    else chars.append(StringUtils.leftPad("+"+DurationFormatter.durationToString(pr.getPenaltyTime(), dispFormat, roundMode),dispFormatLength));
                } else chars.append(StringUtils.leftPad("---",dispFormatLength));
            }
            // chip time
            if (! hideTime) chars.append(StringUtils.leftPad(DurationFormatter.durationToString(pr.getChipFinish(), dispFormat, roundMode), dispFormatLength));
            if (showGun && ! hideTime) chars.append(StringUtils.leftPad(DurationFormatter.durationToString(pr.getGunFinish(), dispFormat, roundMode), dispFormatLength));
            if (showPace && ! hideTime) chars.append(StringUtils.leftPad(pace.getPace(race.getRaceDistance().floatValue(), race.getRaceDistanceUnits(), pr.getChipFinish()),pace.getFieldWidth()+1));

            
            chars.append(System.lineSeparator());
            if (showAwards && awardWinnersByBibMap.containsKey(pr.getParticipant().getBib())){
                for(String a: awardWinnersByBibMap.get(pr.getParticipant().getBib())){
                    chars.append("\t\tAward Winner: " + a + System.lineSeparator());
                }
                chars.append(System.lineSeparator());
            }
        
        
        });
            
        
        report += chars.toString();
        
        if (customHeaders && race.getStringAttribute("textFooter") != null && !race.getStringAttribute("textFooter").isEmpty()) {
            report += System.lineSeparator();
            report += race.getStringAttribute("textFooter");
            report += System.lineSeparator();
        }
        
        return report;
    }
    
    
    
}
