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
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author John Garner <segfaultcoredump@gmail.com>
 */
public class OverallHTML implements RaceReportType{
    Race race;

    Boolean showDQ = true;
    Boolean inProgress = false;
    Boolean showSplits = false;
    Boolean showSegments = false;
    Boolean showSegmentPace = false;
    Boolean showDNF = true;
    Boolean showPace = true;
    Boolean showGun = true;

    Map<String,Boolean> supportedOptions = new HashMap();
    
    public OverallHTML(){
        supportedOptions.put("showDQ", true);
        supportedOptions.put("inProgress", false);
        supportedOptions.put("showSplits", false);
        supportedOptions.put("showSegments", true);
        supportedOptions.put("showSegmentPace", false);
        supportedOptions.put("showDNF", false);
        supportedOptions.put("showPace", true);
        supportedOptions.put("showGun", true);
        supportedOptions.put("hideCustomHeaders", false);
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
        System.out.println("OverallHTML.process() Called... ");
        String report = new String();
        
        race = rr.getRace();
        
        Event event = Event.getInstance();  // fun with singletons... 
        
        supportedOptions.keySet().forEach(k -> supportedOptions.put(k, rr.getBooleanAttribute(k)));


        showDQ = supportedOptions.get("showDQ");
        inProgress = supportedOptions.get("inProgress");
        showSplits = supportedOptions.get("showSplits");
        showSegments = supportedOptions.get("showSegments");
        showSegmentPace = supportedOptions.get("showSegmentPace");
        showDNF = supportedOptions.get("showDNF");
        showPace = supportedOptions.get("showPace");
        showGun = supportedOptions.get("showGun");
        
        Boolean customHeaders = race.getBooleanAttribute("useCustomHeaders");
        Boolean textOnlyHeaders = race.getBooleanAttribute("textOnlyHeaders");
        if (customHeaders == null || (customHeaders == true && supportedOptions.get("hideCustomHeaders"))) customHeaders = false;
        if (customHeaders && textOnlyHeaders == null) textOnlyHeaders = false;
        
        String dispFormat = race.getStringAttribute("TimeDisplayFormat");
        String roundMode = race.getStringAttribute("TimeRoundingMode");
        Pace pace = Pace.valueOf(race.getStringAttribute("PaceDisplayFormat"));
        
        Integer dispFormatLength;  // add a space
        if (dispFormat.contains("[HH:]")) dispFormatLength = dispFormat.length()-1; // get rid of the two brackets and add a space
        else dispFormatLength = dispFormat.length()+1;
        
        Duration cutoffTime = Duration.ofNanos(race.getRaceCutoff());
        String cutoffTimeString = DurationFormatter.durationToString(cutoffTime, dispFormat, roundMode);
        
       
        report += "<HTML> " +  System.lineSeparator();
        report += "  <HEAD> " +  System.lineSeparator();
        
        report += "<meta name=\"viewport\" content=\"width=device-width; initial-scale=1.0; maximum-scale=1.0; minimum-scale=1.0; user-scalable=0;\" />\n";
        
// Google Analytics
        if (customHeaders && race.getStringAttribute("GACode") != null && !race.getStringAttribute("GACode").isEmpty()) {
            report +=   "<!-- Google Analytics -->\n" +
                        "<script>\n" +
                        "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){\n" +
                        "(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),\n" +
                        "m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)\n" +
                        "})(window,document,'script','https://www.google-analytics.com/analytics.js','ga');\n" +
                        "\n" +
                        "ga('create', '" + race.getStringAttribute("GACode") + "', 'auto');\n" +
                        "ga('send', 'pageview');\n" +
                        "</script>\n" +
                        "<!-- End Google Analytics -->";
            report += System.lineSeparator();
        }
        
        report +=   "<!-- Stylesheets / JS Includes-->\n" +
                    "<link rel=\"stylesheet\" type=\"text/css\" href=\"https://cdn.datatables.net/v/dt/jq-2.2.4/dt-1.10.13/fh-3.1.2/r-2.1.0/sc-1.4.2/datatables.min.css\"/>\n" +
                    " \n" +
                    "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/v/dt/jq-2.2.4/dt-1.10.13/fh-3.1.2/r-2.1.0/sc-1.4.2/datatables.min.js\"></script>\n" +
                    " \n" +
                    "<script type=\"text/javascript\" src=\"https://cdn.datatables.net/plug-ins/1.10.12/sorting/natural.js\"></script>\n";
        
        // Custom CSS 
        if (customHeaders && race.getStringAttribute("CSSUrl") != null && !race.getStringAttribute("CSSUrl").isEmpty()) {
            report += "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + race.getStringAttribute("CSSUrl") + "\"/>\n";
            report += System.lineSeparator();
        }
        report += "<!-- End Stylesheets / JS Indludes-->";

        // DataTables
        report += "<script type=\"text/javascript\" class=\"init\">\n" +
                    "	\n" +
                    "\n" +
                    "$(document).ready(function() {\n" +
                    "var search = \"\";\n" +
                    "	if ( window.location.hash !== \"\" ) {\n" +
                    "		search = window.location.hash.substring( 1 );\n" +
                    "	}" +
                    "var oTable = $('#results').DataTable({\n" +
                        "   \"oSearch\": { \"sSearch\": search }," + 
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
                        "    deferRender: true,\n" +
                        "   \"fnInitComplete\": function () {\n" +
                        "	this.fnAdjustColumnSizing();\n" +
                        "	$('div.dataTables_filter input').focus();\n" +
                        "   }\n" +
                        "});\n" +
                    "   setTimeout( function () {\n" +
                    "		if (search !== \"\") oTable.rows( {search:'applied'} ).every(function(index){\n" +
                    "			var row = oTable.row(index);\n" +
                    "			row.node().click();\n" +
                    "		});\n" +
                    "		\n" +
                    "	}, 300 );" +
                    "} );\n" +
                    "\n" +
                    "\n" +
                    "	</script>";
        report += "<!-- End DataTables -->";
        
        report += "  </HEAD> " +  System.lineSeparator();
        report += "  <BODY> " +  System.lineSeparator();
        
        if (customHeaders){
            if (textOnlyHeaders) report += race.getStringAttribute("textHeader");
            else report += race.getStringAttribute("htmlHeader");
            report += System.lineSeparator();
        }
        
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
        
        if (customHeaders){
            if (textOnlyHeaders) report += race.getStringAttribute("textMessage");
            else report += race.getStringAttribute("htmlMessage");
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
            for (int i = 2; i < race.splitsProperty().size(); i++) {
                report += "      <th data-priority=\"100\">" + race.splitsProperty().get(i-1).getSplitName() + "</th>" +  System.lineSeparator();
            }
        }
        if (showSegments) {
            final StringBuilder chars = new StringBuilder();
            Integer dispLeg = dispFormatLength;
            race.getSegments().forEach(seg -> {
                chars.append("      <th data-priority=\"80\">" + seg.getSegmentName()+ "</th>" +  System.lineSeparator());
                if (showSegmentPace) chars.append("      <th data-priority=\"95\"> Pace</th>" +  System.lineSeparator()); // pace.getFieldWidth()+1
            });
            report += chars.toString();
        }
        // Chip time
        report += "      <th data-priority=\"1\">Finish</th>" +  System.lineSeparator(); // 9R Need to adjust for the format code
       
        // gun time
        if (showGun) report += "      <th data-priority=\"90\">Gun</th>" +  System.lineSeparator(); // 9R ibid
        // pace
        if (showPace) report += "      <th data-priority=\"85\">Pace</th>" +  System.lineSeparator(); // 10R
        
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
            
            Boolean oco = false;
            if (dnf || dq) oco = false;
            else if (cutoffTime.isZero() 
                        || cutoffTime.compareTo(pr.getChipFinish()) >= 0 
                        || cutoffTimeString.equals(DurationFormatter.durationToString(pr.getChipFinish(), dispFormat, roundMode))) {
                oco = false;
            } else {
                oco=true;
                if (!showDNF && !inProgress) return;

            }
            
            chars.append("<td></td>" +  System.lineSeparator()); // dummy for control
            
            if (dq) chars.append("<td >*****DQ****</td>"
                        + "<td>--</td>\n" +
                        " <td>--</td>" +  System.lineSeparator());
            else if (dnf) chars.append("<td>***DNF***</td>"
                        + "<td>--</td>\n" +
                        " <td>--</td>" +  System.lineSeparator());
            else if (oco) chars.append("<td>***OCO***</td>"
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
                        chars.append("<td>"+ DurationFormatter.durationToString(pr.getSplit(i), dispFormat, roundMode)+ "</td>" +  System.lineSeparator());
                    else chars.append("<td>---</td>" +  System.lineSeparator());
                }
            }
            
            if (showSegments) {
                Boolean hst = hideSplitTimes;
                race.getSegments().forEach(seg -> {
                    if (!hst) 
                        chars.append("<td>"+ DurationFormatter.durationToString(pr.getSegmentTime(seg.getID()), dispFormat, roundMode)+ "</td>" +  System.lineSeparator());
                    else chars.append("<td>---</td>" +  System.lineSeparator());
                    if (showSegmentPace) {
                        if (!hst)
                            if (pr.getSegmentTime(seg.getID()) != null ) chars.append("<td>"+ pace.getPace(seg.getSegmentDistance(), race.getRaceDistanceUnits(), pr.getSegmentTime(seg.getID()))+ "</td>" +  System.lineSeparator());
                            else chars.append("<td>---</td>" +  System.lineSeparator());
                        else chars.append("<td>---</td>" +  System.lineSeparator());
                    }
                });
            }
            
            // chip time
            if (dq) chars.append("<td>"+ pr.getParticipant().getNote() + "</td>" +  System.lineSeparator());
            else if (! hideTime) chars.append("<td>"+DurationFormatter.durationToString(pr.getChipFinish(), dispFormat, roundMode)+ "</td>" +  System.lineSeparator());
            else chars.append("<td>---</td>" +  System.lineSeparator());
            
            if (showGun && ! hideTime) chars.append("<td>"+DurationFormatter.durationToString(pr.getGunFinish(), dispFormat, roundMode)+ "</td>" +  System.lineSeparator());
            else if (showGun && hideTime) chars.append("<td>---</td>" +  System.lineSeparator());
            
            if (showPace && ! hideTime) chars.append("<td>"+pace.getPace(race.getRaceDistance().floatValue(), race.getRaceDistanceUnits(), pr.getChipFinish())+ "</td>" +  System.lineSeparator());
            else if (showPace && hideTime) chars.append("<td>---</td>" +  System.lineSeparator());


            
            chars.append("</tr>" + System.lineSeparator());
        
        
        });
            
        report += chars.toString();
        report += "</tbody>" +  System.lineSeparator();
        report += "</table>" +  System.lineSeparator();
        if (customHeaders){
            if (textOnlyHeaders) report += race.getStringAttribute("textFooter");
            else report += race.getStringAttribute("htmlFooter");
            report += System.lineSeparator();
        }
        report += "  </BODY> " +  System.lineSeparator();
        report += "</HTML> " +  System.lineSeparator();
        
        return report;
    }
    
    
}
