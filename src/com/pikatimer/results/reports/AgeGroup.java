/*
 * Copyright (C) 2016 jcgarner
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
import com.pikatimer.results.ProcessedResult;
import com.pikatimer.results.RaceReport;
import com.pikatimer.results.RaceReportType;
import com.pikatimer.util.AlphanumericComparator;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.Pace;
import java.math.RoundingMode;
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
public class AgeGroup implements RaceReportType {
    Race race;
    
    // Defaults
    Boolean showDQ = true;
    Boolean inProgress = false;
    Boolean showSplits = true;
    Boolean showDNF = true;
    Boolean showPace = true;
    Boolean showGun = true;
    IntegerProperty fullNameLength = new SimpleIntegerProperty(10);
    
    @Override
    public void init(Race r) {
        race = r;
    }
    
    @Override
    public Boolean optionSupport(String feature) {
        return true;
    }
    
    @Override
    public String process(List<ProcessedResult> prList, RaceReport rr) {
        System.out.println("AgeGroup.process() Called... ");
        String report = new String();
        
        race = rr.getRace();
        
        Event event = Event.getInstance();  // fun with singletons... 
        
        rr.getKnownAttributeNames().forEach(s -> {System.out.println("Known Attribute: " + s);});

        showDQ = rr.getBooleanAttribute("showDQ");
        inProgress = rr.getBooleanAttribute("inProgress");
        showSplits = rr.getBooleanAttribute("showSplits");
        showDNF = rr.getBooleanAttribute("showDNF");
        showPace = rr.getBooleanAttribute("showPace");
        showGun = rr.getBooleanAttribute("showGun");
        
        // what is the longest name?
        
        prList.forEach (pr ->{
            if(pr.getParticipant().fullNameProperty().length().getValue() > fullNameLength.getValue()) 
                fullNameLength.setValue(pr.getParticipant().fullNameProperty().length().getValue());
        });
        fullNameLength.setValue(fullNameLength.getValue() + 1);
       
        
        report += StringUtils.center(event.getEventName(),80) + System.lineSeparator();
        report += StringUtils.center(race.getRaceName(),80) + System.lineSeparator();
        report += StringUtils.center(event.getLocalEventDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),80) + System.lineSeparator();
        report += System.lineSeparator();
        
        if(inProgress) {
            report += StringUtils.center("*******In Progress*******",80) + System.lineSeparator();
            report += System.lineSeparator();
        }
        
        
        // Split the list into a 
        Map<String,List<ProcessedResult>> agResultsMap = prList.stream()
                    .collect(Collectors.groupingBy(pr -> {return pr.getSex() + pr.getAGCode();}));
        
        
        
        List<String> agCatList = new ArrayList(agResultsMap.keySet());
        agCatList.sort(new AlphanumericComparator());       
        
        StringBuilder chars = new StringBuilder();
        agCatList.forEach(ag -> {
            String longAG = ag.replace("F", "Female ").replace("M","Male ").replace("-"," -> "); 
            chars.append(StringUtils.center(StringUtils.center(longAG,30, "*"),80)).append(System.lineSeparator());
            chars.append(System.lineSeparator());
            chars.append(printAG(agResultsMap.get(ag)));
            
            chars.append(System.lineSeparator());
            chars.append(System.lineSeparator());
        });
        
        report += chars.toString();
        
        
        return report; 
    }
    
    public String printAG(List<ProcessedResult> prList){
        
        final StringBuilder chars = new StringBuilder();
        
        chars.append(printHeader());
        
        prList.forEach(pr -> {
            
            // if they are a DNF or DQ swap out the placement stats
            Boolean hideTime = false; 
            Boolean dnf = pr.getParticipant().getDNF();
            Boolean dq = pr.getParticipant().getDQ();
            if (dnf || dq) hideTime = true;
            
            if (pr.getChipFinish() == null && showDNF) dnf = true;
                
            if (!showDNF && dnf) return;
            if (!showDQ && dq) return;
            
            if (inProgress && pr.getChipFinish() == null) {
                chars.append(StringUtils.center("**Started**",14));
                hideTime = true;
            } else if (pr.getChipFinish() == null){
                return;
            } else if (! dnf && ! dq) { 
                chars.append(StringUtils.leftPad(pr.getOverall().toString(),4));
                chars.append(StringUtils.leftPad(pr.getSexPlace().toString(),5));
                chars.append(StringUtils.leftPad(pr.getAGPlace().toString(),5)); 
            } else {
                if (dnf) chars.append(StringUtils.center("***DNF***",14));
                else chars.append(StringUtils.center("***DQ****",14));
                    
            }
            
            chars.append(StringUtils.leftPad(pr.getParticipant().getBib(),5));
            chars.append(StringUtils.leftPad(pr.getAge().toString(),4));
            chars.append(StringUtils.center(pr.getSex(),4));
            chars.append(StringUtils.center(pr.getAGCode(),7));
            chars.append(StringUtils.rightPad(pr.getParticipant().fullNameProperty().getValueSafe(),fullNameLength.get()));
            chars.append(StringUtils.rightPad(pr.getParticipant().getCity(),18));
            chars.append(StringUtils.center(pr.getParticipant().getState(),4));

            // Insert split stuff here 
            if (showSplits && ! hideTime) {
            // do stuff
                for (int i = 2; i < race.splitsProperty().size(); i++) {
                    chars.append(StringUtils.leftPad(DurationFormatter.durationToString(pr.getSplit(i), 0, Boolean.FALSE, RoundingMode.DOWN), 9));
                }
            }
            // chip time
            if (! hideTime) chars.append(StringUtils.leftPad(DurationFormatter.durationToString(pr.getChipFinish(), 0, Boolean.FALSE, RoundingMode.DOWN), 8));
            if (showGun && ! hideTime) chars.append(StringUtils.leftPad(DurationFormatter.durationToString(pr.getGunFinish(), 0, Boolean.FALSE, RoundingMode.DOWN), 9));
            if (showPace && ! hideTime) chars.append(StringUtils.leftPad(StringUtils.stripStart(Pace.getPace(race.getRaceDistance().floatValue(), race.getRaceDistanceUnits(), pr.getChipFinish(), Pace.MPM), "0"),10));
//            System.out.println("Results: " + r.getRaceName() + ": "
//                    + r.getParticipant().fullNameProperty().getValueSafe() 
//                    + "(" + pr.getSex() + pr.getAGCode() + "): " 
//                    + DurationFormatter.durationToString(pr.getChipFinish())
//                    + " O:" + pr.getOverall() + " S:" + pr.getSexPlace() 
//                    + " AG:" + pr.getAGPlace()
//            );
            
            chars.append(System.lineSeparator());
        
        
        });
            
        
        
        
        return chars.toString();
    }
    
    private String printHeader(){
        String report = new String();
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
        report += " ST "; // 4C for the state code
         
        // Insert split stuff here
        if (showSplits) {
            // do stuff
            // 9 chars per split
            for (int i = 2; i < race.splitsProperty().size(); i++) {
                report += StringUtils.leftPad(race.splitsProperty().get(i-1).getSplitName(),9);
            }
        }
        
        // Chip time
        report += "   Finish "; // 9R Need to adjust for the format code
       
        // gun time
        if (showGun) report += "    Gun  "; // 9R ibid
        // pace
        if (showPace) report += "   Pace"; // 10R
        report += System.lineSeparator();
        
        return report; 
    }
    
}
