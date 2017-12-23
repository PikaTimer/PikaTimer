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
import com.pikatimer.participant.CustomAttribute;
import com.pikatimer.participant.ParticipantDAO;
import com.pikatimer.race.AwardCategory;
import com.pikatimer.race.AwardWinner;
import com.pikatimer.race.Race;
import com.pikatimer.race.RaceAwards;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.results.ProcessedResult;
import com.pikatimer.results.RaceReport;
import com.pikatimer.results.RaceReportType;
import com.pikatimer.util.DurationFormatter;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author jcgarner
 */
public class Award implements RaceReportType {
    Race race;
    IntegerProperty fullNameLength = new SimpleIntegerProperty(10);
    List<ProcessedResult> prList;
    
    BooleanProperty showCountry = new SimpleBooleanProperty(true);
    BooleanProperty showState = new SimpleBooleanProperty(true);
    
    Boolean showCustomAttributes = false;
    
    Boolean showIndividualAwards = false;
    List<AwardCategory> individualAwardLlist = new ArrayList();
    
    List<CustomAttribute> customAttributesList = new ArrayList();
    Map<Integer,Integer> customAttributeSizeMap = new HashMap();
    
    Map<String,Boolean> supportedOptions = new HashMap();
    
    public Award(){
        supportedOptions.put("showCustomAttributes", false);
        supportedOptions.put("hideCustomHeaders", false);
        supportedOptions.put("showIndividualAwards",false);
    }
    
    @Override
    public Boolean optionSupport(String feature) {
        return supportedOptions.containsKey(feature);
    }
    
    @Override
    public void init(Race r) {
        race = r;

    }
    
    @Override
    public String process(List<ProcessedResult> resList, RaceReport rr) {
        System.out.println("Award.process() Called... ");
        race = rr.getRace(); 
        supportedOptions.keySet().forEach(k -> supportedOptions.put(k, rr.getBooleanAttribute(k)));
        
        showCustomAttributes = supportedOptions.get("showCustomAttributes");
        showIndividualAwards  = supportedOptions.get("showIndividualAwards");
        
        Duration cutoffTime = Duration.ofNanos(race.getRaceCutoff());
        String dispFormat = race.getStringAttribute("TimeDisplayFormat");
        String roundMode = race.getStringAttribute("TimeRoundingMode");
        String cutoffTimeString = DurationFormatter.durationToString(cutoffTime, dispFormat, roundMode);

        Boolean customHeaders = race.getBooleanAttribute("useCustomHeaders");
        if (customHeaders && supportedOptions.get("hideCustomHeaders")) customHeaders = false;
        
        Boolean showCO = false;
        Boolean showST = false;
        for (ProcessedResult x : resList){
            if (! x.getParticipant().getCountry().isEmpty()) showCO=true;
            if (! x.getParticipant().getState().isEmpty()) showST=true;
        }
        // Stupid lambda workarounds....
        showCountry.setValue(showCO);
        showState.setValue(showST);
        
        
        // Take the list we recieved and filter all DNF's, DQ's, and folks 
        // with no finish times. 
        // The result our own copy to screw with
        prList = new ArrayList(
                resList.stream().filter(p -> p.getChipFinish() != null)
                    .filter(p -> p.getParticipant().getDNF() != true)
                    .filter(p -> p.getParticipant().getDQ() != true)
                    .filter(p -> {
                        if (cutoffTime.isZero()) return true;
                        if (    cutoffTime.compareTo(p.getChipFinish()) >= 0 ||
                                cutoffTimeString.equals(DurationFormatter.durationToString(p.getChipFinish(), dispFormat, roundMode))
                            ) return true;
                        return false;
                    })
                    .collect(Collectors.toList())
                ); 
        
        if (showIndividualAwards) individualAwardLlist = race.getAwards().getAwardCategories().stream().filter( a -> {
            if (rr.getBooleanAttribute(a.getUUID()) != null )
                    return rr.getBooleanAttribute(a.getUUID());
                return false;
        }).collect(Collectors.toList());
            
            
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
        
        String report = new String();
        
        Map<String,List<ProcessedResult>> agAwardMap = new HashMap();
        RaceAwards awardParams = race.getAwards();
        if (awardParams == null){
            report += "Award depths have not yet been set up!" + System.lineSeparator();
            report += "Go to the Award tab and select " + race.getRaceName() + " to configure the award depths.";
            return report;
        }
        
        if (showIndividualAwards && individualAwardLlist.isEmpty()) {
            report += "The option to show only selected results is enabled" + System.lineSeparator();
            report += "but no reports have been selected" + System.lineSeparator();
            report += "Please select at least 1 report to show for this report";
            return report;
        }
        Event event = Event.getInstance();  // fun with singletons... 
        
        Map<AwardCategory,Map<String,List<AwardWinner>>> awardWinnersMap = awardParams.getAwardWinners(prList);
        
        // what is the longest name?
        fullNameLength.setValue(10);
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
        
        report += StringUtils.center("***Awards***",80) + System.lineSeparator();
            report += System.lineSeparator();
        
        if (customHeaders && race.getStringAttribute("textMessage") != null && !race.getStringAttribute("textMessage").isEmpty()) {
            report += race.getStringAttribute("textMessage");
            report += System.lineSeparator();
        }
            
        StringBuilder awardPrintout = new StringBuilder();
        awardParams.awardCategoriesProperty().forEach(ac -> {
            if ((showIndividualAwards && !individualAwardLlist.contains(ac)) || (!showIndividualAwards && !ac.getVisible())) return;
            Map<String,List<AwardWinner>> resultsMap = awardWinnersMap.get(ac);
            List<String> categories = resultsMap.keySet().stream().sorted((k1,k2) -> k1.compareTo(k2)).collect(Collectors.toList());
            categories.forEach(cat -> {
                String description = (ac.getName() + " " +  cat).trim();
                awardPrintout.append(StringUtils.center("********" + description +"********",80) + System.lineSeparator());
                awardPrintout.append(outputHeader());
                awardPrintout.append(printWinners(resultsMap.get(cat)));
                awardPrintout.append(System.lineSeparator());
                awardPrintout.append(System.lineSeparator());
            });
            
        
        });
        report += awardPrintout.toString();
        
        return report;
      }
    
    private String outputHeader(){
        String dispFormat = race.getStringAttribute("TimeDisplayFormat");
        Integer dispFormatLength;  // add a space
        if (dispFormat.contains("[HH:]")) dispFormatLength = dispFormat.length()-1; // get rid of the two brackets and add a space
        else dispFormatLength = dispFormat.length()+1;
        
        String report = new String();
        // print the headder
        report += "Place"; // 4R chars 
        report += StringUtils.rightPad(" Name",fullNameLength.get()); // based on the longest name
        report += "   BIB"; // 5R chars for the bib #
        report += " AGE"; // 4R for the age
        report += " SEX"; // 4R for the sex
        report += " AG   "; //6L for the AG Group
        report += " City               "; // 18L for the city
        if (showState.get()) report += "ST  "; // 4C for the state code
        if (showCountry.get()) report += " CO"; // 4C for the state code
        if (showCustomAttributes) {
            for( CustomAttribute a: customAttributesList){
                report += StringUtils.rightPad(" " + a.getName(),customAttributeSizeMap.get(a.getID()));
            }
        }
        report += StringUtils.leftPad(" Time",dispFormatLength); // Need to adjust for the format code
        report += System.lineSeparator();

        return report;
    }
    
    private String printWinners(List<AwardWinner> winners) {
        String dispFormat = race.getStringAttribute("TimeDisplayFormat");
        String roundMode = race.getStringAttribute("TimeRoundingMode");
        Integer dispFormatLength;  // add a space
        if (dispFormat.contains("[HH:]")) dispFormatLength = dispFormat.length()-1; // get rid of the two brackets and add a space
        else dispFormatLength = dispFormat.length()+1;
        
        String report = new String();
        for(AwardWinner aw: winners) {
            
           report += StringUtils.center(aw.awardPlace.toString(),6); // 4R chars  
           report += StringUtils.rightPad(aw.participant.fullNameProperty().getValue(),fullNameLength.get()); // based on the longest name
            report += StringUtils.leftPad(aw.participant.getBib(),5); // 5R chars for the bib #
            report += StringUtils.leftPad(aw.participant.getAge().toString(),4); // 4R for the age
            report += StringUtils.center(aw.participant.getSex(),5); // 4R for the sex
            report += StringUtils.rightPad(aw.processedResult.getAGCode(),5); //6L for the AG Group
            report += " ";
            report += StringUtils.rightPad(aw.participant.getCity(),18); // 18L for the city
            if (showState.get())  report += StringUtils.center(aw.participant.getState(),4); // 4C for the state code
            if (showCountry.get())  report += StringUtils.leftPad(aw.participant.getCountry(),4); // 4C for the state code
            if (showCustomAttributes) {
                for( CustomAttribute a: customAttributesList){
                    report += StringUtils.rightPad(" " + aw.participant.getCustomAttribute(a.getID()).getValueSafe(),customAttributeSizeMap.get(a.getID()));
                }
            }
            report += StringUtils.leftPad(DurationFormatter.durationToString(aw.awardTime, dispFormat, roundMode), dispFormatLength);
            report += System.lineSeparator();
        }
        return report;
    }
    

    
}
