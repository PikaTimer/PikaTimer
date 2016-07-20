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
import com.pikatimer.race.RaceDAO;
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
        
        report += "<meta name=\"viewport\" content=\"width=device-width; initial-scale=1.0; maximum-scale=1.0; minimum-scale=1.0; user-scalable=0;\" />\n";
        
        report +=   "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.datatables.net/v/dt/jq-2.2.3/dt-1.10.12/fh-3.1.2/r-2.1.0/sc-1.4.2/datatables.min.css\"/>\n" +
                    " \n" +
                    "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/v/dt/jq-2.2.3/dt-1.10.12/fh-3.1.2/r-2.1.0/sc-1.4.2/datatables.min.js\"></script>\n" +
                    " \n" +
                    "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/plug-ins/1.10.12/sorting/natural.js\"></script>\n";
        report += "<script type=\"text/javascript\" class=\"init\">\n" +
                    "	\n" +
                    "\n" +
                    "$(document).ready(function() {\n" +
                    "	$('#results').DataTable({\n" + 
                        "    responsive: {\n" +
                        "            details: {\n" +
                        "                renderer: $.fn.dataTable.Responsive.renderer.tableAll()," +
                        "                type: 'column',\n" +
                        "                target: 'tr'\n" +
                        "            }\n" +
                        "        },\n" +
                        "    columnDefs: [ " +
                        "            { type: 'natural', targets: '_all' },\n" +
                        "            {className: 'control', orderable: false, targets:   0}\n" +
                        "        ],"  +
                        "    scrollY: '60vh',\n" +
                        //"    scrollCollapse: true,\n" +
                        "    scroller:    true,\n" +
                        "    deferRender: true\n" +
                        "});\n" +
                    "} );\n" +
                    "\n" +
                    "\n" +
                    "	</script>";
        
        report += "  </HEAD> " +  System.lineSeparator();
        report += "  <BODY> " +  System.lineSeparator();
        report += "    <H1>" + event.getEventName() + "</H1>" + System.lineSeparator();
        report += "    <H2>" ;
        if (RaceDAO.getInstance().listRaces().size() > 1) 
            report += race.getRaceName() + "<br>" + System.lineSeparator();
        
        report += "    " + event.getLocalEventDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) + "</h2>" + System.lineSeparator();
        report += System.lineSeparator();
        
        if(inProgress) {
            report += "    <H2>" + "*******In Progress*******" + "</h2>" + System.lineSeparator();
            report += System.lineSeparator();
        }
        
        // Start the table
        report += "  <TABLE id=\"results\" class=\"display responsive nowrap\" > " +  System.lineSeparator();
        // print the headder
        report += "    <thead><tr>" +  System.lineSeparator();
        report += "      <th></th>"+  System.lineSeparator(); // dummy for control box
        report += "      <th data-priority=\"10\">OA#</th>" +  System.lineSeparator();
        report += "      <th data-priority=\"20\">SEX#</th>" +  System.lineSeparator(); // 5R chars
        report += "      <th data-priority=\"30\">AG#</th>" +  System.lineSeparator(); // 5R chars
        report += "      <th data-priority=\"5\">BIB</th>" +  System.lineSeparator(); // 5R chars for the bib #
        report += "      <th data-priority=\"9\">AGE</th>" +  System.lineSeparator(); // 4R for the age
        report += "      <th data-priority=\"5\">SEX</th>" +  System.lineSeparator(); // 4R for the sex
        report += "      <th data-priority=\"29\">AG</th>" +  System.lineSeparator(); //6L for the AG Group
        report += "      <th data-priority=\"1\">Name</th>" +  System.lineSeparator(); // based on the longest name
        report += "      <th data-priority=\"40\">City</th>" +  System.lineSeparator(); // 18L for the city
        report += "      <th data-priority=\"40\">ST</th>" +  System.lineSeparator(); // 4C for the state code
         
        // Insert split stuff here
        if (showSplits) {
            // do stuff
            // 9 chars per split
            for (int i = 2; i < race.splitsProperty().size(); i++) {
                report += "      <th data-priority=\"100\">" + race.splitsProperty().get(i-1).getSplitName() + "</th>" +  System.lineSeparator();
            }
        }
        
        // Chip time
        report += "      <th data-priority=\"1\">Finish</th>" +  System.lineSeparator(); // 9R Need to adjust for the format code
       
        // gun time
        if (showGun) report += "      <th data-priority=\"90\">Gun</th>" +  System.lineSeparator(); // 9R ibid
        // pace
        if (showPace) report += "      <th data-priority=\"90\">Pace</th>" +  System.lineSeparator(); // 10R
        
        report += "</tr></thead>" +  System.lineSeparator(); 
        report += "<tbody>"+ System.lineSeparator();
        
        
        
        final StringBuilder chars = new StringBuilder();
        
        prList.forEach(pr -> {
            
            // if they are a DNF or DQ swap out the placement stats
            Boolean hideTime = false; 
            Boolean hideSplitTimes = false;
            
            Boolean dnf = pr.getParticipant().getDNF();
            Boolean dq = pr.getParticipant().getDQ();

            if (pr.getChipFinish() == null && showDNF && !inProgress) dnf = true;
            
            if (!showDNF && dnf) return;
            if (!showDQ && dq) return;
            
            if (dnf || dq) hideTime = true;
            if (dq) hideSplitTimes = true; 
            
            if (!inProgress && pr.getChipFinish() == null && !showDNF) return;
            
            chars.append("<td></td>" +  System.lineSeparator()); // dummy for control
            
            if (dq) chars.append("<td >*****DQ****</td>"
                        + "<td>--</td>\n" +
                        " <td>--</td>" +  System.lineSeparator());
            else if (dnf) chars.append("<td>***DNF***</td>"
                        + "<td>--</td>\n" +
                        " <td>--</td>" +  System.lineSeparator());
            else if (pr.getChipFinish() != null){ 
                chars.append("<td>"+ pr.getOverall().toString() + " </td>" +  System.lineSeparator());
                chars.append("<td>"+ pr.getSexPlace().toString() + " </td>" +  System.lineSeparator());
                chars.append("<td>"+ pr.getAGPlace().toString() + " </td>" +  System.lineSeparator());
            } else {
                chars.append("<td> **Started** </td>\n"
                        + "<td>--</td>\n" +
                        " <td>--</td>" +  System.lineSeparator());
                hideTime = true;
            }
            
            
            
            
            
//            if (inProgress && pr.getChipFinish() == null) {
//                chars.append("<td> **Started** </td>\n"
//                        + "<td>--</td>\n" +
//                        " <td>--</td>" +  System.lineSeparator());
//                hideTime = true;
//            } else if (pr.getChipFinish() == null) {
//                return;
//            } else if (! dnf && ! dq) { 
//                chars.append("<td>"+ pr.getOverall().toString() + " </td>" +  System.lineSeparator());
//                chars.append("<td>"+ pr.getSexPlace().toString() + " </td>" +  System.lineSeparator());
//                chars.append("<td>"+ pr.getAGPlace().toString() + " </td>" +  System.lineSeparator()); 
//            } else {
//                if (dnf) chars.append("<td  colspan=\"3\">***DNF***</td>"
//                        + "<td style=\"display: none;\">--</td>\n" +
//                        " <td style=\"display: none;\">--</td>" +  System.lineSeparator());
//                else chars.append("<td  colspan=\"3\">*****DQ****</td>"
//                        + "<td style=\"display: none;\">--</td>\n" +
//                        " <td style=\"display: none;\">--</td>" +  System.lineSeparator());
//                    
//            }
            
            chars.append("<td>"+ pr.getParticipant().getBib() + "</td>" +  System.lineSeparator());
            chars.append("<td>"+ pr.getAge().toString() + "</td>" +  System.lineSeparator());
            chars.append("<td>"+ pr.getSex() + "</td>" +  System.lineSeparator());
            chars.append("<td>"+ pr.getAGCode() + "</td>" +  System.lineSeparator());
            chars.append("<td>"+ pr.getParticipant().fullNameProperty().getValueSafe() + "</td>" +  System.lineSeparator());
            chars.append("<td>"+ pr.getParticipant().getCity() + "</td>" +  System.lineSeparator());
            chars.append("<td>"+ pr.getParticipant().getState() + "</td>" +  System.lineSeparator());

            // Insert split stuff here 
            if (showSplits) {
            // do stuff
                for (int i = 2; i < race.splitsProperty().size(); i++) {
                    if (!hideSplitTimes) 
                        chars.append("<td>"+ DurationFormatter.durationToString(pr.getSplit(i), 0, Boolean.FALSE, RoundingMode.DOWN)+ "</td>" +  System.lineSeparator());
                    else chars.append("<td>---</td>" +  System.lineSeparator());
                }
            }
            // chip time
            if (! hideTime) chars.append("<td>"+DurationFormatter.durationToString(pr.getChipFinish(), 0, Boolean.FALSE, RoundingMode.DOWN)+ "</td>" +  System.lineSeparator());
            else chars.append("<td>---</td>" +  System.lineSeparator());
            
            if (showGun && ! hideTime) chars.append("<td>"+DurationFormatter.durationToString(pr.getGunFinish(), 0, Boolean.FALSE, RoundingMode.DOWN)+ "</td>" +  System.lineSeparator());
            else if (showGun && hideTime) chars.append("<td>---</td>" +  System.lineSeparator());
            
            if (showPace && ! hideTime) chars.append("<td>"+StringUtils.stripStart(Pace.getPace(race.getRaceDistance().floatValue(), race.getRaceDistanceUnits(), pr.getChipFinish(), Pace.MPM), "0")+ "</td>" +  System.lineSeparator());
            else if (showPace && hideTime) chars.append("<td>---</td>" +  System.lineSeparator());

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
