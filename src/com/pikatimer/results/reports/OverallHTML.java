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
package com.pikatimer.results.reports;

import com.pikatimer.event.Event;
import com.pikatimer.race.Race;
import com.pikatimer.results.ProcessedResult;
import com.pikatimer.results.RaceReport;
import com.pikatimer.results.RaceReportType;
import com.pikatimer.util.DurationFormatter;
import com.pikatimer.util.Pace;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class OverallHTML implements RaceReportType{
    Race race;

    @Override
    public void init(Race r) {
        race = r;
    }

    @Override
    public String process(List<ProcessedResult> prList, RaceReport rr) {
        System.out.println("OverallHTML.process() Called... ");
        String report = new String();
        
        race = rr.getRace();
        
        Event event = Event.getInstance();  // fun with singletons... 
        
        rr.getKnownAttributeNames().forEach(s -> {System.out.println("Known Attribute: " + s);});

        Boolean showDQ = rr.getBooleanAttribute("showDQ");
        Boolean inProgress = rr.getBooleanAttribute("inProgress");
        Boolean showSplits = rr.getBooleanAttribute("showSplits");
        Boolean showDNF = rr.getBooleanAttribute("showDNF");
        Boolean showPace = rr.getBooleanAttribute("showPace");
        Boolean showGun = rr.getBooleanAttribute("showGun");
        
        // what is the longest name?
//        IntegerProperty fullNameLength = new SimpleIntegerProperty(10);
//        prList.forEach (pr ->{
//            if(pr.getParticipant().fullNameProperty().length().getValue() > fullNameLength.getValue()) 
//                fullNameLength.setValue(pr.getParticipant().fullNameProperty().length().getValue());
//        });
//        fullNameLength.setValue(fullNameLength.getValue() + 1);
       
        report += "<HTML> " +  System.lineSeparator();
        report += "  <HEAD> " +  System.lineSeparator();
        
        // TODO: Insert CSS and JS includes here
        
        report += "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.datatables.net/v/dt/jq-2.2.3/jszip-2.5.0/pdfmake-0.1.18/dt-1.10.12/af-2.1.2/b-1.2.1/b-html5-1.2.1/b-print-1.2.1/fh-3.1.2/kt-2.1.2/r-2.1.0/sc-1.4.2/se-1.2.0/datatables.min.css\"/>\n" +
                    " \n" +
                    "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/v/dt/jq-2.2.3/jszip-2.5.0/pdfmake-0.1.18/dt-1.10.12/af-2.1.2/b-1.2.1/b-html5-1.2.1/b-print-1.2.1/fh-3.1.2/kt-2.1.2/r-2.1.0/sc-1.4.2/se-1.2.0/datatables.min.js\"></script>";
        
        report += "<script type=\"text/javascript\" class=\"init\">\n" +
                    "	\n" +
                    "\n" +
                    "$(document).ready(function() {\n" +
                    "	$('#results').DataTable({\n" +
                        "    responsive: true,\n" +
                        "    scrollY: '400px',\n" +
                        "    scrollCollapse: true,\n" +
                        "    paging: false\n" +
                        "});\n" +
                    "} );\n" +
                    "\n" +
                    "\n" +
                    "	</script>";
        
        report += "  </HEAD> " +  System.lineSeparator();
        report += "  <BODY> " +  System.lineSeparator();
        report += "    <H1>" + event.getEventName() + "</H1>" + System.lineSeparator();
        report += "    <H2>" + race.getRaceName() + "</h2>" + System.lineSeparator();
        
        report += "    <H2>" + event.getLocalEventDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) + "</h2>" + System.lineSeparator();
        report += System.lineSeparator();
        
        if(inProgress) {
            report += "    <H2>" + "*******In Progress*******" + "</h2>" + System.lineSeparator();
            report += System.lineSeparator();
        }
        
        // Start the table
        report += "  <TABLE id=\"results\" class=\"display responsive nowrap\" > " +  System.lineSeparator();
        // print the headder
        report += "    <thead><tr>" +  System.lineSeparator();
        report += "      <th data-priority=\"1\">OA#</th>" +  System.lineSeparator();
        report += "      <th data-priority=\"1\">SEX</th>" +  System.lineSeparator(); // 5R chars
        report += "      <th data-priority=\"2\">AG#</th>" +  System.lineSeparator(); // 5R chars
        report += "      <th data-priority=\"2\">BIB</th>" +  System.lineSeparator(); // 5R chars for the bib #
        report += "      <th data-priority=\"1\">AGE</th>" +  System.lineSeparator(); // 4R for the age
        report += "      <th data-priority=\"1\">SEX</th>" +  System.lineSeparator(); // 4R for the sex
        report += "      <th data-priority=\"2\">AG</th>" +  System.lineSeparator(); //6L for the AG Group
        report += "      <th data-priority=\"1\">Name</th>" +  System.lineSeparator(); // based on the longest name
        report += "      <th data-priority=\"2\">City</th>" +  System.lineSeparator(); // 18L for the city
        report += "      <th data-priority=\"2\">ST</th>" +  System.lineSeparator(); // 4C for the state code
         
        // Insert split stuff here
        if (showSplits) {
            // do stuff
            // 9 chars per split
            for (int i = 2; i < race.splitsProperty().size(); i++) {
                report += "      <th data-priority=\"4\">" + race.splitsProperty().get(i-1).getSplitName() + "</th>" +  System.lineSeparator();
            }
        }
        
        // Chip time
        report += "      <th data-priority=\"1\">Finish</th>" +  System.lineSeparator(); // 9R Need to adjust for the format code
       
        // gun time
        if (showGun) report += "      <th data-priority=\"3\">Gun</th>" +  System.lineSeparator(); // 9R ibid
        // pace
        if (showPace) report += "      <th data-priority=\"3\">Pace</th>" +  System.lineSeparator(); // 10R
        
        report += "</tr></thead>" +  System.lineSeparator(); 
        report += "<tbody>"+ System.lineSeparator();
        
        
        
        final StringBuilder chars = new StringBuilder();
        
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
                chars.append("<td  colspan=\"3\"> **Started** </td>" +  System.lineSeparator());
                hideTime = true;
            } else if (pr.getChipFinish() == null) {
                return;
            } else if (! dnf && ! dq) { 
                chars.append("<td>"+ pr.getOverall().toString() + " </td>" +  System.lineSeparator());
                chars.append("<td>"+ pr.getSexPlace().toString() + " </td>" +  System.lineSeparator());
                chars.append("<td>"+ pr.getAGPlace().toString() + " </td>" +  System.lineSeparator()); 
            } else {
                if (dnf) chars.append("<td  colspan=\"3\">***DNF***</td>" +  System.lineSeparator());
                else chars.append("<td  colspan=\"3\">*****DQ****</td>" +  System.lineSeparator());
                    
            }
            
            chars.append("<td>"+ pr.getParticipant().getBib() + "</td>" +  System.lineSeparator());
            chars.append("<td>"+ pr.getAge().toString() + "</td>" +  System.lineSeparator());
            chars.append("<td>"+ pr.getSex() + "</td>" +  System.lineSeparator());
            chars.append("<td>"+ pr.getAGCode() + "</td>" +  System.lineSeparator());
            chars.append("<td>"+ pr.getParticipant().fullNameProperty().getValueSafe() + "</td>" +  System.lineSeparator());
            chars.append("<td>"+ pr.getParticipant().getCity() + "</td>" +  System.lineSeparator());
            chars.append("<td>"+ pr.getParticipant().getState() + "</td>" +  System.lineSeparator());

            // Insert split stuff here 
            if (showSplits && ! hideTime) {
            // do stuff
                for (int i = 2; i < race.splitsProperty().size(); i++) {
                    chars.append("<td>"+ DurationFormatter.durationToString(pr.getSplit(i), 0, Boolean.FALSE, RoundingMode.DOWN)+ "</td>" +  System.lineSeparator());
                }
            }
            // chip time
            if (! hideTime) chars.append("<td>"+DurationFormatter.durationToString(pr.getChipFinish(), 0, Boolean.FALSE, RoundingMode.DOWN)+ "</td>" +  System.lineSeparator());
            if (showGun && ! hideTime) chars.append("<td>"+DurationFormatter.durationToString(pr.getGunFinish(), 0, Boolean.FALSE, RoundingMode.DOWN)+ "</td>" +  System.lineSeparator());
            if (showPace && ! hideTime) chars.append("<td>"+StringUtils.stripStart(Pace.getPace(race.getRaceDistance().floatValue(), race.getRaceDistanceUnits(), pr.getChipFinish(), Pace.MPM), "0")+ "</td>" +  System.lineSeparator());
//            System.out.println("Results: " + r.getRaceName() + ": "
//                    + r.getParticipant().fullNameProperty().getValueSafe() 
//                    + "(" + pr.getSex() + pr.getAGCode() + "): " 
//                    + DurationFormatter.durationToString(pr.getChipFinish())
//                    + " O:" + pr.getOverall() + " S:" + pr.getSexPlace() 
//                    + " AG:" + pr.getAGPlace()
//            );
            
            chars.append("</tr>" + System.lineSeparator());
        
        
        });
            
        report += chars.toString();
        report += "</tbody>" +  System.lineSeparator();
        report += "  </BODY> " +  System.lineSeparator();
        report += "</HTML> " +  System.lineSeparator();
        
        return report;
    }
    
    
}
