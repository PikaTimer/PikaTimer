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
public class Award implements RaceReportType {
    Race race;
    IntegerProperty fullNameLength = new SimpleIntegerProperty(10);
    List<ProcessedResult> prList;

    @Override
    public void init(Race r) {
        race = r;
    }
    
    @Override
    public String process(List<ProcessedResult> resList, RaceReport rr) {
        System.out.println("Award.process() Called... ");

        prList = new ArrayList(resList); // our own copy to screw with
        race = rr.getRace(); 
        
        String report = new String();
        
        Map<String,List<ProcessedResult>> agAwardMap = new HashMap();
        RaceAwards awardParams = race.getAwards();
        
        Event event = Event.getInstance();  // fun with singletons... 
        
        rr.getKnownAttributeNames().forEach(s -> {System.out.println("Award: Known Attribute: " + s);});

        // We really don't care about these in this context
        // Boolean showDQ = rr.getBooleanAttribute("showDQ");
        // Boolean inProgress = rr.getBooleanAttribute("inProgress");
        // Boolean showSplits = rr.getBooleanAttribute("showSplits");
        // Boolean showDNF = rr.getBooleanAttribute("showDNF");
        // Boolean showPace = rr.getBooleanAttribute("showPace");
        // Boolean showGun = rr.getBooleanAttribute("showGun");
        
        // what is the longest name?
        
        prList.forEach (pr ->{
            if(pr.getParticipant().fullNameProperty().length().getValue() > fullNameLength.getValue()) 
                fullNameLength.setValue(pr.getParticipant().fullNameProperty().length().getValue());
        });
        fullNameLength.setValue(fullNameLength.getValue() + 1);
       
        
        report += StringUtils.center(event.getEventName(),80) + System.lineSeparator();
        if (RaceDAO.getInstance().listRaces().size() > 1) 
            report += StringUtils.center(race.getRaceName(),80) + System.lineSeparator();
        report += StringUtils.center(event.getLocalEventDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),80) + System.lineSeparator();
        report += System.lineSeparator();
        
        report += StringUtils.center("***Awards***",80) + System.lineSeparator();
            report += System.lineSeparator();
        
        
            
        // Sort out who gets what
        Boolean pull = awardParams.getBooleanAttribute("OverallPull");
        Boolean chip = awardParams.getBooleanAttribute("OverallChip");
        Integer depth = awardParams.getIntegerAttribute("OverallFemaleDepth");
        
        // Overall Female
        report += StringUtils.center("********Overall Female********",80) + System.lineSeparator();    
            
        List<ProcessedResult> overall; // a filtered and sorted list
        if (chip) overall = prList.stream()
                .filter(p -> p.getSex().equalsIgnoreCase("F"))
                .filter(p -> p.getChipFinish() != null)
                .sorted((p1, p2) -> p1.getChipFinish().compareTo(p2.getChipFinish()))
                .collect(Collectors.toList());
        else overall = prList.stream()
                .filter(p -> p.getSex().equalsIgnoreCase("F"))
                .filter(p -> p.getGunFinish() != null)
                .sorted((p1, p2) -> p1.getGunFinish().compareTo(p2.getGunFinish()))
                .collect(Collectors.toList());
        
        report += outputHeader();
        report += printWinners(overall, depth, pull, chip);
//        for (int i = 0; i < depth; i++) {
//            report += StringUtils.center(Integer.toString(i+1),6); // 4R chars 
//            report += StringUtils.rightPad(overall.get(i).getParticipant().fullNameProperty().getValue(),fullNameLength.get()); // based on the longest name
//            report += StringUtils.leftPad(overall.get(i).getParticipant().getBib(),5); // 5R chars for the bib #
//            report += StringUtils.leftPad(overall.get(i).getAge().toString(),4); // 4R for the age
//            report += StringUtils.leftPad(overall.get(i).getSex(),4); // 4R for the sex
//            report += StringUtils.leftPad(overall.get(i).getAGCode(),6); //6L for the AG Group
//            report += StringUtils.rightPad(overall.get(i).getParticipant().getCity(),18); // 18L for the city
//            report += StringUtils.leftPad(overall.get(i).getParticipant().getState(),4); // 4C for the state code
//            if (chip) report += StringUtils.leftPad(DurationFormatter.durationToString(overall.get(i).getChipFinish(), 0, Boolean.FALSE, RoundingMode.DOWN), 8); // 9R Need to adjust for the format code
//            else report += StringUtils.leftPad(DurationFormatter.durationToString(overall.get(i).getGunFinish(), 0, Boolean.FALSE, RoundingMode.DOWN), 8);
//            if (pull) prList.remove(overall.get(i));
//            report += System.lineSeparator();
//
//        }
        
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
//        for (int i = 0; i < depth; i++) {
//            report += StringUtils.center(Integer.toString(i+1),6); // 4R chars 
//            report += StringUtils.rightPad(overall.get(i).getParticipant().fullNameProperty().getValue(),fullNameLength.get()); // based on the longest name
//            report += StringUtils.leftPad(overall.get(i).getParticipant().getBib(),5); // 5R chars for the bib #
//            report += StringUtils.leftPad(overall.get(i).getAge().toString(),4); // 4R for the age
//            report += StringUtils.leftPad(overall.get(i).getSex(),4); // 4R for the sex
//            report += StringUtils.leftPad(overall.get(i).getAGCode(),6); //6L for the AG Group
//            report += StringUtils.rightPad(overall.get(i).getParticipant().getCity(),18); // 18L for the city
//            report += StringUtils.leftPad(overall.get(i).getParticipant().getState(),4); // 4C for the state code
//            if (chip) report += StringUtils.leftPad(DurationFormatter.durationToString(overall.get(i).getChipFinish(), 0, Boolean.FALSE, RoundingMode.DOWN), 8); // 9R Need to adjust for the format code
//            else report += StringUtils.leftPad(DurationFormatter.durationToString(overall.get(i).getGunFinish(), 0, Boolean.FALSE, RoundingMode.DOWN), 8);
//            
//            report += System.lineSeparator();
//            if (pull) prList.remove(overall.get(i));
//            
//        }            
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
        
        return report;
      }
    
    private String outputHeader(){
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
        report += " Time"; // 9R Need to adjust for the format code
        report += System.lineSeparator();

        return report;
    }
    
    private String printWinners(List<ProcessedResult> overall, int depth, boolean pull, boolean chip) {
        String report = new String();
        if (overall.size() < depth) depth = overall.size();
        
        for (int i = 0; i < depth; i++) {
            //if (overall.size() < i || overall.get(i) == null) break;
            report += StringUtils.center(Integer.toString(i+1),6); // 4R chars 
            report += StringUtils.rightPad(overall.get(i).getParticipant().fullNameProperty().getValue(),fullNameLength.get()); // based on the longest name
            report += StringUtils.leftPad(overall.get(i).getParticipant().getBib(),5); // 5R chars for the bib #
            report += StringUtils.leftPad(overall.get(i).getAge().toString(),4); // 4R for the age
            report += StringUtils.leftPad(overall.get(i).getSex(),4); // 4R for the sex
            report += StringUtils.leftPad(overall.get(i).getAGCode(),6); //6L for the AG Group
            report += " ";
            report += StringUtils.rightPad(overall.get(i).getParticipant().getCity(),18); // 18L for the city
            report += StringUtils.leftPad(overall.get(i).getParticipant().getState(),4); // 4C for the state code
            if (chip) report += StringUtils.leftPad(DurationFormatter.durationToString(overall.get(i).getChipFinish(), 0, Boolean.FALSE, RoundingMode.DOWN), 8); // 9R Need to adjust for the format code
            else report += StringUtils.leftPad(DurationFormatter.durationToString(overall.get(i).getGunFinish(), 0, Boolean.FALSE, RoundingMode.DOWN), 8);
            if (pull) prList.remove(overall.get(i));
            report += System.lineSeparator();

        }
        
        return report;
    }
}
