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
import com.pikatimer.race.RaceAwards;
import com.pikatimer.race.RaceDAO;
import com.pikatimer.results.ProcessedResult;
import com.pikatimer.results.RaceReport;
import com.pikatimer.results.RaceReportType;
import com.pikatimer.util.AlphanumericComparator;
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
public class Award implements RaceReportType {
    Race race;
    IntegerProperty fullNameLength = new SimpleIntegerProperty(10);
    List<ProcessedResult> prList;
    
    

    Map<String,Boolean> supportedOptions = new HashMap();
    
    public Award(){
        supportedOptions.put("hideCustomHeaders", false);
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
        
        Duration cutoffTime = Duration.ofNanos(race.getRaceCutoff());
        String dispFormat = race.getStringAttribute("TimeDisplayFormat");
        String roundMode = race.getStringAttribute("TimeRoundingMode");
        String cutoffTimeString = DurationFormatter.durationToString(cutoffTime, dispFormat, roundMode);

        Boolean customHeaders = race.getBooleanAttribute("useCustomHeaders");
        if (customHeaders && supportedOptions.get("hideCustomHeaders")) customHeaders = false;
        
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
        
        
        String report = new String();
        
        Map<String,List<ProcessedResult>> agAwardMap = new HashMap();
        RaceAwards awardParams = race.getAwards();
        if (awardParams == null){
            report += "Award depths have not yet been set up!" + System.lineSeparator();
            report += "Go to the Award tab and select " + race.getRaceName() + " to configure the award depths.";
            return report;
        }
        Event event = Event.getInstance();  // fun with singletons... 
        
        //rr.getKnownAttributeNames().forEach(s -> {System.out.println("Award: Known Attribute: " + s);});

        
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
            
        // Sort out who gets what
        Boolean pull = awardParams.getBooleanAttribute("OverallPull");
        Boolean chip = awardParams.getBooleanAttribute("OverallChip");
        Integer depth = awardParams.getIntegerAttribute("OverallFemaleDepth");
        
        // Overall Female
        report += StringUtils.center("********Overall Female********",80) + System.lineSeparator();    
            
        List<ProcessedResult> overall; // a filtered and sorted list
        if (chip) overall = prList.stream()
                .filter(p -> p.getSex().equalsIgnoreCase("F"))
                .sorted((p1, p2) -> p1.getChipFinish().compareTo(p2.getChipFinish()))
                .collect(Collectors.toList());
        else overall = prList.stream()
                .filter(p -> p.getSex().equalsIgnoreCase("F"))
                .sorted((p1, p2) -> p1.getGunFinish().compareTo(p2.getGunFinish()))
                .collect(Collectors.toList());
        
        report += outputHeader();
        report += printWinners(overall, depth, pull, chip);

        
        report += System.lineSeparator();
        report += System.lineSeparator();

        // Overall Male    
        depth = awardParams.getIntegerAttribute("OverallMaleDepth");
        report += StringUtils.center("*********Overall Male*********",80) + System.lineSeparator();    
        if (chip) overall = prList.stream()
                .filter(p -> p.getSex().equalsIgnoreCase("M"))
                .filter(p -> p.getChipFinish() != null)
                .sorted((p1, p2) -> p1.getChipFinish().compareTo(p2.getChipFinish()))
                .collect(Collectors.toList());
        else overall = prList.stream()
                .filter(p -> p.getSex().equalsIgnoreCase("M"))
                .filter(p -> p.getGunFinish() != null)
                .sorted((p1, p2) -> p1.getGunFinish().compareTo(p2.getGunFinish()))
                .collect(Collectors.toList());
        
        report += outputHeader();
        report += printWinners(overall, depth, pull, chip);
            
        report += System.lineSeparator();
        report += System.lineSeparator();

        // Masters
        pull = awardParams.getBooleanAttribute("MastersPull");
        Integer startAge = race.getAgeGroups().getMasters();
        chip = awardParams.getBooleanAttribute("MastersChip");
        // Masters Female   
        
        // Masters Male
        depth = awardParams.getIntegerAttribute("MastersFemaleDepth");
        
        report += StringUtils.center("********Female Masters********",80) + System.lineSeparator();   
        List<ProcessedResult> masters;
        if (chip) masters = prList.stream()
                .filter(p -> p.getSex().equalsIgnoreCase("F"))
                .filter(p -> p.getAge().compareTo(startAge) >= 0)
                .filter(p -> p.getChipFinish() != null)
                .sorted((p1, p2) -> p1.getChipFinish().compareTo(p2.getChipFinish()))
                .collect(Collectors.toList());
        else masters = prList.stream()
                .filter(p -> p.getSex().equalsIgnoreCase("F"))
                .filter(p -> p.getAge().compareTo(startAge) >= 0)
                .filter(p -> p.getGunFinish() != null)
                .sorted((p1, p2) -> p1.getGunFinish().compareTo(p2.getGunFinish()))
                .collect(Collectors.toList());
        
        report += outputHeader();
        report += printWinners(masters, depth, pull, chip);
        report += System.lineSeparator();
        report += System.lineSeparator();
        
        depth = awardParams.getIntegerAttribute("MastersMaleDepth");

        report += StringUtils.center("*********Male Masters*********",80) + System.lineSeparator();   

        if (chip) masters = prList.stream()
                .filter(p -> p.getSex().equalsIgnoreCase("M"))
                .filter(p -> p.getAge().compareTo(startAge) >= 0)
                .filter(p -> p.getChipFinish() != null)
                .sorted((p1, p2) -> p1.getChipFinish().compareTo(p2.getChipFinish()))
                .collect(Collectors.toList());
        else masters = prList.stream()
                .filter(p -> p.getSex().equalsIgnoreCase("M"))
                .filter(p -> p.getAge().compareTo(startAge) >= 0)
                .filter(p -> p.getGunFinish() != null)
                .sorted((p1, p2) -> p1.getGunFinish().compareTo(p2.getGunFinish()))
                .collect(Collectors.toList());
        
        report += outputHeader();
        report += printWinners(masters, depth, pull, chip);
         
        report += System.lineSeparator();
        report += System.lineSeparator();
        
        // Age Group Awards
        List<ProcessedResult> agList; // a filtered and sorted list
        Boolean agPull = awardParams.getBooleanAttribute("AGPull");
        
        Boolean agChip = awardParams.getBooleanAttribute("AGChip");

        int agFemaleDepth = awardParams.getIntegerAttribute("AGFemaleDepth");
        int agMaleDepth = awardParams.getIntegerAttribute("AGMaleDepth");
        
        if (chip) agList = prList.stream()
                .filter(p -> p.getChipFinish() != null)
                .sorted((p1, p2) -> p1.getChipFinish().compareTo(p2.getChipFinish()))
                .collect(Collectors.toList());
        else agList = prList.stream()
                .filter(p -> p.getGunFinish()!= null)
                .sorted((p1, p2) -> p1.getGunFinish().compareTo(p2.getGunFinish()))
                .collect(Collectors.toList());
        
        agList.forEach(r -> {
            String agCat;
            
            if (r.getParticipant().getSex().startsWith("F")) {
                if (!agAwardMap.containsKey("Female " + r.getAGCode())) agAwardMap.put("Female " + r.getAGCode(), new ArrayList());
                agAwardMap.get("Female " + r.getAGCode()).add(r);
            } else {
                if (!agAwardMap.containsKey("Male " + r.getAGCode())) agAwardMap.put("Male " + r.getAGCode(), new ArrayList());
                agAwardMap.get("Male " + r.getAGCode()).add(r);
            }
            
        
        
        });
        
        StringBuilder chars = new StringBuilder();
        
        List<String> agCatList = new ArrayList(agAwardMap.keySet());
        agCatList.sort(new AlphanumericComparator());       
        agCatList.forEach(ag -> {
            
            chars.append(StringUtils.center(StringUtils.center(ag,30, "*"),80)).append(System.lineSeparator());
            chars.append(System.lineSeparator());
            if(ag.startsWith("F")) {
                chars.append(outputHeader());
                chars.append(printWinners(agAwardMap.get(ag), agFemaleDepth, agPull, agChip));
                
            } else {
                chars.append(outputHeader());
                chars.append(printWinners(agAwardMap.get(ag), agMaleDepth, agPull, agChip)); 
            }
            chars.append(System.lineSeparator());
            chars.append(System.lineSeparator());
        });
        
        report += chars.toString();
        
        if (customHeaders && race.getStringAttribute("textFooter") != null && !race.getStringAttribute("textFooter").isEmpty()) {
            report += System.lineSeparator();
            report += race.getStringAttribute("textFooter");
            report += System.lineSeparator();
        }
        
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
        report += " ST "; // 4C for the state code
        report += StringUtils.leftPad(" Time",dispFormatLength); // Need to adjust for the format code
        report += System.lineSeparator();

        return report;
    }
    
    private String printWinners(List<ProcessedResult> overall, int depth, boolean pull, boolean chip) {
        String dispFormat = race.getStringAttribute("TimeDisplayFormat");
        String roundMode = race.getStringAttribute("TimeRoundingMode");
        Integer dispFormatLength;  // add a space
        if (dispFormat.contains("[HH:]")) dispFormatLength = dispFormat.length()-1; // get rid of the two brackets and add a space
        else dispFormatLength = dispFormat.length()+1;
        
        Boolean permitTies = race.getBooleanAttribute("permitTies") != null && race.getBooleanAttribute("permitTies");
        //System.out.println("Award::printWinners: permitTies is " + permitTies);
        
        String report = new String();
        if (overall.size() < depth) depth = overall.size();
        
        Integer currentPlace = 0;
        String lastTime = "";
        String currentTime;
        
        for (int i = 0; i < depth; i++) {
            //if (overall.size() < i || overall.get(i) == null) break;
            if (chip) currentTime = DurationFormatter.durationToString(overall.get(i).getChipFinish(), dispFormat, roundMode);
            else currentTime = DurationFormatter.durationToString(overall.get(i).getGunFinish(), dispFormat, roundMode);
            
            //System.out.println("Award::printWinners: Comparing previous " + lastTime + " to " + currentTime);
            if (permitTies && !lastTime.equals(currentTime)) currentPlace = i+1;
            else if (!permitTies) currentPlace++;
            
            lastTime = currentTime;
            
            report += StringUtils.center(currentPlace.toString(),6); // 4R chars 
            report += StringUtils.rightPad(overall.get(i).getParticipant().fullNameProperty().getValue(),fullNameLength.get()); // based on the longest name
            report += StringUtils.leftPad(overall.get(i).getParticipant().getBib(),5); // 5R chars for the bib #
            report += StringUtils.leftPad(overall.get(i).getAge().toString(),4); // 4R for the age
            report += StringUtils.leftPad(overall.get(i).getSex(),4); // 4R for the sex
            report += StringUtils.leftPad(overall.get(i).getAGCode(),6); //6L for the AG Group
            report += " ";
            report += StringUtils.rightPad(overall.get(i).getParticipant().getCity(),18); // 18L for the city
            report += StringUtils.leftPad(overall.get(i).getParticipant().getState(),4); // 4C for the state code
            report += StringUtils.leftPad(currentTime, dispFormatLength);
            if (pull) prList.remove(overall.get(i));
            report += System.lineSeparator();
            
        }
        
        if (permitTies && depth < overall.size()){
            int i = depth;
            if (chip) currentTime = DurationFormatter.durationToString(overall.get(depth).getChipFinish(), dispFormat, roundMode);
            else currentTime = DurationFormatter.durationToString(overall.get(depth).getGunFinish(), dispFormat, roundMode);
            
            if(lastTime.equals(currentTime)) {
                report += StringUtils.center(currentPlace.toString(),6); // 4R chars 
                report += StringUtils.rightPad(overall.get(i).getParticipant().fullNameProperty().getValue(),fullNameLength.get()); // based on the longest name
                report += StringUtils.leftPad(overall.get(i).getParticipant().getBib(),5); // 5R chars for the bib #
                report += StringUtils.leftPad(overall.get(i).getAge().toString(),4); // 4R for the age
                report += StringUtils.leftPad(overall.get(i).getSex(),4); // 4R for the sex
                report += StringUtils.leftPad(overall.get(i).getAGCode(),6); //6L for the AG Group
                report += " ";
                report += StringUtils.rightPad(overall.get(i).getParticipant().getCity(),18); // 18L for the city
                report += StringUtils.leftPad(overall.get(i).getParticipant().getState(),4); // 4C for the state code
                report += StringUtils.leftPad(currentTime, dispFormatLength);
                if (pull) prList.remove(overall.get(i));
                report += System.lineSeparator();
            }
        }
        
        return report;
    }

    
}
